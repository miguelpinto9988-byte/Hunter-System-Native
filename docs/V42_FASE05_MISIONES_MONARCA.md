# Hunter System V42.4 — Fase 05: Misiones Monarca

**Rama**: `design/v42-fase05-misiones-monarca`.  
**Base**: `design/v42-fase04-dashboard-monarca`.  
**Versión**: `42.4-monarca-fase5`.  
**Estado**: implementada para revisión por APK en dispositivo.

## Componentes

- Pantalla de Misiones con encabezado Monarca y modo de entrenamiento original.
- Selector de los siete días de la semana, con tipo de sesión y color fijo: **azul** (fuerza superior), **rosa** (fuerza inferior), **naranja** (cardio), **verde** (movilidad), **gris** (descanso). Los colores se muestran sin alterar el tema visual elegido.
- Misión contextual: título, fecha, estado real, progreso obtenido con `NativeStore.fraction()`, número de ejercicios y tareas.
- Tarjetas individuales de ejercicio: ficha textual, series objetivo, acceso a demostración, series confirmadas y casilla de confirmación manual.
- Tareas complementarias, con persistencia mediante `NativeStore.toggleTask()`.
- Flujo de registro detallado, sesión guiada y acceso a ficha de ejercicio ya existentes.
- Bloqueo de edición cuando existe actividad; historial no editable y días futuros como vista previa.
- Pantalla de descanso y estados sin ejercicios con aviso y sin inicio ficticio de sesión.
- Progreso semanal con sesiones realmente completadas; revisión semanal conectada.
- Calendario mensual y modal de selección de fecha, reutilizando Misiones, sin registros duplicados.
- Recordatorios existentes, acompañante elegido desde Perfil y ajustes del plan.
- Al completar desde Registro, se solicita confirmación explícita. Después de guardar, la confirmación permanece abierta hasta elegir «Continuar».

## Reglas de paridad funcional

1. Mantener el catálogo de **1 200 ejercicios** y las **342 demostraciones MP4**. No añadir miniaturas a las fichas.
2. No introducir Internet, servidor ni permisos adicionales para navegar Misiones.
3. No alterar planificador, generadores, compatibilidad con lesiones ni la generación determinista.
4. Marcar un ejercicio **no contabiliza** automáticamente series, repeticiones ni kilogramos.
5. No se confirman misiones de días pasados ni futuros. La confirmación de cierre utiliza `NativeStore.finish` y las mismas validaciones de RPE y objetivos que antes.
6. El calendario refleja datos guardados y proyecciones. Sin registros confirmados no se inventa progreso.
7. Conservar las pantallas anteriores mediante las funciones de compatibilidad `PlanScreen`, `MissionContent` y `CalendarPanel`, que delegan en el nuevo módulo.

## Revisión manual indispensable

- Android 8+, teléfonos angostos de 320 dp, tablets 8–12 pulgadas y modo horizontal.
- Cambiar cada uno de los nueve temas, aumentar fuente del sistema y recorrer con TalkBack.
- Comprobar que la pantalla conserva el desplazamiento al marcar tareas o series.
- Escoger hoy, ayer y mañana: únicamente hoy debe permitir marcar y registrar; mañana puede personalizarse solo si no existe actividad y lo permite el motor.
- Verificar los cuatro tipos de sesión, descanso, objetivos vacíos, ejercicio archivado y bloqueo por restricciones físicas.
- Confirmar cierre con una actividad pendiente: el motor debe rechazarlo sin crear una sesión completada.
- Registrar series y comprobar que XP y estadísticas reales se actualizan en Inicio.
- Comprobar la firma de actualización sobre una V42 instalada y el funcionamiento sin conexión.

El flujo de notificaciones mantiene el **panel de recordatorios preexistente**. Esta fase no declara un servicio nuevo de notificaciones automáticas. La animación 3D de acompañantes y el reproductor de ejercicios se abordan en fases separadas.
