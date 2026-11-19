'use strict';
// Regression QA of actual exterior normal-offset silhouettes, not internal anatomy.
// Run: node docs/layer3/silhouette.test.js
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {smoothStations}=require('./smooth.js');
const data=JSON.parse(fs.readFileSync(path.join(__dirname,'profiles/calf.json'),'utf8'));
const EPS=1e-8;
function cross(a,b,c){return (b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);}
function intersects(a,b,c,d){
  const x=cross(a,b,c),y=cross(a,b,d),z=cross(c,d,a),w=cross(c,d,b);
  return (x>EPS&&y<-EPS||x<-EPS&&y>EPS)&&(z>EPS&&w<-EPS||z<-EPS&&w>EPS);
}
function outline(stations,inflate=1,peak=0){
  const pts=smoothStations(stations,4),left=[],right=[];
  for(let i=0;i<pts.length;i++){
    const [x,y,l,r]=pts[i],prev=pts[Math.max(0,i-1)],next=pts[Math.min(pts.length-1,i+1)];
    const dx=next[0]-prev[0],dy=next[1]-prev[1],len=Math.hypot(dx,dy)||1;
    const nx=-dy/len,ny=dx/len;
    const localized=peak?Math.sin(Math.PI*i/(pts.length-1))**1.3:0;
    const bulge=1+localized*peak;
    left.push([x+nx*l*inflate*bulge,y+ny*l*inflate*bulge]);
    right.push([x-nx*r*inflate*bulge,y-ny*r*inflate*bulge]);
  }
  return left.concat(right.reverse());
}
function checkPolygon(poly,label){
  for(const [x,y] of poly)assert(Number.isFinite(x)&&Number.isFinite(y)&&x>=-EPS&&x<=720+EPS&&y>=-EPS&&y<=800+EPS,`${label}: outside 720x800`);
  const n=poly.length;
  for(let i=0;i<n;i++)for(let j=i+2;j<n;j++){
    if(i===0&&j===n-1)continue;
    if(intersects(poly[i],poly[(i+1)%n],poly[j],poly[(j+1)%n]))throw Error(`${label}: exterior self-intersection at edges ${i}/${j}`);
  }
}
let checks=0;
for(const p of data.profiles)for(const inflate of [0.9,1,1.1])for(const phase of [0,0.125,0.25,0.375,0.5,0.625,0.75,0.875]){
  const k=(1-Math.cos(2*Math.PI*phase))/2;
  const swell=p.type==='calf'?.12*k:0;
  checkPolygon(outline(p.stations,inflate,swell),`${p.id} inflate=${inflate} phase=${phase}`);
  checks++;
}
assert.equal(data.profiles.length,24);
assert.equal(checks,576);
console.log('PASS: 576 exterior silhouette polygon checks (24 profiles × 3 reliefs × 8 phases)');
