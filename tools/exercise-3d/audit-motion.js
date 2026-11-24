// A fixed, level stance can be recovered from a single view. Moving feet,
// raised platforms and apparatus supports need an exercise-specific solution.
const groundCompatible = new Set(['DUMBBELL','KETTLEBELL','BARBELL','PLATE','BAND','CABLE','STICK','MED_BALL']);

export function auditMotion(clip) {
  if (clip.planted) return {status:'authored'};
  if (clip.suspended || clip.handSupport || clip.posture!=='UPRIGHT') return {status:'special-support'};
  if (clip.view!=='SIDE' && clip.view!=='FRONT') return {status:'depth-required'};
  if (clip.props.some(([kind])=>kind!=='mat') ||
      (clip.gear ?? []).some(([gear])=>!groundCompatible.has(gear))) return {status:'apparatus-support'};
  if (!clip.frames.length) return {status:'missing'};
  const anchors=[8,10].map(index=>{
    const axis=[0,1].map(d=>clip.frames.map(frame=>frame[index][d]));
    const spread=Math.hypot(...axis.map(values=>Math.max(...values)-Math.min(...values)))*clip.scale;
    return {point:axis.map(values=>values.reduce((sum,value)=>sum+value,0)/values.length),spread};
  });
  if (anchors.some(anchor=>anchor.spread>.012)) return {status:'traveling-feet'};
  if (Math.abs(anchors[0].point[1]-anchors[1].point[1])*clip.scale>.035)
    return {status:'uneven-support'};
  if (clip.view==='FRONT' && Math.abs(anchors[0].point[0]-anchors[1].point[0])*clip.scale>.74)
    return {status:'wide-stance'};

  const upright=clip.frames.every(p=>{
    const [x,y]=p[1], [hx,hy]=p[2];
    if (hy-y<Math.abs(hx-x)*.75 || (hy-y)*clip.scale<.17) return false;
    return [7,9].every(k=>p[k+1][1]-hy>0 && (p[k+1][1]-p[k][1])*clip.scale>.08);
  });
  if (!upright) return {status:'position-review'};
  const neutral=clip.frames.reduce((best,frame)=>frame[2][1]<best[2][1] ? frame : best);
  return {status:'anchors',points:anchors.map(({point})=>point),neutral};
}
