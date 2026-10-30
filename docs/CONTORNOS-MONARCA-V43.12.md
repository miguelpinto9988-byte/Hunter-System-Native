# Hunter System V43.12 — contornos Monarca

## Referencia visual
Inspirado en las maquetas aprobadas (modo claro de borde Eclipse y oscuro de borde rosa): un marco casi imperceptible en reposo y un acento intenso al tocar o seleccionar, sin marcos gruesos.

## Implementación nativa
La aplicación usa **Jetpack Compose/Kotlin**, no Godot. La capa de estilo se centraliza en `MonarchOutlineV43.kt`:
- Reposo: **0,8 dp** y mezcla de línea neutra + 16 % del acento, con transparencia.
- Pulsación, selección y foco: **1,4 dp**, acento vigente con 96 % de opacidad.
- Transición: **170 ms**; instantánea si se configura reducir movimiento.
- Respeta el valor existente de `LocalHunter.current.accent` y las esquinas de cada tarjeta. No exige colores nuevos ni cambia la persistencia.

## Alcance
Se aplica a los componentes reutilizables HPanel, HButton, HChip, HInput, HCheck y a selecciones de personajes, temas, días, rutinas, misiones, panel principal, navegación, ejercicios, vista previa de perfil y overlays.

Excepciones deliberadas: el botón de seguridad `Detener por molestias` mantiene su borde de advertencia y la barra inferior conserva un separador neutral. Se respetan las funciones de progreso y los datos guardados.

## Validación
```bash
python3 tools/check_monarch_outlines_v43.py
python3 tools/check_v43_fusion.py
./gradlew :app:testDebugUnitTest :app:assembleRelease
```

Revisar visualmente en dispositivos Android el aspecto en reposo, pulsación, foco, selección y ajuste de movimiento reducido. La referencia previa de tema claro/oscuro y los cinco colores es una **maqueta visual**; esta entrega implementa el estilo de contornos sobre las opciones actuales, sin sustituir todavía el motor de temas por el esquema de dos modos.
