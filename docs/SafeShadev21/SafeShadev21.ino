// ==========================================
// BOARD: Seeed XIAO ESP32C3 (the D1-D6/A0 pin macros below only resolve on
// this board's variant - a generic "ESP32C3 Dev Module" selection fails to
// compile). With BLE + WiFi + HTTPClient + U8g2 all linked in, the binary
// no longer fits the default partition scheme's 1.2MB app slot (~1.47MB
// used, was ~112%). In Arduino IDE: Tools > Board > XIAO_ESP32C3, then
// Tools > Partition Scheme > "No OTA (2MB APP/2MB SPIFFS)" (compiles clean
// at ~70%). Verified with arduino-cli:
//   arduino-cli compile --fqbn "esp32:esp32:XIAO_ESP32C3:PartitionScheme=no_ota" .
// ==========================================
#include <Arduino.h>
#include <Wire.h>
#include <U8g2lib.h>
#include <Adafruit_NeoPixel.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <esp_coexist.h>

// Declared this early (not next to drawShady() itself, where it's used)
// because Arduino's auto-generated function prototypes are inserted
// immediately after the #include block, before any type defined later in
// the file - drawShady()'s ShadyMood parameter must already be visible.
enum ShadyMood {
    SHADY_IDLE,
    SHADY_HAPPY,
    SHADY_SLEEPY,
    SHADY_WORRIED,
    SHADY_EXCITED,
    SHADY_PANICKED
};

// The 7 Adaptive Modes (mirrors the Android app's PersonaMode enum
// exactly - ELDERLY/KIDS/BIKE/PET/HELMET/WRIST/BACKPACK). Synced from the
// app via EXT_CHAR's "MODE:<name>" command, matched by NAME (see
// modeFromName()) rather than raw ordinal, so the two sides can never
// silently desync if either enum's order ever changes independently.
enum PersonaMode {
    MODE_ELDERLY,
    MODE_KIDS,
    MODE_BIKE,
    MODE_PET,
    MODE_HELMET,
    MODE_WRIST,
    MODE_BACKPACK
};

// Mode-specific algorithm/behavior parameters - see getModeProfile()'s doc
// comment (further down) for the full explanation. Declared here (not
// there) for the same auto-prototype-ordering reason as everything else
// on this page: getModeProfile()'s auto-generated prototype needs this
// type visible at the very top of the file, regardless of where the
// function body itself lives.
struct ModeProfile {
    long minImpactThresh;        // 0 = no floor
    bool fallDetectionEnabled;
    bool gyroCrashCheck;         // require rotational jerk alongside linear
    long gyroCrashRotThresh;     // raw |GyX|+|GyY|+|GyZ| sum, needs bench tuning
    bool stillnessCheck;         // Elderly: post-impact stillness raises confidence
    bool rgbMenuDisabled;
    unsigned long gatewayPollIntervalMs;
    unsigned long sosHoldMs;     // overrides the default 1500ms SOS-entry hold
};

// All on-device screens, including the mode-specific priority screens
// (appended at the end, not inserted, so the existing HOME..MESSAGE /
// RGB_MENU..FALL ordinal range-comparisons elsewhere in this file stay
// valid) - only reachable when the matching mode is active, see
// buildModeScreenCycle(). Declared this early for the same reason as
// ModeProfile above - buildModeScreenCycle()'s auto-generated prototype
// needs it.
enum ScreenState {
    SCREEN_HOME,
    SCREEN_WEATHER,
    SCREEN_HEALTH,
    SCREEN_GPS,
    SCREEN_MESSAGE,
    SCREEN_RGB_MENU,
    SCREEN_SLEEP,
    SCREEN_SOS,
    SCREEN_FALL,
    SCREEN_RIDE_STATS,     // Bike
    SCREEN_ACTIVITY,       // Pet
    SCREEN_VITALS,         // Wrist
    SCREEN_IMPACT_LOG,     // Helmet
    SCREEN_SAFE_ZONE,      // Kids
    SCREEN_MEDICATION,     // Elderly
    SCREEN_MODE_SELECT     // all modes - on-device mode picker (triple-click from Home)
};
ScreenState currentScreen = SCREEN_HOME;
ScreenState previousScreen = SCREEN_HOME;

// ==========================================
// GNSS/CELLULAR GATEWAY (optional, additive - see
// docs/SafeShade_Gateway/SafeShade_Gateway.ino, a SEPARATE sketch running
// on a second ESP32-S3+EC200U board). This board talks to that gateway
// only over WiFi, polling it for a real GPS fix and pinging it on a fall.
// If the gateway is off/unreachable, everything below fails silently and
// the existing phone-BLE-driven flow is completely unaffected - see the
// fail-open comments at each call site.
// ==========================================
#define GATEWAY_WIFI_SSID     "SafeShade_GW"
#define GATEWAY_WIFI_PASSWORD "safeshade2026"
#define GATEWAY_BASE_URL      "http://192.168.4.1"

// ==========================================
// 1. PIN DEFINITIONS & HARDWARE CONFIG
// ==========================================
#define PIN_LDR       A0
#define PIN_BTN       D1
#define PIN_LED       D2
#define PIN_BUZZER    D3
#define PIN_NEO       D6
#define SDA_PIN       D4
#define SCL_PIN       D5
#define MPU_ADDR      0x68
#define NUM_LEDS      8
#define DARK_THRESHOLD 1000

// ==========================================
// 2. BLE UUIDs
// ==========================================
#define SERVICE_UUID           "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define WEATHER_CHAR_UUID      "beb5483e-36e1-4688-b7f5-ea07361b26a8"
#define ALERT_CHAR_UUID        "8c5314e3-89ee-4752-9214-4d834311827e"
#define MESSAGE_CHAR_UUID      "1c95d5e3-d8f7-413a-bf3d-7a2e5d7be87e"
#define HEALTH_CHAR_UUID       "2a4d6e8f-1234-5678-abcd-ef0123456789"
#define SETTINGS_CHAR_UUID     "3b5e7f90-2345-6789-bcde-f01234567890"
#define REPLY_CHAR_UUID        "4c6f8a01-3456-789a-cdef-012345678901"
#define TELEMETRY_CHAR_UUID    "5d7f9a02-4567-89ab-def0-123456789012"
#define LED_CHAR_UUID          "6e8a0b13-5678-9abc-ef01-234567890123"
// Generic tagged-command channel (App->Device, WRITE) and unified
// acknowledgement stream (Device->App, NOTIFY) - see EXT_CHAR/ACK_CHAR
// comments near their callback/helper definitions below.
#define EXT_CHAR_UUID           "7f9b1c24-6789-abcd-f012-3456789abcde"
#define ACK_CHAR_UUID           "80ac2d35-789a-bcde-0123-456789abcdef"

// ==========================================
// 3. PHYSICS TUNING
// ==========================================
#define SLEEP_TIMEOUT_MS    150000
#define TEMP_OFFSET         18.0
#define TEMP_UPDATE_MS      15000

#define FREEFALL_THRESH     2000
#define IMPACT_THRESH       50000
#define BRAKE_THRESH        15000
#define MOTION_THRESH       3000

// ==========================================
// 4. DATA STRUCTURES
// ==========================================

struct HealthData {
    String bloodType = "AB+";
    String emergencyContact = "+91 89173 60065";
    String contactName = "M. Ganguly";
    String allergies = "Penicillin, Peanuts";
    int age = 21;
} healthData;

struct DeviceSettings {
    int fallSensitivity = 1;
    int sosVolume = 80;
    bool autoCallEnabled = true;
    // Added for the Android app's SafetySettings fields that previously
    // never reached the device at all - see SettingsCallbacks below.
    bool parentalControlsEnabled = false;
    bool smsFallbackEnabled = false;
} settings;

struct LocationData {
    float lat = 0.0;
    float lon = 0.0;
    String locationName = "Syncing...";
    String locality = "";
    int altitude = 0;
    float accuracy = 0;
    bool valid = false;
} location;

// Real onboard GNSS fix polled from the gateway board (see includes block
// above). Kept entirely separate from `location` (phone-sourced, via BLE)
// so a missing/unreachable gateway can never blank out the phone's own
// location data - drawGPSScreen() prefers this when it's fresh, and falls
// back to `location` otherwise.
struct GatewayGpsData {
    double lat = 0.0;
    double lon = 0.0;
    double altitude = 0.0;
    bool fix = false;
    int satellites = 0;
    unsigned long lastPolledAtMillis = 0; // 0 = never successfully polled
} gatewayGps;
const unsigned long GATEWAY_GPS_MAX_AGE_MS = 30000; // treat fixes older than this as stale
unsigned long lastGatewayPollMillis = 0;
const unsigned long GATEWAY_POLL_INTERVAL_MS = 4000;

// Cellular/modem status polled from the gateway board's /status endpoint -
// purely informational (OLED indicators), never gates the GPS/alert logic
// above. Same fail-open contract: left at its defaults if unreachable.
struct GatewayStatus {
    bool reachable = false;   // last /status poll actually succeeded
    bool networkReady = false;
    int csq = -1;
    String smsState = "idle";
    unsigned long lastPolledAtMillis = 0;
} gatewayStatus;
unsigned long lastGatewayStatusPollMillis = 0;
const unsigned long GATEWAY_STATUS_POLL_INTERVAL_MS = 5000;
const unsigned long GATEWAY_STATUS_MAX_AGE_MS = 15000;

unsigned long lastGatewayMessagesPollMillis = 0;
const unsigned long GATEWAY_MESSAGES_POLL_INTERVAL_MS = 3000;

wl_status_t lastWifiStatus = WL_IDLE_STATUS; // debug: see loop()'s WiFi transition log

// ==========================================
// 4c. PER-MODE PRIORITY FEATURE STATE
// ==========================================

// Bike: Ride Stats - real distance/duration computed purely from
// successive gateway GPS fixes already being polled (no new sensor).
double rideDistanceKm = 0.0;
double rideLastLat = 0.0, rideLastLon = 0.0;
unsigned long rideLastFixMillis = 0;
unsigned long rideStartedAtMillis = 0;
bool rideActive = false;

// Pet: Activity - integrates real motion-delta over time into an
// active-minutes tally; resets once per day-equivalent (device uptime,
// no RTC on this board).
unsigned long activeSecondsToday = 0;
unsigned long lastActivityTickMillis = 0;
// "Virtual leash" - set on an unexpected BLE disconnect while in Pet mode
// (see ServerCallbacks::onDisconnect), cleared on reconnect. Drives a
// distinct NeoPixel blink pattern in loop() to help visually locate the
// pet in the dark, on top of the gateway SMS already fired.
bool petLostAlertActive = false;

// Wrist: sleep tally - accumulates real time spent in SCREEN_SLEEP
// (already an existing mechanism, just totaled instead of discarded).
unsigned long sleepSecondsToday = 0;
unsigned long sleepTallyLastTickMillis = 0;

// Helmet: Impact Log - counts real fall-trigger events and when the last
// one happened, plus the two-step "confirm you're OK" flow state.
int impactCountToday = 0;
unsigned long lastImpactMillis = 0;
bool awaitingConcussionConfirm = false;
bool concussionConfirmPending = false;
unsigned long concussionConfirmAtMillis = 0;
const unsigned long CONCUSSION_CONFIRM_TIMEOUT_MS = 10000;

// Kids: quiet hours - suppresses the non-critical incoming-message buzzer
// chime during a Guardian-set window (e.g. bedtime); fall/SOS alerts are
// never affected by this. -1 = disabled (no quiet hours set).
int quietStartHour = -1;
int quietEndHour = -1;

// Bike/Helmet: turn-by-turn-style navigation. Real distance + bearing
// computed from the actual live gateway GPS fix to a Guardian-set
// destination (same haversine approach as Ride Stats, not fabricated) -
// not full routed turn-by-turn directions, but enough to demo a working
// nav readout on-device.
bool navActive = false;
double navDestLat = 0.0, navDestLon = 0.0;
String navDestLabel = "";

// Elderly: post-impact stillness check - if the wearer shows no motion
// for STILLNESS_CHECK_MS after a fall trigger, that's a real signal they
// may be unresponsive, so a second alert is fired as escalation (not just
// a confidence score with no effect - an actual second SMS attempt).
bool stillnessMonitorActive = false;
bool stillnessMotionSeen = false;
bool stillnessEscalated = false;
unsigned long stillnessMonitorStartMillis = 0;
const unsigned long STILLNESS_CHECK_MS = 3000;

// Elderly: medication reminder - scheduled via EXT "MED:HH:MM" (Phase 4
// wires a real Android-side scheduler UI to this; the firmware-side
// trigger logic is real today). -1 = no reminder set.
int medReminderHour = -1;
int medReminderMinute = -1;
bool medReminderDueNow = false;
int medReminderLastFiredMinuteOfDay = -1; // guards against re-firing all minute

// Helmet: scheduled worker check-ins - same idea as the medication
// reminder but interval-based rather than clock-time-based. Escalates to
// a real gateway SMS if the check-in isn't dismissed in time (a missed
// check-in is exactly the kind of signal this mode exists to catch).
unsigned long checkinIntervalMs = 0;  // 0 = disabled
unsigned long lastCheckinAtMillis = 0;
bool checkinDueNow = false;
bool checkinEscalated = false;
unsigned long checkinDueAtMillis = 0;
const unsigned long CHECKIN_MISS_ESCALATE_MS = 300000; // 5 min unacknowledged

// Kids: geofence status - populated by EXT "GEOFENCE:<zone>:<IN|OUT>"
// once the app's GeofencingClient is wired to send it (Phase 4); firmware
// side already accepts and stores it.
String geofenceZoneName = "";
bool geofenceInside = false;
bool geofenceKnown = false;

// SMS allowlist - populated by EXT "SMSALLOW:<num1>,<num2>,..." (full
// replace each time it's received). Empty (count 0) means "allow
// everything", the same as today's behavior - a filter is only active
// once the app has actually pushed a non-empty list.
#define SMS_ALLOWLIST_MAX 8
char smsAllowlist[SMS_ALLOWLIST_MAX][21];
int smsAllowlistCount = 0;

struct WeatherData {
    int rainChance = 0;
    float uvIndex = 0.0;
    float humidity = 0.0;
    String condition = "SYNCING...";
    String forecast = "";
} weather;

// ==========================================
// 4b. ADAPTIVE MODES
// ==========================================
PersonaMode activeMode = MODE_BACKPACK;

// On-device Mode Select screen interaction state (see drawModeSelectScreen()
// and the SCREEN_MODE_SELECT handling in loop()).
PersonaMode modeSelectPreview = MODE_BACKPACK;
unsigned long modeSelectLastInteractionMillis = 0;
const unsigned long MODE_SELECT_IDLE_TIMEOUT_MS = 8000;

/** Matches the Android app's PersonaMode.name exactly (e.g. "ELDERLY"),
 * not a raw ordinal - see the enum's declaration comment for why. */
PersonaMode modeFromName(const String &name) {
    if (name == "ELDERLY") return MODE_ELDERLY;
    if (name == "KIDS") return MODE_KIDS;
    if (name == "BIKE") return MODE_BIKE;
    if (name == "PET") return MODE_PET;
    if (name == "HELMET") return MODE_HELMET;
    if (name == "WRIST") return MODE_WRIST;
    return MODE_BACKPACK;
}

const char* modeName(PersonaMode m) {
    switch (m) {
        case MODE_ELDERLY:  return "ELDERLY";
        case MODE_KIDS:     return "KIDS";
        case MODE_BIKE:     return "BIKE";
        case MODE_PET:      return "PET";
        case MODE_HELMET:   return "HELMET";
        case MODE_WRIST:    return "WRIST";
        default:            return "BACKPACK";
    }
}

/**
 * Mode-specific algorithm/behavior parameters, looked up fresh wherever
 * needed (cheap - just a switch) rather than cached, so changing
 * `activeMode` takes effect immediately everywhere.
 *
 * Deliberately layered UNDER the existing settings.fallSensitivity
 * mechanism, not replacing it: the Android app already sends a mode's
 * `defaultFallSensitivity` through the SETTINGS characteristic whenever
 * mode changes (and the user can still override it from the Safety
 * screen afterward) - adjustedImpactThresh in loop() computes that as
 * before. `minImpactThresh` here is an additional FLOOR applied on top,
 * for modes that need one regardless of the user's sensitivity choice
 * (Bike/Helmet - vibration/impact realities that shouldn't be overridden
 * down to "most sensitive" and start false-triggering), not a duplicate
 * threshold system.
 */
// (ModeProfile struct itself is declared up near ShadyMood/PersonaMode -
// see the comment there for why.)
ModeProfile getModeProfile(PersonaMode m) {
    ModeProfile p;
    p.minImpactThresh = 0;
    p.fallDetectionEnabled = true;
    p.gyroCrashCheck = false;
    p.gyroCrashRotThresh = 0;
    p.stillnessCheck = false;
    p.rgbMenuDisabled = false;
    p.gatewayPollIntervalMs = GATEWAY_POLL_INTERVAL_MS;
    p.sosHoldMs = 1500;

    switch (m) {
        case MODE_ELDERLY:
            p.stillnessCheck = true;
            break;
        case MODE_KIDS:
            p.gatewayPollIntervalMs = 2000;  // location priority
            p.sosHoldMs = 800;               // faster escalation
            break;
        case MODE_BIKE:
            p.minImpactThresh = 55000;       // raised floor - road vibration isn't a crash
            p.gyroCrashCheck = true;
            p.gyroCrashRotThresh = 20000;
            break;
        case MODE_PET:
            p.fallDetectionEnabled = false;  // activity tracking instead - see SCREEN_ACTIVITY
            break;
        case MODE_HELMET:
            p.minImpactThresh = 70000;       // "6g+" - highest floor of any mode
            p.gyroCrashCheck = true;
            p.gyroCrashRotThresh = 25000;
            p.rgbMenuDisabled = true;        // minimal-distraction UI
            break;
        case MODE_WRIST:
        case MODE_BACKPACK:
        default:
            break;  // baseline - every other mode deviates from this
    }
    return p;
}

// Quick Reply Options
const char* quickReplies[] = {
    "I'm OK!",
    "On my way!",
    "Need 5 min",
    "Call me",
    "Help needed",
    "EXIT"
};
const int NUM_REPLIES = 6;

// ==========================================
// 5. MESSAGE HISTORY (NEW - for timeline)
// ==========================================
#define MAX_MESSAGES 5

struct Message {
    String text;
    bool fromGuardian;  // true = from guardian, false = sent reply
    unsigned long timestamp;
    bool valid;
};

Message messageHistory[MAX_MESSAGES];
int messageCount = 0;

void addMessage(String text, bool fromGuardian) {
    // Shift messages down
    for (int i = MAX_MESSAGES - 1; i > 0; i--) {
        messageHistory[i] = messageHistory[i - 1];
    }
    // Add new message at top
    messageHistory[0].text = text;
    messageHistory[0].fromGuardian = fromGuardian;
    messageHistory[0].timestamp = millis();
    messageHistory[0].valid = true;
    
    if (messageCount < MAX_MESSAGES) messageCount++;
}

// ==========================================
// 6. GLOBAL OBJECTS
// ==========================================
U8G2_SH1106_128X64_NONAME_F_HW_I2C u8g2(U8G2_R0, U8X8_PIN_NONE, SCL_PIN, SDA_PIN);
Adafruit_NeoPixel strip(NUM_LEDS, PIN_NEO, NEO_GRB + NEO_KHZ800);

// BLE
BLEServer* pServer = NULL;
BLECharacteristic* pWeatherChar = NULL;
BLECharacteristic* pAlertChar = NULL;
BLECharacteristic* pMessageChar = NULL;
BLECharacteristic* pHealthChar = NULL;
BLECharacteristic* pSettingsChar = NULL;
BLECharacteristic* pReplyChar = NULL;
BLECharacteristic* pTelemetryChar = NULL;
BLECharacteristic* pLedChar = NULL;
BLECharacteristic* pExtChar = NULL;
BLECharacteristic* pAckChar = NULL;
unsigned long lastTelemetryTick = 0;

bool deviceConnected = false;
bool oldDeviceConnected = false;

// Sensor Data
int16_t AcX, AcY, AcZ, Tmp;
int16_t prevAcX, prevAcY, prevAcZ;
int16_t GyX, GyY, GyZ;
int lightVal = 0;
float currentTemp = 0.0;
float displayTemp = 0.0;
unsigned long lastTempUpdate = 0;

// Simulated Battery
int batteryLevel = 85;
unsigned long lastBatteryTick = 0;

// Message Data
String guardianMessage = "";
bool hasNewMessage = false;
unsigned long messageReceivedTime = 0;

// Device display name - synced from the app via EXT "DEVNAME:" (see
// ExtCallbacks). Previously the app's DeviceSettings.name never reached
// the firmware at all; shown on the Home screen ticker.
String deviceDisplayName = "SafeShade S1";

// Clock
int clockHour = 10;
int clockMinute = 30;
unsigned long lastClockTick = 0;

// Shady's autonomous "takeover" moments on the Home screen - purely
// decorative, no user input, on a randomized timer (see drawHomeScreen()).
unsigned long nextShadyTakeoverAtMillis = 9000; // first one shortly after boot
bool shadyTakeoverActive = false;
unsigned long shadyTakeoverEndsAtMillis = 0;
int shadyTakeoverLineIdx = 0;
// Shady's personality lines - short jokes, quirks, and random musings, not
// system status. Kept mostly under ~20 chars so the speech bubble stays
// comfortable, but drawHomeScreen()'s bubble sizing is defensive about
// width regardless (see the fixed off-screen-bubble bug there).
const char* const shadyQuirkyLines[] = {
    "Beep boop :)",
    "*hums quietly*",
    "Just vibing here.",
    "Ooh, shiny button!",
    "Don't mind me~",
    "Plotting mischief",
    "I like your vibe.",
    "Is it snack time?",
    "*stares blankly*",
    "Bzzt! All good.",
    "I counted 8 LEDs.",
    "Smells like WiFi.",
    "Zero to hero, me.",
    "Loading... loaded!",
    "I believe in you!",
    "Existing, mostly.",
    "This is my screen.",
    "Feature, not a bug.",
    "Blink! On purpose.",
    "Lights out, huh?",
    "Vibe recalibrated.",
    "*speaks in binary*",
    "I dreamt in pixels",
    "My best byte yet.",
    "Sensor says: neat!",
    "Say hi sometime!",
    "Big shield energy.",
    "Antenna: breezy!",
    "Pondering the void",
    "Hydrate, human.",
    "Somebody push me.",
    "Stable as ever!",
    "Got your back!",
    "Powered by vibes.",
    "You're my favorite",
    "Tiny but mighty."
};
const int SHADY_QUIRKY_LINE_COUNT = 36;

// ==========================================
// 7. STATE MACHINE
// ==========================================
// (ScreenState itself, and currentScreen/previousScreen, are declared up
// near ShadyMood/PersonaMode - see the comment there for why.)

// RGB Patterns - REMOVED LANTERN (Issue #7)
enum RGBPattern {
    RGB_TORCH = 0,
    RGB_RAINBOW,
    RGB_CYBER,
    RGB_POLICE,
    RGB_FIRE,
    RGB_OCEAN,
    RGB_PULSE,
    RGB_COUNT  // Now 7 patterns (was 8)
};
int rgbPattern = RGB_TORCH;

// Pattern names for menu
const char* rgbPatternNames[] = {
    "TORCH",
    "RAINBOW", 
    "CYBER",
    "POLICE",
    "FIRE",
    "OCEAN",
    "PULSE"
};

// Input State
int clickCount = 0;
unsigned long lastClickTime = 0;
unsigned long btnDownTime = 0;
bool btnState = false;
bool btnWasHeld = false;

// Quick Reply Mode
bool inReplyMode = false;
int selectedReply = 0;

// Headlamp State
bool isHeadlampOn = false;

// Physics State
bool walkingDetected = false;
unsigned long brakeTimer = 0;
unsigned long lastMotionTime = 0;

// FIXED: Flag to prevent screen cycling after wake (Issue #4)
bool justWokeUp = false;
unsigned long wakeTime = 0;

// SOS auto-alert lock-in: entering SOS (1.5s hold) starts a countdown
// instead of firing the SMS immediately, so a single accidental long-press
// can still be cancelled (tap, or hold past 2s - see loop()) before any
// alert goes out. Real emergencies where the wearer can't respond just
// ride the countdown out.
const unsigned long SOS_ALERT_LOCKIN_MS = 5000;
unsigned long sosEnteredAtMillis = 0;
bool sosAlertSent = false;

// ==========================================
// 8. ICON BITMAPS (8x8)
// ==========================================

const uint8_t icon_bt_connected[] PROGMEM = {
    0b00011000,
    0b00010100,
    0b01010010,
    0b00101100,
    0b00101100,
    0b01010010,
    0b00010100,
    0b00011000
};

const uint8_t icon_bt_searching[] PROGMEM = {
    0b00011000,
    0b00010100,
    0b01010010,
    0b00101100,
    0b00101100,
    0b01010010,
    0b10010101,
    0b00011000
};

const uint8_t icon_location[] PROGMEM = {
    0b00111100,
    0b01111110,
    0b01100110,
    0b01111110,
    0b00111100,
    0b00011000,
    0b00011000,
    0b00001000
};

const uint8_t icon_heart[] PROGMEM = {
    0b01100110,
    0b11111111,
    0b11111111,
    0b11111111,
    0b01111110,
    0b00111100,
    0b00011000,
    0b00000000
};

const uint8_t icon_message[] PROGMEM = {
    0b11111111,
    0b11000011,
    0b10100101,
    0b10011001,
    0b10000001,
    0b10000001,
    0b10000001,
    0b11111111
};

const uint8_t icon_sun[] PROGMEM = {
    0b00100100,
    0b00011000,
    0b01111110,
    0b01011010,
    0b01011010,
    0b01111110,
    0b00011000,
    0b00100100
};

const uint8_t icon_rain[] PROGMEM = {
    0b00010000,
    0b00111000,
    0b00111000,
    0b01111100,
    0b01111100,
    0b11111110,
    0b01111100,
    0b00111000
};

const uint8_t icon_cloud[] PROGMEM = {
    0b00011100,
    0b00111110,
    0b01111111,
    0b11111111,
    0b11111111,
    0b01111110,
    0b00000000,
    0b00000000
};

// ==========================================
// 9. BLE CALLBACKS
// ==========================================

/**
 * Unified acknowledgement stream (ACK_CHAR_UUID, notify-only). Every write
 * callback below that actually changes device state calls this once the
 * change has been applied, so the app can show a real "applied on device"
 * confirmation instead of the optimistic-only pattern used everywhere
 * before this (e.g. the LED picker previously had no device readback at
 * all). Message "ACK:<TAG>" - TAG matches the characteristic/command.
 */
void sendAck(const String &tag) {
    if (!deviceConnected || pAckChar == NULL) return;
    String msg = "ACK:" + tag;
    pAckChar->setValue(msg.c_str());
    pAckChar->notify();
}

/**
 * Switches the active adaptive mode: resets per-mode session state (ride/
 * activity/impact counters) so entering a mode always starts clean, plays
 * a short confirmation tone + a mode-colored NeoPixel flash (the "retro
 * animation confirmation" for a mode switch), and sends its own
 * "ACK:MODE:<name>" over pAckChar so the app stays in sync regardless of
 * whether the switch was app-initiated (EXT "MODE:") or device-initiated
 * (the on-device Mode Select screen).
 */
void applyMode(PersonaMode m) {
    activeMode = m;

    // If the new mode disables the Light Mode menu (Helmet) and it's
    // currently open (this can happen via an app-initiated EXT "MODE:"
    // switch arriving while the wearer has it open), evict it immediately
    // rather than leaving it reachable/open until the wearer happens to
    // double-click out on their own.
    if (getModeProfile(m).rgbMenuDisabled && currentScreen == SCREEN_RGB_MENU) {
        forceLEDsOff();
        currentScreen = SCREEN_HOME;
    }

    rideActive = false;
    rideDistanceKm = 0.0;
    activeSecondsToday = 0;
    impactCountToday = 0;
    awaitingConcussionConfirm = false;
    concussionConfirmPending = false;
    // Defense in depth on top of the activeMode guards at each mode's own
    // due-check (review found real bugs where these could otherwise keep
    // firing/blinking in the background after switching away from the
    // mode that armed them):
    checkinDueNow = false;
    checkinEscalated = false;
    petLostAlertActive = false;
    medReminderDueNow = false;

    tone(PIN_BUZZER, 2200, 60);
    delay(70);
    tone(PIN_BUZZER, 2800, 100);

    uint32_t flashColor;
    switch (m) {
        case MODE_ELDERLY: flashColor = strip.Color(255, 200, 0);  break;
        case MODE_KIDS:    flashColor = strip.Color(0, 200, 255);  break;
        case MODE_BIKE:    flashColor = strip.Color(255, 80, 0);   break;
        case MODE_PET:     flashColor = strip.Color(0, 255, 100);  break;
        case MODE_HELMET:  flashColor = strip.Color(255, 0, 60);   break;
        case MODE_WRIST:   flashColor = strip.Color(0, 150, 255);  break;
        default:           flashColor = strip.Color(150, 0, 255);  break;
    }
    for (int i = 0; i < NUM_LEDS; i++) strip.setPixelColor(i, flashColor);
    strip.show();
    delay(200);
    strip.clear();
    strip.show();

    if (deviceConnected && pAckChar != NULL) {
        String msg = "ACK:MODE:" + String(modeName(m));
        pAckChar->setValue(msg.c_str());
        pAckChar->notify();
    }
}

/**
 * Which screens the single-click cycle visits, and in what order, for the
 * current activeMode - the on-device counterpart to each mode's "UI
 * changes" (reduced cycle for Elderly/Helmet, extra priority screens for
 * Bike/Pet/Wrist/Kids/Elderly). `out` must have room for at least 6.
 * Returns the count. RGB_MENU/SOS/SLEEP/FALL/MODE_SELECT are never part
 * of this cycle - they're reached by their own distinct gestures.
 */
int buildModeScreenCycle(ScreenState *out) {
    int n = 0;
    out[n++] = SCREEN_HOME;
    switch (activeMode) {
        case MODE_ELDERLY:
            out[n++] = SCREEN_HEALTH;
            out[n++] = SCREEN_GPS;
            out[n++] = SCREEN_MEDICATION;
            break;
        case MODE_KIDS:
            out[n++] = SCREEN_WEATHER;
            out[n++] = SCREEN_HEALTH;
            out[n++] = SCREEN_GPS;
            out[n++] = SCREEN_MESSAGE;
            out[n++] = SCREEN_SAFE_ZONE;
            break;
        case MODE_BIKE:
            out[n++] = SCREEN_HEALTH;
            out[n++] = SCREEN_GPS;
            out[n++] = SCREEN_MESSAGE;
            out[n++] = SCREEN_RIDE_STATS;
            break;
        case MODE_PET:
            out[n++] = SCREEN_HEALTH;
            out[n++] = SCREEN_GPS;
            out[n++] = SCREEN_ACTIVITY;
            break;
        case MODE_HELMET:
            out[n++] = SCREEN_HEALTH;
            out[n++] = SCREEN_IMPACT_LOG;
            break;
        case MODE_WRIST:
            out[n++] = SCREEN_HEALTH;
            out[n++] = SCREEN_GPS;
            out[n++] = SCREEN_MESSAGE;
            out[n++] = SCREEN_VITALS;
            break;
        case MODE_BACKPACK:
        default:
            out[n++] = SCREEN_WEATHER;
            out[n++] = SCREEN_HEALTH;
            out[n++] = SCREEN_GPS;
            out[n++] = SCREEN_MESSAGE;
            break;
    }
    return n;
}

class ServerCallbacks : public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
        deviceConnected = true;
        petLostAlertActive = false;  // phone's back in range
        tone(PIN_BUZZER, 1500, 50);
        delay(60);
        tone(PIN_BUZZER, 2000, 50);
        delay(60);
        tone(PIN_BUZZER, 2500, 100);
    }

    void onDisconnect(BLEServer* pServer) {
        deviceConnected = false;
        tone(PIN_BUZZER, 1500, 100);
        delay(110);
        tone(PIN_BUZZER, 1000, 100);

        // Pet mode "virtual leash": an unexpected disconnect (phone drifted
        // out of range) is a real lost-pet signal, using only the
        // connection-state transition and the existing gateway SMS path -
        // no new sensor or endpoint needed.
        if (activeMode == MODE_PET) {
            petLostAlertActive = true;
            tone(PIN_BUZZER, 2500, 80);
            delay(90);
            tone(PIN_BUZZER, 2500, 80);
            triggerGatewayAlert();
        }
    }
};

class WeatherCallbacks : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* pCharacteristic) {
        String value = pCharacteristic->getValue().c_str();
        if (value.length() == 0) return;

        if (value == "CMD_FIND") {
            currentScreen = SCREEN_SOS;
            return;
        }

        int lastIdx = 0;
        String parts[11];
        int partCount = 0;

        for (int i = 0; i <= value.length() && partCount < 11; i++) {
            if (i == value.length() || value[i] == ',') {
                parts[partCount++] = value.substring(lastIdx, i);
                lastIdx = i + 1;
            }
        }

        if (partCount >= 1) weather.rainChance = parts[0].toInt();
        if (partCount >= 2) weather.condition = parts[1];
        if (partCount >= 3) weather.uvIndex = parts[2].toFloat();
        if (partCount >= 4) weather.humidity = parts[3].toFloat();
        if (partCount >= 5) location.lat = parts[4].toFloat();
        if (partCount >= 6) location.lon = parts[5].toFloat();
        if (partCount >= 7) location.locationName = parts[6];
        if (partCount >= 8) location.locality = parts[7];
        if (partCount >= 9) location.altitude = parts[8].toInt();
        if (partCount >= 10) clockHour = parts[9].toInt();
        if (partCount >= 11) clockMinute = parts[10].toInt();

        if (partCount >= 6 && location.lat != 0) {
            location.valid = true;
        }

        sendAck("WEATHER");
    }
};

class MessageCallbacks : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* pCharacteristic) {
        String value = pCharacteristic->getValue().c_str();
        if (value.length() > 0) {
            guardianMessage = value;
            hasNewMessage = true;
            messageReceivedTime = millis();
            
            // Add to message history (Issue #5)
            addMessage(value, true);

            // Kids quiet hours mute the chime only - message still shows.
            if (!(activeMode == MODE_KIDS && inQuietHours())) {
                tone(PIN_BUZZER, 1000, 100);
                delay(110);
                tone(PIN_BUZZER, 1500, 100);
                delay(110);
                tone(PIN_BUZZER, 2000, 150);
            }

            currentScreen = SCREEN_MESSAGE;
        }
    }
};

class HealthCallbacks : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* pCharacteristic) {
        String value = pCharacteristic->getValue().c_str();
        if (value.length() == 0) return;

        int lastIdx = 0;
        String parts[5];
        int partCount = 0;

        for (int i = 0; i <= value.length() && partCount < 5; i++) {
            if (i == value.length() || value[i] == ',') {
                parts[partCount++] = value.substring(lastIdx, i);
                lastIdx = i + 1;
            }
        }

        if (partCount >= 1) healthData.bloodType = parts[0];
        if (partCount >= 2) healthData.emergencyContact = parts[1];
        if (partCount >= 3) healthData.contactName = parts[2];
        if (partCount >= 4) healthData.allergies = parts[3];
        if (partCount >= 5) healthData.age = parts[4].toInt();

        tone(PIN_BUZZER, 2000, 50);
        sendAck("HEALTH");
    }
};

class SettingsCallbacks : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* pCharacteristic) {
        String value = pCharacteristic->getValue().c_str();
        if (value.length() == 0) return;

        // 5 fields now (was 3) - parentalControlsEnabled/smsFallbackEnabled
        // added so the two SafetySettings fields that existed in the app
        // but never reached the device actually do now. Trailing fields
        // are optional (partCount guards below), so an older/shorter
        // payload still parses fine.
        int lastIdx = 0;
        String parts[5];
        int partCount = 0;

        for (int i = 0; i <= value.length() && partCount < 5; i++) {
            if (i == value.length() || value[i] == ',') {
                parts[partCount++] = value.substring(lastIdx, i);
                lastIdx = i + 1;
            }
        }

        if (partCount >= 1) settings.fallSensitivity = parts[0].toInt();
        if (partCount >= 2) settings.sosVolume = parts[1].toInt();
        if (partCount >= 3) settings.autoCallEnabled = (parts[2] == "1");
        if (partCount >= 4) settings.parentalControlsEnabled = (parts[3] == "1");
        if (partCount >= 5) settings.smsFallbackEnabled = (parts[4] == "1");

        tone(PIN_BUZZER, 1800, 50);
        sendAck("SETTINGS");
    }
};

// Companion -> Guardian quick replies (fixes a bug where the app wrote replies
// to pReplyChar but this characteristic had no write property/callback, so
// every reply from the Companion phone silently failed on the wire).
class ReplyCallbacks : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* pCharacteristic) {
        String value = pCharacteristic->getValue().c_str();
        if (value.length() == 0) return;

        addMessage(value, false);
        pReplyChar->setValue(value.c_str());
        pReplyChar->notify();
        // Real two-way relay - the Guardian is almost never the phone
        // that's actually BLE-connected right now, so the notify above
        // alone wouldn't reach them.
        triggerGatewayReply(value);

        tone(PIN_BUZZER, 1500, 80);
    }
};

// Remote LED pattern control - lets the app drive the same rgbPattern the
// physical button already cycles through (see "13. RGB PATTERN FUNCTIONS"),
// just from the other end of the BLE link. Value is the pattern's numeric
// index as ASCII, matching the RGBPattern enum order exactly.
class LedCallbacks : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* pCharacteristic) {
        String value = pCharacteristic->getValue().c_str();
        if (value.length() == 0) return;

        int idx = value.toInt();
        if (idx >= 0 && idx < RGB_COUNT) {
            rgbPattern = idx;
            // FIXED: previously only ever applied from case SCREEN_RGB_MENU
            // in the draw switch, so a BLE-driven pattern change sat inert
            // until the wearer happened to open the Light Mode menu on the
            // device itself. updateRGBPattern() is self-contained (reads
            // rgbPattern/lightVal, ends with strip.show()) so it's safe to
            // call here regardless of what screen is currently showing.
            updateRGBPattern();
            sendAck(String("LED:") + rgbPatternNames[idx]);
        }
    }
};

// Generic tagged-command channel (EXT_CHAR_UUID). Payload is "TAG:payload"
// (payload optional) - one dispatcher instead of a new single-purpose
// characteristic per feature, so adaptive-mode switching, geofence
// events, and reminder scheduling (later phases) don't each need their
// own UUID. Any tag is safely accepted and acknowledged even before its
// real handling lands, so the app's ack round-trip always works.
class ExtCallbacks : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* pCharacteristic) {
        String value = pCharacteristic->getValue().c_str();
        if (value.length() == 0) return;

        int colonIdx = value.indexOf(':');
        String tag = (colonIdx == -1) ? value : value.substring(0, colonIdx);
        String payload = (colonIdx == -1) ? "" : value.substring(colonIdx + 1);

        if (tag == "DEVNAME" && payload.length() > 0) {
            deviceDisplayName = payload;
        } else if (tag == "MODE" && payload.length() > 0) {
            applyMode(modeFromName(payload));  // sends its own tone + ack
            return;
        } else if (tag == "MED") {
            // "HH:MM" schedules a reminder; empty payload clears it.
            int sep = payload.indexOf(':');
            if (sep != -1) {
                medReminderHour = payload.substring(0, sep).toInt();
                medReminderMinute = payload.substring(sep + 1).toInt();
            } else {
                medReminderHour = -1;
                medReminderMinute = -1;
            }
            medReminderDueNow = false;
        } else if (tag == "CHECKIN") {
            // Seconds as a string; empty/0 disables.
            long seconds = payload.toInt();
            checkinIntervalMs = seconds > 0 ? (unsigned long)seconds * 1000UL : 0;
            lastCheckinAtMillis = millis();
            checkinDueNow = false;
            checkinEscalated = false;
        } else if (tag == "GEOFENCE") {
            // "<zoneName>:<IN|OUT>"
            int sep = payload.lastIndexOf(':');
            if (sep != -1) {
                geofenceZoneName = payload.substring(0, sep);
                geofenceInside = payload.substring(sep + 1) == "IN";
                geofenceKnown = true;
            }
        } else if (tag == "SMSALLOW") {
            // "<num1>,<num2>,..." - full-replace each time, up to
            // SMS_ALLOWLIST_MAX entries. Empty payload clears the list
            // (back to "allow everything").
            smsAllowlistCount = 0;
            int start = 0;
            while (start <= (int)payload.length() && smsAllowlistCount < SMS_ALLOWLIST_MAX) {
                int comma = payload.indexOf(',', start);
                String entry = (comma == -1) ? payload.substring(start) : payload.substring(start, comma);
                entry.trim();
                if (entry.length() > 0) {
                    entry.toCharArray(smsAllowlist[smsAllowlistCount], sizeof(smsAllowlist[smsAllowlistCount]));
                    smsAllowlistCount++;
                }
                if (comma == -1) break;
                start = comma + 1;
            }
        } else if (tag == "QUIET") {
            // "<startHour>:<endHour>" (0-23, wraps past midnight if
            // start > end); empty payload disables quiet hours.
            int sep = payload.indexOf(':');
            if (sep != -1) {
                quietStartHour = payload.substring(0, sep).toInt();
                quietEndHour = payload.substring(sep + 1).toInt();
            } else {
                quietStartHour = -1;
                quietEndHour = -1;
            }
        } else if (tag == "NAV") {
            // "<lat>:<lon>:<label>"; empty payload stops/clears navigation.
            if (payload.length() == 0) {
                navActive = false;
            } else {
                int sep1 = payload.indexOf(':');
                int sep2 = (sep1 == -1) ? -1 : payload.indexOf(':', sep1 + 1);
                if (sep1 != -1 && sep2 != -1) {
                    navDestLat = payload.substring(0, sep1).toDouble();
                    navDestLon = payload.substring(sep1 + 1, sep2).toDouble();
                    navDestLabel = payload.substring(sep2 + 1);
                    navActive = true;
                }
            }
        }

        tone(PIN_BUZZER, 1700, 40);
        sendAck(tag);
    }
};

// ==========================================
// 10. SOUND FUNCTIONS
// ==========================================
void playBootSound() {
    tone(PIN_BUZZER, 523, 80); delay(90);
    tone(PIN_BUZZER, 659, 80); delay(90);
    tone(PIN_BUZZER, 784, 80); delay(90);
    tone(PIN_BUZZER, 1047, 150);
}

void playClickSound() {
    tone(PIN_BUZZER, 2000, 15);
}

void playScreenChange() {
    tone(PIN_BUZZER, 1800, 30);
}

void playModeChange() {
    tone(PIN_BUZZER, 2500, 40);
    delay(50);
    tone(PIN_BUZZER, 3000, 40);
}

void playReplySelect() {
    tone(PIN_BUZZER, 1200, 30);
}

void playReplySent() {
    tone(PIN_BUZZER, 1500, 80); delay(90);
    tone(PIN_BUZZER, 2000, 80); delay(90);
    tone(PIN_BUZZER, 2500, 120);
}

// FIXED: Headlamp control with proper state management (Issue #1)
void setHeadlamp(bool on) {
    if (on == isHeadlampOn) return;
    isHeadlampOn = on;
    
    if (on) {
        for (int i = 0; i <= 255; i += 25) {
            analogWrite(PIN_LED, i);
            delay(2);
        }
        analogWrite(PIN_LED, 255);
    } else {
        for (int i = 255; i >= 0; i -= 25) {
            analogWrite(PIN_LED, i);
            delay(2);
        }
        analogWrite(PIN_LED, 0);
    }
}

// Force headlamp off immediately
void forceHeadlampOff() {
    analogWrite(PIN_LED, 0);
    isHeadlampOn = false;
}

// Force all LEDs off
void forceLEDsOff() {
    strip.clear();
    strip.show();
    forceHeadlampOff();
}

// ==========================================
// 11. DRAWING HELPER FUNCTIONS
// ==========================================

void drawIcon8x8(int x, int y, const uint8_t* icon) {
    for (int row = 0; row < 8; row++) {
        uint8_t rowData = pgm_read_byte(&icon[row]);
        for (int col = 0; col < 8; col++) {
            if (rowData & (1 << (7 - col))) {
                u8g2.drawPixel(x + col, y + row);
            }
        }
    }
}

void drawCenteredText(int y, const char* text) {
    int width = u8g2.getStrWidth(text);
    u8g2.setCursor((128 - width) / 2, y);
    u8g2.print(text);
}

void drawBatteryIcon(int x, int y, int percent) {
    u8g2.drawFrame(x, y, 18, 9);
    u8g2.drawBox(x + 18, y + 2, 2, 5);
    int fillWidth = 14 * percent / 100;
    if (fillWidth > 0) {
        u8g2.drawBox(x + 2, y + 2, fillWidth, 5);
    }
}

/**
 * Small 4-bar cellular signal indicator (procedural, no new icon bitmap
 * needed) driven by the gateway's AT+CSQ reading (0-31, higher=better;
 * 99=unknown). Only called when the gateway link is actually fresh - see
 * call sites - so it never shows a signal reading for a gateway that's
 * off or unreachable.
 */
void drawSignalBars(int x, int y, int csq) {
    int bars = 0;
    if (csq >= 0 && csq <= 31) {
        if (csq >= 20) bars = 4;
        else if (csq >= 14) bars = 3;
        else if (csq >= 8) bars = 2;
        else bars = 1;
    }
    for (int i = 0; i < 4; i++) {
        int barHeight = 2 + i * 2;
        int barX = x + i * 3;
        int barY = y + (6 - barHeight);
        if (i < bars) {
            u8g2.drawBox(barX, barY, 2, barHeight);
        } else {
            u8g2.drawFrame(barX, barY, 2, barHeight);
        }
    }
}

// FIXED: Screen dots now vertical on right side (Issue #2)
void drawVerticalScreenDots(int current, int total) {
    int startY = 32 - (total * 4);  // Center vertically
    for (int i = 0; i < total; i++) {
        int y = startY + (i * 9);
        if (i == current) {
            u8g2.drawDisc(124, y, 2);  // Right side, filled
        } else {
            u8g2.drawCircle(124, y, 1);  // Smaller unfilled dots
        }
    }
}

void drawAnimatedPulse() {
    int pulsePos = (millis() / 25) % 128;
    u8g2.drawHLine(0, 63, 128);
    for (int i = -2; i <= 2; i++) {
        int x = pulsePos + i;
        if (x >= 0 && x < 128) {
            u8g2.drawPixel(x, 62);
        }
    }
}

// ==========================================
// 11b. SHADY - the on-device companion character
// ==========================================
// (ShadyMood itself is declared up near the state-machine enums - see the
// comment there for why.)

/**
 * Shady - a small rounded-square "companion device" mascot with a single
 * curved signal-nub antenna, Kirby-style eyes (a cutout circle plus a
 * solid pupil, not just a bare dot), and a mouth that's always present.
 * Deliberately NOT a perfect circle/blob with blank dot eyes - that read
 * as an alien head in an earlier pass. cx,cy is the body center, r is a
 * size scale.
 *
 * Always animating something: a genuine two-axis "floating" drift (not
 * just a vertical bob), blinks, an ambient wink, and - for the calm HAPPY/
 * IDLE moods specifically - a slowly rotating set of personality "quirk"
 * expressions (star eyes, big closed-eye grin, tongue out) so Shady reads
 * as having his own personality ticking along, not just reacting to state.
 */
void drawShady(int cx, int cy, int r, ShadyMood mood, unsigned long tick) {
    // Floating drift - vertical bob plus a slower horizontal sway on a
    // different period, so the motion reads as floating/hovering rather
    // than a single-axis mechanical wobble. Held still only when sleepy
    // (resting) or panicked (a shake reads better than a float there).
    // Sleepy used to be fully rigid (floatY/X pinned to 0) - with Shady now
    // the largest, most eye-catching thing on the Home screen, a fully
    // still Shady reads as the *whole screen* being frozen (ticker/LEDs
    // were still updating underneath, but nothing drew the eye to that).
    // A slow, small "breathing" motion keeps him visibly alive without
    // looking jittery for a resting mood.
    int floatY = (mood == SHADY_SLEEPY)
        ? (int)(sin(tick / 1200.0) * (r * 0.08)) : (int)(sin(tick / 380.0) * (r * 0.16));
    int floatX = (mood == SHADY_PANICKED)
        ? 0 : (mood == SHADY_SLEEPY ? 0 : (int)(sin(tick / 620.0 + 1.3) * (r * 0.14)));
    if (mood == SHADY_PANICKED) floatX = (int)(sin(tick / 60.0) * 1.5);  // quick shake
    int fx = cx + floatX;
    int by = cy + floatY;

    int w = r * 2;
    int h = (int)(r * 1.85);
    int bodyX = fx - w / 2;
    int topY = by - h / 2;
    int cornerR = max(2, r / 2);

    // Curved signal-nub antenna (a couple of short angled segments rather
    // than one straight line) - a "connected companion device" touch,
    // instead of symmetric ear bumps which read too alien-like.
    int antennaH = max(3, r / 3);
    int antennaMidX = fx + max(1, antennaH / 3);
    int antennaTipX = fx + max(1, antennaH / 2);
    u8g2.drawLine(fx, topY, antennaMidX, topY - antennaH / 2);
    u8g2.drawLine(antennaMidX, topY - antennaH / 2, antennaTipX, topY - antennaH);
    u8g2.drawDisc(antennaTipX, topY - antennaH, max(1, r / 8));

    // Body - rounded square, not a circle.
    u8g2.drawRBox(bodyX, topY, w, h, cornerR);

    u8g2.setDrawColor(0);  // cut features into the filled body

    int eyeOffX = w / 4;
    int eyeY = by - h / 8;
    int eyeR = max(2, r / 4);
    bool blink = (mood != SHADY_SLEEPY) && ((tick % 2600) < 110);
    bool quirkWink = (mood == SHADY_HAPPY || mood == SHADY_IDLE || mood == SHADY_EXCITED) &&
                      ((tick % 4000) < 200);
    bool winkLeft = ((tick / 4000) % 2) == 0;

    // Personality quirks: only for the calm moods, cycling on their own
    // clock so Shady visibly "does his own thing" even with nothing to
    // react to. 0=normal, 1=star eyes, 2=big closed-eye grin, 3=tongue out.
    int calmQuirk = (mood == SHADY_HAPPY || mood == SHADY_IDLE) ? (int)((tick / 5500) % 4) : 0;

    // Eyebrows - only for moods that need them, drawn just above each eye;
    // calm/idle/happy/sleepy get none at all.
    bool needsBrows = (mood == SHADY_WORRIED || mood == SHADY_PANICKED || mood == SHADY_EXCITED);
    if (needsBrows) {
        int browY = eyeY - eyeR - 2;
        if (mood == SHADY_WORRIED) {
            u8g2.drawLine(fx - eyeOffX - eyeR, browY + 2, fx - eyeOffX + eyeR, browY);
            u8g2.drawLine(fx + eyeOffX - eyeR, browY, fx + eyeOffX + eyeR, browY + 2);
        } else if (mood == SHADY_PANICKED) {
            u8g2.drawLine(fx - eyeOffX - eyeR, browY, fx - eyeOffX + eyeR, browY - 2);
            u8g2.drawLine(fx + eyeOffX - eyeR, browY - 2, fx + eyeOffX + eyeR, browY);
        } else {  // EXCITED
            u8g2.drawLine(fx - eyeOffX - eyeR, browY + 1, fx - eyeOffX + eyeR, browY - 1);
            u8g2.drawLine(fx + eyeOffX - eyeR, browY - 1, fx + eyeOffX + eyeR, browY + 1);
        }
    }

    // Eyes - a cutout circle with a solid pupil redrawn inside it, rather
    // than a bare dot, for a rounder/friendlier look.
    auto openEye = [&](int ex) {
        u8g2.drawDisc(ex, eyeY, eyeR);
        u8g2.setDrawColor(1);
        u8g2.drawDisc(ex, eyeY, max(1, eyeR / 2));
        u8g2.setDrawColor(0);
    };
    auto closedEye = [&](int ex) {
        u8g2.drawBox(ex - eyeR, eyeY, eyeR * 2, 1);
    };
    auto starEye = [&](int ex) {
        u8g2.drawDisc(ex, eyeY, eyeR);
        u8g2.setDrawColor(1);
        u8g2.drawVLine(ex, eyeY - eyeR / 2, eyeR);
        u8g2.drawHLine(ex - eyeR / 2, eyeY, eyeR);
        u8g2.setDrawColor(0);
    };
    auto happyArcEye = [&](int ex) {
        // A big "^_^" curved grin-eye instead of a round one.
        u8g2.drawLine(ex - eyeR, eyeY + 1, ex, eyeY - eyeR / 2);
        u8g2.drawLine(ex, eyeY - eyeR / 2, ex + eyeR, eyeY + 1);
    };

    if (mood == SHADY_SLEEPY || blink) {
        closedEye(fx - eyeOffX);
        closedEye(fx + eyeOffX);
    } else if (quirkWink) {
        openEye(winkLeft ? fx + eyeOffX : fx - eyeOffX);
        closedEye(winkLeft ? fx - eyeOffX : fx + eyeOffX);
    } else if (calmQuirk == 1) {
        starEye(fx - eyeOffX);
        starEye(fx + eyeOffX);
    } else if (calmQuirk == 2) {
        happyArcEye(fx - eyeOffX);
        happyArcEye(fx + eyeOffX);
    } else {
        openEye(fx - eyeOffX);
        openEye(fx + eyeOffX);
    }

    // Mouth - always present, shape follows mood (and, for calm moods,
    // the current personality quirk).
    int mouthY = by + h / 4;
    switch (mood) {
        case SHADY_WORRIED:
            u8g2.drawLine(fx - eyeR, mouthY + 1, fx, mouthY - 1);
            u8g2.drawLine(fx, mouthY - 1, fx + eyeR, mouthY + 1);
            break;
        case SHADY_PANICKED:
            u8g2.drawDisc(fx, mouthY, max(1, eyeR / 2));
            break;
        case SHADY_EXCITED:
            // A round open-mouth "surprised/happy" shape - avoids
            // drawRBox() here, which at these sizes had a fixed corner
            // radius (2) exceeding half the box's height (eyeR, as low as
            // 2-3px), an out-of-spec call that corrupted rendering nearby
            // (the real cause of the blank band during Shady's takeover,
            // which uses this exact mood).
            u8g2.drawDisc(fx, mouthY, max(1, eyeR - 1));
            break;
        case SHADY_SLEEPY:
            u8g2.drawLine(fx - 2, mouthY, fx + 2, mouthY);
            u8g2.drawPixel(fx + w / 3, topY - antennaH);      // little "z"
            u8g2.drawPixel(fx + w / 3 + 2, topY - antennaH - 2);
            break;
        default:  // HAPPY / IDLE
            if (calmQuirk == 3) {
                // Tongue out - a little silly.
                u8g2.drawLine(fx - eyeR, mouthY, fx, mouthY + eyeR / 2);
                u8g2.drawLine(fx, mouthY + eyeR / 2, fx + eyeR, mouthY);
                u8g2.setDrawColor(1);
                u8g2.drawBox(fx - 1, mouthY + eyeR / 2, 3, max(2, eyeR / 2));
                u8g2.setDrawColor(0);
            } else {
                u8g2.drawLine(fx - eyeR, mouthY, fx, mouthY + eyeR / 2);
                u8g2.drawLine(fx, mouthY + eyeR / 2, fx + eyeR, mouthY);
            }
            break;
    }

    u8g2.setDrawColor(1);
}

// ==========================================
// 11c. RETRO SCREEN CHROME
// ==========================================

// Tracks the currently-*displayed* screen (as opposed to `currentScreen`,
// which the button handler already changes instantly) so the 5 cyclable
// screens can each briefly show a retro "wipe" reveal right after a switch,
// without touching the SOS/FALL blocking-draw path at all. Advanced once
// per loop() iteration, right before the screen switch (see loop()).
ScreenState lastRenderedScreen = SCREEN_HOME;
unsigned long screenEnteredAtMillis = 0;
const unsigned long SCREEN_WIPE_MS = 180;

/** Call as the last thing before sendBuffer() on a cyclable screen - draws
 * a shrinking black reveal-bar over the just-drawn content for ~180ms. */
void drawScreenWipeOverlay() {
    unsigned long elapsed = millis() - screenEnteredAtMillis;
    if (elapsed >= SCREEN_WIPE_MS) return;
    int revealed = (int)((128L * elapsed) / SCREEN_WIPE_MS);
    if (revealed < 128) {
        u8g2.setDrawColor(0);
        u8g2.drawBox(revealed, 0, 128 - revealed, 64);
        u8g2.setDrawColor(1);
    }
}

// ==========================================
// 12. SCREEN DRAWING FUNCTIONS
// ==========================================

void drawHomeScreen() {
    u8g2.clearBuffer();

    // Status bar
    if (deviceConnected) {
        drawIcon8x8(2, 0, icon_bt_connected);
    } else {
        if ((millis() / 500) % 2 == 0) {
            drawIcon8x8(2, 0, icon_bt_searching);
        }
    }

    drawBatteryIcon(106, 1, batteryLevel);

    // Gateway/cellular link indicator - only shown while a /status poll is
    // actually fresh, so it silently disappears if the gateway board is
    // off/unreachable rather than showing stale info.
    bool gatewayStatusFresh = gatewayStatus.reachable &&
        (millis() - gatewayStatus.lastPolledAtMillis) < GATEWAY_STATUS_MAX_AGE_MS;
    if (gatewayStatusFresh) {
        drawSignalBars(88, 1, gatewayStatus.csq);
    }

    u8g2.setFont(u8g2_font_profont10_tr);
    u8g2.setCursor(16, 8);
    if (lightVal < DARK_THRESHOLD) {
        u8g2.print("DARK");
    } else {
        u8g2.print("BRIGHT");
    }

    u8g2.setCursor(55, 8);
    u8g2.print(displayTemp, 1);
    u8g2.print("C");

    u8g2.drawHLine(0, 11, 128);

    unsigned long homeNow = millis();

    // Shady's mood reflects real state whenever he's not off doing a
    // takeover bit: worried on low battery, sleepy approaching auto-sleep,
    // otherwise happy/idle.
    ShadyMood homeMood = SHADY_IDLE;
    if (batteryLevel < 20) homeMood = SHADY_WORRIED;
    else if ((homeNow - lastMotionTime) > (SLEEP_TIMEOUT_MS - 15000)) homeMood = SHADY_SLEEPY;
    else if (deviceConnected) homeMood = SHADY_HAPPY;

    // Autonomous, randomized "Shady takes center stage" moment - no user
    // input, purely a fun life-in-the-device touch. Only arms/resolves
    // while Home is actually being shown (harmless if it fires while the
    // wearer happens to be on another screen - just quietly reschedules).
    // Disabled in Helmet (minimal-distraction UI is the whole point of that
    // mode) and Wrist (watch-face layout has no room for it, and a
    // wandering-personality moment doesn't fit a watch-face) - per your
    // call to keep Shady out of the modes where he doesn't belong.
    // FIXED: Elderly excluded too - it has its own big-clock branch below
    // that never calls drawShady, but this autonomous takeover check runs
    // BEFORE the per-mode branch, so without this it could still arm/flip
    // shadyTakeoverActive while in Elderly mode and blank out the big
    // clock with a takeover the Elderly branch has no way to show.
    bool shadyAllowed = activeMode != MODE_HELMET && activeMode != MODE_WRIST &&
                         activeMode != MODE_ELDERLY;
    if (shadyAllowed && !shadyTakeoverActive && homeNow >= nextShadyTakeoverAtMillis) {
        shadyTakeoverActive = true;
        shadyTakeoverEndsAtMillis = homeNow + 3500;
        shadyTakeoverLineIdx = random(SHADY_QUIRKY_LINE_COUNT);
    } else if (shadyTakeoverActive && homeNow >= shadyTakeoverEndsAtMillis) {
        shadyTakeoverActive = false;
        nextShadyTakeoverAtMillis = homeNow + random(20000, 45000);
    }

    if (shadyTakeoverActive) {
        // Clock hidden, Shady bounces to center stage with a speech bubble.
        int sway = (int)(sin(homeNow / 300.0) * 6);
        int bubbleCx = 64 + sway;

        u8g2.setFont(u8g2_font_profont10_tr);
        const char* line = shadyQuirkyLines[shadyTakeoverLineIdx];
        int lineWidth = u8g2.getStrWidth(line);
        // Cap the bubble to the screen width FIRST so "126 - bubbleW" can
        // never go negative - with that inverted, constrain()'s (low,high)
        // args end up backwards and u8g2's unsigned coordinates wrap into
        // a huge value, drawing the whole bubble off-screen (this was a
        // real bug: a long quirky line could blank out that band of the
        // screen entirely).
        int bubbleW = min(124, lineWidth + 10);
        int bubbleX = constrain(bubbleCx - bubbleW / 2, 2, 126 - bubbleW);
        u8g2.drawRFrame(bubbleX, 16, bubbleW, 16, 4);
        u8g2.drawTriangle(bubbleCx - 4, 31, bubbleCx + 4, 31, bubbleCx, 37);
        u8g2.setCursor(bubbleX + 5, 27);
        u8g2.print(line);

        drawShady(bubbleCx, 48, 13, SHADY_EXCITED, homeNow);
    } else if (activeMode == MODE_ELDERLY) {
        // Elderly mode: bigger, centered clock - accessibility (readable
        // at a glance) matters more here than Shady's screen time, so this
        // reverts to the original large centered clock instead of the
        // smaller left-aligned one the other modes use.
        // FIXED: the font's ':' glyph rendered as a stray vertical line
        // artifact at this size - draw hour/minute as two separate text
        // draws with a gap, and fill the gap with two procedurally-drawn
        // dots (same drawDisc() style already used for Shady elsewhere in
        // this file) instead of relying on the glyph.
        u8g2.setFont(u8g2_font_logisoso32_tn);
        char hourStr[3], minStr[3];
        sprintf(hourStr, "%02d", clockHour);
        sprintf(minStr, "%02d", clockMinute);
        int hourWidth = u8g2.getStrWidth(hourStr);
        int minWidth = u8g2.getStrWidth(minStr);
        const int colonGap = 14;
        int clockWidth = hourWidth + colonGap + minWidth;
        int clockX = (128 - clockWidth) / 2;
        u8g2.setCursor(clockX, 50);
        u8g2.print(hourStr);
        int colonCx = clockX + hourWidth + colonGap / 2;
        u8g2.drawDisc(colonCx, 33, 2);
        u8g2.drawDisc(colonCx, 45, 2);
        u8g2.setCursor(clockX + hourWidth + colonGap, 50);
        u8g2.print(minStr);
        // FIXED: deviceDisplayName ("SafeShade S1" by default) used to be
        // drawn right under the clock here - close enough that its "S1"
        // read as a stray extra "1" tacked onto the time at a glance. This
        // view is meant to be the big, uncluttered clock (Elderly mode's
        // whole point); the device name isn't essential info here.
    } else if (activeMode == MODE_PET) {
        // Pet mode: owner/ICE info always displayed instead of the clock -
        // whoever finds the pet needs this, not the time.
        u8g2.setFont(u8g2_font_profont12_tr);
        u8g2.setCursor(2, 26);
        u8g2.print("Owner:");
        u8g2.setFont(u8g2_font_profont10_tr);
        u8g2.setCursor(2, 38);
        String owner = healthData.contactName;
        if (owner.length() > 18) owner = owner.substring(0, 18);
        u8g2.print(owner);
        u8g2.setCursor(2, 50);
        u8g2.print(healthData.emergencyContact);

        drawShady(105, 40, 12, homeMood, homeNow);
        drawCenteredText(62, "SafeShade S1");
    } else if (activeMode == MODE_WRIST) {
        // Wrist mode: digital-watch layout. FIXED: the round analog-style
        // bezel (two concentric circles) looked off/weird - replaced with a
        // rectangular digital-watch "case" frame, big centered digital
        // time, and the same live-reactive simulated HR "complication"
        // below a divider line (same value as SCREEN_VITALS) instead of Shady.
        u8g2.drawRFrame(14, 13, 100, 48, 6);
        u8g2.drawRFrame(17, 16, 94, 42, 4);

        u8g2.setFont(u8g2_font_logisoso18_tr);
        char timeStr[6];
        sprintf(timeStr, "%02d:%02d", clockHour, clockMinute);
        int tw = u8g2.getStrWidth(timeStr);
        u8g2.setCursor(64 - tw / 2, 40);
        u8g2.print(timeStr);

        u8g2.drawHLine(22, 45, 84);

        long motion = abs(AcX) + abs(AcY) + abs(AcZ);
        int simulatedHr = 60 + (int)constrain(motion / 800, 0, 40);
        u8g2.setFont(u8g2_font_profont10_tr);
        char hrStr[8];
        snprintf(hrStr, sizeof(hrStr), "%d bpm", simulatedHr);
        drawCenteredText(55, hrStr);
    } else {
        // Clock - left-aligned and a size down from before, freeing up the
        // right side for a bigger, more expressive Shady.
        u8g2.setFont(u8g2_font_logisoso24_tn);
        char timeStr[6];
        sprintf(timeStr, "%02d:%02d", clockHour, clockMinute);
        u8g2.setCursor(2, 44);
        u8g2.print(timeStr);

        // Helmet keeps this corner clear too - minimal-distraction UI.
        if (activeMode != MODE_HELMET) {
            drawShady(97, 34, 14, homeMood, homeNow);
        }

        // Rotating footer ticker - real, changing info instead of static text.
        u8g2.setFont(u8g2_font_profont10_tr);
        char battStr[16];
        snprintf(battStr, sizeof(battStr), "Battery %d%%", batteryLevel);
        const char* tickerMsgs[3] = { deviceDisplayName.c_str(), battStr, "Tap: next screen" };
        int tickerIdx = (homeNow / 2500) % 3;
        u8g2.setCursor(4, 62);
        u8g2.print(tickerMsgs[tickerIdx]);
    }

    drawAnimatedPulse();
    drawScreenWipeOverlay();
    u8g2.sendBuffer();
}

void drawWeatherScreen() {
    u8g2.clearBuffer();

    u8g2.setFont(u8g2_font_profont11_tr);
    drawIcon8x8(4, 2, icon_cloud);
    u8g2.setCursor(16, 10);
    u8g2.print("WEATHER");

    u8g2.drawHLine(0, 14, 118);  // Shortened for dots

    drawIcon8x8(10, 20, icon_rain);

    u8g2.setFont(u8g2_font_logisoso24_tn);
    char rainStr[4];
    sprintf(rainStr, "%d", weather.rainChance);
    int rainWidth = u8g2.getStrWidth(rainStr);
    u8g2.setCursor(24, 45);
    u8g2.print(rainStr);

    u8g2.setFont(u8g2_font_profont15_tr);
    u8g2.setCursor(24 + rainWidth + 2, 38);
    u8g2.print("%");

    u8g2.setFont(u8g2_font_profont10_tr);
    u8g2.setCursor(24, 54);
    u8g2.print("Rain");

    drawIcon8x8(70, 18, icon_sun);
    u8g2.setFont(u8g2_font_profont12_tr);
    u8g2.setCursor(82, 26);
    u8g2.print("UV ");
    u8g2.print(weather.uvIndex, 1);

    int uvBar = constrain(map(weather.uvIndex * 10, 0, 110, 0, 40), 0, 40);
    u8g2.drawFrame(70, 30, 45, 6);
    u8g2.drawBox(70, 30, uvBar, 6);

    // Real, derived risk label (not new data - just a plain-language read
    // of the existing UV index) instead of leaving the raw number to speak
    // for itself.
    u8g2.setFont(u8g2_font_profont10_tr);
    u8g2.setCursor(70, 44);
    if (weather.uvIndex < 3.0) u8g2.print("Risk: LOW");
    else if (weather.uvIndex < 6.0) u8g2.print("Risk: MED");
    else u8g2.print("Risk: HIGH");

    u8g2.setCursor(70, 56);
    u8g2.print("Humid: ");
    u8g2.print(weather.humidity, 0);
    u8g2.print("%");

    String cond = weather.condition;
    if (cond.length() > 16) cond = cond.substring(0, 16);
    u8g2.setCursor(4, 62);
    u8g2.print(cond);

    drawVerticalScreenDots(1, 5);
    drawScreenWipeOverlay();
    u8g2.sendBuffer();
}

void drawHealthScreen() {
    u8g2.clearBuffer();

    u8g2.drawRFrame(0, 0, 120, 64, 4);  // Shortened for dots
    u8g2.drawHLine(0, 13, 120);

    drawIcon8x8(4, 2, icon_heart);
    u8g2.setFont(u8g2_font_profont11_tr);
    u8g2.setCursor(16, 11);
    u8g2.print("MEDICAL ID");

    // Small EKG-style sweep in the free header space right of the title -
    // a heartbeat-cadence pulse, not tied to any real sensor (there's no
    // onboard HR hardware), just a "this ID is alive/current" flourish.
    {
        int ekgX = 78;
        int ekgY = 8;
        int beatPos = (millis() / 20) % 40;
        u8g2.drawHLine(ekgX, ekgY, 40);
        if (beatPos >= 14 && beatPos < 18) {
            u8g2.drawVLine(ekgX + beatPos, ekgY - 4, 4);
        } else if (beatPos >= 18 && beatPos < 22) {
            u8g2.drawVLine(ekgX + beatPos, ekgY, 3);
        }
    }

    u8g2.setFont(u8g2_font_profont10_tr);

    u8g2.setCursor(4, 25);
    u8g2.print("Blood: ");
    u8g2.print(healthData.bloodType);

    u8g2.setCursor(65, 25);
    u8g2.print("Age: ");
    u8g2.print(healthData.age);

    u8g2.setCursor(4, 37);
    u8g2.print("Allergy: ");
    String allergy = healthData.allergies;
    if (allergy.length() > 10) allergy = allergy.substring(0, 10);
    u8g2.print(allergy);

    u8g2.setCursor(4, 49);
    u8g2.print("ICE: ");
    u8g2.print(healthData.emergencyContact);

    u8g2.setCursor(4, 61);
    u8g2.print("Contact: ");
    u8g2.print(healthData.contactName);

    drawVerticalScreenDots(2, 5);
    drawScreenWipeOverlay();
    u8g2.sendBuffer();
}

void drawGPSScreen() {
    u8g2.clearBuffer();

    drawIcon8x8(4, 2, icon_location);
    u8g2.setFont(u8g2_font_profont11_tr);
    u8g2.setCursor(16, 10);
    u8g2.print("LOCATION");

    u8g2.drawHLine(0, 13, 118);

    u8g2.setFont(u8g2_font_profont10_tr);

    // Prefer a fresh onboard GNSS fix from the gateway board over the
    // phone-sourced `location` (BLE) data when we have one - falls back to
    // `location` automatically once the gateway fix goes stale (or was
    // never obtained), so this never regresses behavior when the gateway
    // board is absent/off.
    bool gatewayFresh = gatewayGps.fix &&
        (millis() - gatewayGps.lastPolledAtMillis) < GATEWAY_GPS_MAX_AGE_MS;

    // Bike/Helmet real-distance-and-bearing nav readout to a Guardian-set
    // destination (see ExtCallbacks' "NAV" tag) - not full routed
    // turn-by-turn, but a genuinely computed live distance/bearing rather
    // than a placeholder. Takes over the two lines that otherwise show
    // "Onboard GPS (N sats)"/"Cell:" status, which matter less than nav
    // info while actively navigating.
    bool showNav = navActive && (activeMode == MODE_BIKE || activeMode == MODE_HELMET) && gatewayFresh;

    if (gatewayFresh) {
        if (showNav) {
            double distKm = haversineKm(gatewayGps.lat, gatewayGps.lon, navDestLat, navDestLon);
            double brg = bearingDeg(gatewayGps.lat, gatewayGps.lon, navDestLat, navDestLon);

            u8g2.setCursor(4, 25);
            String label = navDestLabel.length() > 0 ? navDestLabel : "Destination";
            if (label.length() > 16) label = label.substring(0, 16);
            u8g2.print("Nav: ");
            u8g2.print(label);

            u8g2.setCursor(4, 35);
            u8g2.print(distKm, 2);
            u8g2.print(" km ");
            u8g2.print(bearingCompass(brg));
        } else {
            u8g2.setCursor(4, 25);
            u8g2.print("Onboard GPS (");
            u8g2.print(gatewayGps.satellites);
            u8g2.print(" sats)");

            // Purely additive cellular status line - independent of the GPS
            // fix itself, only shown when the gateway's /status poll is fresh.
            bool gatewayStatusFresh = gatewayStatus.reachable &&
                (millis() - gatewayStatus.lastPolledAtMillis) < GATEWAY_STATUS_MAX_AGE_MS;
            if (gatewayStatusFresh) {
                u8g2.setCursor(4, 35);
                u8g2.print("Cell: ");
                u8g2.print(gatewayStatus.networkReady ? "OK" : "No signal");
            }
        }

        u8g2.setCursor(4, 47);
        u8g2.print("LAT: ");
        u8g2.print(gatewayGps.lat, 5);

        u8g2.setCursor(4, 57);
        u8g2.print("LON: ");
        u8g2.print(gatewayGps.lon, 5);

        u8g2.setCursor(80, 47);
        u8g2.print("ALT:");
        u8g2.setCursor(80, 57);
        u8g2.print(gatewayGps.altitude, 0);
        u8g2.print("m");

    } else if (location.valid) {
        if (location.locationName.length() > 0 && location.locationName != "Syncing...") {
            String locName = location.locationName;
            if (locName.length() > 18) locName = locName.substring(0, 18);
            u8g2.setCursor(4, 25);
            u8g2.print(locName);
        }

        if (location.locality.length() > 0) {
            u8g2.setCursor(4, 35);
            u8g2.print(location.locality);
        }

        u8g2.setCursor(4, 47);
        u8g2.print("LAT: ");
        u8g2.print(location.lat, 5);

        u8g2.setCursor(4, 57);
        u8g2.print("LON: ");
        u8g2.print(location.lon, 5);

        u8g2.setCursor(80, 47);
        u8g2.print("ALT:");
        u8g2.setCursor(80, 57);
        u8g2.print(location.altitude);
        u8g2.print("m");

    } else {
        u8g2.setCursor(15, 35);
        u8g2.print("Waiting for sync...");

        int dots = (millis() / 300) % 4;
        for (int i = 0; i < 3; i++) {
            if (i < dots) {
                u8g2.drawDisc(45 + i * 12, 48, 3);
            } else {
                u8g2.drawCircle(45 + i * 12, 48, 3);
            }
        }
    }

    drawVerticalScreenDots(3, 5);
    drawScreenWipeOverlay();
    u8g2.sendBuffer();
}

// FIXED: Message screen renamed to "MESSAGES" with timeline (Issue #5)
void drawMessageScreen() {
    u8g2.clearBuffer();

    // Header - renamed from "GUARDIAN MSG" to "MESSAGES"
    drawIcon8x8(4, 1, icon_message);
    u8g2.setFont(u8g2_font_profont11_tr);
    u8g2.setCursor(16, 9);
    u8g2.print("MESSAGES");

    // New message indicator
    if (hasNewMessage && (millis() / 300) % 2 == 0) {
        u8g2.drawDisc(115, 6, 3);
    }

    u8g2.drawHLine(0, 12, 118);

    // Check if in reply mode
    if (inReplyMode) {
        u8g2.setFont(u8g2_font_profont11_tr);
        u8g2.setCursor(4, 24);
        u8g2.print("QUICK REPLY:");

        u8g2.drawRFrame(4, 28, 112, 24, 3);

        u8g2.setFont(u8g2_font_profont12_tr);
        String reply = quickReplies[selectedReply];
        int replyWidth = u8g2.getStrWidth(reply.c_str());
        u8g2.setCursor((118 - replyWidth) / 2, 44);
        u8g2.print(reply);

        u8g2.setFont(u8g2_font_profont10_tr);
        u8g2.setCursor(8, 44);
        u8g2.print("<");
        u8g2.setCursor(108, 44);
        u8g2.print(">");

        u8g2.setCursor(4, 60);
        u8g2.print("TAP:Next  HOLD:Send");

    } else {
        // MESSAGE TIMELINE VIEW (Issue #5)
        u8g2.setFont(u8g2_font_profont10_tr);
        
        int y = 24;
        int displayedMessages = 0;
        
        for (int i = 0; i < messageCount && displayedMessages < 3 && y < 58; i++) {
            if (!messageHistory[i].valid) continue;
            
            String msg = messageHistory[i].text;
            if (msg.length() > 18) msg = msg.substring(0, 18) + "..";
            
            if (messageHistory[i].fromGuardian) {
                // Guardian message - left aligned with ">"
                u8g2.setCursor(2, y);
                u8g2.print(">");
                u8g2.setCursor(10, y);
                u8g2.print(msg);
            } else {
                // Sent reply - right aligned with "<"
                int msgWidth = u8g2.getStrWidth(msg.c_str());
                u8g2.setCursor(110 - msgWidth, y);
                u8g2.print(msg);
                u8g2.setCursor(112, y);
                u8g2.print("<");
            }
            
            y += 11;
            displayedMessages++;
        }
        
        if (messageCount == 0) {
            u8g2.setCursor(20, 38);
            u8g2.print("No messages");
        }

        // Reply hint at bottom
        if (messageCount > 0) {
            u8g2.setFont(u8g2_font_profont10_tr);
            u8g2.setCursor(4, 62);
            u8g2.print("Hold to reply");
        }

        // Shady briefly celebrates a just-arrived message, then settles.
        if (hasNewMessage && (millis() - messageReceivedTime) < 1500) {
            drawShady(108, 50, 8, SHADY_EXCITED, millis());
        }
    }

    drawVerticalScreenDots(4, 5);
    drawScreenWipeOverlay();
    u8g2.sendBuffer();
}

// FIXED: Modern centered RGB menu (Issue #8)
void drawRGBMenu() {
    u8g2.clearBuffer();

    // Title
    u8g2.setFont(u8g2_font_profont12_tr);
    drawCenteredText(12, "LIGHT MODE");

    u8g2.drawHLine(20, 16, 88);

    // Current pattern in a nice box
    u8g2.drawRFrame(14, 22, 100, 28, 6);

    // Pattern name - perfectly centered
    u8g2.setFont(u8g2_font_helvB14_tr);
    const char* patternName = rgbPatternNames[rgbPattern];
    int nameWidth = u8g2.getStrWidth(patternName);
    u8g2.setCursor((128 - nameWidth) / 2, 40);
    u8g2.print(patternName);

    // Navigation arrows
    u8g2.setFont(u8g2_font_profont15_tr);
    u8g2.setCursor(20, 38);
    u8g2.print("<");
    u8g2.setCursor(104, 38);
    u8g2.print(">");

    // Retro "now playing" level meter - purely decorative, staggered sine
    // bars, cassette-deck-VU-meter flourish under the pattern name.
    unsigned long meterTick = millis();
    for (int i = 0; i < 7; i++) {
        int barH = 2 + (int)((sin(meterTick / 150.0 + i * 0.9) + 1.0) * 2.0);
        u8g2.drawVLine(35 + i * 8, 48 - barH, barH);
    }

    // Pattern indicator dots - vertical stack on the right, same visual
    // language as the screen-cycle pagination dots elsewhere (not a bottom
    // horizontal row, which read as less clearly "more of these below/above").
    drawVerticalScreenDots(rgbPattern, RGB_COUNT);

    // Instructions
    u8g2.setFont(u8g2_font_profont10_tr);
    drawCenteredText(64, "TAP: change  2x: exit");

    u8g2.sendBuffer();
}

void drawSOSScreen() {
    u8g2.clearBuffer();
    drawShady(117, 11, 9, SHADY_PANICKED, millis());

    if ((millis() / 300) % 2 == 0) {
        u8g2.setFont(u8g2_font_logisoso32_tr);
        drawCenteredText(45, "SOS");
    } else {
        u8g2.setFont(u8g2_font_profont12_tr);
        drawCenteredText(35, "EMERGENCY ALERT");
        drawCenteredText(50, "Hold to cancel");
    }

    u8g2.setFont(u8g2_font_profont10_tr);
    if (!sosAlertSent) {
        // Lock-in countdown - see SOS_ALERT_LOCKIN_MS. Gives the wearer a
        // clear window to cancel an accidental long-press before any SMS
        // actually goes out.
        long elapsed = (long)(millis() - sosEnteredAtMillis);
        long remainingMs = (long)SOS_ALERT_LOCKIN_MS - elapsed;
        if (remainingMs < 0) remainingMs = 0;
        char buf[24];
        snprintf(buf, sizeof(buf), "Auto SMS in %lds", (remainingMs / 1000) + 1);
        u8g2.setCursor(2, 62);
        u8g2.print(buf);
    } else {
        // Same live SMS-status confirmation as drawFallScreen(), once the
        // countdown above has actually fired triggerGatewayAlert().
        bool gatewayStatusFresh = gatewayStatus.reachable &&
            (millis() - gatewayStatus.lastPolledAtMillis) < GATEWAY_STATUS_MAX_AGE_MS;
        if (gatewayStatusFresh) {
            u8g2.setCursor(2, 62);
            if (gatewayStatus.smsState == "sending") {
                u8g2.print("SMS...");
            } else if (gatewayStatus.smsState == "sent") {
                u8g2.print("SMS OK");
            } else if (gatewayStatus.smsState == "failed") {
                u8g2.print("SMS FAIL");
            }
        }
    }

    u8g2.sendBuffer();
}

void drawFallScreen() {
    u8g2.clearBuffer();
    drawShady(116, 50, 8, SHADY_PANICKED, millis());

    u8g2.setFont(u8g2_font_logisoso18_tr);
    drawCenteredText(30, "FALL");
    drawCenteredText(50, "DETECTED");

    u8g2.setFont(u8g2_font_profont10_tr);
    if (concussionConfirmPending) {
        // Helmet's two-step confirm, second half - see the button handler.
        drawCenteredText(62, "Confirm OK? Press again");
    } else {
        drawCenteredText(62, "Press to dismiss");
    }

    // Live confirmation that the parallel cellular SMS (triggerGatewayAlert(),
    // fired the moment this screen was entered) actually went out - only
    // shown while the gateway's /status poll is fresh, so it stays silent
    // rather than misleading when the gateway is off/unreachable.
    bool gatewayStatusFresh = gatewayStatus.reachable &&
        (millis() - gatewayStatus.lastPolledAtMillis) < GATEWAY_STATUS_MAX_AGE_MS;
    if (gatewayStatusFresh) {
        u8g2.setCursor(2, 7);
        if (gatewayStatus.smsState == "sending") {
            u8g2.print("SMS...");
        } else if (gatewayStatus.smsState == "sent") {
            u8g2.print("SMS OK");
        } else if (gatewayStatus.smsState == "failed") {
            u8g2.print("SMS FAIL");
        }
    }

    if ((millis() / 200) % 2 == 0) {
        u8g2.drawTriangle(64, 8, 54, 20, 74, 20);
    }

    u8g2.sendBuffer();
}

// ==========================================
// 12b. ADAPTIVE-MODE PRIORITY SCREENS
// ==========================================

/** Bike: real distance/duration from successive gateway GPS fixes
 * (updateRideStats() in loop()), plus a live lean-angle read straight off
 * the accelerometer. */
void drawRideStatsScreen() {
    u8g2.clearBuffer();
    u8g2.setFont(u8g2_font_profont11_tr);
    u8g2.setCursor(4, 10);
    u8g2.print("RIDE STATS");
    u8g2.drawHLine(0, 13, 128);

    u8g2.setFont(u8g2_font_profont10_tr);
    if (rideActive) {
        unsigned long elapsed = (millis() - rideStartedAtMillis) / 1000;
        char buf[24];
        snprintf(buf, sizeof(buf), "Time: %02lu:%02lu", elapsed / 60, elapsed % 60);
        u8g2.setCursor(4, 28);
        u8g2.print(buf);

        u8g2.setCursor(4, 40);
        u8g2.print("Distance: ");
        u8g2.print(rideDistanceKm, 2);
        u8g2.print(" km");
    } else {
        u8g2.setCursor(4, 28);
        u8g2.print("Waiting for GPS fix");
        u8g2.setCursor(4, 40);
        u8g2.print("to start ride...");
    }

    float leanDeg = degrees(atan2((float)AcY, (float)AcZ));
    u8g2.setCursor(4, 54);
    u8g2.print("Lean: ");
    u8g2.print((int)abs(leanDeg));
    u8g2.print(" deg");

    u8g2.sendBuffer();
}

/** Pet: real active-minutes tally (integrated motion) + connection status. */
void drawActivityScreen() {
    u8g2.clearBuffer();
    u8g2.setFont(u8g2_font_profont11_tr);
    u8g2.setCursor(4, 10);
    u8g2.print("ACTIVITY");
    u8g2.drawHLine(0, 13, 128);

    u8g2.setFont(u8g2_font_profont10_tr);
    u8g2.setCursor(4, 30);
    u8g2.print("Active today: ");
    u8g2.print(activeSecondsToday / 60);
    u8g2.print(" min");

    u8g2.setCursor(4, 44);
    u8g2.print(deviceConnected ? "With you :)" : "Away from phone!");

    u8g2.sendBuffer();
}

/** Wrist: watch-face-adjacent vitals view - SIMULATED HR/SpO2 (there's no
 * real HR/SpO2 sensor on this board; the value is live-reactive to real
 * motion rather than a static fake number) + a REAL sleep tally (reuses
 * the existing SCREEN_SLEEP mechanism, just totaled - see loop()). */
void drawVitalsScreen() {
    u8g2.clearBuffer();
    u8g2.setFont(u8g2_font_profont11_tr);
    u8g2.setCursor(4, 10);
    u8g2.print("VITALS");
    u8g2.drawHLine(0, 13, 128);

    long motion = abs(AcX) + abs(AcY) + abs(AcZ);
    int simulatedHr = 60 + (int)constrain(motion / 800, 0, 40);
    int simulatedSpo2 = 96 + (int)((millis() / 3000) % 3);

    u8g2.setFont(u8g2_font_logisoso18_tr);
    u8g2.setCursor(8, 38);
    u8g2.print(simulatedHr);
    u8g2.setFont(u8g2_font_profont10_tr);
    u8g2.setCursor(46, 38);
    u8g2.print("bpm");

    u8g2.setCursor(4, 52);
    u8g2.print("SpO2: ");
    u8g2.print(simulatedSpo2);
    u8g2.print("%");

    u8g2.setCursor(4, 62);
    u8g2.print("Sleep: ");
    u8g2.print(sleepSecondsToday / 60);
    u8g2.print(" min");

    u8g2.sendBuffer();
}

/** Helmet: real fall-trigger count/recency for this mode's higher-stakes
 * impact threshold - see the two-step concussion confirm flow in loop(). */
void drawImpactLogScreen() {
    u8g2.clearBuffer();
    u8g2.setFont(u8g2_font_profont11_tr);
    u8g2.setCursor(4, 10);
    u8g2.print("IMPACT LOG");
    u8g2.drawHLine(0, 13, 128);

    u8g2.setFont(u8g2_font_profont10_tr);
    u8g2.setCursor(4, 30);
    u8g2.print("Today: ");
    u8g2.print(impactCountToday);
    u8g2.print(" impact(s)");

    u8g2.setCursor(4, 44);
    if (lastImpactMillis > 0) {
        unsigned long agoSec = (millis() - lastImpactMillis) / 1000;
        u8g2.print("Last: ");
        u8g2.print(agoSec);
        u8g2.print("s ago");
    } else {
        u8g2.print("No impacts logged");
    }

    // Scheduled worker check-in - see EXT "CHECKIN:" and the trigger/
    // escalation logic in loop().
    if (checkinDueNow) {
        if ((millis() / 300) % 2 == 0) {
            u8g2.setCursor(4, 58);
            u8g2.print("CHECK-IN DUE - press");
        }
    } else if (checkinIntervalMs > 0) {
        unsigned long remaining = (lastCheckinAtMillis + checkinIntervalMs > millis())
            ? (lastCheckinAtMillis + checkinIntervalMs - millis()) / 1000 : 0;
        u8g2.setCursor(4, 58);
        u8g2.print("Next check-in: ");
        u8g2.print(remaining / 60);
        u8g2.print("m");
    }

    u8g2.sendBuffer();
}

/** Kids: geofence status - populated once the app's GeofencingClient is
 * wired to send EXT "GEOFENCE:" (firmware side already accepts it). */
void drawSafeZoneScreen() {
    u8g2.clearBuffer();
    u8g2.setFont(u8g2_font_profont11_tr);
    drawIcon8x8(4, 2, icon_location);
    u8g2.setCursor(16, 10);
    u8g2.print("SAFE ZONE");
    u8g2.drawHLine(0, 13, 128);

    u8g2.setFont(u8g2_font_profont10_tr);
    if (geofenceKnown) {
        String zone = geofenceZoneName;
        if (zone.length() > 18) zone = zone.substring(0, 18);
        u8g2.setCursor(4, 32);
        u8g2.print(zone);
        u8g2.setCursor(4, 46);
        u8g2.print(geofenceInside ? "Status: INSIDE" : "Status: OUTSIDE");
    } else {
        u8g2.setCursor(4, 34);
        u8g2.print("No zone set yet");
    }

    u8g2.sendBuffer();
}

/** Elderly: medication reminder - scheduled via EXT "MED:HH:MM" (see
 * ExtCallbacks and the trigger check in loop()). Bigger font throughout,
 * matching this mode's larger-text UI treatment. */
void drawMedicationScreen() {
    u8g2.clearBuffer();
    u8g2.setFont(u8g2_font_profont11_tr);
    u8g2.setCursor(4, 10);
    u8g2.print("MEDICATION");
    u8g2.drawHLine(0, 13, 128);

    u8g2.setFont(u8g2_font_profont12_tr);
    if (medReminderHour >= 0) {
        char buf[16];
        snprintf(buf, sizeof(buf), "Next: %02d:%02d", medReminderHour, medReminderMinute);
        u8g2.setCursor(4, 34);
        u8g2.print(buf);
    } else {
        u8g2.setCursor(4, 34);
        u8g2.print("No reminders set");
    }

    if (medReminderDueNow) {
        u8g2.setFont(u8g2_font_profont10_tr);
        drawCenteredText(52, "Press to dismiss");
    }

    u8g2.sendBuffer();
}

/** On-device mirror of the app's Adaptive Mode picker - triple-click from
 * Home to enter, single tap cycles a PREVIEW (doesn't apply anything yet),
 * hold ~800ms confirms via applyMode(). Shady's mood previews a hint of
 * that mode's personality. Auto-returns to Home if left idle. */
void drawModeSelectScreen() {
    u8g2.clearBuffer();
    u8g2.setFont(u8g2_font_profont12_tr);
    drawCenteredText(10, "ADAPTIVE MODE");
    u8g2.drawHLine(10, 14, 108);

    u8g2.setFont(u8g2_font_helvB14_tr);
    drawCenteredText(28, modeName(modeSelectPreview));

    ShadyMood previewMood = SHADY_HAPPY;
    switch (modeSelectPreview) {
        case MODE_KIDS:   previewMood = SHADY_EXCITED; break;
        case MODE_HELMET: previewMood = SHADY_IDLE;    break;
        case MODE_WRIST:  previewMood = SHADY_IDLE;    break;
        default:          previewMood = SHADY_HAPPY;   break;
    }
    drawShady(64, 40, 9, previewMood, millis());

    // Vertical stack on the right, same convention as the Light Mode
    // menu's pattern dots and the screen-cycle pagination dots.
    drawVerticalScreenDots((int)modeSelectPreview, 7);

    u8g2.setFont(u8g2_font_profont10_tr);
    drawCenteredText(62, "TAP:cycle HOLD:confirm");

    u8g2.sendBuffer();
}

// ==========================================
// 13. RGB PATTERN FUNCTIONS
// ==========================================
void updateRGBPattern() {
    strip.clear();
    unsigned long tick = millis();

    switch (rgbPattern) {
        case RGB_TORCH: {
            // LDR-aware brightness: full output in the dark, dimmer (still
            // on) in bright ambient light where a torch is less needed -
            // real sensor input driving the output, not a fixed intensity.
            int torchLevel = map(constrain(lightVal, 0, 4095), 0, 4095, 255, 90);
            for (int i = 0; i < NUM_LEDS; i++) {
                strip.setPixelColor(i, strip.Color(torchLevel, torchLevel, torchLevel));
            }
            setHeadlamp(true);
            break;
        }

        case RGB_RAINBOW:
            for (int i = 0; i < NUM_LEDS; i++) {
                strip.setPixelColor(i, strip.ColorHSV((i * 65536L / NUM_LEDS) + (tick * 30)));
            }
            setHeadlamp(false);
            break;

        case RGB_CYBER:
            {
                int pos = (tick / 100) % NUM_LEDS;
                strip.setPixelColor(pos, strip.Color(150, 0, 255));
                strip.setPixelColor((pos + 1) % NUM_LEDS, strip.Color(80, 0, 150));
                strip.setPixelColor((pos + 2) % NUM_LEDS, strip.Color(30, 0, 80));
            }
            setHeadlamp(false);
            break;

        case RGB_POLICE:
            if ((tick / 150) % 2 == 0) {
                for (int i = 0; i < NUM_LEDS / 2; i++) {
                    strip.setPixelColor(i, strip.Color(255, 0, 0));
                }
            } else {
                for (int i = NUM_LEDS / 2; i < NUM_LEDS; i++) {
                    strip.setPixelColor(i, strip.Color(0, 0, 255));
                }
            }
            setHeadlamp(false);
            break;

        case RGB_FIRE:
            {
                static unsigned long lastFireUpdate = 0;
                static uint8_t fireColors[NUM_LEDS][3];

                if (tick - lastFireUpdate > 150) {
                    lastFireUpdate = tick;
                    for (int i = 0; i < NUM_LEDS; i++) {
                        if (random(10) > 3) {
                            fireColors[i][0] = 255;
                            fireColors[i][1] = random(50, 150);
                            fireColors[i][2] = 0;
                        } else {
                            fireColors[i][0] = 200;
                            fireColors[i][1] = random(20, 80);
                            fireColors[i][2] = 0;
                        }
                    }
                }

                for (int i = 0; i < NUM_LEDS; i++) {
                    strip.setPixelColor(i, strip.Color(fireColors[i][0], fireColors[i][1], fireColors[i][2]));
                }
            }
            setHeadlamp(false);
            break;

        case RGB_OCEAN:
            for (int i = 0; i < NUM_LEDS; i++) {
                int wave = sin((tick / 200.0) + (i * 0.8)) * 127 + 128;
                strip.setPixelColor(i, strip.Color(0, wave / 2, wave));
            }
            setHeadlamp(false);
            break;

        case RGB_PULSE:
            {
                int brightness = (sin(tick / 500.0) * 127 + 128);
                for (int i = 0; i < NUM_LEDS; i++) {
                    strip.setPixelColor(i, strip.Color(brightness, brightness / 2, brightness));
                }
            }
            setHeadlamp(false);
            break;
    }

    strip.show();
}

// ==========================================
// 13b. GNSS/CELLULAR GATEWAY CLIENT (WiFi, fail-open)
//
// Polls the separate gateway board (SafeShade_Gateway.ino) for a real GPS
// fix and, on a fall, asks it to send an SMS. Every call here is guarded
// and short-timeout - if the gateway is off or unreachable, these
// functions return quickly having done nothing, and the rest of the
// firmware (BLE to the phone, OLED, fall detection) is completely
// unaffected. This must stay that way - never make the core safety flow
// depend on WiFi/the gateway being present.
// ==========================================

/**
 * Non-blocking-ish (short HTTP timeout) poll of the gateway's /gps
 * endpoint. Only updates gatewayGps on a successful, parseable response -
 * on any failure it's left untouched, so drawGPSScreen()'s staleness check
 * (GATEWAY_GPS_MAX_AGE_MS) is what naturally falls back to phone-sourced
 * `location` rather than this function needing to know about that itself.
 */
void pollGatewayGps() {
    if (WiFi.status() != WL_CONNECTED) return;

    HTTPClient http;
    http.setConnectTimeout(1500);
    http.setTimeout(2000);
    if (!http.begin(String(GATEWAY_BASE_URL) + "/gps")) {
        Serial.println("[GW] /gps http.begin() failed");
        return;
    }

    int code = http.GET();
    Serial.print("[GW] /gps -> ");
    Serial.println(code);
    if (code == 200) {
        String body = http.getString();

        // Tiny hand-rolled JSON field extraction - avoids pulling in a JSON
        // library for 5 known, fixed-shape fields. Not a general parser.
        auto extractNumber = [&](const char* key) -> double {
            int keyIdx = body.indexOf(key);
            if (keyIdx == -1) return 0.0;
            int colonIdx = body.indexOf(':', keyIdx);
            if (colonIdx == -1) return 0.0;
            int endIdx = body.indexOf(',', colonIdx);
            int endIdx2 = body.indexOf('}', colonIdx);
            if (endIdx == -1 || (endIdx2 != -1 && endIdx2 < endIdx)) endIdx = endIdx2;
            if (endIdx == -1) return 0.0;
            return body.substring(colonIdx + 1, endIdx).toDouble();
        };

        bool fix = body.indexOf("\"fix\":true") != -1;
        if (fix) {
            gatewayGps.lat = extractNumber("\"lat\"");
            gatewayGps.lon = extractNumber("\"lon\"");
            gatewayGps.altitude = extractNumber("\"altitude\"");
            gatewayGps.satellites = (int)extractNumber("\"satellites\"");
            gatewayGps.fix = true;
            gatewayGps.lastPolledAtMillis = millis();

            if (activeMode == MODE_BIKE) {
                updateRideStats(gatewayGps.lat, gatewayGps.lon);
            }
        }
    }
    http.end();
}

/**
 * Bike mode's Ride Stats: real distance accumulated from successive
 * gateway GPS fixes (haversine great-circle distance between consecutive
 * fixes), not fabricated - the first fix of a ride just anchors the
 * starting point without adding distance.
 */
void updateRideStats(double lat, double lon) {
    unsigned long now = millis();
    if (!rideActive) {
        rideActive = true;
        rideStartedAtMillis = now;
        rideDistanceKm = 0.0;
        rideLastLat = lat;
        rideLastLon = lon;
        rideLastFixMillis = now;
        return;
    }

    const double R_KM = 6371.0;
    double lat1 = radians(rideLastLat), lat2 = radians(lat);
    double dLat = radians(lat - rideLastLat);
    double dLon = radians(lon - rideLastLon);
    double a = sin(dLat / 2) * sin(dLat / 2) +
               cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2);
    double c = 2 * atan2(sqrt(a), sqrt(1 - a));
    double segmentKm = R_KM * c;

    // Ignore GPS jitter while stationary (a stale/noisy fix can otherwise
    // add phantom "distance" even with the rider standing still).
    if (segmentKm > 0.005) {
        rideDistanceKm += segmentKm;
    }
    rideLastLat = lat;
    rideLastLon = lon;
    rideLastFixMillis = now;
}

/** Great-circle distance in km between two coordinates - same haversine
 * math as updateRideStats() above, factored out so Bike/Helmet nav can
 * reuse it for a real (not fabricated) distance-to-destination readout. */
double haversineKm(double lat1, double lon1, double lat2, double lon2) {
    const double R_KM = 6371.0;
    double la1 = radians(lat1), la2 = radians(lat2);
    double dLat = radians(lat2 - lat1);
    double dLon = radians(lon2 - lon1);
    double a = sin(dLat / 2) * sin(dLat / 2) +
               cos(la1) * cos(la2) * sin(dLon / 2) * sin(dLon / 2);
    double c = 2 * atan2(sqrt(a), sqrt(1 - a));
    return R_KM * c;
}

/** Initial compass bearing (0-360, true north) from point 1 to point 2. */
double bearingDeg(double lat1, double lon1, double lat2, double lon2) {
    double la1 = radians(lat1), la2 = radians(lat2);
    double dLon = radians(lon2 - lon1);
    double y = sin(dLon) * cos(la2);
    double x = cos(la1) * sin(la2) - sin(la1) * cos(la2) * cos(dLon);
    double deg = degrees(atan2(y, x));
    return deg < 0 ? deg + 360.0 : deg;
}

/** 8-point compass label for a bearing in degrees. */
const char* bearingCompass(double deg) {
    static const char* labels[8] = { "N", "NE", "E", "SE", "S", "SW", "W", "NW" };
    int idx = (int)((deg + 22.5) / 45.0) % 8;
    return labels[idx];
}

/** Kids mode quiet hours - real Guardian-set window (see ExtCallbacks'
 * "QUIET" tag), wraps past midnight when startHour > endHour. Disabled
 * (-1) means never quiet. Only ever gates the non-critical incoming-
 * message chime - fall/SOS alerts are never suppressed by this. */
bool inQuietHours() {
    if (quietStartHour < 0 || quietEndHour < 0) return false;
    if (quietStartHour == quietEndHour) return false;
    if (quietStartHour < quietEndHour) {
        return clockHour >= quietStartHour && clockHour < quietEndHour;
    }
    return clockHour >= quietStartHour || clockHour < quietEndHour;
}

/** Fire-and-forget ping to the gateway's /alert endpoint - asks it to send a real SMS with the last known GPS fix. Never blocks/delays the BLE alert path above it. */
void triggerGatewayAlert() {
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("[GW] /alert skipped - WiFi not connected");
        return;
    }

    HTTPClient http;
    http.setConnectTimeout(1000);
    http.setTimeout(1500);
    if (!http.begin(String(GATEWAY_BASE_URL) + "/alert")) {
        Serial.println("[GW] /alert http.begin() failed");
        return;
    }
    int code = http.POST("");
    Serial.print("[GW] /alert -> ");
    Serial.println(code);
    http.end();
}

/**
 * Relays a Guardian reply (physical quick-reply, or an app-sent Companion
 * reply arriving over BLE) out via the gateway's real SMS channel, in
 * parallel with the existing BLE notify - so a reply reaches the Guardian
 * even when their phone isn't the one currently BLE-connected to this
 * device (the normal case: the wearer's replies are meant for someone
 * else's phone). Same fail-open contract as triggerGatewayAlert().
 */
void triggerGatewayReply(const String &text) {
    if (WiFi.status() != WL_CONNECTED) return;

    HTTPClient http;
    http.setConnectTimeout(1000);
    http.setTimeout(1500);
    if (!http.begin(String(GATEWAY_BASE_URL) + "/reply")) return;
    http.addHeader("Content-Type", "text/plain");
    int code = http.POST(text);
    Serial.print("[GW] /reply -> ");
    Serial.println(code);
    http.end();
}

/**
 * Purely informational poll of the gateway's /status endpoint (modem/
 * network/SMS state) - drives OLED status indicators only. Same
 * fail-open contract as pollGatewayGps(): on any failure gatewayStatus
 * is left untouched and `reachable` naturally goes stale via
 * GATEWAY_STATUS_MAX_AGE_MS, so a missing gateway just makes the new
 * indicators disappear rather than show wrong info.
 */
void pollGatewayStatus() {
    if (WiFi.status() != WL_CONNECTED) return;

    HTTPClient http;
    http.setConnectTimeout(1000);
    http.setTimeout(1500);
    if (!http.begin(String(GATEWAY_BASE_URL) + "/status")) {
        Serial.println("[GW] /status http.begin() failed");
        return;
    }

    int code = http.GET();
    Serial.print("[GW] /status -> ");
    Serial.println(code);
    if (code == 200) {
        String body = http.getString();

        auto extractBool = [&](const char* key) -> bool {
            return body.indexOf(String(key) + ":true") != -1;
        };
        auto extractInt = [&](const char* key) -> int {
            int keyIdx = body.indexOf(key);
            if (keyIdx == -1) return -1;
            int colonIdx = body.indexOf(':', keyIdx);
            if (colonIdx == -1) return -1;
            int endIdx = body.indexOf(',', colonIdx);
            int endIdx2 = body.indexOf('}', colonIdx);
            if (endIdx == -1 || (endIdx2 != -1 && endIdx2 < endIdx)) endIdx = endIdx2;
            if (endIdx == -1) return -1;
            return body.substring(colonIdx + 1, endIdx).toInt();
        };
        auto extractString = [&](const char* key) -> String {
            int keyIdx = body.indexOf(key);
            if (keyIdx == -1) return "";
            int startQ = body.indexOf('"', body.indexOf(':', keyIdx) + 1);
            int endQ = body.indexOf('"', startQ + 1);
            if (startQ == -1 || endQ == -1) return "";
            return body.substring(startQ + 1, endQ);
        };

        gatewayStatus.networkReady = extractBool("\"networkReady\"");
        gatewayStatus.csq = extractInt("\"csq\"");
        gatewayStatus.smsState = extractString("\"smsState\"");
        gatewayStatus.reachable = true;
        gatewayStatus.lastPolledAtMillis = millis();
    }
    http.end();
}

/**
 * Extracts a JSON string value for `key`, honoring backslash-escapes
 * (\" and \n specifically - the only ones the gateway's /messages
 * endpoint emits) - unlike pollGatewayStatus()'s local extractString()
 * lambda, this is safe for arbitrary free-text SMS bodies that can
 * contain quotes/newlines, not just the fixed enum-like status strings.
 */
String extractJsonString(const String &body, const char* key) {
    int keyIdx = body.indexOf(key);
    if (keyIdx == -1) return "";
    int colonIdx = body.indexOf(':', keyIdx);
    if (colonIdx == -1) return "";
    int startQ = body.indexOf('"', colonIdx);
    if (startQ == -1) return "";

    String result;
    int i = startQ + 1;
    while (i < (int)body.length()) {
        char c = body.charAt(i);
        if (c == '\\' && i + 1 < (int)body.length()) {
            char next = body.charAt(i + 1);
            result += (next == 'n') ? '\n' : next;
            i += 2;
            continue;
        }
        if (c == '"') break;
        result += c;
        i++;
    }
    return result;
}

/**
 * SMS allowlist filter, used by pollGatewayMessages() below: compares the
 * message's `from` field's last 10 digits (stripped of any non-digit
 * formatting, e.g. leading "+91") against each smsAllowlist[] entry's last
 * 10 digits the same way, so "+91 89173 60065" vs "8917360065" vs
 * "91-89173-60065" all match. smsAllowlistCount == 0 means no filter has
 * ever been pushed - allow everything, same as before this feature
 * existed.
 */
bool isSmsSenderAllowed(const String &from) {
    if (smsAllowlistCount == 0) return true;

    String fromDigits = "";
    for (int i = 0; i < (int)from.length(); i++) {
        if (isDigit(from.charAt(i))) fromDigits += from.charAt(i);
    }
    String fromLast10 = fromDigits.length() > 10 ?
        fromDigits.substring(fromDigits.length() - 10) : fromDigits;
    if (fromLast10.length() == 0) return false;

    for (int i = 0; i < smsAllowlistCount; i++) {
        String entry = String(smsAllowlist[i]);
        String entryDigits = "";
        for (int j = 0; j < (int)entry.length(); j++) {
            if (isDigit(entry.charAt(j))) entryDigits += entry.charAt(j);
        }
        String entryLast10 = entryDigits.length() > 10 ?
            entryDigits.substring(entryDigits.length() - 10) : entryDigits;
        if (entryLast10.length() > 0 && entryLast10 == fromLast10) return true;
    }
    return false;
}

/**
 * Two-way messaging: polls the gateway for the oldest pending incoming
 * SMS (from the app, when BLE is out of range, or from literally any
 * other phone texting the SIM directly) and, if there is one, feeds it
 * into the exact same message-arrival path a BLE-delivered Guardian
 * message uses (see MessageCallbacks::onWrite) - buzzes, timelines it,
 * and switches to the Messages screen. The sender's last 4 digits are
 * prefixed so the wearer can tell it came from a real phone number.
 */
void pollGatewayMessages() {
    if (WiFi.status() != WL_CONNECTED) return;

    HTTPClient http;
    http.setConnectTimeout(1500);
    http.setTimeout(2000);
    if (!http.begin(String(GATEWAY_BASE_URL) + "/messages")) return;

    int code = http.GET();
    if (code == 200) {
        String body = http.getString();
        if (body.indexOf("\"pending\":true") != -1) {
            String from = extractJsonString(body, "\"from\"");
            String text = extractJsonString(body, "\"text\"");
            // The gateway's handleMessages() already popped this message
            // off its queue before responding, so it's "consumed" either
            // way - a filtered-out sender is simply not shown, never left
            // fetched-but-unprocessed / re-fetched.
            if (text.length() > 0 && isSmsSenderAllowed(from)) {
                String tag = from.length() >= 4 ? from.substring(from.length() - 4) : from;
                String displayText = tag.length() > 0 ? ("[" + tag + "] " + text) : text;

                guardianMessage = displayText;
                hasNewMessage = true;
                messageReceivedTime = millis();
                addMessage(displayText, true);

                // Kids quiet hours mute the chime only - message still shows.
                if (!(activeMode == MODE_KIDS && inQuietHours())) {
                    tone(PIN_BUZZER, 1000, 100);
                    delay(110);
                    tone(PIN_BUZZER, 1500, 100);
                    delay(110);
                    tone(PIN_BUZZER, 2000, 150);
                }

                currentScreen = SCREEN_MESSAGE;
            }
        }
    }
    http.end();
}

// ==========================================
// 14. SETUP
// ==========================================
void setup() {
    Serial.begin(115200);

    pinMode(PIN_LDR, INPUT);
    pinMode(PIN_LED, OUTPUT);
    pinMode(PIN_BUZZER, OUTPUT);
    pinMode(PIN_BTN, INPUT_PULLUP);

    Wire.begin(SDA_PIN, SCL_PIN);
    // FIXED: without a timeout, a stuck MPU6050 I2C bus (e.g. after
    // idle/sleep) hangs loop() forever with zero recovery path - a wedged
    // bus now times out instead of requiring a power-cycle.
    Wire.setTimeout(1000);

    Wire.beginTransmission(MPU_ADDR);
    Wire.write(0x6B);
    Wire.write(0);
    Wire.endTransmission(true);

    u8g2.begin();
    u8g2.enableUTF8Print();
    u8g2.setContrast(200);

    strip.begin();
    strip.setBrightness(80);

    // Initialize message history
    for (int i = 0; i < MAX_MESSAGES; i++) {
        messageHistory[i].valid = false;
    }

    // Retro "console boot" sequence - real init status per line (not just
    // a splash), redrawn as each subsystem actually comes up.
    char bootLines[4][20];
    strcpy(bootLines[0], "[OK] Display");
    strcpy(bootLines[1], "[OK] Sensors");
    strcpy(bootLines[2], "[..] Bluetooth");
    strcpy(bootLines[3], "[..] Cell Gateway");

    auto drawBootScreen = [&]() {
        u8g2.clearBuffer();
        u8g2.setFont(u8g2_font_profont11_tr);
        drawCenteredText(10, "SAFESHADE OS");
        u8g2.drawHLine(10, 14, 108);
        u8g2.setFont(u8g2_font_profont10_tr);
        for (int i = 0; i < 4; i++) {
            u8g2.setCursor(8, 26 + i * 9);
            u8g2.print(bootLines[i]);
        }
        u8g2.sendBuffer();
    };
    drawBootScreen();
    delay(200);

    // BLE Setup
    BLEDevice::init("SafeShade_S1");
    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new ServerCallbacks());

    BLEService* pService = pServer->createService(BLEUUID(SERVICE_UUID), 30);

    pWeatherChar = pService->createCharacteristic(
        WEATHER_CHAR_UUID,
        BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE
    );
    pWeatherChar->setCallbacks(new WeatherCallbacks());

    pAlertChar = pService->createCharacteristic(
        ALERT_CHAR_UUID,
        BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY
    );
    pAlertChar->addDescriptor(new BLE2902());

    pMessageChar = pService->createCharacteristic(
        MESSAGE_CHAR_UUID,
        BLECharacteristic::PROPERTY_WRITE
    );
    pMessageChar->setCallbacks(new MessageCallbacks());

    pHealthChar = pService->createCharacteristic(
        HEALTH_CHAR_UUID,
        BLECharacteristic::PROPERTY_WRITE
    );
    pHealthChar->setCallbacks(new HealthCallbacks());

    pSettingsChar = pService->createCharacteristic(
        SETTINGS_CHAR_UUID,
        BLECharacteristic::PROPERTY_WRITE
    );
    pSettingsChar->setCallbacks(new SettingsCallbacks());

    pReplyChar = pService->createCharacteristic(
        REPLY_CHAR_UUID,
        BLECharacteristic::PROPERTY_READ |
        BLECharacteristic::PROPERTY_NOTIFY |
        BLECharacteristic::PROPERTY_WRITE
    );
    pReplyChar->addDescriptor(new BLE2902());
    pReplyChar->setCallbacks(new ReplyCallbacks());

    pTelemetryChar = pService->createCharacteristic(
        TELEMETRY_CHAR_UUID,
        BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY
    );
    pTelemetryChar->addDescriptor(new BLE2902());

    pLedChar = pService->createCharacteristic(
        LED_CHAR_UUID,
        BLECharacteristic::PROPERTY_WRITE
    );
    pLedChar->setCallbacks(new LedCallbacks());

    pExtChar = pService->createCharacteristic(
        EXT_CHAR_UUID,
        BLECharacteristic::PROPERTY_WRITE
    );
    pExtChar->setCallbacks(new ExtCallbacks());

    pAckChar = pService->createCharacteristic(
        ACK_CHAR_UUID,
        BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY
    );
    pAckChar->addDescriptor(new BLE2902());

    pService->start();

    BLEAdvertising* pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->setMinPreferred(0x06);
    BLEDevice::startAdvertising();

    strcpy(bootLines[2], "[OK] Bluetooth");
    drawBootScreen();
    delay(150);

    // Optional GNSS/cellular gateway link (see includes block at top of
    // file). Station-only - this board only ever connects TO the gateway's
    // AP, it never needs to host its own, so WIFI_AP_STA (which also spins
    // up an unused SoftAP) was pure extra radio contention against the BLE
    // peripheral role already in use above with no benefit. begin() here is
    // non-blocking - it kicks off the connection attempt in the background;
    // we never spin-wait on it, so boot time is unaffected whether or not
    // the gateway board is powered on.
    WiFi.mode(WIFI_STA);
    WiFi.begin(GATEWAY_WIFI_SSID, GATEWAY_WIFI_PASSWORD);
    Serial.print("[WIFI] Connecting to ");
    Serial.println(GATEWAY_WIFI_SSID);

    // ESP32-C3 has a single shared 2.4GHz radio - active BLE advertising/
    // GATT traffic can starve the WiFi STA handshake often enough that it
    // never completes. Bias the coexistence arbiter toward WiFi just for
    // this initial connection window; loop() reverts to BALANCE the moment
    // WiFi actually connects, so BLE (the core safety path to the phone)
    // is never permanently deprioritized.
    esp_coex_preference_set(ESP_COEX_PREFER_WIFI);

    // Cell Gateway line stays honestly "checking" - WiFi.begin() above is
    // non-blocking, so success/failure genuinely isn't known yet here.
    drawBootScreen();
    delay(250);

    playBootSound();
    lastMotionTime = millis();
    lastBatteryTick = millis();
    lastTempUpdate = millis();

    // Shady's first appearance + wordmark, a beat before normal operation.
    unsigned long splashStart = millis();
    while (millis() - splashStart < 900) {
        u8g2.clearBuffer();
        drawShady(64, 16, 11, SHADY_HAPPY, millis());
        u8g2.setFont(u8g2_font_profont12_tr);
        drawCenteredText(46, "SafeShade");
        u8g2.setFont(u8g2_font_profont10_tr);
        drawCenteredText(58, "Safety Companion");
        u8g2.sendBuffer();
        delay(30);
    }

    // Boot animation
    for (int i = 0; i < NUM_LEDS; i++) {
        strip.setPixelColor(i, strip.Color(100, 0, 200));
        strip.show();
        delay(50);
    }
    strip.clear();
    strip.show();
}

// ==========================================
// 15. MAIN LOOP
// ==========================================
void loop() {
    unsigned long currentMillis = millis();

    // BLE Reconnect
    if (!deviceConnected && oldDeviceConnected) {
        delay(500);
        pServer->startAdvertising();
        oldDeviceConnected = deviceConnected;
    }
    if (deviceConnected && !oldDeviceConnected) {
        oldDeviceConnected = deviceConnected;
    }

    // Simulated Battery (no fuel-gauge circuit on this PCB revision - see
    // hardware docs. Still real device state, not app-side fakery: this is
    // the same batteryLevel drawn on the OLED battery icon.)
    if (currentMillis - lastBatteryTick > 180000) {
        lastBatteryTick = currentMillis;
        if (batteryLevel > 5) batteryLevel--;
    }

    // Debug: log WiFi connection transitions only (not every loop) - lets
    // the gateway link be diagnosed over Serial without spamming it.
    if (WiFi.status() != lastWifiStatus) {
        lastWifiStatus = WiFi.status();
        Serial.print("[WIFI] status -> ");
        Serial.println((int)lastWifiStatus);
        if (lastWifiStatus == WL_CONNECTED) {
            Serial.print("[WIFI] Connected, IP: ");
            Serial.print(WiFi.localIP());
            Serial.print(" RSSI: ");
            Serial.println(WiFi.RSSI());
            // Handshake window is over - hand the radio back to BALANCE so
            // BLE-to-phone reliability isn't sacrificed for the rest of runtime.
            esp_coex_preference_set(ESP_COEX_PREFER_BALANCE);
        }
    }

    // Poll the GNSS/cellular gateway board for a real GPS fix, if it's
    // reachable over WiFi. Fails silently (see pollGatewayGps()) - the
    // phone-sourced `location` struct keeps working regardless. Kids mode
    // polls faster ("location priority" per its ModeProfile) - this was
    // previously computed but never actually used anywhere (dead code,
    // found in review).
    unsigned long gatewayPollInterval = getModeProfile(activeMode).gatewayPollIntervalMs;
    if (currentMillis - lastGatewayPollMillis > gatewayPollInterval) {
        lastGatewayPollMillis = currentMillis;
        pollGatewayGps();
    }

    // Informational cellular/modem status - see pollGatewayStatus() comment.
    if (currentMillis - lastGatewayStatusPollMillis > GATEWAY_STATUS_POLL_INTERVAL_MS) {
        lastGatewayStatusPollMillis = currentMillis;
        pollGatewayStatus();
    }

    // Two-way messaging - real, device-independent of BLE proximity (see
    // pollGatewayMessages() doc comment).
    if (currentMillis - lastGatewayMessagesPollMillis > GATEWAY_MESSAGES_POLL_INTERVAL_MS) {
        lastGatewayMessagesPollMillis = currentMillis;
        pollGatewayMessages();
    }

    // Live telemetry notify - real MPU6050/LDR/battery readings, replacing
    // what used to be pure Math.random() fakery on the app side. AcX/AcY/AcZ
    // are raw MPU6050 LSBs (16384 LSB/g at the default +-2g range).
    if (deviceConnected && currentMillis - lastTelemetryTick > 1000) {
        lastTelemetryTick = currentMillis;
        char payload[80];
        snprintf(payload, sizeof(payload), "%.3f,%.3f,%.3f,%.1f,%d,%d",
                 AcX / 16384.0, AcY / 16384.0, AcZ / 16384.0,
                 currentTemp, lightVal, batteryLevel);
        pTelemetryChar->setValue(payload);
        pTelemetryChar->notify();
    }

    // Clock Tick
    if (currentMillis - lastClockTick > 60000) {
        lastClockTick = currentMillis;
        clockMinute++;
        if (clockMinute >= 60) {
            clockMinute = 0;
            clockHour++;
            if (clockHour >= 24) clockHour = 0;
        }

        // Elderly medication reminder - fires once per scheduled minute
        // (medReminderLastFiredMinuteOfDay guards against re-firing every
        // tick while the minute matches). Gated on activeMode for the same
        // reason as the Helmet check-in above - a reminder scheduled while
        // in Elderly mode shouldn't force-switch the screen after the
        // wearer has since switched to a different mode.
        if (activeMode == MODE_ELDERLY && medReminderHour >= 0) {
            int nowMinuteOfDay = clockHour * 60 + clockMinute;
            int dueMinuteOfDay = medReminderHour * 60 + medReminderMinute;
            if (nowMinuteOfDay == dueMinuteOfDay && medReminderLastFiredMinuteOfDay != nowMinuteOfDay) {
                medReminderLastFiredMinuteOfDay = nowMinuteOfDay;
                medReminderDueNow = true;
                currentScreen = SCREEN_MEDICATION;
                tone(PIN_BUZZER, 1800, 150);
                delay(160);
                tone(PIN_BUZZER, 1800, 150);
            }
        }
    }

    // Wrist mode: real sleep-time tally, reusing the existing SCREEN_SLEEP
    // mechanism (already tracks idle time via lastMotionTime) instead of
    // fabricating a sleep metric from nothing.
    if (currentScreen == SCREEN_SLEEP && currentMillis - sleepTallyLastTickMillis > 1000) {
        sleepTallyLastTickMillis = currentMillis;
        sleepSecondsToday++;
    } else if (currentScreen != SCREEN_SLEEP) {
        sleepTallyLastTickMillis = currentMillis;
    }

    // Sensor Readings
    lightVal = analogRead(PIN_LDR);

    Wire.beginTransmission(MPU_ADDR);
    Wire.write(0x3B);
    Wire.endTransmission(false);
    Wire.requestFrom(MPU_ADDR, 14, true);

    if (Wire.available() >= 14) {
        prevAcX = AcX;
        prevAcY = AcY;
        prevAcZ = AcZ;
        AcX = Wire.read() << 8 | Wire.read();
        AcY = Wire.read() << 8 | Wire.read();
        AcZ = Wire.read() << 8 | Wire.read();
        Tmp = Wire.read() << 8 | Wire.read();
        // Gyro bytes were already being clocked off the bus by the 14-byte
        // burst read above and silently discarded - reading them costs zero
        // extra I2C traffic. Used for Bike/Helmet mode's combined linear+
        // rotational crash signature (see adjustedImpactThresh block).
        GyX = Wire.read() << 8 | Wire.read();
        GyY = Wire.read() << 8 | Wire.read();
        GyZ = Wire.read() << 8 | Wire.read();
    }

    currentTemp = (Tmp / 340.0) + 36.53 - TEMP_OFFSET;

    if (currentMillis - lastTempUpdate > TEMP_UPDATE_MS) {
        lastTempUpdate = currentMillis;
        displayTemp = currentTemp;
    }

    // Physics Engine
    long totalMotion = abs(AcX) + abs(AcY) + abs(AcZ);
    long deltaMotion = abs(AcX - prevAcX) + abs(AcY - prevAcY) + abs(AcZ - prevAcZ);

    if (deltaMotion > MOTION_THRESH) {
        lastMotionTime = currentMillis;
    }

    // Helmet: scheduled check-in due check + missed-checkin escalation.
    // Gated on activeMode - without this, switching away from Helmet with
    // a check-in still armed left it silently ticking in the background
    // and could fire a real emergency SMS minutes later in whatever mode
    // the wearer was actually in by then, with zero on-screen indication
    // (a real bug found in review, not hypothetical).
    if (activeMode == MODE_HELMET && checkinIntervalMs > 0) {
        if (!checkinDueNow && currentMillis - lastCheckinAtMillis > checkinIntervalMs) {
            checkinDueNow = true;
            checkinDueAtMillis = currentMillis;
            checkinEscalated = false;
            tone(PIN_BUZZER, 1900, 120);
            delay(130);
            tone(PIN_BUZZER, 1900, 120);
        }
        if (checkinDueNow && !checkinEscalated &&
            currentMillis - checkinDueAtMillis > CHECKIN_MISS_ESCALATE_MS) {
            checkinEscalated = true;
            triggerGatewayAlert();  // missed check-in is a real signal, not just a UI badge
        }
    }

    // Pet mode: active-minutes tally, integrated from real motion deltas.
    if (activeMode == MODE_PET && currentMillis - lastActivityTickMillis > 1000) {
        lastActivityTickMillis = currentMillis;
        if (deltaMotion > MOTION_THRESH) activeSecondsToday++;
    }

    if (deltaMotion > BRAKE_THRESH) {
        walkingDetected = true;
    } else if (walkingDetected && deltaMotion < 2000) {
        walkingDetected = false;
        brakeTimer = currentMillis;
    }

    long adjustedImpactThresh = IMPACT_THRESH;
    switch (settings.fallSensitivity) {
        case 0: adjustedImpactThresh = 70000; break;
        case 1: adjustedImpactThresh = 50000; break;
        case 2: adjustedImpactThresh = 35000; break;
    }

    // Adaptive-mode layer on top of the user's own sensitivity choice - see
    // ModeProfile's doc comment for why this is a floor, not a replacement.
    ModeProfile modeProfile = getModeProfile(activeMode);
    if (modeProfile.minImpactThresh > 0 && adjustedImpactThresh < modeProfile.minImpactThresh) {
        adjustedImpactThresh = modeProfile.minImpactThresh;
    }

    bool crashSignature = totalMotion > adjustedImpactThresh;
    if (modeProfile.gyroCrashCheck) {
        // Bike/Helmet: require rotational jerk alongside linear impact -
        // now possible since GyX/GyY/GyZ are actually being read (see the
        // sensor-read block above) - distinguishes a real crash from a
        // hard bump/pothole, which has high linear accel but little
        // rotation. Threshold is a first-pass estimate; needs bench tuning.
        long totalRotation = abs(GyX) + abs(GyY) + abs(GyZ);
        crashSignature = crashSignature && (totalRotation > modeProfile.gyroCrashRotThresh);
    }

    if (modeProfile.fallDetectionEnabled && crashSignature &&
        currentScreen != SCREEN_FALL && currentScreen != SCREEN_SOS) {
        currentScreen = SCREEN_FALL;
        impactCountToday++;
        lastImpactMillis = currentMillis;
        awaitingConcussionConfirm = (activeMode == MODE_HELMET);
        if (modeProfile.stillnessCheck) {
            stillnessMonitorActive = true;
            stillnessMotionSeen = false;
            stillnessEscalated = false;
            stillnessMonitorStartMillis = currentMillis;
        }
        if (deviceConnected) {
            pAlertChar->setValue("FALL_DETECTED");
            pAlertChar->notify();
        }
        // Parallel, independent cellular alert path via the gateway board -
        // fails silently if WiFi/gateway isn't available, and never delays
        // or replaces the BLE notify above (which fires first, always).
        triggerGatewayAlert();
    }

    // Elderly stillness escalation: if there's real motion during the
    // check window, the wearer is likely moving/responsive - no escalation
    // needed. If they stay still the whole window, that's a real signal
    // worth a second alert attempt, not just a UI-only "confidence score".
    if (stillnessMonitorActive) {
        if (deltaMotion > MOTION_THRESH) stillnessMotionSeen = true;
        if (currentMillis - stillnessMonitorStartMillis > STILLNESS_CHECK_MS) {
            if (!stillnessMotionSeen && !stillnessEscalated && currentScreen == SCREEN_FALL) {
                stillnessEscalated = true;
                triggerGatewayAlert();
            }
            stillnessMonitorActive = false;
        }
    }

    // Helmet two-step confirm fail-safe: never leave the wearer stuck on
    // this screen indefinitely if the second press never comes.
    if (concussionConfirmPending && currentMillis - concussionConfirmAtMillis > CONCUSSION_CONFIRM_TIMEOUT_MS) {
        concussionConfirmPending = false;
        currentScreen = SCREEN_HEALTH;
        noTone(PIN_BUZZER);
    }

    // FIXED: Clear wake flag after delay (Issue #4)
    if (justWokeUp && (currentMillis - wakeTime > 500)) {
        justWokeUp = false;
    }

    // Button Input Handling
    bool isPressed = (digitalRead(PIN_BTN) == LOW);

    // Button press detected
    if (isPressed && !btnState) {
        btnState = true;
        btnDownTime = currentMillis;
        btnWasHeld = false;
        playClickSound();

        // Wake from sleep - FIXED (Issue #4)
        if (currentScreen == SCREEN_SLEEP) {
            currentScreen = SCREEN_HOME;
            lastMotionTime = currentMillis;
            justWokeUp = true;
            wakeTime = currentMillis;
            return;
        }

        // Helmet check-in dismiss - any press on the Impact Log screen
        // while a check-in is due acknowledges it and restarts the timer.
        if (currentScreen == SCREEN_IMPACT_LOG && checkinDueNow) {
            checkinDueNow = false;
            checkinEscalated = false;
            lastCheckinAtMillis = currentMillis;
            return;
        }

        // Elderly medication reminder dismiss - the on-screen "Press to
        // dismiss" text had nothing behind it before (real bug: only an
        // app-sent EXT "MED:" command could clear medReminderDueNow, so a
        // wearer with no phone nearby had no way to silence it on-device).
        if (currentScreen == SCREEN_MEDICATION && medReminderDueNow) {
            medReminderDueNow = false;
            noTone(PIN_BUZZER);
            return;
        }

        // FIXED: Dismiss fall alert goes to Medical ID (Issue #3)
        if (currentScreen == SCREEN_FALL) {
            // Helmet: a head-impact-scale event warrants a deliberate
            // two-step confirm rather than a single passive dismiss - the
            // first press just quiets the alarm and asks "you sure?"; a
            // second press actually confirms OK.
            if (awaitingConcussionConfirm) {
                awaitingConcussionConfirm = false;
                concussionConfirmPending = true;
                concussionConfirmAtMillis = currentMillis;
                noTone(PIN_BUZZER);
                return;
            }
            if (concussionConfirmPending) {
                concussionConfirmPending = false;
            }
            currentScreen = SCREEN_HEALTH;  // Changed from SCREEN_HOME
            lastMotionTime = currentMillis;
            noTone(PIN_BUZZER);
            return;
        }
    }

    // Button held check
    if (isPressed && btnState && !btnWasHeld) {
        unsigned long holdTime = currentMillis - btnDownTime;

        if (holdTime > 800 && currentScreen == SCREEN_MESSAGE && !inReplyMode && messageCount > 0) {
            inReplyMode = true;
            selectedReply = 0;
            btnWasHeld = true;
            playModeChange();
        }
        else if (holdTime > 800 && inReplyMode) {
            if (selectedReply < NUM_REPLIES - 1) {
                // Send reply via BLE
                if (deviceConnected) {
                    pReplyChar->setValue(quickReplies[selectedReply]);
                    pReplyChar->notify();
                }
                // Real two-way relay via the gateway's SMS channel - same
                // reasoning as ReplyCallbacks::onWrite above.
                triggerGatewayReply(quickReplies[selectedReply]);
                // FIXED: Add sent reply to message history (Issue #5)
                addMessage(quickReplies[selectedReply], false);
                playReplySent();
                inReplyMode = false;
                hasNewMessage = false;
            } else {
                inReplyMode = false;
                playScreenChange();
            }
            btnWasHeld = true;
        }
        else if (holdTime > 800 && currentScreen == SCREEN_MODE_SELECT) {
            // Confirms the previewed mode - the on-device mirror of the
            // app's mode picker. applyMode() handles the confirmation
            // tone/LED flash and its own ACK notify.
            applyMode(modeSelectPreview);
            currentScreen = SCREEN_HOME;
            btnWasHeld = true;
        }
        else if (holdTime > modeProfile.sosHoldMs && currentScreen != SCREEN_SOS &&
                 currentScreen != SCREEN_MODE_SELECT && !inReplyMode) {
            currentScreen = SCREEN_SOS;
            btnWasHeld = true;
            // Start the lock-in countdown instead of alerting immediately -
            // see SOS_ALERT_LOCKIN_MS. The actual triggerGatewayAlert() call
            // happens in loop() once the countdown expires unconfirmed.
            sosEnteredAtMillis = currentMillis;
            sosAlertSent = false;
        }
    }

    // Button released
    if (!isPressed && btnState) {
        btnState = false;
        unsigned long pressDuration = currentMillis - btnDownTime;

        if (pressDuration < 400 && !btnWasHeld && !justWokeUp) {
            clickCount++;
            lastClickTime = currentMillis;
        }
        btnWasHeld = false;
    }

    // Process click count
    if (clickCount > 0 && (currentMillis - lastClickTime > 350)) {
        if (currentScreen == SCREEN_SOS) {
            // Any number of taps cancels SOS, checked before the 1x/2x
            // branches below - a panicked user mashing the button should
            // always dismiss it, never risk landing in an unrelated menu.
            // (Real bug, now fixed: 2 taps used to fall through to the "2x:
            // open RGB Light Mode" handler instead of cancelling.)
            currentScreen = SCREEN_HOME;
            noTone(PIN_BUZZER);
            sosAlertSent = false;
        } else if (clickCount == 1) {
            if (inReplyMode) {
                selectedReply++;
                if (selectedReply >= NUM_REPLIES) {
                    inReplyMode = false;
                    playScreenChange();
                } else {
                    playReplySelect();
                }
            } else if (currentScreen == SCREEN_RGB_MENU) {
                rgbPattern++;
                if (rgbPattern >= RGB_COUNT) rgbPattern = 0;
                playClickSound();
            } else if (currentScreen == SCREEN_MODE_SELECT) {
                modeSelectPreview = (PersonaMode)((modeSelectPreview + 1) % 7);
                modeSelectLastInteractionMillis = currentMillis;
                playClickSound();
            } else {
                // Mode-aware screen cycle - which screens are reachable
                // (and in what order) now depends on activeMode, see
                // buildModeScreenCycle(). Replaces the old fixed
                // "(currentScreen+1)%5 over HOME..MESSAGE" cycling.
                ScreenState cycle[8];
                int cycleCount = buildModeScreenCycle(cycle);
                int idx = -1;
                for (int i = 0; i < cycleCount; i++) {
                    if (cycle[i] == currentScreen) { idx = i; break; }
                }
                if (idx != -1) {
                    playScreenChange();
                    currentScreen = cycle[(idx + 1) % cycleCount];
                }
            }
        } else if (clickCount == 2) {
            if (!inReplyMode && currentScreen != SCREEN_MODE_SELECT) {
                if (currentScreen == SCREEN_RGB_MENU) {
                    // FIXED: Turn off LEDs when exiting RGB menu (Issue #6)
                    playModeChange();
                    forceLEDsOff();
                    currentScreen = SCREEN_HOME;
                } else if (!modeProfile.rgbMenuDisabled) {
                    // Helmet keeps this disabled - minimal-distraction UI,
                    // no fiddling with lights while riding/working.
                    playModeChange();
                    previousScreen = currentScreen;
                    currentScreen = SCREEN_RGB_MENU;
                }
            }
        } else if (clickCount >= 3 && currentScreen == SCREEN_HOME) {
            // Triple-click from Home - on-device mirror of the app's
            // Adaptive Mode picker.
            modeSelectPreview = activeMode;
            modeSelectLastInteractionMillis = currentMillis;
            currentScreen = SCREEN_MODE_SELECT;
            playModeChange();
        }
        clickCount = 0;
    }

    // Mode Select idle timeout - never leave it stuck open; an unconfirmed
    // preview simply reverts without applying anything.
    if (currentScreen == SCREEN_MODE_SELECT &&
        currentMillis - modeSelectLastInteractionMillis > MODE_SELECT_IDLE_TIMEOUT_MS) {
        currentScreen = SCREEN_HOME;
    }

    // Hold to exit SOS - requires a FRESH press that started after SOS was
    // entered (btnDownTime >= sosEnteredAtMillis). Without that guard this
    // fired off the same continuous press used to enter SOS (hold >1.5s to
    // enter, isPressed still true past the 2s mark of that same press),
    // cancelling SOS within ~0.5s of entering it almost every time and
    // never letting the lock-in countdown run - a real bug, not intended.
    if (currentScreen == SCREEN_SOS && isPressed &&
        btnDownTime >= sosEnteredAtMillis && (currentMillis - btnDownTime > 2000)) {
        currentScreen = SCREEN_HOME;
        noTone(PIN_BUZZER);
        sosAlertSent = false;
    }

    // SOS auto-alert: fires once, only after the lock-in countdown expires
    // with SOS still active (i.e. not cancelled by a tap or the hold-to-exit
    // check above) - see SOS_ALERT_LOCKIN_MS comment at its declaration.
    if (currentScreen == SCREEN_SOS && !sosAlertSent &&
        (currentMillis - sosEnteredAtMillis > SOS_ALERT_LOCKIN_MS)) {
        sosAlertSent = true;
        triggerGatewayAlert();
    }

    // REMOVED: Auto Sleep. The device screen itself no longer sleeps/shuts
    // off on inactivity - only Shady's mood goes SHADY_SLEEPY (see homeMood
    // above, driven by the same lastMotionTime/SLEEP_TIMEOUT_MS signal)
    // while the OLED and everything else stays fully live. SCREEN_SLEEP is
    // simply never entered anymore; its draw case and the wake-on-press
    // handling below are harmless dead code, kept in case this needs
    // reverting rather than fully ripped out.

    // Drawing & LED Updates
    bool isBraking = (currentMillis - brakeTimer < 1500) && (currentMillis - brakeTimer > 0);

    if (currentScreen != lastRenderedScreen) {
        lastRenderedScreen = currentScreen;
        screenEnteredAtMillis = currentMillis;
    }

    switch (currentScreen) {
        case SCREEN_HOME:
            drawHomeScreen();
            break;

        case SCREEN_WEATHER:
            drawWeatherScreen();
            break;

        case SCREEN_HEALTH:
            drawHealthScreen();
            break;

        case SCREEN_GPS:
            drawGPSScreen();
            break;

        case SCREEN_MESSAGE:
            drawMessageScreen();
            if (hasNewMessage && (currentMillis - messageReceivedTime > 30000)) {
                hasNewMessage = false;
            }
            break;

        case SCREEN_RIDE_STATS:
            drawRideStatsScreen();
            break;

        case SCREEN_ACTIVITY:
            drawActivityScreen();
            break;

        case SCREEN_VITALS:
            drawVitalsScreen();
            break;

        case SCREEN_IMPACT_LOG:
            drawImpactLogScreen();
            break;

        case SCREEN_SAFE_ZONE:
            drawSafeZoneScreen();
            break;

        case SCREEN_MEDICATION:
            drawMedicationScreen();
            break;

        case SCREEN_MODE_SELECT:
            drawModeSelectScreen();
            break;

        case SCREEN_RGB_MENU:
            drawRGBMenu();
            updateRGBPattern();
            return;

        case SCREEN_SOS: {
            drawSOSScreen();

            // FIXED: non-blocking siren (was the root cause of "SOS can't be
            // dismissed"). This used to call delay(100) twice per loop()
            // pass (~200ms blocked per call) while on this screen, which
            // meant digitalRead(PIN_BTN) - sampled once per loop(), before
            // this switch - never got re-sampled while SOS was showing, so
            // taps/holds to cancel were silently dropped. Now a millis()
            // timed phase toggle drives the same alternating tone/LED
            // effect with zero delay() calls.
            static unsigned long lastSosToneToggle = 0;
            static bool sosPhaseHigh = false;
            if (currentMillis - lastSosToneToggle >= 100) {
                lastSosToneToggle = currentMillis;
                sosPhaseHigh = !sosPhaseHigh;
            }

            if (sosPhaseHigh) {
                for (int i = 0; i < NUM_LEDS; i++) {
                    strip.setPixelColor(i, strip.Color(255, 0, 0));
                }
                strip.show();
                digitalWrite(PIN_LED, HIGH);
            } else {
                strip.clear();
                strip.show();
                digitalWrite(PIN_LED, LOW);
            }

            // FIXED: settings.sosVolume (0-100) was parsed off BLE but
            // never actually read anywhere - make it real by gating the
            // tone with a duty cycle inside each 250ms window instead of
            // sounding continuously. 100% = tone plays the whole window,
            // 20% = tone plays for ~50ms out of every 250ms, 0% = silent
            // (LED/strip siren still runs). No delay() - same millis()
            // timed approach as the phase toggle above.
            int sosVol = constrain(settings.sosVolume, 0, 100);
            unsigned long onMs = (unsigned long)(250UL * (unsigned long)sosVol / 100UL);
            unsigned long phaseInWindow = currentMillis % 250UL;
            if (sosVol > 0 && phaseInWindow < onMs) {
                tone(PIN_BUZZER, sosPhaseHigh ? 1000 : 2000);
            } else {
                noTone(PIN_BUZZER);
            }
            return;
        }

        case SCREEN_FALL:
            drawFallScreen();
            if ((currentMillis / 200) % 2 == 0) {
                for (int i = 0; i < NUM_LEDS; i++) {
                    strip.setPixelColor(i, strip.Color(255, 150, 0));
                }
            } else {
                for (int i = 0; i < NUM_LEDS; i++) {
                    strip.setPixelColor(i, strip.Color(255, 0, 0));
                }
            }
            strip.show();
            if (currentMillis % 1000 < 200) {
                tone(PIN_BUZZER, 2000);
            } else {
                noTone(PIN_BUZZER);
            }
            return;

        case SCREEN_SLEEP:
            return;
    }

    // FIXED: Normal LED behavior - properly turn off when bright (Issue #1)
    if (currentScreen != SCREEN_RGB_MENU) {
        if (petLostAlertActive) {
            // Pet mode "virtual leash" - highest priority, helps visually
            // locate the pet in the dark, independent of ambient light.
            bool on = (currentMillis / 300) % 2 == 0;
            for (int i = 0; i < NUM_LEDS; i++) {
                strip.setPixelColor(i, on ? strip.Color(0, 255, 100) : 0);
            }
            strip.show();
        } else if (isBraking) {
            if (activeMode == MODE_BIKE) {
                // Bike mode: brighter/wider brake signal - a fast strobe
                // reads as far more urgent to traffic behind than a
                // steady glow, matching this mode's "commuter safety"
                // priority feature.
                bool on = (currentMillis / 80) % 2 == 0;
                for (int i = 0; i < NUM_LEDS; i++) {
                    strip.setPixelColor(i, on ? strip.Color(255, 0, 0) : strip.Color(40, 0, 0));
                }
            } else {
                // Brake light - solid red
                for (int i = 0; i < NUM_LEDS; i++) {
                    strip.setPixelColor(i, strip.Color(255, 0, 0));
                }
            }
            strip.show();
        } else if (activeMode == MODE_KIDS && lightVal >= DARK_THRESHOLD) {
            // Kids mode: a gentle idle rainbow shimmer instead of fully off
            // in normal light - part of this mode's playful UI treatment.
            if (isHeadlampOn) forceHeadlampOff();
            for (int i = 0; i < NUM_LEDS; i++) {
                strip.setPixelColor(i, strip.ColorHSV((i * 65536L / NUM_LEDS) + (currentMillis * 8), 180, 40));
            }
            strip.show();
        } else if (lightVal < DARK_THRESHOLD) {
            // Dark mode - subtle red tail light (Elderly gets a brighter,
            // wider path-light instead - this mode's night-safety priority
            // feature, more useful than a dim 2-pixel trail for the
            // bathroom-at-3am scenario this whole mode exists for).
            if (!isHeadlampOn) setHeadlamp(true);
            if (activeMode == MODE_ELDERLY) {
                for (int i = 0; i < NUM_LEDS; i++) {
                    strip.setPixelColor(i, strip.Color(120, 90, 60));
                }
                strip.show();
            } else {
                int pos = (currentMillis / 100) % NUM_LEDS;
                strip.clear();
                strip.setPixelColor(pos, strip.Color(150, 0, 0));
                strip.setPixelColor((pos + 1) % NUM_LEDS, strip.Color(50, 0, 0));
                strip.show();
            }
        } else {
            // FIXED: Bright mode - turn everything OFF (Issue #1)
            if (isHeadlampOn) {
                forceHeadlampOff();
            }
            strip.clear();
            strip.show();
        }
    }
}
