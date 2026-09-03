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
String smsRxBuffer = "";
// Set when queueAlertSms() is called while a send is already in flight
// (e.g. the mainboard's Elderly-mode stillness escalation fires a second
// alert ~3s after the first, well within typical send time) - without
// this the second, more urgent alert was silently swallowed. Consumed by
// pumpSms() the moment the in-flight send finishes.
bool resendRequested = false;

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

void buildAlertMessage() {
    if (liveFixUsable()) {
        smsPendingBody =
            "SafeShade ALERT: Fall/SOS detected. Lat " + String(liveFix.lat, 6) +
            " Lon " + String(liveFix.lon, 6) +
            " https://maps.google.com/?q=" + String(liveFix.lat, 6) + "," + String(liveFix.lon, 6);
    } else {
        smsPendingBody =
            "SafeShade ALERT: Fall/SOS detected. Loc: " + String(DEMO_ADDRESS) +
            ". Lat " + String(DEMO_LAT, 6) + " Lon " + String(DEMO_LON, 6) +
            " https://maps.google.com/?q=" + String(DEMO_LAT, 6) + "," + String(DEMO_LON, 6);
    }
}

/** Kicks off an SMS send, or - if one is already in flight - marks a
 * resend so the newer alert (built fresh, with whatever the freshest fix/
 * message is at that moment) still goes out right after the current send
 * finishes, instead of being silently dropped. */
void queueAlertSms() {
    if (smsState != SMS_IDLE && smsState != SMS_DONE && smsState != SMS_FAILED) {
        resendRequested = true;
        return;
    }
    buildAlertMessage();
    smsState = SMS_START;
}

void pumpSms() {
    switch (smsState) {
        case SMS_IDLE:
            return;
        case SMS_DONE:
        case SMS_FAILED:
            if (resendRequested) {
                resendRequested = false;
                buildAlertMessage();
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
                EC200U.print("AT+CMGS=\"" + String(EMERGENCY_NUMBER) + "\"\r\n");
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
    }

    pumpSms();
}
