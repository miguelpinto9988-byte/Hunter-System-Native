# Hunter System V42.7 — Fase 08 · Personalización Monarca

Rama: `design/v42-fase08-personalizacion`, creada desde fase 07.

## Cambios
- Selector de IGRIS, IRON, TANK, BERU y DOKJA KIM concentrado exclusivamente en **Perfil**.
- Tarjetas visuales seleccionables con vista previa del personaje; se aplican al guardar.
- Preferencias locales persistentes: acompañante flotante visible/oculto, velocidad de flotación desactivada/lenta/normal/rápida, tamaño pequeño/normal/grande y estilo del mensaje equilibrado/estratégico/cercano.
- La pantalla principal, el acompañante flotante y el panel de guía consultan el mismo personaje persistido en el perfil. No duplicar edición en Inicio.
- Mantiene los nueve temas existentes y agrega nueve opciones de acento (incluida la opción original del tema); el acento se aplica a los tokens Compose solo después de guardar el perfil.
- No introduce un chat con IA online ni nuevos personajes 3D. Todas las ilustraciones usan los recursos `ui33/companion-*.webp` existentes.
- Las opciones de estética no deben cambiar los objetivos, rutinas, registros o estadísticas. El formato de copia de seguridad existente conserva los campos del perfil.

## Criterios de validación
La compilación CI verifica los invariantes estáticos y produce la APK release firmada con los secretos del repositorio. Pendiente verificación manual en Android: selección y persistencia después de reiniciar, contraste de nueve temas x nueve acentos, tamaños y áreas tocables en teléfonos y tablets, rendimiento y reducción del movimiento; además de comprobar que no cambia la planificación de ejercicios al modificar únicamente opciones visuales.

## Regla de seguridad
La guía de entrenamiento sigue respondiendo mediante reglas locales, no es diagnóstico médico. Se conservan los 1.200 ejercicios y 342 demostraciones MP4, sin reintroducir las 1.200 miniaturas.
