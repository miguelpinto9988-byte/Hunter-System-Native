import { MathUtils } from 'three';

const blend = MathUtils.lerp;
const clamp = MathUtils.clamp;

export function phaseInterval(clip, phase) {
  const n=clip.times.length;
  if (clip.hold || n<2) return {i:0,j:0,before:0,after:0,t:0,span:1,previousSpan:1,nextSpan:1};
  phase=phase-Math.floor(phase);
  let i=n-1;
  while (i>0 && clip.times[i]>phase) i--;
  const j=(i+1)%n, before=(i+n-1)%n, after=(j+1)%n;
  const span=(j===0 ? 1 : clip.times[j])-clip.times[i];
  const previousSpan=i===0 ? 1-clip.times[before] : clip.times[i]-clip.times[before];
  const nextSpan=(after===0 ? 1 : clip.times[after])-clip.times[j];
  return {i,j,before,after,t:span>0 ? clamp((phase-clip.times[i])/span,0,1) : 0,
    span,previousSpan,nextSpan};
}

function slope(a,b,h1,h2) {
  if (a*b<=0 || h1<=0 || h2<=0) return 0;
  const w1=2*h2+h1, w2=h2+2*h1;
  return (w1+w2)/(w1/a+w2/b);
}

export function samplePose(clip,interval) {
  const {i,j,before,after,t,span,previousSpan,nextSpan}=interval;
  if (i===j || t===0) return clip.frames[i];
  const start=clip.frames[i], end=clip.frames[j];
  const t2=t*t, t3=t2*t;
  return start.map((joint,k)=>joint.map((a,d)=>{
    const b=end[k][d], delta=(b-a)/span;
    const left=previousSpan>0 ? (a-clip.frames[before][k][d])/previousSpan : 0;
    const right=nextSpan>0 ? (clip.frames[after][k][d]-b)/nextSpan : 0;
    const v=(2*t3-3*t2+1)*a+(t3-2*t2+t)*span*slope(left,delta,previousSpan,span)
      +(-2*t3+3*t2)*b+(t3-t2)*span*slope(delta,right,span,nextSpan);
    return clamp(v,Math.min(a,b),Math.max(a,b));
  }));
}

export function sampleEquipment(clip,{i,j,t}) {
  const start=clip.equipment[i], next=clip.equipment[j];
  if (i===j || !t || !next) return start;
  const sprites=start.sprites.map((item,k)=>{
    const b=next.sprites[k];
    if (!b || b[0]!==item[0] || b[6]!==item[6]) return item;
    const angle=((b[5]-item[5]+540)%360)-180;
    return [item[0],blend(item[1],b[1],t),blend(item[2],b[2],t),
      blend(item[3],b[3],t),blend(item[4],b[4],t),item[5]+angle*t,item[6]];
  });
  const links=start.links.map((item,k)=>{
    const b=next.links[k];
    if (!b || b[7]!==item[7]) return item;
    return item.map((value,n)=>n<7 && Number.isFinite(value) && Number.isFinite(b[n])
      ? blend(value,b[n],t) : value);
  });
  return {sprites,links};
}
