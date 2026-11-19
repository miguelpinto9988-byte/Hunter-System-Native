'use strict';
/** Diagnostic-only classification of existing C3 exterior overlap evidence. */
const SEAM_PAIRS=[
  [
    "torso",
    "pectoral_superficial"
  ],
  [
    "cadera_frontal",
    "muslo_frontal"
  ],
  [
    "gluteo_posterior",
    "muslo_posterior"
  ],
  [
    "deltoides_frontal",
    "brazo_frontal"
  ],
  [
    "hombro_posterior",
    "brazo_posterior"
  ],
  [
    "cuello_exterior",
    "trapecio_base_cuello"
  ],
  [
    "trapecio_base_cuello",
    "torso"
  ],
  [
    "cuello_exterior",
    "torso"
  ],
  [
    "muslo_frontal",
    "rotula_frontal"
  ],
  [
    "rotula_frontal",
    "pantorrilla_frontal"
  ],
  [
    "pantorrilla_frontal",
    "tobillo_frontal"
  ],
  [
    "pantorrilla_posterior",
    "tobillo_posterior"
  ],
  [
    "tobillo_posterior",
    "pie_posterior"
  ],
  [
    "tobillo_frontal",
    "pie_frontal"
  ],
  [
    "muslo_posterior",
    "pantorrilla_posterior"
  ],
  [
    "muslo_frontal",
    "pantorrilla_frontal"
  ],
  [
    "antebrazo_frontal",
    "mano_frontal"
  ],
  [
    "antebrazo_posterior",
    "mano_posterior"
  ],
  [
    "brazo_frontal",
    "antebrazo_frontal"
  ],
  [
    "brazo_posterior",
    "antebrazo_posterior"
  ],
  [
    "cuello_exterior",
    "cabeza_exterior"
  ],
  [
    "torso",
    "cadera_frontal"
  ],
  [
    "torso",
    "gluteo_posterior"
  ],
  [
    "pectoral_superficial",
    "deltoides_frontal"
  ],
  [
    "trapecio_base_cuello",
    "deltoides_frontal"
  ],
  [
    "trapecio_base_cuello",
    "hombro_posterior"
  ],
  [
    "trapecio_base_cuello",
    "cabeza_exterior"
  ]
];
const pairKey=(a,b)=>[a,b].sort().join('|');
function classify(audit,order,profiles){
 if(audit.frames!==48||audit.profiles!==24||audit.pairCount!==276)throw Error('Unexpected audit dimensions');
 if(!Array.isArray(audit.pairs)||audit.pairs.length!==audit.overlappingPairs)throw Error('Audit pair count inconsistent');
 const ids=new Set(profiles.map(p=>p.id)),painter=order.backToFront;
 if(ids.size!==24||painter.length!==24||new Set(painter).size!==24||painter.some(id=>!ids.has(id)))throw Error('Painter order mismatch');
 const groups=Object.fromEntries(Object.entries(order.groups).map(([k,v])=>[k,new Set(v)]));
 if(new Set(Object.values(order.groups).flat()).size!==24)throw Error('Occlusion grouping incomplete');
 const seams=new Set(SEAM_PAIRS.map(([a,b])=>{if(!ids.has(a)||!ids.has(b)||a===b)throw Error('Unknown seam');return pairKey(a,b)}));
 if(seams.size!==SEAM_PAIRS.length)throw Error('Duplicate seam');
 const pairs=audit.pairs.map(p=>{
  const [a,b]=p.ids;
  if(!ids.has(a)||!ids.has(b)||a===b||p.framesWithOverlap<1||p.framesWithOverlap>48)throw Error('Invalid pair');
  const ga=Object.keys(groups).find(k=>groups[k].has(a)),gb=Object.keys(groups).find(k=>groups[k].has(b));
  if(!ga||!gb)throw Error('Unknown occlusion group');
  const category=seams.has(pairKey(a,b))?'intended_seam_candidate':ga!==gb?'layered_occlusion_candidate':'needs_visual_review';
  return {ids:p.ids,category,framesWithOverlap:p.framesWithOverlap,maxAreaEstimate:p.maxAreaEstimate,peakFrame:p.peakFrame,frontmost:painter.indexOf(a)>painter.indexOf(b)?a:b};
 });
 const counts={intended_seam_candidate:0,layered_occlusion_candidate:0,needs_visual_review:0};
 for(const p of pairs)counts[p.category]++;
 if(Object.values(counts).reduce((a,b)=>a+b,0)!==audit.overlappingPairs)throw Error('Lost audited pair');
 return {format:'MonarcaC3InterpatchTriage',version:1,exercise:'calf',source:'interpatch_calf.qa.json',occlusionSource:'occlusion_order_calf.json',frames:48,profileCount:24,overlappingPairs:audit.overlappingPairs,counts,pairs,limitations:['Categories are hypotheses, not evidence of correct physical contact','Polygon overlap is not exact Bézier penetration','Browser screenshots and visual verification remain pending']};
}
module.exports={SEAM_PAIRS,pairKey,classify};
