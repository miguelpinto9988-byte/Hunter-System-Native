# Hunter System V43.6 — Jefes Monarca en la pantalla principal

- El antiguo recuadro **«Eclipse × Monarca / Tu próxima evolución»** sigue eliminado.
- En su posición se presenta la **misión especial semanal Monarca**, reubicando su único panel existente antes del héroe y del resto de widgets.
- La portada cambia según la misión real determinada por `MonarchMissionCatalog`:
  - **Guardia del Umbral:** **Igris**, jefe de la mazmorra de cambio de clase: armadura rojo sangre, penacho rojo, casco Y de ojos blancos y mandoble de bronce.
  - **Asalto a la Fortaleza:** **Rey Hormiga de Jeju**, forma enemiga anterior a Beru: exoesqueleto negro, ojos rojos, cuatro alas transparentes, garras y pinzas. **Beru** es el nombre posterior de su forma de sombra.
  - **Juicio del Trono Monarca:** **Baran**, rey de los demonios y monarca de las llamas blancas: piel gris, cuatro cuernos, cabello de llamas blancas y armadura clara. Se ilustra la apariencia de jefe del Castillo Demoníaco.
- Tres ilustraciones vectoriales originales `app/src/main/assets/monarch-boss/{guardia,fortaleza,trono}.svg` se cargan sin conexión con el reproductor SVG Android existente. No son imágenes oficiales ni archivos extraídos de la obra.
- Para evitar desplazar toda la pantalla principal, el cartel de jefe y el tiempo restante permanecen visibles y los objetivos detallados pueden **expandirse u ocultarse** mediante botón.
- Se conservan intactas las reglas: una misión voluntaria por semana, renovación domingo a medianoche local, control por nivel/restricciones, registro manual, cancelación sin penalización, objetivos y sellos.
- No se duplica la misión ni se suma avance automático.
- Fondos por ventana V43.4, splash animado, 2.000 SVG de ejercicios y estado de las 800 propuestas pendientes se conservan.
- PR #17 sigue en borrador. QA en Android físico y contraste del nuevo arte siguen pendientes.

## Referencias consultadas para fidelidad (27 octubre 2026)
- Crunchyroll guía de bestias: https://www.crunchyroll.com/es/news/guides/2024/3/30/solo-leveling-monsters-bestias-magicas
- Igris: https://solo-leveling.fandom.com/wiki/Blood-Red_Commander_Igris
- Rey Hormiga: https://solo-leveling.fandom.com/wiki/Ant_King
- Beru (forma posterior): https://solo-leveling.fandom.com/wiki/Beru
- Baran: https://solo-leveling.fandom.com/wiki/Baran

Todas las portadas son **reinterpretaciones vectoriales originales** basadas en la descripción y referencias; no se empaquetan capturas, pósteres ni ilustraciones oficiales sin licencia. La comprobación visual humana sigue pendiente.
