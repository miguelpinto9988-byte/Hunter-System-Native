#!/usr/bin/env bash
set -euo pipefail
mkdir -p app/build/ui-captures
bash gradlew --no-daemon --max-workers=2 :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
for theme in Blanco Negro; do
  adb shell pm clear com.huntersystem.app
  adb shell am instrument -w -e class com.huntersystem.app.ProfileOnDeviceTest -e captureTheme "$theme" com.huntersystem.app.test/androidx.test.runner.AndroidJUnitRunner | tee "app/build/ui-captures/test-$theme.txt"
  files="$(adb shell run-as com.huntersystem.app ls files | tr -d '\r' | grep '^capture-' || true)"
  for f in $files; do
    adb exec-out run-as com.huntersystem.app cat "files/$f" > "app/build/ui-captures/$f"
    test -s "app/build/ui-captures/$f"
  done
  grep -q 'OK (1 test)' "app/build/ui-captures/test-$theme.txt"
done
