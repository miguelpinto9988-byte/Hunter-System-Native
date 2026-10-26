# Hunter System V43 — Integración Kotlin + Jetpack Compose

## Rama de integración
- Base Android nativa **Monarca 42.9** (código Kotlin, interfaz Jetpack Compose y Canvas).
- Componentes de **Eclipse 41.0**: iconografía, portal, reliquia 3D geométrica y galería de seis temas; navegación y pantallas Monarca preservadas.
- Cambios del PR **Hunter 2000**: `Catalog.kt` admite 1200 registros originales más 800 adicionales sin colisiones; `Rules.kt` excluye de la planificación automática las propuestas sin validación.

## Importante: estado del catálogo
**Esta rama de GitHub por sí sola todavía no contiene los 800 JSON ni los 2000 SVG reemplazados.** Permanece compilable con el catálogo original de 1200 hasta importar el paquete externo. No debe etiquetarse como versión final de 2000 ejercicios antes de subir los recursos y ejecutar las pruebas.

El paquete original suministrado en ChatGPT se llama `Hunter_System_V41_Paquete_Integracion_2000_SVG.zip`. Contiene 2000 SVG y 800 fichas adicionales, marcadas como pendientes de revisión biomecánica y de ilustraciones.

Para importarlo después de clonar esta rama:
```bash
python3 tools/import_hunter2000_assets.py /ruta/Hunter_System_V41_Paquete_Integracion_2000_SVG.zip
python3 tools/import_hunter2000_assets.py --check
./gradlew :app:testDebugUnitTest :app:assembleDebug
```
Posteriormente revisar diferencias, agregar `app/src/main/assets/` al control de versiones, abrir PR y compilar APK firmada desde GitHub Actions.

## Criterios de cierre
1. Verificar en CI que `Catalog.exercises.size==2000` con paquete instalado, 2000 IDs únicos y 2000 SVG presentes.
2. No recomendar automáticamente las 800 fichas nuevas hasta QA específica por ejercicio.
3. Probar pantallas, cambios de tema, restauración del perfil e historiales con catálogo ampliado.
4. Probar SVG/animaciones, sesión y notificaciones en Android real, incluyendo rendimiento y accesibilidad.
5. Validar la firma de la APK y compatibilidad con instalaciones anteriores.

La aplicación continúa usando Kotlin/Compose. Los SVG son recursos vectoriales nativos reproducidos por código Android; no se añade HTML, CSS, JavaScript ni WebView.
