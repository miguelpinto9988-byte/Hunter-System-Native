# Hunter System V43.12 · Perfil independiente

## Primer inicio

1. Android muestra la pantalla de carga de Monarca y abre los datos guardados.
2. Cuando el dispositivo **no tiene un perfil**, se abre **Crear tu perfil** a pantalla completa, sin barra de navegación inferior ni encabezado de pestañas.
3. El usuario completa los datos de entrenamiento, la semana, equipo, preferencias y restricciones.
4. Al pulsar **Guardar perfil**, `NativeStore.saveProfile` persiste la configuración, aplica la interpretación del objetivo y llama a `refreshWeek()` para preparar la planificación.
5. Solo después del guardado correcto, `NativeUi.profileSaved()` lleva al **Panel**; no hay diálogo que interrumpa el cambio de pantalla.
6. Al volver a abrir la APK con un perfil válido, se salta la creación inicial y aparece el Panel.

## Editar desde el encabezado

El avatar del extremo superior derecho conserva `ui.go("profile")`, que ahora abre el editor independiente. No es una pestaña. La barra inferior desaparece mientras se edita, y **VOLVER AL PANEL** descarta solo los cambios no guardados. Guardar lleva al Panel con datos actualizados.

## Compatibilidad y protección de datos

No se modifican archivos de catálogo, misiones, estadísticas, equipos, historia ni modelos. La regeneración de días pendientes y la conservación de progreso completado siguen siendo responsabilidad de la implementación existente de `NativeStore`. Si los datos están corruptos, la pantalla de recuperación sigue teniendo prioridad. Se conserva el estado de apertura del editor ante una recreación de la actividad. Las copias de seguridad que carecen de perfil también abren la pantalla de creación.

Pruebas: `StandaloneProfileNavigationTest.kt`, `ComposeSmokeTest.kt` y `tools/check_standalone_profile.py`.
