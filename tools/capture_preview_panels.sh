#!/usr/bin/env bash
set -u
mkdir -p app/build/ui-captures
result=0
for theme in Blanco Negro; do
  adb shell input keyevent KEYCODE_WAKEUP || true
  adb shell wm dismiss-keyguard || true
  adb shell pm clear com.huntersystem.app || true
  bash gradlew --no-daemon --max-workers=2 -Pandroid.testInstrumentationRunnerArguments.class=com.huntersystem.app.MainPanelsCaptureTest -Pandroid.testInstrumentationRunnerArguments.captureTheme="$theme" :app:connectedDebugAndroidTest --rerun > "app/build/ui-captures/test-$theme.txt" 2>&1 || result=1
  cat "app/build/ui-captures/test-$theme.txt"
  adb exec-out screencap -p > "app/build/ui-captures/diagnostic-$theme.png" || true
  adb logcat -d -s HunterStartup HunterProfile AndroidRuntime '*:S' > "app/build/ui-captures/log-$theme.txt" || true
  files="$(adb shell run-as com.huntersystem.app ls files | tr -d '\r' | grep '^capture-' || true)"
  for f in $files; do
    adb exec-out run-as com.huntersystem.app cat "files/$f" > "app/build/ui-captures/$f" || result=1
  done
done
exit "$result"
