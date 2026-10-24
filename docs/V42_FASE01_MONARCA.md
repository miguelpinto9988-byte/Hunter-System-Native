# Hunter System V42 — Fase 01: Monarca A+ / Ascenso

**Fecha:** 24 de octubre de 2026
**Rama aislada:** `design/v42-fase01-monarca-a-plus`
**Base conservada:** V39.2 nativa, sin miniaturas.
**Estado:** Fase de integración visual y comprobación de compilación; la migración integral de todas las pantallas corresponde a fases posteriores.

## Objetivo y alcance

Implementar la primera expresión de la identidad original **Monarca A+**, con atmósfera de cazador, sombras, profundidad y energía cian-violeta inspirada en el género RPG de progresión. No se copian personajes, logotipos ni escenas de Solo Leveling o Lector Omnisciente.

La primera fase se limita a:
1. Paleta Eclipse Monarca principal con tonos obsidiana, cian eléctrico, violeta, texto frío y paneles oscuros; otras paletas anteriores siguen disponibles.
2. Fundación Kotlin de tokens de color, tamaños y espaciado en `MonarchV42.kt`.
3. Cinco iconos de navegación dibujados con Compose Canvas, sin assets externos.
4. Prototipo de Inicio Monarca sobre datos REALES del usuario: nombre, rango, nivel, experiencia, misión del día y acompañante elegido en Perfil.
5. Tipografía uniforme para títulos utilizando los recursos Barlow existentes y botón principal de al menos 48 dp.
6. Comprobación estática para que no vuelvan miniaturas de ejercicios.
7. APK firmada de prueba y validación de las pantallas antes de iniciar la siguiente fase.

## Paleta principal

| Token | Color | Aplicación |
|---|---|---|
| `Obsidian` | `#080D19` | Fondo general |
| `Night` | `#111C31` | Tarjeta de portada |
| `Surface` | `#18263F` | Paneles flotantes |
| `Cyan` | `#25C8FF` | Acción principal, brillo |
| `Violet` | `#7F5CFF` | Auras y relieve |
| `Text` | `#E8F1FF` | Textos principales |
| `Muted` | `#B4C6DB` | Información secundaria |
| `Border` | `#36506C` | Contornos |

## Arquitectura de los componentes

- `MonarchV42Palette`: constantes de color, se separarán en tokens semánticos en fase 02.
- `MonarchV42Metrics`: tamaños y duración de referencia de microanimación.
- `MonarchGlyph()`: iconos de navegación en Canvas; sustituyen los caracteres de símbolo dentro de la barra inferior, se mantienen los textos y las rutas actuales.
- `MonarchHero()`: hero original con escena tipo portal estático 2.5D, acompañante activo y estadísticas del perfil. Conserva la navegación a la misión.
- `HunterTheme()`: Eclipse hereda colores Monarca A+, sin alterar rutinas ni otros temas.
- `tools/check_v42_phase1.py`: audita catálogo, vídeos, ausencia de miniaturas y módulos funcionales críticos.

## Restricciones y criterios de validación

- **SIN miniaturas de ejercicios.** No añadir imágenes al listado de 1200 movimientos. Mantener demostraciones animadas SVG/MP4 en reproductor.
- El progreso, nivel, XP, personaje y título de misión se leen del estado local real; no son valores de ejemplo.
- No cambiar algoritmos de rutinas, restricciones, misiones, sincronización ni formato de guardado.
- La selección del acompañante seguirá ocurriendo solamente desde Perfil.
- Mantener completamente offline el entrenamiento.
- Acción de fase 01 reversible en su rama; no fusionar con `main` sin evaluación.
- Compilar release con firma, ejecutar auditoría y revisar Inicio, barra inferior y pantallas de perfil/misiones.
- En fase 02: bibliotecas de componentes premium, estados, microinteracciones, transiciones y adaptación de accesibilidad antes de migrar pantallas masivamente.

## Fases posteriores

Fase 02: sistema de diseño reutilizable, selector temático expandido e iconografía completa.
Fase 03: navegación, carga y acceso.
Fase 04+: migración progresiva de Misiones, Rutinas, Ejercicios, Perfil, Estadísticas, acompañantes 2D/2.5D/3D y QA.
