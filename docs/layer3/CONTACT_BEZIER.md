# C3 exterior: convergence audit of Bézier contact boundaries

This incremental QA improvement tests actual sampled cubic contour curves, rather than only station polygons. It **does not** change geometry, C1, C2, 48 historical calf poses, anchors, height or APK.

Run `node docs/layer3/contact_bezier.test.js`. Regenerate JSON with `node docs/layer3/contact_bezier.js`.

- 192 pair-frame analyses, 4 review pairs, 276 crossing points.
- Maximum polygon-to-8× Bézier crossing shift: 0.0130 SVG units; mean 0.0026.
- Maximum 4×-to-8× Bézier shift: 0.0010 SVG units; mean 0.0002.
- Frame counts remain 23, 48, 19, 48 (same as earlier polygon QA).
- Limitations: convergence is not exact analytic Bézier intersection; no browser visual review; crossings may represent legitimate seams.

C3 documented coverage: 1/1200 original exercises. C1 total is not independently verified. 800 additional exercises without historical motion remain pending. Historical named prototypes were not available in prior repo search; only actual docs/layer3 sources used.
