/** Monarca C3: outer-surface sculpt; C1/C2 and historical poses are read-only. */
'use strict';
const L3Sculpt=(()=>{
 const presets={
  "torso": {
    "a": 0.07,
    "b": 0.06,
    "focus": 0.32,
    "spread": 0.35
  },
  "hombro_posterior": {
    "a": 0.105,
    "b": 0.09,
    "focus": 0.36,
    "spread": 0.31
  },
  "deltoides_frontal": {
    "a": 0.12,
    "b": 0.095,
    "focus": 0.35,
    "spread": 0.32
  },
  "pectoral_superficial": {
    "a": 0.095,
    "b": 0.12,
    "focus": 0.32,
    "spread": 0.35
  },
  "brazo_posterior": {
    "a": 0.1,
    "b": 0.09,
    "focus": 0.36,
    "spread": 0.31
  },
  "brazo_frontal": {
    "a": 0.095,
    "b": 0.1,
    "focus": 0.38,
    "spread": 0.31
  },
  "antebrazo_posterior": {
    "a": 0.075,
    "b": 0.08,
    "focus": 0.43,
    "spread": 0.38
  },
  "antebrazo_frontal": {
    "a": 0.07,
    "b": 0.09,
    "focus": 0.43,
    "spread": 0.38
  },
  "gluteo_posterior": {
    "a": 0.095,
    "b": 0.08,
    "focus": 0.43,
    "spread": 0.33
  },
  "cadera_frontal": {
    "a": 0.095,
    "b": 0.085,
    "focus": 0.43,
    "spread": 0.33
  },
  "muslo_posterior": {
    "a": 0.095,
    "b": 0.08,
    "focus": 0.36,
    "spread": 0.34
  },
  "muslo_frontal": {
    "a": 0.11,
    "b": 0.085,
    "focus": 0.37,
    "spread": 0.35
  },
  "pantorrilla_posterior": {
    "a": 0.14,
    "b": 0.105,
    "focus": 0.34,
    "spread": 0.25,
    "act": 0.24
  },
  "pantorrilla_frontal": {
    "a": 0.125,
    "b": 0.145,
    "focus": 0.33,
    "spread": 0.26,
    "act": 0.24
  }
};
 function windowBump(t,focus=.5,spread=.32){
   if(t<=0||t>=1)return 0;
   return Math.pow(Math.sin(Math.PI*t),1.5)*Math.exp(-.5*((t-focus)/spread)**2);
 }
 function computeSurface(profile,points,phase,inflate=1){
   if(!Number.isFinite(phase)||phase<0||phase>1||!Number.isFinite(inflate)||inflate<=0||points.length<3)throw Error('invalid phase or relief');
   const spec=presets[profile.id]||{a:0,b:0,focus:.5,spread:.3};
   const k=(1-Math.cos(2*Math.PI*phase))/2;
   const A=[],B=[],ridge=[],shadow=[];
   for(let i=0;i<points.length;i++){
     const [x,y,l,r]=points[i],p=points[Math.max(0,i-1)],q=points[Math.min(points.length-1,i+1)];
     const dx=q[0]-p[0],dy=q[1]-p[1],len=Math.hypot(dx,dy)||1,nx=-dy/len,ny=dx/len,t=i/(points.length-1);
     const rest=windowBump(t,spec.focus,spec.spread);
     const action=(spec.act||0)*windowBump(t,.36,.27)*k;
     const wl=l*inflate*(1+spec.a*rest+action),wr=r*inflate*(1+spec.b*rest+action);
     A.push([x+nx*wl,y+ny*wl]);
     B.push([x-nx*wr,y-ny*wr]);
     ridge.push([x+nx*wl*.36,y+ny*wl*.36]);
     shadow.push([x-nx*wr*.46,y-ny*wr*.46]);
   }
   return {A,B,ridge,shadow};
 }
 return {presets,windowBump,computeSurface};
})();
if(typeof module!=='undefined')module.exports=L3Sculpt;
