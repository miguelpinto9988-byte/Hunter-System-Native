# Misiones Monarca — Hunter System V43

Sección nativa Android **Kotlin + Jetpack Compose**, visible en **Inicio** inmediatamente después de la misión diaria. Los desafíos Monarca son **voluntarios** e independientes del calendario de entrenamiento ordinario.

| Desafío | Nivel mínimo | Objetivos | Ventana | Recuperación |
|---|---|---|---|---|
| Guardia del Umbral | Intermedio | 40 sentadillas, 60 jumping jacks y 45 segundos de plancha | 30 minutos | 4 días |
| Asalto a la Fortaleza | Avanzado | 60 sentadillas, 20 burpees y 24 zancadas inversas | 45 minutos | 4 días |
| Juicio del Trono Monarca | Avanzado | 100 sentadillas, 50 burpees y 30 flexiones | 60 minutos | 7 días |

## Funcionalidad
- Selector de desafíos y botón de **inicio explícito** tras aceptar voluntariamente.
- Contador de tiempo restante a partir de un plazo absoluto persistido; cerrar la aplicación no reinicia la ventana.
- Contadores manuales (+1, +5, +10, −1), progreso visual y enlaces a las demostraciones SVG originales del catálogo.
- Al completar todas las metas dentro del plazo, se guarda un **Sello Monarca**. No se inventan series medidas, repeticiones verificadas por sensores ni XP del entrenamiento.
- Se conserva el estado en el JSON local y sus copias exportables; las misiones anteriores se archivan en un historial limitado.
- Si expira el plazo o el usuario se detiene, el progreso se conserva **sin castigos** y entra el intervalo de recuperación.
- Bloqueo a menores, perfiles con dolor, lesiones/limitaciones y ejercicios incompatibles según `Rules.kt`. No se utilizan propuestas de los 800 ejercicios Hunter 2000 sin QA.
- Si cambian las restricciones del perfil mientras se ejecuta el reto, ya no permite registrar ni reclamar el sello sin cumplir nuevamente las reglas.

## Archivos
- `app/src/main/java/com/huntersystem/app/MonarchSpecialMissionState.kt`: presets, estado, recuperación, límites y persistencia.
- `app/src/main/java/com/huntersystem/app/MonarchSpecialMissionUI.kt`: tarjeta de inicio en Compose y temporizador.
- `app/src/main/java/com/huntersystem/app/Screens.kt`: llamada desde `HomeScreen`.
- `app/src/test/java/com/huntersystem/app/MonarchSpecialMissionTest.kt`: pruebas del catálogo de misiones, plazos y no elegibilidad.

## Comprobación
```bash
bash gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest :app:assembleRelease
```

Las cantidades son un **reto extremo de ejemplo**, no una prescripción universal. La condición de adulto/nivel avanzado y la compatibilidad de reglas de software no equivalen a una evaluación médica, fisiológica o biomecánica. Antes de lanzar el reto se recomienda validar su adecuación con profesionales y hacer pruebas en dispositivos Android reales.
