# Clasificación de contactos C3 (calf)
Diagnóstico sin modificación de geometría, C1, C2 ni poses. Fuentes: `interpatch_calf.qa.json` (276 pares examinados, 38 solapamientos) y `occlusion_order_calf.json`.

**26** posibles costuras de unión, **8** posibles oclusiones de capas y **4** contactos sin explicación topológica suficiente. Son hipótesis basadas en conectividad y orden de pintura; **no** demuestran que los solapamientos sean físicamente correctos.

Revisión prioritaria:
- pantorrilla posterior / pie posterior: 21/48 fotogramas, área máxima estimada 32,62 SVG² (fotograma 0).
- trapecio / pectoral: 48/48, área máxima estimada 31,04 SVG² (fotograma 11).
- pantorrilla frontal / pie frontal: 19/48, área máxima estimada 23,79 SVG² (fotograma 2).
- cuello / pectoral: 48/48, área máxima estimada 9,50 SVG² (fotograma 19).

Prueba: `node docs/layer3/interpatch_triage.test.js`. Pendiente inspección visual en navegador de las cuatro parejas y detección de cruces Bézier exactos. Cobertura C3: 1/1200. Los 800 ejercicios sin poses verificadas siguen pendientes.
