// ==========================================
// SafeShade GNSS + Cellular Gateway
// ESP32-S3 + Quectel EC200U-CN
//
// Fully automated AT-command bring-up, GNSS polling, and SMS sending -
// no manual typing into Serial Monitor required. Exposes a tiny WiFi
// SoftAP + HTTP API that the SafeShade mainboard (docs/SafeShadev21/
// SafeShadev21.ino, ESP32-C3) polls/pings over WiFi.
//
// UART pins/baud/PWRKEY below were found experimentally for this exact
// board (see docs/EC200U_Project_Notes_and_Troubleshooting.md) - do not
// change them casually.
// ==========================================

#include <WiFi.h>
#include <WebServer.h>

#define EC200U_RX 18
#define EC200U_TX 17
#define PW_KEY    10

HardwareSerial EC200U(2);
WebServer server(80);

// Must stay byte-identical to GATEWAY_WIFI_SSID/PASSWORD/BASE_URL in
// SafeShadev21.ino.
#define AP_SSID     "SafeShade_GW"
#define AP_PASSWORD "safeshade2026"
#define AP_CHANNEL  6

// ==========================================
// Demo alert configuration
// ==========================================
#define EMERGENCY_NUMBER "+918917360065"
#define DEMO_ADDRESS     "27/A/1, Campus 12, KIIT University, Patia-751024"
const double DEMO_LAT = 20.35520740274678;
const double DEMO_LON = 85.81951928060086;
const double DEMO_ALT = 45.0;

// ==========================================
// Status state
// ==========================================
bool modemReady = false;
bool networkReady = false;
int  lastCsq = -1;

struct GpsFix {
    bool fix = false;
    double lat = 0;
    double lon = 0;
    double altitude = 0;
    int satellites = 0;
    unsigned long updatedAtMillis = 0;
};
GpsFix liveFix;

unsigned long lastGnssPollMillis = 0;
const unsigned long GNSS_POLL_INTERVAL_MS = 4000;
unsigned long lastCsqPollMillis = 0;
const unsigned long CSQ_POLL_INTERVAL_MS = 10000;

// A live fix counts as usable only while fresh and reporting satellites -
// otherwise /gps and /alert both fall back to the fixed demo location so
// the mainboard always has something real to show/send, live or demo.
const unsigned long LIVE_FIX_MAX_AGE_MS = 30000;

enum SmsState {
    SMS_IDLE,
    SMS_START,
    SMS_WAIT_CMGF,
    SMS_WAIT_CSCS,
    SMS_WAIT_PROMPT,
    SMS_WAIT_RESULT,
    SMS_DONE,
    SMS_FAILED
};
SmsState smsState = SMS_IDLE;
unsigned long smsStateEnteredAt = 0;
String smsPendingBody = "";
String smsTargetNumber = EMERGENCY_NUMBER;  // who the in-flight send is going to
String smsRxBuffer = "";
// Set when queueSms() is called while a send is already in flight (e.g.
// the mainboard's Elderly-mode stillness escalation fires a second alert
// ~3s after the first, well within typical send time) - without this the
// second, more urgent message was silently swallowed. Consumed by
// pumpSms() the moment the in-flight send finishes. Generalized to carry
// any target+body (not just the alert message) so a queued Guardian reply
// can't be dropped either.
bool resendRequested = false;
String resendTarget = "";
String resendBody = "";

// ==========================================
// Two-way SMS messaging (real, device-independent of BLE/phone proximity)
// ==========================================
// Any SMS sent to this SIM's own number - from the app (when BLE is out of
// range) or from literally any other phone - is picked up here and relayed
// to the mainboard's Messages screen over /messages. Replies (physical
// quick-reply or an app-sent Companion reply relayed by the mainboard via
// /reply) go back to whoever sent the most recent incoming message,
// falling back to EMERGENCY_NUMBER if none has arrived yet this session.
struct IncomingMsg {
    String sender;
    String body;
};
const int INCOMING_QUEUE_SIZE = 5;
IncomingMsg incomingQueue[INCOMING_QUEUE_SIZE];
int incomingQueueCount = 0;
String lastIncomingSender = "";

unsigned long lastIncomingSmsPollMillis = 0;
const unsigned long INCOMING_SMS_POLL_INTERVAL_MS = 5000;

// ==========================================
// AT command engine
// ==========================================

void flushEC200U() {
    while (EC200U.available()) EC200U.read();
}

/** Blocking-with-timeout AT round trip. Only used during setup()/GNSS/CSQ
 * polling - never while an SMS send is in flight (see loop() guard) so it
 * can never interleave with the SMS state machine's own serial traffic. */
String sendAT(const String &cmd, unsigned long timeoutMs = 2000) {
    flushEC200U();
    EC200U.print(cmd);
    EC200U.print("\r\n");

    String resp;
    unsigned long start = millis();
    while (millis() - start < timeoutMs) {
        while (EC200U.available()) {
            resp += (char)EC200U.read();
        }
        if (resp.indexOf("OK") != -1 || resp.indexOf("ERROR") != -1) {
            delay(20);
            while (EC200U.available()) resp += (char)EC200U.read();
            break;
        }
    }
    return resp;
}

bool sendATExpect(const String &cmd, const char* expect, unsigned long timeoutMs = 2000) {
    return sendAT(cmd, timeoutMs).indexOf(expect) != -1;
}

/** n-th (0-indexed) comma-separated field of a CSV response fragment. */
String csvField(const String &s, int index) {
    int start = 0, cur = 0;
    while (cur < index) {
        int comma = s.indexOf(',', start);
        if (comma == -1) return "";
        start = comma + 1;
        cur++;
    }
    int end = s.indexOf(',', start);
    if (end == -1) end = s.length();
    return s.substring(start, end);
}

/** n-th (0-indexed) "..."-quoted field. Needed for AT+CMGL responses -
 * csvField() alone can't parse them because the timestamp field itself
 * contains a comma ("25/09/03,14:23:10+22"), which would otherwise be
 * mistaken for a field separator. */
String quotedField(const String &s, int index) {
    int pos = 0;
    for (int i = 0; i <= index; i++) {
        int start = s.indexOf('"', pos);
        if (start == -1) return "";
        int end = s.indexOf('"', start + 1);
        if (end == -1) return "";
        if (i == index) return s.substring(start + 1, end);
        pos = end + 1;
    }
    return "";
}

// ==========================================
// Modem bring-up
// ==========================================

void powerOnModem() {
    pinMode(PW_KEY, OUTPUT);
    digitalWrite(PW_KEY, LOW);
    delay(1500);
    digitalWrite(PW_KEY, HIGH);
}

/** Polls plain AT until OK instead of a fixed startup delay - the EC200U
 * can take anywhere from a few seconds to 20-30s to respond after a cold
 * power-up, especially right after a SIM swap or VBAT reconnection. */
bool waitForModem(unsigned long maxWaitMs) {
    unsigned long start = millis();
    while (millis() - start < maxWaitMs) {
        if (sendATExpect("AT", "OK", 1000)) return true;
        delay(500);
    }
    return false;
}

/** Polls AT+CREG? for home (1) or roaming (5) registration. Continues
 * regardless of the outcome (fail-open) - SMS just won't succeed until
 * this becomes true, but GNSS/HTTP still start either way. */
bool waitForNetwork(unsigned long maxWaitMs) {
    unsigned long start = millis();
    while (millis() - start < maxWaitMs) {
        String r = sendAT("AT+CREG?", 1500);
        int idx = r.indexOf("+CREG:");
        if (idx != -1) {
            int comma = r.indexOf(',', idx);
            if (comma != -1 && comma + 1 < (int)r.length()) {
                char stat = r.charAt(comma + 1);
                if (stat == '1' || stat == '5') return true;
            }
        }
        delay(2000);
    }
    return false;
}

void startGNSS() {
    sendAT("AT+QGPSCFG=\"gnssnmeatype\",1", 1500);
    sendAT("AT+QGPSCFG=\"nmeasrc\",1", 1500);
    String r = sendAT("AT+QGPS=1,30,50,0,1", 2000);
    // Per docs/EC200U_Project_Notes_and_Troubleshooting.md #13: "+CME
    // ERROR: 504" here just means GNSS was already running - not a fault.
    if (r.indexOf("OK") == -1 && r.indexOf("504") == -1) {
        Serial.println("[GNSS] unexpected AT+QGPS response, continuing anyway:");
        Serial.println(r);
    }
}

// ==========================================
// GNSS + signal polling (runtime, timer-based)
// ==========================================

/** AT+QGPSLOC=2 -> "+QGPSLOC: <utc>,<lat>,<lon>,<hdop>,<alt>,<fix>,<cog>,
 * <spkm>,<spkn>,<date>,<nsat>" (decimal degrees). Field indices below
 * assume this shape - verify against the bench response and adjust if a
 * given firmware build differs. */
void pollLiveGnss() {
    String r = sendAT("AT+QGPSLOC=2", 1500);
    int idx = r.indexOf("+QGPSLOC:");
    if (idx == -1) {
        liveFix.fix = false;
        return;
    }
    String data = r.substring(idx + 9);
    data.trim();

    double lat = csvField(data, 1).toDouble();
    double lon = csvField(data, 2).toDouble();
    double alt = csvField(data, 4).toDouble();
    int nsat = csvField(data, 10).toInt();

    if (lat != 0.0 && lon != 0.0 && nsat > 0) {
        liveFix.fix = true;
        liveFix.lat = lat;
        liveFix.lon = lon;
        liveFix.altitude = alt;
        liveFix.satellites = nsat;
        liveFix.updatedAtMillis = millis();
    } else {
        liveFix.fix = false;
    }
}

void pollCsq() {
    String r = sendAT("AT+CSQ", 1000);
    int idx = r.indexOf("+CSQ:");
    if (idx == -1) return;
    String data = r.substring(idx + 5);
    data.trim();
    lastCsq = csvField(data, 0).toInt();
}

/** Re-checks registration at runtime (not just once at boot) so /status
 * reflects reality if signal is lost/regained during the demo. */
void refreshNetworkReady() {
    String r = sendAT("AT+CREG?", 1500);
    int idx = r.indexOf("+CREG:");
    if (idx == -1) return;
    int comma = r.indexOf(',', idx);
    if (comma != -1 && comma + 1 < (int)r.length()) {
        char stat = r.charAt(comma + 1);
        networkReady = (stat == '1' || stat == '5');
    }
}

bool liveFixUsable() {
    return liveFix.fix && liveFix.satellites > 0 &&
           (millis() - liveFix.updatedAtMillis) < LIVE_FIX_MAX_AGE_MS;
}

void enqueueIncoming(const String &sender, const String &body) {
    if (body.length() == 0) return;
    if (incomingQueueCount >= INCOMING_QUEUE_SIZE) {
        // drop the oldest to make room - fail-open, never blocks new messages
        for (int i = 1; i < INCOMING_QUEUE_SIZE; i++) incomingQueue[i - 1] = incomingQueue[i];
        incomingQueueCount = INCOMING_QUEUE_SIZE - 1;
    }
    incomingQueue[incomingQueueCount].sender = sender;
    incomingQueue[incomingQueueCount].body = body;
    incomingQueueCount++;
    lastIncomingSender = sender;
    Serial.print("[SMS] Incoming from ");
    Serial.print(sender);
    Serial.print(": ");
    Serial.println(body);
}

/**
 * Polls for unread received SMS via AT+CMGL (not the +CMT: URC approach) -
 * a deliberate choice: this shares the exact same "poll on a timer, guard
 * against an in-flight send" pattern already used for GNSS/CSQ, so
 * incoming-message handling can never interleave with (and corrupt) the
 * outbound SMS state machine's own AT traffic on the same UART. Each
 * matched message is queued then explicitly deleted (AT+CMGD) so it's
 * never re-read on the next poll.
 */
void pollIncomingSms() {
    String r = sendAT("AT+CMGL=\"REC UNREAD\"", 3000);

    int pos = 0;
    while (true) {
        int idx = r.indexOf("+CMGL:", pos);
        if (idx == -1) break;

        int headerEnd = r.indexOf('\n', idx);
        if (headerEnd == -1) break;
        String header = r.substring(idx, headerEnd);

        int msgIndex = header.substring(7, header.indexOf(',')).toInt();
        String sender = quotedField(header, 1);  // 0=status, 1=sender, 2=timestamp

        int bodyEnd = r.indexOf('\n', headerEnd + 1);
        if (bodyEnd == -1) bodyEnd = r.length();
        String body = r.substring(headerEnd + 1, bodyEnd);
        body.trim();

        if (sender.length() > 0 && body.length() > 0) {
            enqueueIncoming(sender, body);
        }
        sendAT("AT+CMGD=" + String(msgIndex), 1000);  // free the SIM storage slot

        pos = bodyEnd;
    }
}

// ==========================================
// SMS state machine - advanced one step per loop() iteration so a send
// (which takes several seconds end-to-end) never blocks the HTTP server
// or the rest of the gateway. /alert just sets smsState = SMS_START and
// returns immediately.
// ==========================================

void drainInto(String &buf) {
    while (EC200U.available()) buf += (char)EC200U.read();
}

const char* smsStateName() {
    switch (smsState) {
        case SMS_IDLE:        return "idle";
        case SMS_START:       return "sending";
        case SMS_WAIT_CMGF:   return "sending";
        case SMS_WAIT_CSCS:   return "sending";
        case SMS_WAIT_PROMPT: return "sending";
        case SMS_WAIT_RESULT: return "sending";
        case SMS_DONE:        return "sent";
        case SMS_FAILED:      return "failed";
    }
    return "idle";
}

String buildAlertMessageText() {
    if (liveFixUsable()) {
        return "SafeShade ALERT: Fall/SOS detected. Lat " + String(liveFix.lat, 6) +
            " Lon " + String(liveFix.lon, 6) +
            " https://maps.google.com/?q=" + String(liveFix.lat, 6) + "," + String(liveFix.lon, 6);
    }
    return "SafeShade ALERT: Fall/SOS detected. Loc: " + String(DEMO_ADDRESS) +
        ". Lat " + String(DEMO_LAT, 6) + " Lon " + String(DEMO_LON, 6) +
        " https://maps.google.com/?q=" + String(DEMO_LAT, 6) + "," + String(DEMO_LON, 6);
}

/** Kicks off an SMS send to [number], or - if one is already in flight -
 * marks a resend so this one still goes out right after the current send
 * finishes, instead of being silently dropped. Generic across alert
 * messages and two-way Guardian replies - both go through this. */
void queueSms(const String &number, const String &body) {
    if (smsState != SMS_IDLE && smsState != SMS_DONE && smsState != SMS_FAILED) {
        resendRequested = true;
        resendTarget = number;
        resendBody = body;
        return;
    }
    smsTargetNumber = number;
    smsPendingBody = body;
    smsState = SMS_START;
}

void queueAlertSms() {
    queueSms(EMERGENCY_NUMBER, buildAlertMessageText());
}

void pumpSms() {
    switch (smsState) {
        case SMS_IDLE:
            return;
        case SMS_DONE:
        case SMS_FAILED:
            if (resendRequested) {
                resendRequested = false;
                smsTargetNumber = resendTarget;
                smsPendingBody = resendBody;
                smsState = SMS_START;
            }
            return;

        case SMS_START:
            flushEC200U();
            smsRxBuffer = "";
            EC200U.print("AT+CMGF=1\r\n");
            smsState = SMS_WAIT_CMGF;
            smsStateEnteredAt = millis();
            break;

        case SMS_WAIT_CMGF:
            drainInto(smsRxBuffer);
            if (smsRxBuffer.indexOf("OK") != -1) {
                smsRxBuffer = "";
                EC200U.print("AT+CSCS=\"GSM\"\r\n");
                smsState = SMS_WAIT_CSCS;
                smsStateEnteredAt = millis();
            } else if (millis() - smsStateEnteredAt > 2000) {
                smsState = SMS_FAILED;
            }
            break;

        case SMS_WAIT_CSCS:
            drainInto(smsRxBuffer);
            if (smsRxBuffer.indexOf("OK") != -1) {
                smsRxBuffer = "";
                EC200U.print("AT+CMGS=\"" + smsTargetNumber + "\"\r\n");
                smsState = SMS_WAIT_PROMPT;
                smsStateEnteredAt = millis();
            } else if (millis() - smsStateEnteredAt > 2000) {
                smsState = SMS_FAILED;
            }
            break;

        case SMS_WAIT_PROMPT:
            drainInto(smsRxBuffer);
            if (smsRxBuffer.indexOf('>') != -1) {
                EC200U.print(smsPendingBody);
                EC200U.write(26); // Ctrl+Z submits the message body
                smsRxBuffer = "";
                smsState = SMS_WAIT_RESULT;
                smsStateEnteredAt = millis();
            } else if (millis() - smsStateEnteredAt > 3000) {
                smsState = SMS_FAILED;
            }
            break;

        case SMS_WAIT_RESULT:
            drainInto(smsRxBuffer);
            if (smsRxBuffer.indexOf("+CMGS:") != -1 && smsRxBuffer.indexOf("OK") != -1) {
                Serial.println("[SMS] Sent successfully.");
                smsState = SMS_DONE;
            } else if (smsRxBuffer.indexOf("ERROR") != -1) {
                Serial.println("[SMS] Failed:");
                Serial.println(smsRxBuffer);
                smsState = SMS_FAILED;
            } else if (millis() - smsStateEnteredAt > 15000) {
                Serial.println("[SMS] Timed out waiting for send result.");
                smsState = SMS_FAILED;
            }
            break;
    }
}

// ==========================================
// HTTP API
// ==========================================

void handleGps() {
    bool useLive = liveFixUsable();
    double lat = useLive ? liveFix.lat : DEMO_LAT;
    double lon = useLive ? liveFix.lon : DEMO_LON;
    double alt = useLive ? liveFix.altitude : DEMO_ALT;
    int sats = useLive ? liveFix.satellites : 0;

    String json = "{\"fix\":true,\"lat\":" + String(lat, 8) +
                  ",\"lon\":" + String(lon, 8) +
                  ",\"altitude\":" + String(alt, 1) +
                  ",\"satellites\":" + String(sats) + "}";
    server.send(200, "application/json", json);
}

void handleAlert() {
    server.send(200, "application/json", "{\"queued\":true}");
    queueAlertSms();
}

/** Oldest pending incoming SMS (FIFO), if any - the mainboard polls this
 * on a timer and drains the queue one message at a time. Works for a
 * message sent from the app (when BLE is out of range) exactly the same
 * as one texted in from any other phone - both just land in this queue. */
void handleMessages() {
    if (incomingQueueCount == 0) {
        server.send(200, "application/json", "{\"pending\":false}");
        return;
    }
    String sender = incomingQueue[0].sender;
    String body = incomingQueue[0].body;
    for (int i = 1; i < incomingQueueCount; i++) incomingQueue[i - 1] = incomingQueue[i];
    incomingQueueCount--;

    // Minimal JSON-string escaping - SMS bodies are free text and could
    // contain quotes/backslashes/newlines.
    String safeBody = body;
    safeBody.replace("\\", "\\\\");
    safeBody.replace("\"", "\\\"");
    safeBody.replace("\r", "");
    safeBody.replace("\n", "\\n");

    String json = "{\"pending\":true,\"from\":\"" + sender + "\",\"text\":\"" + safeBody + "\"}";
    server.send(200, "application/json", json);
}

/** Guardian reply relay (from the mainboard - either the physical
 * quick-reply button or an app-sent Companion reply arriving over BLE) -
 * sent via SMS to whoever texted in most recently, so the loop is
 * genuinely two-way and independent of BLE range on either end. */
void handleReply() {
    String body = server.hasArg("plain") ? server.arg("plain") : "";
    server.send(200, "application/json", "{\"queued\":true}");
    if (body.length() == 0) return;
    String target = lastIncomingSender.length() > 0 ? lastIncomingSender : String(EMERGENCY_NUMBER);
    queueSms(target, body);
}

void handleStatus() {
    String json = "{";
    json += "\"modemReady\":" + String(modemReady ? "true" : "false") + ",";
    json += "\"networkReady\":" + String(networkReady ? "true" : "false") + ",";
    json += "\"csq\":" + String(lastCsq) + ",";
    json += "\"gnssFix\":" + String(liveFix.fix ? "true" : "false") + ",";
    json += "\"satellites\":" + String(liveFix.satellites) + ",";
    json += "\"smsState\":\"" + String(smsStateName()) + "\"";
    json += "}";
    server.send(200, "application/json", json);
}

// ==========================================
// Setup / Loop
// ==========================================

void setup() {
    Serial.begin(115200);
    EC200U.begin(115200, SERIAL_8N1, EC200U_RX, EC200U_TX);
    delay(500);

    Serial.println();
    Serial.println("====================================");
    Serial.println("   SafeShade Gateway (EC200U-CN)");
    Serial.println("====================================");

    powerOnModem();

    Serial.println("[SETUP] Waiting for EC200U to respond to AT...");
    modemReady = waitForModem(20000);

    if (modemReady) {
        Serial.println("[SETUP] EC200U responding.");
        sendAT("ATE0", 1000);       // echo off - cleaner parsing everywhere else
        sendAT("AT+CMGF=1", 1000);  // text-mode SMS
        sendAT("AT+CSCS=\"GSM\"", 1000);

        Serial.println("[SETUP] Waiting for cellular network registration...");
        networkReady = waitForNetwork(30000);
        Serial.println(networkReady
            ? "[SETUP] Network registered."
            : "[SETUP] Not registered yet - continuing anyway, will keep working in the background.");

        Serial.println("[SETUP] Starting GNSS...");
        startGNSS();
    } else {
        Serial.println("[SETUP] WARNING: EC200U never responded to AT. Check power (VBAT) and UART pins.");
        Serial.println("[SETUP] Continuing anyway - WiFi/HTTP API will still start (GNSS/SMS will just fail-open until the modem is available).");
    }

    WiFi.mode(WIFI_AP);
    WiFi.softAP(AP_SSID, AP_PASSWORD, AP_CHANNEL);
    Serial.print("[SETUP] SoftAP '" AP_SSID "' started, IP: ");
    Serial.println(WiFi.softAPIP());

    server.on("/gps", HTTP_GET, handleGps);
    server.on("/alert", HTTP_POST, handleAlert);
    server.on("/status", HTTP_GET, handleStatus);
    server.on("/messages", HTTP_GET, handleMessages);
    server.on("/reply", HTTP_POST, handleReply);
    server.begin();
    Serial.println("[SETUP] HTTP server started. Ready.");
    Serial.println();
}

void loop() {
    server.handleClient();

    unsigned long now = millis();
    bool smsInFlight = (smsState != SMS_IDLE && smsState != SMS_DONE && smsState != SMS_FAILED);

    // GNSS/CSQ polling shares the EC200U UART with the SMS state machine -
    // never run both at once, or their AT traffic would interleave.
    if (modemReady && !smsInFlight) {
        if (now - lastGnssPollMillis > GNSS_POLL_INTERVAL_MS) {
            lastGnssPollMillis = now;
            pollLiveGnss();
        }
        if (now - lastCsqPollMillis > CSQ_POLL_INTERVAL_MS) {
            lastCsqPollMillis = now;
            pollCsq();
            refreshNetworkReady();
        }
        if (now - lastIncomingSmsPollMillis > INCOMING_SMS_POLL_INTERVAL_MS) {
            lastIncomingSmsPollMillis = now;
            pollIncomingSms();
        }
    }

    pumpSms();
}
