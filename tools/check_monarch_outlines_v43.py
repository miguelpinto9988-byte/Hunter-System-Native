#!/usr/bin/env python3
"""Auditoría de contornos suaves V43.12; sin modificar los archivos."""
from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/java/com/huntersystem/app"
FILES = {p.name:p.read_text(encoding="utf-8") for p in SOURCE.glob("*.kt")}
checks = {
    "espesor reposo 0.8 dp": "if(emphasis)1.4.dp else .8.dp" in FILES.get("MonarchOutlineV43.kt", ""),
    "color normal sutil y acento activo": all(
        fragment in FILES.get("MonarchOutlineV43.kt", "")
        for fragment in ("lerp(c.line,c.accent,.16f)", "c.accent.copy(alpha=.96f)")),
    "transición 170 ms y movimiento reducido": all(
        fragment in FILES.get("MonarchOutlineV43.kt", "")
        for fragment in ("tween(170)", "if(reduced)snap()", "animateDpAsState", "animateColorAsState")),
    "tarjeta pulsable": ".monarchOutline(shape,pressed=pressed)" in FILES.get("Design.kt", ""),
    "botón pulsable": ".monarchOutline(shape,active=primary,pressed=pressed,enabled=enabled)" in FILES.get("Design.kt", ""),
    "chip seleccionable": ".monarchOutline(shape,active=selected,pressed=pressed)" in FILES.get("Design.kt", ""),
    "campo enfocado": ".monarchOutline(shape,focused=focused)" in FILES.get("Design.kt", ""),
    "cuadro de diálogo": ".monarchOutline(RoundedCornerShape(HunterDesignSystem.Dimensions.cornerDialog))" in FILES.get("Overlays.kt", ""),
    "perfil y vista previa": ".monarchOutline(shape)" in FILES.get("HunterProfileLayoutV43.kt", ""),
    "panel Monarca": ".monarchOutline(shape)" in FILES.get("MonarchDashboardV42.kt", ""),
    "misiones y días pulsables": ".monarchPressable(" in FILES.get("MonarchMissionsV42.kt", ""),
    "rutinas pulsables": ".monarchPressable(" in FILES.get("MonarchRoutinesV42.kt", ""),
    "navegación": ".monarchOutline(shape,active=selected,pressed=pressed)" in FILES.get("MonarchNavigationV42.kt", ""),
    "selectores": ".monarchPressable(" in FILES.get("EclipseComponents.kt", ""),
    "sin perder datos": all(x in FILES.get("Design.kt", "") for x in ("onClick:()->Unit", "enabled:Boolean", "onValueChange=onChange")),
}
stats = {
    "archivosKotlin": len(FILES),
    "contornosCentralizados": sum(s.count(".monarchOutline(") for s in FILES.values()),
    "contenedoresInteractivos": sum(s.count(".monarchPressable(") for s in FILES.values()),
    "bordesDirectosRemanentes": sum(s.count(".border(") for s in FILES.values()),
}
errors = [k for k,v in checks.items() if not v]
print(json.dumps({"estado":"OK" if not errors else "ERROR", "comprobaciones":checks,
                  "metricas":stats, "errores":errors},ensure_ascii=False,indent=2))
if errors: raise SystemExit(1)
