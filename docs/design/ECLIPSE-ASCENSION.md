# Hunter System · Eclipse Ascensión

Rediseño de la aplicación Android nativa existente. La interfaz utiliza Kotlin,
Jetpack Compose, Canvas y recursos locales. El catálogo, la planificación,
las restricciones de salud y los registros conservan sus implementaciones.

## Dirección visual

Una atmósfera de fantasía oscura, portales y constelaciones acompaña el progreso
físico. La jerarquía prioriza la misión del día, el registro y la navegación.
Los fondos oscuros, bordes discretos, superficies redondeadas y acentos luminosos
se comparten en pantallas, formularios y ventanas. La tipografía Barlow incluida
se emplea tanto en títulos como en texto, con tamaños y pesos diferenciados.

| Tema | Identificador | Acento | Inspiración visual |
| --- | --- | --- | --- |
| Eclipse | eclipse | Violeta | Sombras y despertar |
| Constelación | constellation | Azul celeste | Lectura y capítulos |
| Glaciar | ice | Cian | Calma y precisión |
| Carmesí | aurora | Rosa | Caballero y voluntad |
| Jade | ceniza | Verde | Equilibrio |
| Ámbar | amanecer | Dorado | Amanecer y evolución |

Los identificadores históricos de los cuatro temas se conservan. El tema se
selecciona en Perfil y se aplica con **Guardar perfil**. Los ajustes de
profundidad, movimiento, aura, tamaño y acceso flotante se guardan al cambiarlos,
en `eclipseAppearance`, independientemente del formulario de entrenamiento.

## Superficies

- Inicio: ilustración de portal, rango, XP, misión del día, accesos a rutinas y
  progreso, acompañante, calendario semanal y objetivos.
- Misiones: controles de registro, selección de días, sesión guiada y objetivos.
- Rutinas: dos entradas para entrenamiento sin conexión y gimnasio, generador
  con selección múltiple, tiempo, nivel y tarjetas de secuencias.
- Ejercicios: fichas de texto con iconografía; demostración animada en su ventana.
- Perfil: presentación de acceso, datos, seis vistas previas de temas, galería de
  acompañantes, calendario, restricciones y preferencias visuales.
- Progreso: anillo de nivel, métricas registradas, barras de series y diario.
- Ventanas: cabecera fija con volver y cerrar, contenido desplazable y ancho
  máximo para tablet. El desplazamiento se reinicia al abrir una ventana distinta.
- Navegación: cinco destinos; barra inferior en pantallas estrechas y barra
  lateral desde 840 dp. Objetivos y progreso se enlazan desde el inicio.
- Carga, icono de aplicación y notificaciones: identidad Eclipse compartida.

## Profundidad y acompañantes

- **2D**: ilustración plana.
- **2.5D**: ilustración en capas, aura y oscilación visual suave.
- **3D**: reliquia de ocho caras con coordenadas tridimensionales, rotación,
  proyección en perspectiva, orden de profundidad e iluminación. El visor
  permite arrastrarla. El personaje que la acompaña conserva su ilustración 2D.

No se incluyen nuevos personajes 3D articulados ni se transforman las
1200 demostraciones de ejercicio en modelos 3D. Las demostraciones existentes
mantienen su reproductor y sus controles de velocidad y espejo.

Los acompañantes IGRIS, IRON, TANK, BERU y DOKJA KIM se eligen en Perfil. Las
ilustraciones proceden de los recursos existentes. El modo 3D añade una reliquia;
no presenta esas ilustraciones como mallas tridimensionales.

Las animaciones ambientales consultan el estado de la actividad y los ajustes
de movimiento. El temporizador de ejercicio continúa siendo independiente de
la velocidad visual. Las imágenes se decodifican fuera del hilo de interfaz y
se reutilizan mediante una caché acotada a 24 MiB.

## Recursos nuevos

- `app/src/main/assets/eclipse/portal.webp`: fondo generado con imagegen y
  convertido de PNG a WebP para empaquetarlo. La imagen original tiene
  1536 × 1024 píxeles. No contiene textos ni botones incrustados.
- `EclipseVisuals.kt`: 29 símbolos vectoriales dibujados en Canvas, paletas,
  constelaciones y geometría de la reliquia.
- `app_icon.xml` y `notification_icon.xml`: insignia de corona Eclipse.

Prompt del fondo (herramienta integrada imagegen):

> Cinematic dark fantasy environment for a native Android fitness RPG, inspired
> by the atmospheric shadow dungeons and constellation storytelling of Solo
> Leveling and Omniscient Reader. Monumental circular dimensional gate with
> carved obsidian arches, sparse floating shards on the right, luminous violet
> and ice blue light, distant columns and subtle stars. Empty scene, no
> characters. Painterly premium manhwa concept art, restrained bloom, inky
> navy-black and violet stone. Landscape 1536x1024, dark negative space on the
> left, bottom fading to near-black. No UI, icons, lettering or watermark.

## Verificación

El flujo `Verificar Eclipse Android` compila la variante debug y ejecuta las
pruebas de datos, planificación, geometría, contraste y renderizado Compose.
Publica los informes y capturas como artefactos de GitHub Actions. El flujo
existente compila la variante release con la firma configurada en el repositorio.

Las capturas de pruebas no equivalen a una medición de rendimiento en un teléfono.
No se certifica una tasa de 90 FPS. Las comprobaciones en dispositivos físicos,
la entrega de avisos por el sistema y la instalación sobre versiones ajenas a
este repositorio requieren validación adicional.
