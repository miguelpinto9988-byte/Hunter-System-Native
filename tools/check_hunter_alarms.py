#!/usr/bin/env python3
"""Auditoría de alarmas V43.12; acompaña las pruebas reales JVM y del APK."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/com/huntersystem/app"
read=lambda path:(ROOT/path).read_text(encoding="utf-8")
engine=read("app/src/main/java/com/huntersystem/app/HunterAlarms.kt")
ui=read("app/src/main/java/com/huntersystem/app/HunterAlarmUi.kt")
manifest=read("app/src/main/AndroidManifest.xml")
main=read("app/src/main/java/com/huntersystem/app/MainActivity.kt")
profile=read("app/src/main/java/com/huntersystem/app/Profile.kt")
supplement=read("app/src/main/java/com/huntersystem/app/Supplement.kt")
tests=read("app/src/test/java/com/huntersystem/app/HunterAlarmsTest.kt")
checks={
 "programacion y receptor":all(x in engine for x in ("object HunterAlarms","class HunterAlarmReceiver","setExactAndAllowWhileIdle","setAndAllowWhileIdle")),
 "permisos honestos":all(x in manifest for x in ("POST_NOTIFICATIONS","SCHEDULE_EXACT_ALARM","RECEIVE_BOOT_COMPLETED")),
 "rearme del reloj":all(x in manifest for x in ("BOOT_COMPLETED","TIMEZONE_CHANGED","TIME_SET","MY_PACKAGE_REPLACED")),
 "avisos y acciones":all(x in engine for x in ("NotificationChannel","CATEGORY_ALARM","Posponer 10 min","Descartar","SNOOZE","DISMISS")),
 "permisos opcionales":all(x in engine+ui for x in ("canScheduleExactAlarms","ACTION_REQUEST_SCHEDULE_EXACT_ALARM")),
 "guardado sin conexión":all(x in engine for x in ("hunterAlarms43","store.save()","persist(","legacy(","next(")),
 "edicion en Compose":all(x in ui for x in ("fun HunterAlarmScreen(","TimePickerDialog","Mis alarmas","Guardar","Eliminar")),
 "integracion de app":all(x in main+profile+supplement for x in ("HunterAlarms.sync(","HunterAlarmScreen(","Alarmas del cazador")),
 "pruebas recurrencia migracion":all(x in tests for x in ("multipleDaysAndNextWeek","migrateOldDailyAndInterval","jsonRoundTrip","supportsDaylightSaving")),
 "sin notificador antiguo":"ReminderScheduler" not in "\n".join(p.read_text() for p in SRC.glob("*.kt")),
 "sin menu lateral":not (SRC/"HunterSideMenu.kt").exists() and "ModalNavigationDrawer" not in main,
}
print(json.dumps({"version":"43.12-alarmas","checks":checks,
 "status":"OK" if all(checks.values()) else "ERROR"},indent=2,ensure_ascii=False))
if not all(checks.values()):raise SystemExit(1)
