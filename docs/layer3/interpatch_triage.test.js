'use strict';
const assert=require('node:assert/strict');
const {classify,pairKey}=require('./interpatch_triage.js');
const audit=require('./interpatch_calf.qa.json');
const order=require('./occlusion_order_calf.json');
const profiles=require('./profiles/calf.json').profiles;
const report=require('./interpatch_calf.triage.json');
assert.deepEqual(classify(audit,order,profiles),report);
assert.equal(pairKey('torso','pectoral_superficial'),pairKey('pectoral_superficial','torso'));
assert.deepEqual(report.counts,{intended_seam_candidate:26,layered_occlusion_candidate:8,needs_visual_review:4});
assert.equal(new Set(report.pairs.map(p=>pairKey(...p.ids))).size,38);
for(const p of report.pairs){
 assert(p.peakFrame>=0&&p.peakFrame<48);
 assert(p.framesWithOverlap>=1&&p.framesWithOverlap<=48);
 assert(order.backToFront.includes(p.frontmost));
}
assert.throws(()=>classify({...audit,frames:47},order,profiles));
assert.throws(()=>classify(audit,{...order,backToFront:order.backToFront.slice(1)},profiles));
console.log('PASS: 38 overlaps triaged, four visual review candidates');
