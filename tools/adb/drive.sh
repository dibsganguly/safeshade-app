SP="${SP:-$TEMP/safeshade-drive}"; mkdir -p "$SP"
# A Windows path for python: with MSYS_NO_PATHCONV set below, /c/... is not
# translated and python cannot open it, so TOOLS is resolved to C:/... first.
TOOLS="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -W 2>/dev/null || pwd)"
export MSYS_NO_PATHCONV=1
shot(){ adb shell screencap -p /sdcard/$1.png; adb pull /sdcard/$1.png "$SP/$1.png" >/dev/null; }
tapfind(){ r=$(python "$TOOLS/tapfind.py" "$1" "$SP/ui.xml" 2>/dev/null); echo "$1 -> $r"; if [ "$r" != "NOTFOUND" ]; then adb shell input tap $r; adb shell sleep ${2:-1.5}; fi; }
field(){ r=$(python "$TOOLS/tapfind.py" "$1" "$SP/ui.xml" 2>/dev/null); if [ "$r" != "NOTFOUND" ]; then set -- $r; adb shell input tap $1 $(( $2 + 95 )); adb shell sleep 0.8; else echo "field $1 NOTFOUND"; fi; }
hideime(){ if adb shell dumpsys input_method | grep -q "mInputShown=true"; then adb shell input keyevent 4; adb shell sleep 0.6; fi; }
waitapp(){ for i in $(seq 1 20); do if adb shell dumpsys window 2>/dev/null | grep mCurrentFocus | grep -q com.safeshade; then adb shell sleep 2; return; fi; adb shell sleep 1; done; }
launch(){ adb shell am force-stop com.safeshade; adb shell am start -n com.safeshade/.MainActivity >/dev/null; waitapp; }
tab(){ case $1 in board) x=99;; circle) x=320;; safety) x=759;; device) x=979;; esac; adb shell input tap $x 2244; adb shell sleep 1.5; }
