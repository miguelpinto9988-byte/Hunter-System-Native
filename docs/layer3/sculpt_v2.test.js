'use strict';
const assert=require('node:assert/strict');
const {computeSurface,presets}=require('./sculpt_v2.js');
const {smoothStations}=require('./smooth.js');
const profiles=require('./profiles/calf.json').profiles;
const settings=require('./sculpt_calf_v2.json');
assert.deepEqual(presets,settings.profiles);
assert.equal(profiles.length,24);
const cross=(p,q,r)=>(q[0]-p[0])*(r[1]-p[1])-(q[1]-p[1])*(r[0]-p[0]);
function crosses(a,b,c,d){return cross(a,b,c)*cross(a,b,d)<-1e-8&&cross(c,d,a)*cross(c,d,b)<-1e-8}
function intersects(poly){
 const n=poly.length;
 for(let i=0;i<n;i++)for(let j=i+2;j<n;j++){
  if(i===0&&j===n-1)continue;
  const a=poly[i],b=poly[(i+1)%n],c=poly[j],d=poly[(j+1)%n];
  if(Math.max(a[0],b[0])<Math.min(c[0],d[0])||Math.max(c[0],d[0])<Math.min(a[0],b[0])||Math.max(a[1],b[1])<Math.min(c[1],d[1])||Math.max(c[1],d[1])<Math.min(a[1],b[1]))continue;
  if(crosses(a,b,c,d))return true;
 }
 return false;
}
let polygons=0;
for(const p of profiles){
 const s=smoothStations(p.stations,4);
 for(const relief of [.7,1,1.3])for(let frame=0;frame<48;frame++){
  const g=computeSurface(p,s,frame/48,relief),plain=computeSurface({...p,id:'no-preset'},s,frame/48,relief);
  assert(!intersects([...g.A,...g.B.slice().reverse()]),p.id+' intersection frame '+frame);
  for(const q of [...g.A,...g.B]){
   assert(q.every(Number.isFinite));
   const x=q[0]*1.12-45.3,y=q[1]*1.12-119.41;
   assert(x>=0&&x<=720&&y>=30&&y<=800,p.id+' out of 720x800');
  }
  for(const side of ['A','B'])for(const i of [0,s.length-1]){
   assert(Math.hypot(g[side][i][0]-plain[side][i][0],g[side][i][1]-plain[side][i][1])<1e-8,p.id+' displaced anchor');
  }
  polygons++;
 }
}
assert.throws(()=>computeSurface(profiles[0],smoothStations(profiles[0].stations,4),NaN));
console.log('PASS: '+polygons+' tests (24 profiles × 48 frames × 3 relief values)');
