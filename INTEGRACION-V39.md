# Rediseño Hunter System V39

Integración visual sobre la aplicación nativa V38.50, conservando el motor y los registros. Branch: `redesign/v39-eclipse-aurora-ceniza-amanecer`.

## Elementos ya integrados en Kotlin

- Temas únicos: `eclipse` (azul), `aurora` (rosa), `ceniza` (oscuro), `amanecer` (dorado). Descartados los selectores de temas y paletas V38.
- Componentes de la interfaz renovados: tarjetas redondeadas, gradientes, botones, chips, encabezados y barra inferior de cinco secciones. Progreso se abre desde Panel.
- Semana: siete botones táctiles de Lun a Dom; cada toque cicla superior, inferior, cardio, movilidad, core, descanso. Se guarda al instante en perfiles existentes y al guardar el perfil nuevo. Se mantiene el formato nativo `weekPlan/weekFocus`.
- Esquema semántico de colores constante; un tema solo cambia superficies y acentos.
- `V39Artwork`: portada única por ID si existe `assets/v39-miniatures/ex_<id>.webp`; si no, sigue utilizando la demostración SVG correspondiente. Los 1200 ID siguen presentes y la navegación a sus demostraciones sigue intacta.
- Imágenes/animaciones originales de Igris, Iron, Tank, Beru y Dokja conservadas.
- Retirados los iconos antiguos de barra inferior, marca SVG ornamental, marcos decorativos y fuente Pirata que ya no usa la UI.
- Misiones, calendario, generadores, notificaciones, respaldo, historial, progreso y recursos offline conservados.

## Paso pendiente: importar las 12 imágenes de muestra

Las 12 miniaturas físicas del ZIP de referencia no están disponibles en este repositorio hasta que se importen con la utilidad o con el ZIP pequeño renombrado en Codespaces. El paquete `Hunter-V39-12-Miniaturas-Compatibles.zip` contiene únicamente las imágenes renombradas con los IDs reales.

1. Subir `Hunter-V39-12-Miniaturas-Compatibles.zip` a la raíz del Codespace, en esta rama.
2. Desde la terminal en la raíz del repositorio:

```bash
git switch redesign/v39-eclipse-aurora-ceniza-amanecer
unzip -oq "Hunter-V39-12-Miniaturas-Compatibles.zip" -d .
git add app/src/main/assets/v39-miniatures
git commit -m "assets(v39): incluir 12 portadas especificas"
git push origin redesign/v39-eclipse-aurora-ceniza-amanecer
```

Los 12 archivos cubren 12/1200 IDs; los restantes 1188 siguen mostrando sus propias demostraciones SVG. El ZIP de referencia no permite entregar 1200 miniaturas originales únicas.

## Pruebas

- GitHub Actions: `Compilar Hunter System Android APK` para confirmar compilación y firma.
- Prueba manual pendiente en Android: instalación, días, colores, navegación, portada WebP, fallback SVG, misiones, perfil y compañeros.
- No se ha añadido un backend Convex: el cambio es visual y la aplicación conserva su funcionamiento offline.
