# Monarca C3 V2 — esculpido del volumen exterior
**Base:** 24 perfiles existentes, 104 estaciones originales y 48 poses del calf, intactos.

- 14 perfiles reciben esculpido asimétrico localizado en torso, pecho, hombros, brazos, caderas, muslos y pantorrillas. Los anclajes extremos mantienen coordenadas.
- Solo las pantorrillas tienen variación dinámica vinculada a la elevación de talones. No se inventan poses; la fase pertenece al movimiento histórico.
- Bandas de luz y sombra siguen la superficie exterior, no músculos internos.
- La comparación original/V2 es animada en el mismo HTML offline; el contorno punteado conserva la versión anterior.
- Comprobaciones realizadas con la implementación JS: **3456** contornos poligonales y **1152** contornos muestreados de curvas Bézier; 0 autointersecciones detectadas; anclajes y lienzo 720×800 verificados en tres valores de relieve.
- Aumento de área *poligonal de silueta por parche en reposo* frente al contorno no esculpido: torso +3.18%, hombro posterior +4.92%, muslo frontal +5.08%, pantorrilla frontal +7.09%, posterior +6.27%. Estas áreas de parches no son medidas antropométricas.
- Pendientes: revisión visual real en navegadores y oclusiones/contactos entre diferentes perfiles, curva Bézier exacta entre muestras y comprobación integral del APK.

Repetir en Node: `node docs/layer3/sculpt_v2.test.js`. Abrir `docs/layer3/comparacion_visual_calf.html` en un navegador.
Cobertura C3: **1/1200** ejercicios originales. Los 800 sin poses históricas verificadas permanecen pendientes.
