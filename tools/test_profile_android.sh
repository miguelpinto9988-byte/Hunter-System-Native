#!/usr/bin/env bash
# The emulator runner launches individual YAML lines in separate shells.
# Run all commands in one process so the Gradle exit status is preserved.
set +e
bash gradlew --no-daemon --max-workers=2 :app:connectedDebugAndroidTest
result=$?
mkdir -p app/build/qa
adb logcat -d -s HunterStartup HunterProfile AndroidRuntime '*:S' > app/build/qa/startup-logcat.txt || true
for name in 1-claro 2-claro 3-claro 3-oscuro 4-oscuro; do
  capture="profile-$name.png"
  if ! adb exec-out run-as com.huntersystem.app cat "files/$capture" > "app/build/qa/$capture"; then
    rm -f "app/build/qa/$capture"
  fi
done
exit "$result"
