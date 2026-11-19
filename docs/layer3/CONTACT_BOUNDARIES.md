# QA de cruces de fronteras exteriores C3
Se añadió `contact_boundaries.js` y `contact_boundaries_calf.qa.json` para comprobar **cruces entre segmentos de contornos de diferentes superficies**, no solo área de solapamiento aproximada por scanline. Se analizan los cuatro pares pendientes del triage en las 48 poses originales: **192** casos.

- pantorrilla_posterior / pie_posterior: cruces en 23/48 poses; máximo 2 cruces por pose; pose del máximo de área anterior 1
- trapecio_base_cuello / pectoral_superficial: cruces en 48/48 poses; máximo 2 cruces por pose; pose del máximo de área anterior 12
- pantorrilla_frontal / pie_frontal: cruces en 19/48 poses; máximo 2 cruces por pose; pose del máximo de área anterior 3
- cuello_exterior / pectoral_superficial: cruces en 48/48 poses; máximo 2 cruces por pose; pose del máximo de área anterior 20

La diferencia entre 21 poses de solapamiento scanline y 23 con cruces de frontera en el primer par refleja diferencias de resolución y criterio; no es prueba de empeoramiento geométrico. Las curvas se aproximan por segmentos muestreados. Los cruces pueden ser contactos intencionados. **No se ha corregido geometría ni se han obtenido capturas de navegador**. Ejecutar `node docs/layer3/contact_boundaries.js` y `node docs/layer3/contact_boundaries.test.js`. No se alteran C1, C2, las 48 poses, las proporciones, main ni APK. Cobertura C3 1/1200; 800 adicionales sin poses pendientes.
