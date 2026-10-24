# Hunter System V42.3 — Fase 04: Inicio / Dashboard Monarca

**Rama:** `design/v42-fase04-dashboard-monarca`  
**Origen:** `design/v42-fase03-navegacion-acceso`  
**Versión:** `42.3-monarca-fase4`

## Implementación en código nativo

El Inicio deja atrás los recuadros repetidos y presenta, de arriba a abajo:

1. **Bienvenida contextual.** Muestra el nombre del perfil, sin solicitar Internet.
2. **Hero Monarca 2.5D.** Fondo con círculos, líneas de profundidad y dos capas; ilustración WebP local del acompañante elegido en Perfil; nombre, nivel, rango y barra de XP. No incluye modelos tridimensionales ni efectos costosos en tiempo real.
3. **Misión principal.** Abre el flujo de misión que ya existía; distingue activa, completada, pausada, descanso y ausencia de actividades. Calcula progreso con `NativeStore.fraction(log)`; la acción editar solo aparece cuando no existen registros activos.
4. **Indicadores reales.** Racha, días completados, ejercicios realizados y series ejecutadas provienen de `NativeStore.stats()`; pulsar abre Progreso. No se inventan niveles ni se usa la cifra estática de rutinas.
5. **Semana de siete días.** Pestañas individuales con información del día, tipo, estado completado y navegación a misión. Revisión semanal enlazada con el flujo existente.
6. **Accesos rápidos.** Rutinas offline, objetivos, academia y recordatorios (rutas existentes).
7. **Acompañante.** Refleja personaje configurado en Perfil y abre consejos; la selección sigue limitada a Perfil.
8. **Objetivos personales.** Permite marcar metas reales, añadir una nueva o ver toda la colección.
9. **Plan actual.** Resume objetivo, días configurados e intensidad, enlazando con Perfil y mostrando la razón del plan según el motor de reglas.

El diseño usa **colores semánticos del tema seleccionado** y redistribuye tarjetas automáticamente en dos columnas para teléfonos y cuatro para pantallas de >=700 dp. El contenedor mantiene el máximo de 960 dp establecido en la fase anterior. Las tarjetas mantienen iconografía original y etiquetas accesibles.

## Compatibilidad

- Android Kotlin + Jetpack Compose, totalmente offline para sesiones y recursos ya integrados.
- Se conserva `NativeStore`, `Rules`, `Planner`, `Goals` y la estructura de archivos locales.
- El catálogo conserva **1200 ejercicios** y **342 videos MP4**.
- No se introducen miniaturas de ejercicios ni se recrea lógica de planificación.
- La fase no supone una nueva firma ni cambia `applicationId`. La APK continúa utilizando el secreto de firma existente en GitHub Actions.
- La ilustración de personaje procede de `ui33/companion-*.webp`, ya incluida localmente.

## Archivos modificados

| Archivo | Motivo |
|---|---|
| `MonarchDashboardV42.kt` | Componentes específicos del Inicio |
| `MonarchV42.kt` | Mantiene `MonarchHero` como fachada del diseño nuevo |
| `Screens.kt` | Sustitución de `HomeScreen` sin alterar pantallas adyacentes |
| `app/build.gradle.kts` | Versión 42.3 |
| `tools/check_v42_phase4.py` | Reglas de integración, conservación de activos y conexiones |
| `.github/workflows/android-apk.yml` | Auditoría y compilación de APK Fase 04 |

## QA requerido en dispositivo

Revisar: ancho 320–400 dp, tablet 8–12 pulgadas, orientación horizontal, fuentes grandes y TalkBack. Verificar primero que los botones no se recorten con barras del sistema. Confirmar que la misma misión aparece antes/después de abrir el Inicio, que XP se actualiza al guardar ejercicio y que el botón de personalizar desaparece al registrar actividad. Comparar pantallas con los nueve temas.

**CI debe comprobar compilación y auditorías.** Ni la prueba estática ni una compilación exitosa certifican que no existan errores visuales en un dispositivo real.
