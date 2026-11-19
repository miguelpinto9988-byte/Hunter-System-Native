# Capa 3 — modelo de volumen exterior

Esta carpeta guarda los recursos reutilizables para mejorar progresivamente la **tercera capa exterior deformable** de las demostraciones en Hunter System Monarca Eclipse.

## Alcance

- **Capa 1**: movimiento original de `app/src/main/assets/demos30/<id>.json`, sin cambios.
- **Capa 2**: estética original o prototipo estético, sin cambios.
- **Capa 3**: perfiles vectoriales exteriores con estaciones de contorno `[centroX,centroY,anchoIzq,anchoDer]`, separados por grupo y asignados a pistas de rig. No es un atlas muscular interno.
- Escala de referencia SVG fija: `720×800`. Preservar proporciones, duración, articulaciones, poses, datos de ejercicios y valores de entrenamiento.

## Estado actual y objetivo progresivo

El ejemplo `calf.json` contiene **24 perfiles exteriores y 104 estaciones editables**, diseñado para `calf` / «Elevación de talones». El prototipo interactivo se encuentra en `prototype_calf.html`. No aplicar automáticamente los mismos perfiles a todos los ejercicios: se requieren adaptaciones y controles por postura, perspectiva, aparato y trayectoria. Un ejercicio solo cuenta como cubierto cuando tenga su propio JSON de capa 3 validado.

Hay 1.200 ilustraciones originales `media2d`, 1.200 JSON de movimientos históricos y 800 ejercicios nuevos sin estas poses. **Los 800 nuevos no deben recibir movimientos inventados.**

### Comprobación

```sh
python3 tools/check_layer3_profiles.py
```

Este programa valida presencia del rig, número de poses, anchura y rango de coordenadas de cada perfil y produce contadores de cobertura. Después se necesitarán pruebas automatizadas de continuidad de Bézier, ausencia de intersecciones y comparación visual de fotogramas antes de dar los modelos por terminados.

### Fuentes

- [OpenStax — miembros superiores](https://openstax.org/books/anatomy-and-physiology-2e/pages/11-5-muscles-of-the-pectoral-girdle-and-upper-limbs)
- [OpenStax — miembros inferiores](https://openstax.org/books/anatomy-and-physiology-2e/pages/11-6-appendicular-muscles-of-the-pelvic-girdle-and-lower-limbs)
- [ASMR: Adaptive Skeleton–Mesh Rigging and Skinning](https://arxiv.org/abs/2503.13579) — referencia técnica investigada con Nimble.

No introducir SVG animados `demos30/*.svg`, ni mezclar la capa 3 con la estética. Cualquier mejora debe pasar por PR y pruebas antes de integrarse en la aplicación.
