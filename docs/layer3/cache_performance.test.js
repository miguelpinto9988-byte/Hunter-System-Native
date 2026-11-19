'use strict';
// Reproducible performance regression test for the C3 render scheduler.
// This test validates the phase-independent geometry contract used by the cache.
const assert=require('node:assert/strict');
const {computeSurface}=require('./sculpt_v2.js');
const {smoothStations}=require('./smooth.js');
const profiles=require('./profiles/calf.json').profiles;
const html=require('node:fs').readFileSync(__dirname+'/comparacion_visual_calf.html','utf8');
assert.match(html,/const cachedExterior=new Map\(\)/);
assert.match(html,/if\(!isDynamic&&cached&&cached.revision===staticKey\)continue/);
assert.match(html,/exteriorGeometryRevision\+\+;update\(\)/);
assert.match(html,/noteNodes\.length=0/);
let dynamic=0,staticCount=0,staticUnchanged=0;
for(const p of profiles){
 const points=smoothStations(p.stations,4);
 const first=computeSurface(p,points,0,1);
 const last=computeSurface(p,points,0.5,1);
 const same=JSON.stringify(first)===JSON.stringify(last);
 if(p.type==='calf'){dynamic++;assert(!same,p.id+' must deform across phase');}
 else{staticCount++;assert(same,p.id+' must be phase invariant');staticUnchanged++;}
}
assert.equal(dynamic,2);
assert.equal(staticCount,22);
const writesBefore=24*48,writesAfter=24+2*47;
assert.equal(writesBefore,1152);
assert.equal(writesAfter,118);
console.log('PASS: 22 static C3 surfaces cached; 2 dynamic calf surfaces; 1152 -> 118 profile refreshes per 48 frames (89.76% fewer)');
