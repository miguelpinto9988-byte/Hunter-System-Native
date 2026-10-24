# Hunter System V42.9 · Fase 10 — Microinteracciones y fluidez

## Realizado
- Nuevo controlador `MonarchMotionV42.kt`, compartido mediante `CompositionLocal` por toda la aplicación, desde Splash hasta las gráficas.
- Preferencia **Reducir animaciones y transiciones** persistida en `SharedPreferences`, por separado del perfil. Cambiarla **no llama a saveProfile**, no regenera misiones ni modifica objetivos.
- Cuando Android indica escala global de animadores igual a cero, se respetan ajustes de accesibilidad del sistema.
- Los controles `HPanel` clicables, `HButton` y `HChip` ofrecen respuesta breve al presionar. Se aplica la transformación al `graphicsLayer` de Compose para no recalcular las dimensiones de los elementos.
- `HProgress` ahora anima los valores reales y expone semántica de progreso para lectores de pantalla. Los gráficos de series/modalidades usan la misma política.
- Las transiciones de pestañas usan un deslizamiento discreto y se eliminan bajo movimiento reducido. El pulso continuo de Splash y del acompañante se suspende en modo reducido.
- No se introducen vibraciones repetitivas, animaciones estroboscópicas ni dependencias de red.

## Compatibilidad y QA
- Se conservan el catálogo de 1200 ejercicios, 342 MP4 locales, los perfiles y registros, nueve temas y colores de acento.
- CI conserva las auditorías de las fases 01–09 y agrega `tools/check_v42_phase10.py`.
- Ejecutar pruebas manuales pendientes en dispositivos físicos: navegación rápidamente alternada, Android con `ANIMATOR_DURATION_SCALE=0`, modo reducido manual, rotación, tablet, acceso de lector de pantalla y condiciones térmicas/energía.
- La compilación exitosa **no demuestra** 90 FPS ni garantiza compatibilidad en todos los Android; se requiere medición en dispositivo.
