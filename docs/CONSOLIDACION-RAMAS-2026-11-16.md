# Consolidación de ramas — 16 de noviembre de 2026

## Rama principal

`main` conserva Hunter System V43.12 Monarca Eclipse y el motor
Asterion de clasificación, progreso y adaptación integrado mediante
[PR #26](https://github.com/miguelpinto9988-byte/Hunter-System-Native/pull/26),
merge `d0ddabd8fa38f77da8f5e5b7cc798a6093e1453b`.

La compilación Android y la batería independiente de pruebas de
Asterion pasaron en GitHub Actions sobre ese merge. La inspección física
de la experiencia de usuario y la unificación de los dos planificadores
de progreso siguen pendientes.

## Ramas eliminadas (21)

Se eliminaron 17 ramas después de verificar que el commit de su
puntero era ancestro de `main`, por lo que no tenían commits exclusivos:

- `codex/eclipse-ascension-redesign`
- `design/v42-fase01-monarca-a-plus`
- `design/v42-fase02-componentes-temas`
- `design/v42-fase03-navegacion-acceso`
- `design/v42-fase04-dashboard-monarca`
- `design/v42-fase05-misiones-monarca`
- `design/v42-fase06-rutinas-monarca`
- `design/v42-fase07-reproductor-monarca`
- `design/v42-fase08-personalizacion`
- `design/v42-fase09-estadisticas`
- `design/v42-fase10-microinteracciones`
- `design/v42-plan-y-opciones-20261024`
- `feature/hunter2000-svg-catalog-gated`
- `feature/v43-12-pixel-art-png-complete`
- `integration/hunter-v43-compose-2000`
- `redesign/v39-referencia-20261023`
- `feat/asterion-rules-engine-v44`

Se eliminaron otras cuatro ramas obsoletas después de crear y verificar
etiquetas que apuntan exactamente a sus commits originales:

| Rama retirada | Etiqueta de recuperación |
| --- | --- |
| `redesign/v39-eclipse-aurora-ceniza-amanecer` | `archive/retired-scenes/v39-eclipse-aurora-ceniza-amanecer` |
| `redesign/v40-premium-compose-20261024` | `archive/retired-scenes/v40-premium-compose-20261024` |
| `redesign/v41-full-concept-20261024` | `archive/retired-scenes/v41-full-concept-20261024` |
| `feature/agenda-clara-png-rendering-parity` | `archive/retired-scenes/agenda-clara-png-parity` |

Las etiquetas son copias históricas del puntero de Git, no cambios
integrados en `main`. Para recuperar una de estas variantes, crear
una nueva rama desde la etiqueta correspondiente; no se debe afirmar
que ese trabajo exclusivo esté integrado en producción.

## Ramas conservadas con cambios exclusivos (7)

La limpieza **no** destruyó cambios aún no incorporados y relacionados
con Hunter System Monarca Eclipse:

- `feature/monarca-eclipse-progress-rules` — motor alternativo de progreso, PR #25.
- `integration/v43-monarca-eclipse-updates` — actualizaciones de escenas, integración y diseño.
- `integracion/v42-monarca-hunter2000` — integración del catálogo y reglas Hunter 2000, PR #18.
- `feature/v43-12-exact-reference-png` — iconografía de referencia.
- `feature/v43-12-optimal-compose-png-icons` — optimización de renderizado.
- `fix/v43-12-png-icons-dashboard-complete` — ajustes de iconos del panel.
- `preview/v43-12-pixel-icons-real-device` — propuesta de iconos para revisión en equipo físico.

Estas ramas **no están validadas como integradas** en `main`.
Eliminar alguna sin revisar sus commits exclusivos podría perder
trabajo del proyecto.

## Solicitudes de cambio

- PR #26: fusionado en `main`.
- PR #3, #4 y #5–#14: cerrados como rediseños/fases históricas.
- PR #18 y #25: permanecen disponibles por sus cambios exclusivos,
  sujetos a revisión de compatibilidad antes de fusionarlos.

## Integridad

El flujo de limpieza verificó la ancestría antes de borrar las primeras
17 ramas y comprobó que cada una de las cuatro etiquetas históricas
apuntaba al SHA original antes de borrar sus ramas. El historial de
`main`, los recursos del catálogo y las ramas relevantes no se
reescribieron ni se forzaron.
