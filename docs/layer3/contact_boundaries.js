'use strict';
/** C3 exterior contact boundary crossings; historical calf C1 is read-only. */
const {smoothStations}=require('./smooth.js');
const {computeSurface}=require('./sculpt_v2.js');
const {transformPoint}=require('./interpatch_audit.js');
const profiles=require('./profiles/calf.json').profiles;
const triage=require('./interpatch_calf.triage.json');
function intersection(a,b,c,d){
 const sub=(x,y)=>[x[0]-y[0],x[1]-y[1]],cross=(x,y)=>x[0]*y[1]-x[1]*y[0];
 const r=sub(b,a),q=sub(d,c),den=cross(r,q);
 if(Math.abs(den)<1e-9)return null;
 const t=cross(sub(c,a),q)/den,u=cross(sub(c,a),r)/den;
 return t>1e-7&&t<1-1e-7&&u>1e-7&&u<1-1e-7?[a[0]+t*r[0],a[1]+t*r[1]]:null;
}
function crossings(A,B){
 const out=[];for(let i=0;i<A.length;i++)for(let j=0;j<B.length;j++){
  const p=intersection(A[i],A[(i+1)%A.length],B[j],B[(j+1)%B.length]);if(p)out.push(p);
 }return out;
}
function analyze(motion){
 if(motion.body.length!==48||motion.heel.length!==48||profiles.length!==24)throw Error('Original calf inventory mismatch');
 const byId=new Map(profiles.map(p=>[p.id,p]));
 const cases=triage.pairs.filter(p=>p.category==='needs_visual_review');
 if(cases.length!==4)throw Error('Unexpected review case count');
 const prepared=new Map(profiles.map(p=>[p.id,smoothStations(p.stations,4)]));
 const pairs=cases.map(c=>{
  const frames=[];
  for(let f=0;f<48;f++){
   const polys=c.ids.map(id=>{
    const p=byId.get(id);if(!p)throw Error('Missing exterior profile');
    const g=computeSurface(p,prepared.get(id),f/48,1);
    return [...g.A,...g.B.slice().reverse()].map(pt=>transformPoint(p,pt,motion.body[f],motion.heel[f]));
   });
   const points=crossings(...polys).map(pt=>pt.map(v=>Number(v.toFixed(3))));
   frames.push({frame:f,crossings:points.length,points});
  }
  return {ids:c.ids,peakFrame:c.peakFrame,framesWithBoundaryCrossings:frames.filter(f=>f.crossings).length,maxCrossings:Math.max(...frames.map(f=>f.crossings)),frames};
 });
 return {format:'MonarcaC3BoundaryCrossings',version:1,exercise:'calf',frames:48,profileCount:24,reviewPairs:pairs.length,algorithm:'Strict polygon segment intersections, excluding shared endpoints and collinear segments',pairs,limitations:['Boundary crossings are not proof of incorrect penetration','Polygon segments approximate Bézier contours','No browser rendering or visual adjudication']};
}
if(require.main===module){
 const fs=require('node:fs'),path=require('node:path');
 const html=fs.readFileSync(path.join(__dirname,'comparacion_visual_calf.html'),'utf8');
 const match=html.match(/const stage=\$\('stage'\),M=(.*?);\n/);
 if(!match)throw Error('Historical C1 motion missing');
 const motion=Function('return ('+match[1]+')')();
 fs.writeFileSync(path.join(__dirname,'contact_boundaries_calf.qa.json'),JSON.stringify(analyze(motion),null,2)+'\n');
}
module.exports={intersection,crossings,analyze};
