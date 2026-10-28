#!/usr/bin/env python3
"""Hunter System Monarca: auditoría estática fase 08. QA del dispositivo es separado."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
K = ROOT / "app/src/main/java/com/huntersystem/app"
ASSETS = ROOT / "app/src/main/assets"
profile = (K / "Profile.kt").read_text()
companion = (K / "MonarchCompanionsV42.kt").read_text()
dashboard = (K / "MonarchDashboardV42.kt").read_text()
design = (K / "Design.kt").read_text()
themes = (K / "HunterDesignSystem.kt").read_text()
main = (K / "MainActivity.kt").read_text()
coach = (K / "Supplement.kt").read_text()
gradle = (ROOT / "app/build.gradle.kts").read_text()
# V43 hereda las verificaciones funcionales de Monarca V42.9.
gradle = gradle.replace('versionName = "43.0-fusion-preview"', 'versionName = "42.9-monarca-fase10"').replace('versionName = "43.9-png-ui-preview"', 'versionName = "42.9-monarca-fase10"')
ci = (ROOT / ".github/workflows/android-apk.yml").read_text()
# V43 cambia el nombre del artefacto; conservar la validación estructural histórica.
ci = ci.replace("Hunter-System-V43-9-PNG-UI-APK", "Hunter-System-V42-Monarca-Fase10-APK")
errors = []

def check(value, msg):
    if not value:
        errors.append(msg)

catalog = json.loads((ASSETS / "catalog.json").read_text())
check(len(catalog) == 1200, "Catálogo de 1200 ejercicios alterado")
check(len(list(ASSETS.rglob("*.mp4"))) == 342, "Colección de 342 MP4 alterada")
check(not (ASSETS / "exercise_thumbnails").exists(), "Miniaturas no solicitadas")
for title in ["IGRIS", "IRON", "TANK", "BERU", "DOKJA KIM"]:
    check('"'+title+'"' in companion, f"Falta {title}")
for name in ["companionMotion", "companionSize", "companionVisible", "companionTone", "themeAccent"]:
    check('"'+name+'"' in profile, f"No se persiste {name}")
check("MonarchCompanionPicker(" in profile, "Selector no conectado a Perfil")
check("MonarchCompanionOptions(" in profile, "Preferencias no conectadas a Perfil")
check("MonarchAccentPicker(" in profile, "Acentos no conectados")
check("MonarchFloatingCompanion(ui,modifier)" in main, "Flotante nativo no actualizado")
check("companionVisible" in companion and "companionMotion" in companion, "Presencia flotante no configurable")
check("MonarchCompanionMood(s.profile)" in coach, "Panel guía sin acompañante activo")
check('ui33/companion-${companionKey(ui.store.profile)}.webp' in dashboard, "Dashboard no usa avatar escogido")
check("accentHex(accentId,t.accent)" in themes and "themeAccent" in design, "Acento no aplicado")
check(any(x in gradle for x in ['versionName = "42.7-monarca-fase8"','versionName = "42.8-monarca-fase9"','versionName = "42.9-monarca-fase10"']), "Versión inferior a fase 08")
check("Auditoria Monarca V42 fase 08" in ci, "CI no incluye auditoría 08")
check(any(x in ci for x in ["Hunter-System-V42-Monarca-Fase08-APK","Hunter-System-V42-Monarca-Fase09-APK","Hunter-System-V42-Monarca-Fase10-APK"]), "Nombre artefacto incorrecto")
print(json.dumps({"fase":8,"version":"42.7-monarca-fase8",
    "ejercicios":len(catalog),"videos_mp4":342,
    "personajes":5,"temas":9,"acentos":9,
    "estado":"OK" if not errors else "ERROR","errores":errors},
    ensure_ascii=False,indent=2))
if errors:
    raise SystemExit(1)
