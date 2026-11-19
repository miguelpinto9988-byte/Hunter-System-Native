/** Deterministic exterior seam QA. Uses ONLY original 48-frame C1 samples. */
'use strict';
const SEAMS={
 frontKnee:['rotula_frontal','pantorrilla_frontal'],
 rearKnee:['muslo_posterior','pantorrilla_posterior'],
 frontAnkle:['pantorrilla_frontal','tobillo_frontal'],
 rearAnkle:['pantorrilla_posterior','tobillo_posterior']
};
function atY(side,y){
 const xs=[];
 for(let i=0;i<side.length-1;i++){
   const a=side[i],b=side[i+1];
   if(y>=Math.min(a[1],b[1])&&y<=Math.max(a[1],b[1])&&Math.abs(a[1]-b[1])>1e-8)
     xs.push(a[0]+(y-a[1])/(b[1]-a[1])*(b[0]-a[0]));
 }
 return xs;
}
function span(g,y){
 const xs=[...atY(g.A,y),...atY(g.B,y)];
 return xs.length>=2?[Math.min(...xs),Math.max(...xs)]:null;
}
function trackPoint(profile,point,body,heel){
 const [x,y]=point;
 if(profile.track==='body')return [x,y+body];
 if(profile.track==='shin-near'||profile.track==='shin-far'){
   const knee=570,den=685.69-knee,scale=1+(heel-body)/den;
   return [x,body+knee+(y-knee)*scale];
 }
 throw Error('Unsupported seam track '+profile.track);
}
function evaluateSeams(profiles,motion,smoothStations,computeSurface){
 if(motion.body.length!==48||motion.heel.length!==48)throw Error('48 historical poses required');
 const byId=new Map(profiles.map(p=>[p.id,p]));
 const results={};
 for(const [name,ids] of Object.entries(SEAMS)){
   const pair=ids.map(id=>{const p=byId.get(id);if(!p)throw Error('Missing profile '+id);return {p,stations:smoothStations(p.stations,4)}});
   const frames=[];
   for(let frame=0;frame<48;frame++){
     const body=motion.body[frame],heel=motion.heel[frame];
     if(!Number.isFinite(body)||!Number.isFinite(heel))throw Error('Invalid historical pose');
     const geometries=pair.map(({p,stations})=>{
       const g=computeSurface(p,stations,frame/48,1);
       return {A:g.A.map(q=>trackPoint(p,q,body,heel)),B:g.B.map(q=>trackPoint(p,q,body,heel))};
     });
     let maxOverlap=0,sharedRows=0;
     // Half-unit scan through the complete lower-body region; no guessed pose.
     for(let y=535+body;y<=700+body;y+=.5){
       const a=span(geometries[0],y),b=span(geometries[1],y);
       if(!a||!b)continue;
       sharedRows++;
       maxOverlap=Math.max(maxOverlap,Math.min(a[1],b[1])-Math.max(a[0],b[0]));
     }
     frames.push({frame,sharedRows,maxOverlap:+maxOverlap.toFixed(4)});
   }
   results[name]={
     profiles:ids,framesChecked:48,
     minOverlap:+Math.min(...frames.map(f=>f.maxOverlap)).toFixed(4),
     maxOverlap:+Math.max(...frames.map(f=>f.maxOverlap)).toFixed(4),
     minSharedRows:Math.min(...frames.map(f=>f.sharedRows)),
     maxSharedRows:Math.max(...frames.map(f=>f.sharedRows)),
     maxTemporalDelta:+Math.max(...frames.slice(1).map((f,i)=>Math.abs(f.maxOverlap-frames[i].maxOverlap)),Math.abs(frames[0].maxOverlap-frames[47].maxOverlap)).toFixed(4),
     failingFrames:frames.filter(f=>f.sharedRows===0||f.maxOverlap<=0).map(f=>f.frame)
   };
 }
 return {format:'MonarcaC3SeamQA',version:1,exercise:'calf',motionFrames:48,scanStep:0.5,seams:results,limitations:['This detects positive cross-sectional overlap, not full 2D polygon collision or visual occlusion','No browser screenshot or inter-patch clipping proof']};
}
if(typeof module!=='undefined')module.exports={SEAMS,atY,span,trackPoint,evaluateSeams};
