#!/usr/bin/env python3
"""Auditoría estática de la redistribución de sesiones guiadas V43.12."""
from pathlib import Path
import json

root=Path(__file__).resolve().parents[1]
src=root/"app/src/main/java/com/huntersystem/app"
overlays=(src/"Overlays.kt").read_text(encoding="utf-8")
layout=(src/"GuidedSessionLayoutV43.kt").read_text(encoding="utf-8")
player=overlays.split("@Composable fun SessionPlayer(",1)[-1]
checks={
 "sesión con cabecera específica":'if(overlay.type=="session")' in overlays and 'ENTRENAMIENTO EN CURSO' in overlays,
 "pasos y progreso visibles":'GuidedSessionHeader(preview' in player and 'HProgress(index.toFloat()' in layout,
 "demo adaptativa y sin recorte de ventana":'GuidedSessionDemo(e.name,e.demo' in player and 'BoxWithConstraints' in layout and 'MonarchExerciseStage(' in layout,
 "temporizador integrado con progreso":'GuidedSessionTimer(remaining' in player and 'guidedSessionProgress(totalSeconds,remaining)' in layout,
 "botones principales y opciones":all(x in layout for x in ('REPRODUCIR','REINICIAR','Velocidad','ESPEJO','onToggle','onRestart','onMirror','onSpeed')),
 "serie y navegación intactas":all(x in player for x in ('ui.open("series"','"‹ Anterior"','"Siguiente ›"','pending=true','persist()')),
 "seguridad y guardado":all(x in player for x in ('Lifecycle.Event.ON_STOP','running=false;persist()','ui.open("pain"','s.state.put("guidedSessions"')),
 "ruta de vista previa intacta":all(x in player for x in ('val preview=data.has("routineId")','if(!preview&&date==today())')),
 "temporizador no cambia criterio de segundos":'delay(1000);remaining--' in player,
}
failed=[name for name,ok in checks.items() if not ok]
print(json.dumps({"checks":checks,"status":"OK" if not failed else "ERROR","failed":failed},ensure_ascii=False,indent=2))
if failed:raise SystemExit(1)
