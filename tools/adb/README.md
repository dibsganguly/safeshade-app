# Driving the app over adb

`source tools/adb/drive.sh` in Git Bash, then:

- `launch` — force-stop, start, and wait until MainActivity holds focus.
- `tab board|circle|safety|device` — tap a bottom-bar tab.
- `tapfind "label"` — find a node whose text or content description matches
  (exact first, then contains) and tap it. Rows announce as "Name, State,
  detail", so `tapfind "medical id"` hits the Medical ID row.
- `field "label"` — tap the text field 95px under a PlateField's label.
- `hideime` — send back only if the keyboard is showing (back with no keyboard
  pops the screen).
- `shot name` — screenshot to `$SP/name.png`.

Traps, all paid for: taps fired before `waitapp` land on whatever was there;
`uiautomator dump` prints a MIUI stack trace and still works; MIUI ignores
`cmd uimode`, use the app's own Appearance setting; the photo picker is
multi-select and needs its Done button; never `adb pull /sdcard/` wholesale.
