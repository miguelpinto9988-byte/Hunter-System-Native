# Hunter System V43.5 — Jefes Monarca en la pantalla principal

- El antiguo recuadro **«Eclipse × Monarca / Tu próxima evolución»** sigue eliminado.
- En su posición se presenta la **misión especial semanal Monarca**, reubicando su único panel existente antes del héroe y del resto de widgets.
- La portada cambia según la misión real determinada por `MonarchMissionCatalog`:
  - **Guardia del Umbral:** caballero carmesí, interpretación original inspirada en Igris.
  - **Asalto a la Fortaleza:** monarca insectoide, interpretación original inspirada en Beru.
  - **Juicio del Trono Monarca:** demonio de las tormentas, interpretación original inspirada en Baran.
- Tres ilustraciones vectoriales originales `app/src/main/assets/monarch-boss/{guardia,fortaleza,trono}.svg` se cargan sin conexión con el reproductor SVG Android existente. No son imágenes oficiales ni archivos extraídos de la obra.
- Para evitar desplazar toda la pantalla principal, el cartel de jefe y el tiempo restante permanecen visibles y los objetivos detallados pueden **expandirse u ocultarse** mediante botón.
- Se conservan intactas las reglas: una misión voluntaria por semana, renovación domingo a medianoche local, control por nivel/restricciones, registro manual, cancelación sin penalización, objetivos y sellos.
- No se duplica la misión ni se suma avance automático.
- Fondos por ventana V43.4, splash animado, 2.000 SVG de ejercicios y estado de las 800 propuestas pendientes se conservan.
- PR #17 sigue en borrador. QA en Android físico y contraste del nuevo arte siguen pendientes.
