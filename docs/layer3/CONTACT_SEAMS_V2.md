# Calf C3 — adaptive contact seam QA V2

The former fixed global 0.5 SVG-unit row scan undercounted the posterior knee contact (only 1–2 sampled rows). V2 scans from the intersection of the actual vertical bounds of each surface at 0.05 SVG-unit intervals, so sampling follows the joint deformation without changing C1 or C3 geometry.

The new report adds minimum/maximum continuous vertical overlap. It distinguishes a genuinely thin seam from an arbitrary scan-grid alignment. This is a **diagnostic improvement**, not a claim of improved rendered anatomy.

| Contact | Old min sampled rows | V2 min sampled rows | V2 min vertical overlap |
|---|---:|---:|---:|
| frontKnee | 12 | 119 | 18.94319 |
| rearKnee | 1 | 15 | 5.36822 |
| frontAnkle | 30 | 300 | 15 |
| rearAnkle | 24 | 246 | 13.7056 |

All 192 seam/frame combinations pass the current positive-overlap and minimum 1 SVG-unit vertical contact checks. Source: the repository's 48 original calf motion frames, unchanged.

Reproduce with `node docs/layer3/contact_seams.test.js`. Limitations: sampled contours do not certify exact Bézier crossings, inter-patch penetration or browser-rendered occlusion. No browser screenshot test was performed.

Coverage: C3 verified on calf only (1/1200 original exercises); 800 additional exercises lack verified historical poses and remain blocked.
