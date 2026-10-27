# Hunter System V43.2 — Estado de integración SVG

- La rama de desarrollo integra Monarca 42.9 y Eclipse 41.0 en Kotlin / Jetpack Compose.
- Catálogo: 1200 + 800 = 2000 ejercicios, rutas de demostración originales `demoPath30`.
- Las 2000 demostraciones SVG han sido recuperadas de un commit previo y vuelven a reproducirse mediante `OriginalSvg` (Android Canvas / AndroidSVG).
- Se ha retirado la dependencia Rive, el componente `HunterRiveExercise` y el directorio `demos-rive`; las auditorías verifican que no estén en la APK.
- Las 800 fichas adicionales permanecen excluidas de planificación automática y pendientes de QA.
- La batería de CI comprueba XML, SHA-256 y presencia de assets, pero se requieren pruebas visuales, rendimiento y evaluación biomecánica en Android real.
- Protocolo: [QA-ANDROID-SVG-V43.md](docs/QA-ANDROID-SVG-V43.md). PR #17 sigue en borrador. No modificar `main` mientras haya puertas de QA abiertas.
