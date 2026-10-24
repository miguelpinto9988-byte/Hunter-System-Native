# Hunter System V42 — Fase 03: navegación y acceso

**Rama:** `design/v42-fase03-navegacion-acceso`  
**Versión de prueba:** `42.2-monarca-fase3`  
**Dependencia:** fase 02, con sistema visual Monarca y nueve temas.

## Cambios implementados

1. **Arranque seguro y offline**: pantalla de carga Monarca en Compose con indicador indeterminado real; botón de reintento cuando el proceso falla. No se añade login, registro, servidor ni operaciones de red al arranque.
2. **Splash de plataforma**: fondo obsidiana y icono de app en Android 12 o superior (`values-v31/styles.xml`); color del fondo de ventana integrado para versiones anteriores.
3. **Navegación inferior móvil**: cinco destinos fijos — Panel, Misiones, Rutinas, Ejercicios, Perfil — con iconos vectoriales originales, estados activos, colores del tema, 75 dp de alto e inset de navegación de Android.
4. **Navegación lateral de tablet**: a partir de 840 dp de ancho, se sustituye la barra inferior por un rail estrecho. Esto evita los botones excesivamente anchos en pantallas grandes.
5. **Transiciones**: cambios entre pantallas con desvanecimiento y desplazamiento horizontal muy corto. Duran menos de 220 ms y no requieren motor gráfico de terceros.
6. **Accesibilidad**: los destinos incluyen un texto de accesibilidad y estado seleccionado; los tamaños táctiles de la fase 02 se conservan.
7. **Retención de UI**: se introdujo `rememberSaveableStateHolder` para el estado guardable de rutas y `ScrollState.Saver` para preservar desplazamiento al alternar pestañas.
8. **Recuperación**: si no se puede leer el guardado, el menú de recuperación explica el problema y permite cargar un respaldo válido sin borrar silenciosamente los datos.
9. **Pantallas adaptables**: `SceneColumn` centra la información de la pantalla y limita el contenido a 960 dp para facilitar la lectura en tablets.
10. **Restauración de Activity**: persiste la ruta seleccionada y el estado de las secciones `top`/`library` durante recreaciones.

## Archivos principales

- `MonarchNavigationV42.kt`: cinco pestañas, splash, recovery, transiciones y navegación por rail.
- `MainActivity.kt`: host de rutas, responsive y restablecimiento de estado.
- `Screens.kt`: retención de desplazamiento, ancho máximo.
- `styles.xml` y `values-v31/styles.xml`: identidad de la ventana/splash.
- `check_v42_phase3.py`: control del contrato visual y de recursos dentro de CI.

## Compatibilidad y restricciones

- Base **Android nativo Kotlin + Jetpack Compose**, mínimo API 26.
- Catálogo conservado: **1200 ejercicios**. Demostraciones conservadas: **342 MP4** y recursos SVG.
- No se reintroducen miniaturas de ejercicios.
- Se conservan `NativeStore`, `Rules`, `Planner`, perfiles, historial y lógica de generación.
- Los cambios no se fusionan con la rama estable hasta la aprobación.
- Los campos de formulario **que todavía no empleen `rememberSaveable`** no deben asumirse retenidos entre pantallas; la pantalla de perfil y sus borradores son objeto de revisión posterior.

## Matriz de validación

| Escenario | Comprobación |
|---|---|
| Inicio por primera vez | Presenta Perfil sin login ni servicio externo |
| Inicio con perfil | Abre Panel mostrando datos reales |
| Android 12+ | Splash de plataforma enlaza con carga Compose |
| Android <12 | Fondo de ventana obsidiana enlaza con carga Compose |
| Teléfono compacto | Barra inferior con cinco destinos utilizables |
| Tablet >= 840 dp | Rail lateral en lugar de la barra inferior |
| Volver | Cierra primero ventana; después subtipo; finalmente retorna a Panel |
| Cambio de sección | No borra datos de entrenamiento ni perfil guardado |
| Cierre y retorno | Recupera la sección guardada por Activity cuando corresponda |
| Guardado defectuoso | Ofrece importar copia; no sobrescribe el respaldo corrupto |
| QA manual | Revisar rotación, TalkBack, tamaño de fuente alto y teclados |

El éxito de CI acredita compilación y controles estáticos, **no** equivale a prueba manual de Android o certificación de rendimiento.
