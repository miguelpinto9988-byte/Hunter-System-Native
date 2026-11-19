# C3: inter-patch overlap audit (calf)
New reproducible scanline audit of **all 276 unordered pairs** of the 24 existing exterior profiles across all **48 original poses**. Uses actual historical body/heel motion and the existing C3 sculpt function. Does not change C1, C2, proportions or exterior geometry.
Run: `node docs/layer3/interpatch_audit.test.js`.
Result: **38 pairs** with at least one positive sampled overlap, **238 pairs** without sampled overlap. Top pair: `torso ↔ pectoral_superficial`, sampled max area 2016.02 square local SVG units (frame 1).
A positive overlap is **not automatically a defect**: the torso/limb stacking and adjoining seam contacts are intentional in many cases. This is a reproducible risk inventory for selecting actual fixes, not a claim that all intersections have been eliminated.
Sampling at 1 SVG unit with a midpoint guard is approximate. Exact Bézier clipping and screenshot QA remain pending.
C3 coverage remains 1/1200; 800 additional exercises without historical poses remain blocked. The two historical prototype filenames were not found in previous repository searches.
