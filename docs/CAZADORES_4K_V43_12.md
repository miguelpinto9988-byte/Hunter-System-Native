# Integración de cazadores 4K — Hunter System V43.12

Esta integración cambia la ilustración de la pantalla de selección de perfil,
su vista previa con fondo y la tarjeta de evolución del dashboard. El retrato
mostrado en la tarjeta principal corresponde a la colección elegida (Kim Dokja,
Yoo Joonghyuk, cazador masculino o cazadora femenina), **no** al acompañante de
misiones. Los acompañantes existentes conservan su funcionamiento independiente.

## Recursos binarios (obligatorios para activar los rediseños nuevos)

Copiar los cuatro WebP del paquete entregado a:
`app/src/main/assets/hunter-4k/`

- `reader.webp` (Kim Dokja)
- `regressor.webp` (Yoo Joonghyuk)
- `hunter-male.webp` (cazador masculino)
- `hunter-female.webp` (cazadora femenina)

Resolución de cada retrato: **2560 × 3840**, canal alfa.

El código **no** presupone que estos recursos existan en GitHub: si no están,
utiliza `ui34` o `ui33` sin producir una imagen rota. La conexión de GitHub
no acepta referencias locales de archivos binarios; es necesario copiar
estos WebP al árbol del repositorio y confirmarlos en la rama V43.12.

Si se generan las 24 ilustraciones específicas de los rangos, podrán añadirse
como `hunter-4k/<personaje>-<rango>.webp`. Esta versión no afirma que las
24 evoluciones estén generadas.

No se altera la lógica de experiencia, niveles, datos guardados o entrenamiento.
**Pendiente**: verificación visual en dispositivo físico, y subida de los
cuatro archivos binarios desde el paquete de integración.
