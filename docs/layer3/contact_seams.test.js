'use strict';
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {evaluateSeams}=require('./contact_seams.js');
const {smoothStations}=require('./smooth.js');
const {computeSurface}=require('./sculpt_v2.js');
const profiles=require('./profiles/calf.json').profiles;
const html=fs.readFileSync(path.join(__dirname,'comparacion_visual_calf.html'),'utf8');
const match=html.match(/const stage=\$\('stage'\),M=(\{.*?\});/);
assert(match,'historical pose data missing');
const M=Function('return ('+match[1]+')')();
const report=evaluateSeams(profiles,M,smoothStations,computeSurface);
const expected=require('./contact_seams_calf.qa.json');
assert.deepEqual(report,expected,'QA report drift');
assert.equal(report.motionFrames,48);
for(const [name,seam] of Object.entries(report.seams)){
 assert.equal(seam.framesChecked,48);
 assert.deepEqual(seam.failingFrames,[],name+' has open seam');
 assert(seam.minOverlap>=10,name+' weak overlap');
 assert(seam.maxTemporalDelta<=2,name+' temporal seam instability');
}
console.log('PASS: 192 C3 seam/pose combinations, no open seam, report parity');
