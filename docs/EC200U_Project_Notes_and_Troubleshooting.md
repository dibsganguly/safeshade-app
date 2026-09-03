# EC200U-CN + ESP32-S3 Project Notes

## Purpose

This is a hand-off/reference file for continuing the EC200U project in a
new chat. It records what was tried, what failed, what worked, how the
working UART was discovered, exact AT commands, Arduino code,
SIM-specific notes, GNSS troubleshooting, and future automation plans.

> **Important:** Do not casually change the UART pins, PWRKEY behavior,
> baud rate, or other working configuration while troubleshooting a
> different subsystem. Change one thing at a time.

------------------------------------------------------------------------

# 1. Hardware

**Board:** Sharvi / 7Semi ESP32-S3 + Quectel EC200U-CN 4G LTE Cat-1 WiFi
Bluetooth GNSS IoT Smart Modem

Known board information:

-   Manufacturer part: `ESP32-S3-EC200U-C`
-   Hardware Version: `1.2`
-   Modem: `Quectel EC200U-CN`
-   Default baud: `115200`
-   PWRKEY: ESP32 `GPIO10`

Board page:
https://sharvielectronics.com/product/esp32-s3-with-ec200u-4g-lte-cat-1-wifi-bluetooth-gnss-iot-smart-modem/

## Confirmed UART pinout for this exact board

The correct UART was found experimentally:

  ESP32 pin   Function   EC200U side
  ----------- ---------- ------------------
  GPIO18      ESP32 RX   EC200U TX
  GPIO17      ESP32 TX   EC200U RX
  GPIO10      PWRKEY     EC200U Power Key

Use:

``` cpp
#define EC200U_RX 18
#define EC200U_TX 17
#define PW_KEY    10
```

and:

``` cpp
EC200U.begin(115200, SERIAL_8N1, EC200U_RX, EC200U_TX);
```

------------------------------------------------------------------------

# 2. How We Found the Correct UART

Several combinations were tested.

### RX 12 / TX 13

Result: **No response.**

### RX 17 / TX 18

Result: **Corrupted/garbled response.**

### RX 16 / TX 17

Result: **No response.**

### RX 18 / TX 17 @ 115200

Result: **Clean, repeated `AT` -\> `OK`.**

This is the established working configuration.

------------------------------------------------------------------------

# 3. Working AT Terminal Arduino Code

This is the current terminal sketch. Preserve the UART and PWRKEY logic
unless there is a specific reason to change it.

``` cpp
#define EC200U_RX 18
#define EC200U_TX 17
#define PW_KEY    10

HardwareSerial EC200U(2);

void setup() {

  // EC200U Power Key
  pinMode(PW_KEY, OUTPUT);
  digitalWrite(PW_KEY, LOW);

  // Arduino Serial Monitor
  Serial.begin(115200);

  // EC200U UART
  EC200U.begin(115200, SERIAL_8N1, EC200U_RX, EC200U_TX);

  delay(1000);

  Serial.println();
  Serial.println("====================================");
  Serial.println("       EC200U AT TERMINAL");
  Serial.println("====================================");
  Serial.println("RX = GPIO18");
  Serial.println("TX = GPIO17");
  Serial.println("PWRKEY = GPIO10");
  Serial.println("Baud = 115200");
  Serial.println("====================================");
  Serial.println();

  // EC200U POWER-ON
  Serial.println("Starting EC200U...");

  digitalWrite(PW_KEY, LOW);
  delay(1500);

  digitalWrite(PW_KEY, HIGH);
  delay(8000);

  Serial.println("EC200U startup sequence complete.");
  Serial.println();
  Serial.println("READY");
  Serial.println();

  Serial.println("For SMS:");
  Serial.println("AT+CMGF=1");
  Serial.println("AT+CSCS="GSM"");
  Serial.println("AT+CMGS="+91XXXXXXXXXX"");
  Serial.println("Type your message, then type #");
  Serial.println("# sends Ctrl+Z (ASCII 26)");
  Serial.println();
}

void loop() {

  // Arduino IDE Serial Monitor -> EC200U
  while (Serial.available()) {

    char c = Serial.read();

    // '#' = Ctrl+Z / ASCII 26
    if (c == '#') {
      EC200U.write(26);
      Serial.println();
      Serial.println("[Ctrl+Z SENT]");
    }
    else {
      EC200U.write(c);
    }
  }

  // EC200U -> Arduino IDE Serial Monitor
  while (EC200U.available()) {
    char c = EC200U.read();
    Serial.write(c);
  }
}
```

### Serial Monitor note

The Arduino IDE can make commands appear duplicated, e.g.:

``` text
AT+CGATT?AT+CGATT?
```

This can be local echo / line-ending behavior rather than the modem
actually receiving two commands. Judge success from the actual modem
response.

------------------------------------------------------------------------

# 4. First Major Breakthrough

A baud-rate test produced:

``` text
Testing baud: 115200
--------------------------------------
AT
OK
AT
OK
AT
OK
======================================
*** AT RESPONSE FOUND ***
======================================
CORRECT BAUD IS PROBABLY: 115200
```

This established:

-   modem communication works
-   UART can be controlled from ESP32
-   `115200` is correct
-   the eventual working pins are GPIO18/GPIO17

------------------------------------------------------------------------

# 5. Cellular Diagnostics

Useful baseline commands:

``` text
AT
ATI
AT+CPIN?
AT+CSQ
AT+CREG?
AT+CGREG?
AT+CEREG?
AT+COPS?
AT+CGATT?
AT+QNWINFO
AT+QENG="servingcell"
AT+CGDCONT?
```

An earlier network check showed:

``` text
+CGATT: 1
```

meaning packet-domain attachment was successful.

------------------------------------------------------------------------

# 6. SIMs

Two SIMs are available:

-   **BSNL**
-   **Jio**

Potential uses:

-   SMS
-   voice calling
-   mobile data
-   overall cellular operation

### Current practical recommendation

The user reported that **Jio is working for the cellular/voice setup**.

BSNL data definitely worked, but BSNL voice calling produced
`NO CARRIER`.

Therefore:

-   use **Jio** as the preferred voice-call demo SIM
-   keep **BSNL** available for data/SMS and further testing
-   GPS is primarily a GNSS/antenna issue and does not depend on which
    SIM is inserted

------------------------------------------------------------------------

# 7. BSNL Data Configuration --- WORKING

The BSNL APN that worked was:

``` text
bsnlnet
```

Commands:

``` text
AT+CGDCONT=1,"IP","bsnlnet"
AT+QICSGP=1,1,"bsnlnet","","",1
AT+QICSGP=1
AT+QIACT=1
AT+QIACT?
AT+CGPADDR=1
```

Internet test:

``` text
AT+QPING=1,"8.8.8.8"
```

Successful result:

``` text
+QPING: 0,"8.8.8.8",64,59,255
+QPING: 0,"8.8.8.8",64,60,255
+QPING: 0,4,4,0,54,176,73
```

### Conclusion

**BSNL packet data was proven to work.**

This is important because it proves the BSNL SIM/network path is usable
for data.

------------------------------------------------------------------------

# 8. SMS

Basic setup:

``` text
AT+CMGF=1
AT+CSCS="GSM"
```

Send:

``` text
AT+CMGS="+91XXXXXXXXXX"
```

Wait for:

``` text
>
```

Then type the message.

Submit with **Ctrl+Z / ASCII 26**.

The terminal sketch maps:

``` text
#
```

to:

``` cpp
EC200U.write(26);
```

Expected success:

``` text
+CMGS: <reference>
OK
```

Example:

``` text
AT+CMGF=1
AT+CSCS="GSM"
AT+CMGS="+919609778573"
```

Then:

``` text
Hello! This is a test message sent using my EC200U and BSNL SIM.
```

Then type:

``` text
#
```

------------------------------------------------------------------------

# 9. Voice Calling

Correct dial syntax:

``` text
ATD+91XXXXXXXXXX;
```

The trailing semicolon is important.

Hang up:

``` text
ATH
```

Call status:

``` text
AT+CLCC
```

## BSNL result

Observed:

``` text
ATD+919609778573;
OK
NO CARRIER
```

So the dial command itself was accepted, but the call did not remain
established.

------------------------------------------------------------------------

# 10. BSNL IMS / VoLTE Investigation

Diagnostics performed:

``` text
AT+QCFG="ims"
```

Result:

``` text
+QCFG: "ims",1,0
OK
```

IMS registration:

``` text
AT+CIREG?
```

Result:

``` text
+CIREG: 0,0
OK
```

This indicated no IMS registration at that time.

Firmware:

``` text
AT+QGMR
```

Result:

``` text
EC200UCNAAR03A13M08
OK
```

`ATI`:

``` text
Quectel
EC200U
Revision: EC200UCNAAR03A13M08
OK
```

Operator:

``` text
AT+COPS?
```

Result:

``` text
+COPS: 0,0,"CellOne",7
OK
```

Network:

``` text
AT+QNWINFO
```

Result:

``` text
+QNWINFO: "TDD LTE","40476","LTE BAND 41",40140
OK
```

Serving cell:

``` text
AT+QENG="servingcell"
```

Result:

``` text
+QENG: "servingcell","NOCONN","LTE","TDD",404,76,81F3E01,207,40140,41,5,5,8716,-94,-7,-70,77,11
OK
```

## Commands that did not work

``` text
AT+QMBNCFG="list"
AT+QMBNCFG="List"
```

returned:

``` text
+CME ERROR: invalid command line
```

This also failed:

``` text
AT+QIMSCFG="ims_enable"
```

with:

``` text
+CME ERROR: Operation not allowed
```

And:

``` text
AT+QIMSXML="VERSION"
```

returned:

``` text
+CME ERROR: invalid command line
```

### Future BSNL calling plan

Do not blindly modify MBN/IMS configuration.

First collect:

``` text
AT+CPIN?
AT+CSQ
AT+COPS?
AT+QNWINFO
AT+CEREG?
AT+CIREG?
AT+QCFG="ims"
AT+QENG="servingcell"
```

Then try:

``` text
ATD+91XXXXXXXXXX;
```

and immediately check:

``` text
AT+CLCC
AT+CIREG?
```

A registered IMS state would be required for a normal VoLTE path.

### Firmware note

Current firmware is:

``` text
EC200UCNAAR03A13M08
```

There are reports from the Quectel community involving later EC200U-CN
firmware and VoLTE/GNSS behavior. However, do **not** upgrade firmware
merely as a guess. Firmware flashing should be a separate, deliberate
task after establishing that it is actually necessary.

### Practical demo workaround

If voice calling is required:

> **Use the Jio SIM.**

If BSNL calling specifically needs to be demonstrated, continue the
IMS/VoLTE investigation separately.

------------------------------------------------------------------------

# 11. GNSS / GPS

The EC200U-CN GNSS subsystem was enabled with:

``` text
AT+QGPS=1
```

Result:

``` text
OK
```

Then:

``` text
AT+QGPSLOC=0
```

returned:

``` text
+CME ERROR: 516
```

This means there was no usable location fix in this situation.

------------------------------------------------------------------------

# 12. GNSS Diagnostics

Status:

``` text
AT+QGPS?
```

Result:

``` text
+QGPS: 1
OK
```

Therefore GNSS was enabled.

GNSS chipset information:

``` text
AT+QGPSINFO
```

Result:

``` text
+QGPSINFO: UC6228CI,G1B1E1,N/A,R3.4.21.0Build16229,N/A,N/A
OK
```

GGA:

``` text
AT+QGPSGNMEA="GGA"
```

Result:

``` text
+QGPSGNMEA: $GNGGA,,,,,,0,00,99.99,,,,,,*56
OK
```

GSV:

``` text
AT+QGPSGNMEA="GSV"
```

Result:

``` text
+QGPSGNMEA: $GPGSV,1,1,00,0*65
+QGPSGNMEA: $GAGSV,1,1,00,0*74
OK
```

### Interpretation

The key observation is:

``` text
00
```

satellites.

Therefore:

-   GNSS engine = ON
-   GNSS chipset = responding
-   satellites detected = **0**
-   valid fix = **NO**

This points much more strongly toward antenna/RF reception than toward
an AT syntax problem.

------------------------------------------------------------------------

# 13. GNSS Error 504

After GNSS was already active:

``` text
AT+QGPS=1
```

returned:

``` text
+CME ERROR: 504
```

Because:

``` text
AT+QGPS?
```

already returned:

``` text
+QGPS: 1
```

do not interpret this as proof that GNSS is broken.

It is consistent with trying to start a GNSS session that is already
active.

------------------------------------------------------------------------

# 14. GNSS Antenna Is Now the Main Suspect

Current evidence:

``` text
+QGPS: 1
```

works.

``` text
+QGPSINFO: UC6228CI,...
```

works.

But:

``` text
$GPGSV,1,1,00
$GAGSV,1,1,00
```

shows zero satellites.

And:

``` text
$GNGGA,,,,,,0,00,99.99,...
```

shows no fix.

Therefore the next priority is physical GNSS reception.

Possible causes:

1.  Active ceramic antenna is broken.
2.  Antenna is connected to the wrong connector.
3.  Connector is not seated correctly.
4.  Antenna cable/connector is damaged.
5.  Active antenna bias/power arrangement is incompatible.
6.  This board/variant may work better with a passive antenna.
7.  Testing indoors is preventing reception.
8.  Less likely: GNSS configuration/firmware issue.

A Quectel forum case with EC200U-CN had the same general symptoms (`GSV`
= 00 and error 516) and the user later reported that changing the
antenna connector fixed the issue. Another Quectel response specifically
recommended checking the antenna connection for error 516.

------------------------------------------------------------------------

# 15. GNSS Test Procedure

### Step 1

Do not repeatedly send:

``` text
AT+QGPS=1
```

if:

``` text
AT+QGPS?
```

already says:

``` text
+QGPS: 1
```

### Step 2

Connect the GNSS antenna correctly.

### Step 3

Test outdoors with an unobstructed sky view.

### Step 4

Leave GNSS running for several minutes.

### Step 5

Check:

``` text
AT+QGPSGNMEA="GSV"
```

Look for a non-zero satellite count.

### Step 6

Check:

``` text
AT+QGPSGNMEA="GGA"
```

Look for a valid fix.

### Step 7

Once a fix exists:

``` text
AT+QGPSLOC=0
```

------------------------------------------------------------------------

# 16. Additional GNSS Commands for Future Testing

Depending on the exact firmware:

``` text
AT+QGPSPOWER=1
```

may be relevant. Some EC200U-CN firmware versions/documentation use it
for GNSS power-up.

Potentially:

``` text
AT+QGPSPOWER?
```

if supported.

Stop GNSS:

``` text
AT+QGPSEND
```

Then:

``` text
AT+QGPS=1
```

GNSS configuration query:

``` text
AT+QGPSCFG?
```

Do not blindly apply GNSS configuration commands from another
firmware/variant if the current firmware rejects them.

------------------------------------------------------------------------

# 17. GPS -\> SMS Automation Goal

The final project should ideally do:

``` text
ESP32 boots
   |
   v
Start EC200U
   |
   v
Check SIM/network
   |
   v
Start GNSS
   |
   v
Wait for valid fix
   |
   +---- no fix ----> retry / antenna diagnostic
   |
   v
Read latitude + longitude
   |
   v
Create Google Maps link
   |
   v
Send SMS
```

Example SMS:

``` text
Location Alert

Current location:
Latitude: 22.5726
Longitude: 88.3639

Map:
https://maps.google.com/?q=22.5726,88.3639
```

------------------------------------------------------------------------

# 18. Hardcoded GPS Fallback for Demo

If the active ceramic antenna is confirmed broken and there is not
enough time to replace it, use a predefined location for the
demonstration.

Example:

``` cpp
const char* DEMO_LAT = "22.5726";
const char* DEMO_LON = "88.3639";
```

Generate:

``` text
https://maps.google.com/?q=22.5726,88.3639
```

Recommended demo wording:

``` text
DEMO LOCATION

GPS antenna unavailable.
Using predefined demo coordinates.

Location:
22.5726, 88.3639

Map:
https://maps.google.com/?q=22.5726,88.3639
```

The fallback should be clearly labelled so it cannot be mistaken for
live GPS data.

------------------------------------------------------------------------

# 19. Recommended Arduino Automation Structure

The eventual program should use functions such as:

``` cpp
bool sendAT(const char* command, const char* expected, unsigned long timeout);
bool startModem();
bool waitForNetwork();
bool startGNSS();
bool getGPSLocation(String &latitude, String &longitude);
bool sendSMS(const String &number, const String &message);
bool makeCall(const String &number);
void hangupCall();
```

This is preferable to putting every command directly in `loop()`.

------------------------------------------------------------------------

# 20. Basic SMS Automation Function

Starting point:

``` cpp
bool sendSMS(String number, String message) {

  EC200U.println("AT+CMGF=1");
  delay(500);

  EC200U.println("AT+CSCS="GSM"");
  delay(500);

  EC200U.print("AT+CMGS="");
  EC200U.print(number);
  EC200U.println(""");

  delay(1000);

  EC200U.print(message);

  // Ctrl+Z
  EC200U.write(26);

  delay(5000);

  return true;
}
```

This is only a starting implementation.

A robust version should:

1.  wait for the `>` prompt
2.  send the message
3.  send ASCII 26
4.  wait for `+CMGS:`
5.  verify `OK`
6.  detect `ERROR` / `+CMS ERROR`

------------------------------------------------------------------------

# 21. Basic Voice Automation

``` cpp
void makeCall(String number) {

  EC200U.print("ATD");
  EC200U.print(number);
  EC200U.println(";");

}
```

Example:

``` cpp
makeCall("+919XXXXXXXXX");
```

Hang up:

``` cpp
EC200U.println("ATH");
```

A final version should implement a timeout and call-status checking.

------------------------------------------------------------------------

# 22. Basic GNSS Automation Skeleton

``` cpp
bool startGNSS() {

  EC200U.println("AT+QGPS=1");

  delay(1000);

  // Future version should wait for and parse
  // the actual modem response.

  return true;
}
```

Then query:

``` cpp
EC200U.println("AT+QGPSGNMEA="GGA"");
```

and:

``` cpp
EC200U.println("AT+QGPSGNMEA="GSV"");
```

A robust parser should eventually determine:

-   fix status
-   satellite count
-   latitude
-   longitude
-   altitude
-   HDOP

Do not rely only on fixed delays.

------------------------------------------------------------------------

# 23. Three Useful Final Demo Modes

## Mode 1 --- Real GPS + SMS

``` text
GNSS antenna working
        |
        v
Real coordinates
        |
        v
SMS with Google Maps link
```

## Mode 2 --- GPS fallback

``` text
GNSS has no fix
        |
        v
Predefined demo coordinates
        |
        v
Clearly labelled DEMO LOCATION SMS
```

## Mode 3 --- SMS + voice

Prefer:

``` text
Jio SIM
```

Sequence:

``` text
Check network
    |
    +--> Send SMS
    |
    +--> Make call
    |
    +--> Hang up
```

------------------------------------------------------------------------

# 24. Recommended SIM Strategy

## Jio

Use first when:

-   voice calling is required
-   general cellular demo is required
-   the project needs the simplest working cellular path

## BSNL

Use when:

-   demonstrating BSNL specifically
-   testing SMS
-   testing packet data
-   continuing VoLTE/IMS investigation

Known working BSNL data:

``` text
bsnlnet
```

Known BSNL voice problem:

``` text
ATD+91XXXXXXXXXX;
OK
NO CARRIER
```

------------------------------------------------------------------------

# 25. Useful Cellular Diagnostic Set

Start with:

``` text
AT
ATI
AT+CPIN?
AT+CSQ
AT+COPS?
AT+CREG?
AT+CGREG?
AT+CEREG?
AT+CGATT?
AT+QNWINFO
AT+QENG="servingcell"
```

Data:

``` text
AT+CGDCONT?
AT+QICSGP=1
AT+QIACT?
AT+CGPADDR=1
```

BSNL:

``` text
AT+CGDCONT=1,"IP","bsnlnet"
AT+QICSGP=1,1,"bsnlnet","","",1
AT+QIACT=1
AT+QIACT?
AT+QPING=1,"8.8.8.8"
```

For Jio, first inspect:

``` text
AT+CGDCONT?
```

and use the correct Jio APN rather than blindly reusing the BSNL APN.

------------------------------------------------------------------------

# 26. Useful SMS Command Set

``` text
AT+CMGF=1
AT+CSCS="GSM"
AT+CMGS="+91XXXXXXXXXX"
```

Then message, then Ctrl+Z.

With the current terminal sketch:

``` text
#
```

sends Ctrl+Z.

------------------------------------------------------------------------

# 27. Useful Voice Command Set

Dial:

``` text
ATD+91XXXXXXXXXX;
```

Status:

``` text
AT+CLCC
```

Hang up:

``` text
ATH
```

------------------------------------------------------------------------

# 28. Useful GNSS Command Set

Start:

``` text
AT+QGPS=1
```

Status:

``` text
AT+QGPS?
```

Location:

``` text
AT+QGPSLOC=0
```

GNSS information:

``` text
AT+QGPSINFO
```

GGA:

``` text
AT+QGPSGNMEA="GGA"
```

GSV:

``` text
AT+QGPSGNMEA="GSV"
```

Stop:

``` text
AT+QGPSEND
```

Firmware-dependent GNSS power command:

``` text
AT+QGPSPOWER=1
```

------------------------------------------------------------------------

# 29. GNSS Cheat Sheet

### GNSS enabled

``` text
+QGPS: 1
```

### No satellites

``` text
$GPGSV,1,1,00
$GAGSV,1,1,00
```

### No fix

``` text
$GNGGA,,,,,,0,00,99.99,...
```

### Location unavailable

``` text
+CME ERROR: 516
```

In the current situation, interpret this as no valid GNSS fix and
investigate reception/antenna first.

### Repeated start

``` text
AT+QGPS=1
+CME ERROR: 504
```

after:

``` text
+QGPS: 1
```

Do not repeatedly restart GNSS.

------------------------------------------------------------------------

# 30. Current Project Status

  -----------------------------------------------------------------------
  Feature                 Status                  Notes
  ----------------------- ----------------------- -----------------------
  ESP32 \<-\> EC200U UART **WORKING**             GPIO18 RX, GPIO17 TX,
                                                  115200

  EC200U AT terminal      **WORKING**             `AT` -\> `OK`

  PWRKEY startup          **CURRENT WORKING       GPIO10; preserve
                          SETUP**                 current code

  BSNL SIM/network        **WORKING**             Data tested

  BSNL mobile data        **WORKING**             QPING succeeded

  BSNL SMS                **AVAILABLE**           Standard SMS sequence

  BSNL voice              **NOT WORKING YET**     `NO CARRIER`; IMS not
                                                  registered

  Jio                     **WORKING CELLULAR      Preferred for voice
                          SETUP**                 demo

  GNSS engine             **ON/RESPONDING**       `QGPS=1`, `QGPS?`,
                                                  `QGPSINFO` work

  GNSS satellites         **NOT WORKING YET**     GSV = 0

  GPS fix                 **NOT WORKING YET**     `QGPSLOC=0` -\> 516

  GNSS antenna            **SUSPECT**             Active ceramic antenna
                                                  may be damaged/wrong
                                                  connection

  Hardcoded GPS fallback  **PLANNED**             Use only as clearly
                                                  labelled demo fallback
  -----------------------------------------------------------------------

------------------------------------------------------------------------

# 31. What Must NOT Be Changed Casually

Established:

``` text
EC200U_RX = 18
EC200U_TX = 17
Baud = 115200
PWRKEY = 10
```

Do not change these while investigating GPS.

Do not randomly alter:

-   IMS settings
-   MBN settings
-   PDP context
-   GNSS configuration
-   firmware

Change one variable at a time.

------------------------------------------------------------------------

# 32. Next Steps

### Priority 1 --- GNSS antenna

1.  Inspect active ceramic antenna.
2.  Confirm correct GNSS connector.
3.  Reseat the antenna.
4.  Test outdoors.
5.  Leave GNSS running.
6.  Check `GSV`.
7.  Check `GGA`.
8.  Try `QGPSLOC=0` only after satellites/fix appear.
9.  Test a known-good passive GNSS antenna if available.
10. If another antenna/connector produces satellites, the original
    antenna path is the problem.

### Priority 2 --- Automatic GPS SMS

Once `QGPSLOC=0` works:

``` text
GNSS -> coordinates -> ESP32 -> SMS -> phone
```

### Priority 3 --- Voice demo

Use Jio first.

### Priority 4 --- BSNL VoLTE

Investigate IMS registration separately after the main demo works.

------------------------------------------------------------------------

# 33. Future Cloud/Data Automation

Because BSNL data worked, the EC200U can later support:

``` text
ESP32
  |
  +-- EC200U
       |
       +-- LTE data
             |
             +-- HTTP/HTTPS
             +-- MQTT
             +-- cloud API
```

Possible future payload:

``` text
latitude
longitude
timestamp
device ID
sensor values
battery status
```

This is a separate phase from the SMS/voice/GPS demo.

------------------------------------------------------------------------

# 34. Reference Documentation

Sharvi board page:

https://sharvielectronics.com/product/esp32-s3-with-ec200u-4g-lte-cat-1-wifi-bluetooth-gnss-iot-smart-modem/

Exact EC200U AT Commands Manual used for this project:

https://sharvielectronics.com/wp-content/uploads/2024/09/EC200U_AT_Commands_Manual_V1.0.pdf

Quectel EC200U product page:

https://www.quectel.com/product/lte-ec200u-series/

Quectel EC200U GNSS Application Note:

https://quectel.com/content/uploads/2024/04/Quectel_EC200U_SeriesEG912U-GL_GNSS_Application_Note_V1.2.pdf

Useful Quectel EC200U-CN GNSS thread:

https://forums.quectel.com/t/how-to-get-gps-data-from-ec200u-cn/42933

Useful EC200U-CN GNSS/antenna thread:

https://forums.quectel.com/t/ec200u-version-ec200ucnaar03a13m08-gnss-fix-not-happening/49038

------------------------------------------------------------------------

# 35. Final "Start Here" Block for a New Chat

Paste this at the top of a future troubleshooting chat if necessary:

``` text
I am using a Sharvi/7Semi ESP32-S3 + Quectel EC200U-CN board, hardware version 1.2.

Confirmed working UART:
ESP32 RX = GPIO18
ESP32 TX = GPIO17
Baud = 115200

PWRKEY = GPIO10

Current firmware:
EC200UCNAAR03A13M08

I have both BSNL and Jio SIMs.

Known:
AT -> OK works.
BSNL data works with APN bsnlnet and QPING succeeded.
Jio is currently the preferred SIM for voice calling.
BSNL voice currently gives:
ATD+91XXXXXXXXXX;
OK
NO CARRIER

BSNL IMS diagnostic:
AT+QCFG="ims"
+QCFG: "ims",1,0

AT+CIREG?
+CIREG: 0,0

GNSS:
AT+QGPS=1 -> OK
AT+QGPS? -> +QGPS: 1
AT+QGPSINFO -> +QGPSINFO: UC6228CI,G1B1E1,N/A,R3.4.21.0Build16229,N/A,N/A

AT+QGPSGNMEA="GGA"
-> $GNGGA,,,,,,0,00,99.99,,,,,,*56

AT+QGPSGNMEA="GSV"
-> $GPGSV,1,1,00,0*65
-> $GAGSV,1,1,00,0*74

AT+QGPSLOC=0
-> +CME ERROR: 516

Therefore the main unresolved GPS issue is currently zero satellite reception; the active ceramic GNSS antenna may be broken or connected incorrectly. We will test the antenna/connector and a known-good passive antenna before changing firmware/configuration.

If the antenna is confirmed broken and the demo deadline is near, use a clearly labelled hardcoded demo location in the SMS.

Please preserve the working UART/PWRKEY configuration unless there is a specific reason to change it.
```

------------------------------------------------------------------------

# 36. Core Troubleshooting Lesson

The successful debugging path was:

``` text
Unknown/wrong UART
      |
      v
Tested multiple pin combinations
      |
      v
GPIO18 RX + GPIO17 TX @ 115200
      |
      v
Clean AT -> OK
      |
      v
Network/SIM diagnostics
      |
      v
BSNL APN configured
      |
      v
QPING successful
      |
      v
SMS and voice investigated
      |
      v
Jio identified as the safer voice-demo SIM
      |
      v
GNSS enabled
      |
      v
GNSS chipset responds
      |
      v
GSV = 0 satellites
      |
      v
Antenna/reception becomes the primary GNSS suspect
```

**Continue from the last known state instead of restarting the entire
setup.**
