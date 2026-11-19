'use strict';
/** C3 inter-patch scanline overlap audit. Only existing historical C1 poses. */
function transformPoint(p,point,body,heel){
 const [x,y]=point;
 if(p.track==='body')return [x,y+body];
 if(p.track==='shin-near'||p.track==='shin-far'){
  const knee=570,scale=1+(heel-body)/(685.69-knee);
  return [x,body+knee+(y-knee)*scale];
 }
 if(p.track==='foot-near'||p.track==='foot-far'){
  const near=p.track==='foot-near',toe=near?[358,704]:[418,704],h=near?[323.83,685.69]:[383.83,685.69];
  const angle=Math.atan2(h[1]+heel-toe[1],h[0]-toe[0])-Math.atan2(h[1]-toe[1],h[0]-toe[0]);
  const dx=x-toe[0],dy=y-toe[1],c=Math.cos(angle),s=Math.sin(angle);
  return [toe[0]+dx*c-dy*s,toe[1]+dx*s+dy*c];
 }
 throw Error('Unsupported track '+p.track);
}
function intervals(poly,y){
 const xs=[];
 for(let i=0;i<poly.length;i++){
  const a=poly[i],b=poly[(i+1)%poly.length];
  if((a[1]<=y&&y<b[1])||(b[1]<=y&&y<a[1]))
   xs.push(a[0]+(y-a[1])*(b[0]-a[0])/(b[1]-a[1]));
 }
 xs.sort((a,b)=>a-b);
 const out=[];for(let i=0;i+1<xs.length;i+=2)out.push([xs[i],xs[i+1]]);
 return out;
}
function overlap(a,b){let v=0;for(const x of a)for(const y of b)v+=Math.max(0,Math.min(x[1],y[1])-Math.max(x[0],y[0]));return v}
function audit(profiles,motion,smoothStations,computeSurface){
 if(motion.body.length!==48||motion.heel.length!==48)throw Error('48 historical poses required');
 const ready=profiles.map(p=>({p,stations:smoothStations(p.stations,4)}));
 const pairStats=[];
 for(let i=0;i<ready.length;i++)for(let j=i+1;j<ready.length;j++)pairStats.push({i,j,ids:[ready[i].p.id,ready[j].p.id],framesWithOverlap:0,maxWidth:0,maxAreaEstimate:0,peakFrame:0});
 for(let f=0;f<48;f++){
  const body=motion.body[f],heel=motion.heel[f];
  if(!Number.isFinite(body)||!Number.isFinite(heel))throw Error('Nonfinite historical pose');
  const shapes=ready.map(({p,stations})=>{
   const g=computeSurface(p,stations,f/48,1);
   const poly=[...g.A,...g.B.slice().reverse()].map(pt=>transformPoint(p,pt,body,heel));
   const ys=poly.map(q=>q[1]);return {poly,lo:Math.min(...ys),hi:Math.max(...ys)};
  });
  for(const st of pairStats){
   const a=shapes[st.i],b=shapes[st.j],lo=Math.max(a.lo,b.lo),hi=Math.min(a.hi,b.hi);
   if(hi<=lo)continue;
   let maxWidth=0,area=0;
   const samples=[(lo+hi)/2];for(let y=Math.ceil(lo)+.5;y<hi;y++)samples.push(y);
   for(const y of samples){
    if(y<=lo||y>=hi)continue;
    const w=overlap(intervals(a.poly,y),intervals(b.poly,y));
    maxWidth=Math.max(maxWidth,w);
    if(y!==(lo+hi)/2)area+=w;
   }
   if(maxWidth>1e-6){st.framesWithOverlap++;st.maxWidth=Math.max(st.maxWidth,maxWidth);if(area>st.maxAreaEstimate){st.maxAreaEstimate=area;st.peakFrame=f}}
  }
 }
 const pairs=pairStats.filter(s=>s.framesWithOverlap).map(({ids,framesWithOverlap,maxWidth,maxAreaEstimate,peakFrame})=>({ids,framesWithOverlap,maxWidth:+maxWidth.toFixed(3),maxAreaEstimate:+maxAreaEstimate.toFixed(2),peakFrame})).sort((a,b)=>b.maxAreaEstimate-a.maxAreaEstimate);
 return {format:'MonarcaC3InterpatchAudit',version:1,exercise:'calf',frames:48,profiles:profiles.length,pairCount:pairStats.length,overlappingPairs:pairs.length,nonOverlappingPairs:pairStats.length-pairs.length,scanStep:1,pairs,limitations:['Positive overlap may be intended occlusion or seam, not a defect','Polygon sampling cannot prove exact Bézier collision','No browser screenshot or 3D collision proof']};
}
if(typeof module!=='undefined')module.exports={transformPoint,intervals,overlap,audit};
