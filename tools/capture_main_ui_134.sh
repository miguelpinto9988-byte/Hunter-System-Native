#!/usr/bin/env bash
set -u
mkdir -p app/build/ui-captures
adb shell mkdir -p /sdcard/Download/monarca134
result=0
for theme in Blanco Negro; do
  adb shell input keyevent KEYCODE_WAKEUP || true
  adb shell wm dismiss-keyguard || true
  adb shell pm clear com.huntersystem.app || true
  bash gradlew --no-daemon --max-workers=2 -Pandroid.testInstrumentationRunnerArguments.class=com.huntersystem.app.ProfileOnDeviceTest -Pandroid.testInstrumentationRunnerArguments.captureTheme="$theme" :app:connectedDebugAndroidTest --rerun > "app/build/ui-captures/test-$theme.txt" 2>&1 || result=1
  cat "app/build/ui-captures/test-$theme.txt"
  adb pull /sdcard/Download/monarca134/. app/build/ui-captures/ || result=1
  adb logcat -d -s HunterStartup HunterProfile AndroidRuntime '*:S' > "app/build/ui-captures/log-$theme.txt" || true
done
python3 - <<'PY' || result=1
from pathlib import Path
p=Path("app/build/ui-captures")
for theme in ("claro","oscuro"):
    for screen in ("panel","misiones","rutinas","ejercicios"):
        for index in range(7):
            path=p/f"{theme}-{screen}-{index:02}.png"
            assert path.is_file() and path.stat().st_size>1024 and path.read_bytes()[:8]==b"\x89PNG\r\n\x1a\n", path
PY
exit "$result"
