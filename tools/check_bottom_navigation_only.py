#!/usr/bin/env python3
"""Valida que la barra inferior sea la unica navegación principal en Android."""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
source=ROOT/"app/src/main/java/com/huntersystem/app"
main=(source/"MainActivity.kt").read_text(encoding="utf-8")
nav=(source/"MonarchNavigationV42.kt").read_text(encoding="utf-8")
screens=(source/"Screens.kt").read_text(encoding="utf-8")
dashboard=(source/"MonarchDashboardV42.kt").read_text(encoding="utf-8")
profile=(source/"Profile.kt").read_text(encoding="utf-8")
errors=[]
if (source/"HunterSideMenu.kt").exists():
    errors.append("Aun existe HunterSideMenu.kt")
if any(x in main for x in ("ModalNavigationDrawer(","ModalDrawerSheet(","MonarchNavigationRail(","rememberDrawerState(")):
    errors.append("Persisten los componentes de navegación lateral")
if "MonarchBottomNavigation(ui)" not in main or "!tablet" in main:
    errors.append("La navegación inferior no se usa en todas las pantallas")
if "if(hasProfile && !profileStandalone" not in main:
    errors.append("La navegación inferior no debe aparecer en la pantalla de perfil")
if 'setOf("objectives","stats","profile")' in nav:
    errors.append("El perfil no debe ser una pestaña del menú principal")
if "MonarchNavigationRail(" in nav:
    errors.append("Persiste el rail lateral para tabletas")
if "openMenu" in screens or "Abrir menú lateral" in screens:
    errors.append("Persiste el botón de menú lateral")
if "ui.go(\"profile\")" not in screens:
    errors.append("El avatar debe continuar abriendo el perfil")
if 'it.open("academy")' not in dashboard or 'it.open("reminders")' not in dashboard:
    errors.append("Accesos del panel a Academia y Recordatorios no encontrados")
if 'ui.open("reminders")' not in profile:
    errors.append("Acceso a recordatorios del perfil no encontrado")
for tab in ('"home"','"plan"','"routines"','"book"'):
    if tab not in nav:
        errors.append("Destino principal ausente: "+tab)
if errors:
    raise SystemExit("\n".join("ERROR: "+error for error in errors))
print("OK: navegación inferior única, sin menú lateral; accesos secundarios conservados")
