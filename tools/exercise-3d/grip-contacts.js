const tetherGear=new Set(['BAND','CABLE','SKI_ERG','STRAP','BATTLE_ROPE','ROPE','JUMP_ROPE','ROWER']);
const flexible=new Set(['BAND','CABLE','ROPE']);

function gripContacts(clip,pose) {
  const applicable=(clip.gear ?? []).filter(([type])=>tetherGear.has(type));
  if (!applicable.length) return () => null;
  const grip=[4,6].map(i=>{
    const [x,y]=pose[i];
    const dx=x-pose[i-1][0],dy=y-pose[i-1][1];
    const length=Math.hypot(dx,dy);
    const offset=length>0 ? (8*.417/165)/(clip.scale*length) : 0;
    return [x+dx*offset,y+dy*offset];
  });
  const common=applicable.some(([name,mount])=>mount==='SHARED' &&
    !['SKI_ERG','BATTLE_ROPE','JUMP_ROPE'].includes(name));
  const candidates=grip.map((point,i)=>({point,wrist:i===0 ? 'near' : 'far'}));
  if(common) candidates.push({point:[(grip[0][0]+grip[1][0])/2,(grip[0][1]+grip[1][1])/2],wrist:'both'});
  return (x,y,stroke)=>{
    if (stroke && !flexible.has(stroke)) return null;
    let best=.010, result=null;
    for (const {point,wrist} of candidates) {
      const error=Math.hypot(x-point[0],y-point[1])*clip.scale;
      if(error<best || (error===best && wrist==='both')) {
        best=error; result=wrist;
      }
    }
    return result;
  };
}


export function gripBindings(clip) {
  if (!(clip.gear ?? []).some(([type])=>tetherGear.has(type))) return null;
  const links=[], handles=[];
  function vote(table,index,wrist) {
    const tally=table[index] ??= {};
    if (wrist) tally[wrist]=(tally[wrist] ?? 0)+1;
  }
  for (let i=0;i<clip.frames.length;i++) {
    const match=gripContacts(clip,clip.frames[i]);
    const frame=clip.equipment[i];
    frame.links.forEach(([x1,y1,x2,y2,,,,stroke],index)=>{
      vote(links,2*index,match(x1,y1,stroke));
      vote(links,2*index+1,match(x2,y2,stroke));
    });
    frame.sprites.forEach(([asset,x,y],index)=>{
      if(asset==='handle') vote(handles,index,match(x,y));
    });
  }
  const required=clip.frames.length*.72;
  function stable(table) {
    return table.map(tally=>{
      const best=Object.entries(tally).sort((a,b)=>b[1]-a[1])[0];
      return best && best[1]>=required ? best[0] : null;
    });
  }
  const result={links:stable(links),handles:stable(handles)};
  return result.links.some(Boolean) || result.handles.some(Boolean) ? result : null;
}
