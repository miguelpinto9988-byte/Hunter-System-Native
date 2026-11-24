// The side-view sources can show two palms at slightly different screen heights
// even when both are planted. Use one ground plane for the stationary hands.
export function floorHands(clip) {
  // The reverse-plank references keep both palms planted throughout the lift.
  if (reversePlankHands(clip)) return [clip.frames[0][4],clip.frames[0][6]];
  if (/^mountain(?:_(?:easy|short))?$/.test(clip.exerciseId ?? '')) return null;
  if (clip.planted || !clip.handSupport || clip.posture!=='PRONE' ||
      clip.view!=='SIDE' || clip.suspended || clip.props.length ||
      (clip.gear ?? []).length) return null;
  const floor=Math.max(...clip.frames.flatMap(p=>[p[4][1],p[6][1],p[8][1],p[10][1]]));
  const hands=[4,6].map(index=>{
    const [x,y]=clip.frames[0][index];
    if ((floor-y)*clip.scale>.065) return null;
    if (clip.frames.some(p=>Math.hypot(p[index][0]-x,p[index][1]-y)*clip.scale>.012 ||
        (p[index][1]-p[index-1][1])*clip.scale<.09)) return null;
    return [x,y];
  });
  return hands.some(Boolean) ? hands : null;
}

export function reversePlankHands(clip) {
  return clip.view==='SIDE' && clip.posture==='SUPINE' &&
    /^home-reverse-(?:plank-lift|knee)23(?:-(?:pause|tempo)23)?$/.test(clip.exerciseId ?? '');
}
