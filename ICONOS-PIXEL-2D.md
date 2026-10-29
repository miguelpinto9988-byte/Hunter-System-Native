# Hunter System V43.12 — familia de íconos Sólido minimalista HD

Diseño escogido: **Sólido minimalista**, no las propuestas posteriores de estilo 3D, cristal o insignias. Sustituye los 49 SVG pixel art anteriores con siluetas suaves, planas, sin contornos negros ni brillos y fondo transparente. Los SVG son **vectores**, por lo que no tienen una resolución máxima fija.

## Familia de color

| Uso | Color base | Identificador |
|---|---|---|
| Tren superior / fuerza | Azul eléctrico | `#368EFF` |
| Tren central / core | Turquesa | `#04CDB9` |
| Tren inferior | Coral | `#FF526A` |
| Movilidad | Violeta | `#A768F4` |
| Cardio | Rosa intenso | `#FF4086` |
| Descanso | Amarillo cálido | `#FFC640` |

Los destinos principales Panel, Misiones, Rutinas y Ejercicios conservan símbolos familiares: casa azul, corona azul/violeta, mancuerna azul y libro violeta. Acciones secundarias usan los mismos colores y geometría.

## Nitidez y rendimiento

- **49 SVG** en `app/src/main/assets/hunter-icons/`, con `viewBox="0 0 96 96"` y dimensiones de exportación **1024 × 1024 px** (sin fondo).
- El renderizador Android `HunterPngIcon` conserva su nombre y sus 76 claves para mantener compatibilidad.
- Rasterización adaptativa según los **píxeles físicos del dispositivo**, con sobremuestreo ×2, en tamaños de 128, 192, 256, 384, 512 y 1024 px.
- Interpolación `FilterQuality.High` y una caché LRU limitada a **32 MiB**.
- Los iconos dinámicos de `V39WeekPicker.kt` continúan reflejando los valores reales `weekPlan` / `weekFocus`.
- El ícono de inicio nativo Android adopta una corona azul/violeta con fondo sólido.
- Se mantienen fuera de la APK los íconos PNG heredados y los fondos generales de sombras.

## Fuera de alcance

No se modifica el arte de Misiones Monarca, el catálogo de ejercicios, las imágenes de los jefes, los datos de progreso ni la navegación inferior única.

Las comprobaciones CI validan las 49 fuentes SVG, la paleta de categorías, el renderer HD, la inexistencia de imágenes heredadas y las pruebas de escalado.
