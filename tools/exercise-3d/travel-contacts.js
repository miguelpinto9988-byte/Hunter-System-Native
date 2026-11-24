const supportedTravel = /^(walk|jog|fastmarch|kneeliftcardio)(_(easy|short))?$/;

// Use only runs with a stationary ankle at the lowest level of the source.
export function travelContacts(clip, audit) {
  if (!supportedTravel.test(clip.exerciseId ?? '') || audit.status!=='traveling-feet' || clip.view!=='SIDE' || clip.hold ||
      (clip.gear ?? []).length || clip.props.some(([kind])=>kind!=='mat')) return null;
  const frames=clip.frames;
  if (frames.length<24 || frames.some(p=>p[2][1]-p[1][1]<Math.abs(p[2][0]-p[1][0])*.72)) return null;
  const floor=Math.max(...frames.flatMap(p=>[p[8][1],p[10][1]]));
  const contacts=[8,10].map(index=>{
    const result=Array(frames.length).fill(null);
    let start=0;
    while (start<frames.length) {
      const [x,y]=frames[start][index];
      if ((floor-y)*clip.scale>.020 || (y-frames[start][index-1][1])*clip.scale<.09) {
        start++;
        continue;
      }
      let end=start+1;
      while(end<frames.length) {
        const point=frames[end][index];
        if (Math.hypot(point[0]-x,point[1]-y)*clip.scale>.013) break;
        end++;
      }
      if (end-start>=4) {
        const anchor=[x,y];
        for (let i=start;i<end;i++) result[i]=anchor;
      }
      start=end;
    }
    // Two ends of a looping stance should share their exact world anchor.
    const first=result[0],last=result.at(-1);
    if(first && last && Math.hypot(first[0]-last[0],first[1]-last[1])*clip.scale<.012) {
      for(let i=result.length-1;i>=0 && result[i]===last;i--) result[i]=first;
    }
    return result;
  });
  const count=contacts[0].filter(Boolean).length+contacts[1].filter(Boolean).length;
  return count>=Math.ceil(frames.length*.40) ? contacts : null;
}

export function contactAt(timeline, interval) {
  const a=timeline[interval.i], b=timeline[interval.j];
  if (a && a===b) return {point:a,weight:1};
  const mix=interval.t*interval.t*(3-2*interval.t);
  if (a && b) return mix<=.5 ? {point:a,weight:1-2*mix} : {point:b,weight:2*mix-1};
  if (a) return {point:a,weight:1-mix};
  if (b) return {point:b,weight:mix};
  return null;
}
