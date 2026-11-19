'use strict';
/** C3 sampled cubic boundary analysis; never mutates C1/C2. */
const {smoothStations}=require('./smooth.js'),{computeSurface}=require('./sculpt_v2.js'),{transformPoint}=require('./interpatch_audit.js');
const profiles=require('./profiles/calf.json').profiles,triage=require('./interpatch_calf.triage.json');
function sampleSide(points,steps=8){
 if(!Number.isInteger(steps)||steps<1||steps>32||points.length<2)throw Error('Invalid Bézier resolution');
 const out=[points[0]];
 for(let i=0;i<points.length-1;i++){
  const a=points[Math.max(0,i-1)],b=points[i],c=points[i+1],d=points[Math.min(points.length-1,i+2)];
  const h=b.map((v,k)=>v+(c[k]-a[k])/6),j=c.map((v,k)=>v-(d[k]-b[k])/6);
  for(let k=1;k<=steps;k++){const t=k/steps,u=1-t;out.push([0,1].map(v=>u**3*b[v]+3*u*u*t*h[v]+3*u*t*t*j[v]+t**3*c[v]));}
 }
 return out;
}
function intersection(a,b,c,d){
 const r=[b[0]-a[0],b[1]-a[1]],s=[d[0]-c[0],d[1]-c[1]],v=[c[0]-a[0],c[1]-a[1]];
 const cross=(p,q)=>p[0]*q[1]-p[1]*q[0],den=cross(r,s);
 if(Math.abs(den)<1e-10)return null;
 const t=cross(v,s)/den,u=cross(v,r)/den;
 return t>1e-7&&t<1-1e-7&&u>1e-7&&u<1-1e-7?[a[0]+t*r[0],a[1]+t*r[1]]:null;
}
function crossings(A,B){
 const out=[];
 for(let i=0;i<A.length;i++)for(let j=0;j<B.length;j++){
  const a=A[i],b=A[(i+1)%A.length],c=B[j],d=B[(j+1)%B.length];
  if(Math.max(a[0],b[0])<Math.min(c[0],d[0])||Math.max(c[0],d[0])<Math.min(a[0],b[0])||Math.max(a[1],b[1])<Math.min(c[1],d[1])||Math.max(c[1],d[1])<Math.min(a[1],b[1]))continue;
  const p=intersection(a,b,c,d);
  if(p&&!out.some(q=>Math.hypot(p[0]-q[0],p[1]-q[1])<.02))out.push(p);
 }
 return out;
}
function analyze(M,steps=8){
 if(!M||M.body.length!==48||M.heel.length!==48||profiles.length!==24)throw Error('Original C1 inventory mismatch');
 if(!Number.isInteger(steps)||steps<1||steps>32)throw Error('Invalid resolution');
 const byId=new Map(profiles.map(p=>[p.id,p])),prepared=new Map(profiles.map(p=>[p.id,smoothStations(p.stations,4)]));
 const cases=triage.pairs.filter(p=>p.category==='needs_visual_review');
 if(cases.length!==4)throw Error('Review case inventory mismatch');
 const pairs=cases.map(c=>{
  const frames=[];
  for(let f=0;f<48;f++){
   const polygons=c.ids.map(id=>{
    const p=byId.get(id);if(!p)throw Error('Missing C3 profile');
    const g=computeSurface(p,prepared.get(id),f/48,1);
    return [...sampleSide(g.A,steps),...sampleSide(g.B.slice().reverse(),steps)].map(pt=>transformPoint(p,pt,M.body[f],M.heel[f]));
   });
   const pts=crossings(...polygons).map(p=>p.map(v=>+v.toFixed(3)));
   frames.push({frame:f,crossings:pts.length,points:pts});
  }
  return {ids:c.ids,framesWithCrossings:frames.filter(f=>f.crossings).length,maxCrossings:Math.max(...frames.map(f=>f.crossings)),frames};
 });
 return {format:'MonarcaC3BezierBoundaryAudit',version:1,exercise:'calf',frames:48,profiles:24,stepsPerCubic:steps,reviewPairs:4,pairs,limitations:['Sampled cubic approximation, not exact Bézier intersection proof','Exterior seams can legitimately overlap','Browser visual QA pending']};
}
if(require.main===module){
 const fs=require('node:fs'),path=require('node:path');
 const html=fs.readFileSync(path.join(__dirname,'comparacion_visual_calf.html'),'utf8');
 const match=html.match(/const stage=\$\('stage'\),M=(.*?);\n/);
 if(!match)throw Error('C1 missing');
 const M=Function('return ('+match[1]+')')();
 fs.writeFileSync(path.join(__dirname,'contact_bezier_calf.qa.json'),JSON.stringify(analyze(M),null,2)+'\n');
}
module.exports={sampleSide,intersection,crossings,analyze};
