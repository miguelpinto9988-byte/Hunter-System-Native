'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const {analyze,intersection,sampleSide,crossings}=require('./contact_bezier.js');
const html=fs.readFileSync(path.join(__dirname,'comparacion_visual_calf.html'),'utf8');
const match=html.match(/const stage=\$\('stage'\),M=(.*?);\n/);
assert(match);
const M=Function('return ('+match[1]+')')();
const hi=analyze(M,8),lo=analyze(M,4),old=require('./contact_boundaries_calf.qa.json');
assert.deepEqual(hi,require('./contact_bezier_calf.qa.json'));
assert.equal(hi.reviewPairs,4);
assert.equal(hi.pairs.reduce((n,p)=>n+p.frames.length,0),192);
assert.deepEqual(intersection([0,0],[2,2],[0,2],[2,0]),[1,1]);
assert.equal(intersection([0,0],[2,0],[0,1],[2,1]),null);
assert.equal(crossings([[0,0],[2,0],[2,2],[0,2]],[[1,-1],[3,-1],[3,1],[1,1]]).length,2);
assert.throws(()=>sampleSide([[0,0],[1,1]],0));
assert.throws(()=>analyze({...M,body:M.body.slice(1)}));
let maxDrift=0,points=0;
for(let i=0;i<4;i++)for(let f=0;f<48;f++){
 const a=hi.pairs[i].frames[f].points,b=lo.pairs[i].frames[f].points,c=old.pairs[i].frames[f].points;
 assert.equal(a.length,b.length);assert.equal(a.length,c.length);
 for(const p of a){maxDrift=Math.max(maxDrift,Math.min(...b.map(q=>Math.hypot(p[0]-q[0],p[1]-q[1]))));points++}
}
assert.equal(points,276);assert(maxDrift<.002);
console.log('PASS 192 pair-frames, 276 crossings, max 4x/8x drift '+maxDrift.toFixed(4));
