import {Vector3, MathUtils} from 'three';

const stepSeconds=.60;
const includesApproach=clip=>clip.planted==='reverse-lunge' || /^dblunge(?:_(?:slow|pause))?$/.test(clip.exerciseId ?? '');

function deadBugTempo(id) {
  if (id==='deadbug_pause') return [1.35,1,1.35];
  if (id==='deadbug_slow') return [1.35,0,3];
  if (id==='deadbug') return [1.35,0,1.35];
  return null;
}

export function deadBugExtension(clip, phase) {
  const [out,hold,back]=deadBugTempo(clip.exerciseId);
  const halfPhase=phase*2;
  const seconds=(halfPhase%1)*(out+hold+back);
  const progress=MathUtils.clamp(Math.min(seconds/out,(out+hold+back-seconds)/back),0,1);
  // Zero velocity and acceleration at tabletop and at full extension.
  return progress**3*(10+progress*(-15+6*progress));
}

export function playbackDuration(clip) {
  const tempo=deadBugTempo(clip.exerciseId);
  if (tempo) return 2*tempo.reduce((sum,seconds)=>sum+seconds,0);
  return clip.duration+(includesApproach(clip) ? 2*stepSeconds : 0);
}

export function approachFraction(clip) {
  return includesApproach(clip) ? stepSeconds/playbackDuration(clip) : 0;
}

// Hand positions and load shifts follow the NASM, ACE, and variant
// references recorded in the Nimble support audit.
export function reviewedGroundMotion(clip) {
  const id=clip.exerciseId ?? '';
  if (/^home-plankjack30(?:-(?:hold|ecc)30)?$/.test(id)) return 'plank-jack';
  if (/^closepush(?:_(?:pause|slow))?$/.test(id)) return 'close';
  if (id==='pushup-control19') return 'controlled';
  if (id==='archer19') return 'archer';
  return /^home-(diamond23|pushwide30|staggered23)(?:-(?:pause23|tempo23|hold30|ecc30))?$/.exec(id)?.[1];
}

export function plantedMotion(clip, feet, pushUp=reviewedGroundMotion(clip)) {
  const kind=pushUp ? 'pushup' : /^dblunge(?:_(?:slow|pause))?$/.test(clip.exerciseId ?? '') ? 'reverse-lunge' : /^dbcalf(?:_(?:slow|pause))?$/.test(clip.exerciseId ?? '') ? 'calf' : clip.planted;
  if (pushUp==='plank-jack') {
    const separation=frame=>Math.abs(frame[10][0]-frame[8][0]);
    const widths=clip.frames.map(separation);
    const narrow=Math.min(...widths), wide=Math.max(...widths);
    return frame=>{
      // Preserve the source timing, including the hold at maximum width.
      const spread=MathUtils.clamp((separation(frame)-narrow)/Math.max(1,wide-narrow),0,1);
      const lift=.048*Math.sin(Math.PI*spread)**2;
      const width=.093+.202*spread;
      const hip=new Vector3(0,.385,-.49);
      const trunk=new Vector3(0,.076,Math.sqrt(.417**2-.076**2));
      return {
        hip,trunk,
        wrist:side=>new Vector3(side*.213,.052,-.017),
        elbow:side=>new Vector3(side*.23,.245,-.09),
        foot:side=>{
          const y=feet[side].toe+lift;
          const travel=Math.sqrt(Math.max(0,(feet[side].length-.0025)**2-
            (hip.y-y)**2-(width-.155)**2));
          return new Vector3(side*width,y,-.49-travel);
        },
        toePitch:.70,
        contact:()=>lift<.004
      };
    };
  }
  if (kind==='mountain') {
    const baseline=[8,10].map(ankle=>Math.max(...clip.frames.map(frame=>frame[ankle][0])));
    const travel=240;
    const hipY=.545,hipZ=-.408;
    return frame=>{
      const legs={};
      for(const side of [1,-1]){
        const i=side===1 ? 0 : 1;
        const foot=feet[side];
        const drive=MathUtils.clamp((baseline[i]-frame[i===0 ? 8 : 10][0])/travel,0,1);
        const straight=Math.asin((foot.toe-hipY)/foot.length);
        const thigh=MathUtils.lerp(straight,-2.68,drive);
        const kneeY=hipY+Math.sin(thigh)*foot.upper;
        const kneeZ=hipZ-Math.cos(thigh)*foot.upper;
        const ankleY=foot.toe+.105*drive;
        const dy=MathUtils.clamp(ankleY-kneeY,-foot.lower*.98,foot.lower*.98);
        const ankleZ=kneeZ-Math.sqrt(foot.lower**2-dy**2)+.004*(1-drive);
        legs[side]={drive,
          knee:new Vector3(side*.157,kneeY,kneeZ),
          ankle:new Vector3(side*.157,kneeY+dy,ankleZ)};
      }
      return {
        hip:new Vector3(0,hipY,hipZ),
        trunk:new Vector3(0,-.048,Math.sqrt(.417**2-.048**2)),
        foot:side=>legs[side].ankle,
        knee:side=>legs[side].knee,
        wrist:side=>new Vector3(side*.241,.052,.020),
        elbow:side=>new Vector3(side*.266,.28,-.10),
        toePitch:side=>.70-.27*legs[side].drive,
        contact:side=>legs[side].drive<.03
      };
    };
  }
  if (kind==='plank') {
    const ankleY=(feet[1].toe+feet[-1].toe)/2, ankleZ=-1.232;
    const hipHeight=.269, extension=Math.min(feet[1].length,feet[-1].length)-.0035;
    const hipZ=ankleZ+Math.sqrt(extension**2-(hipHeight-ankleY)**2-.022**2);
    return ()=>({
      hip:new Vector3(0,hipHeight,hipZ),
      trunk:new Vector3(0,.033,Math.sqrt(.417*.417-.033*.033)),
      foot:side=>new Vector3(side*.135,feet[side].toe,ankleZ),
      knee:side=>new Vector3(side*.154,.13,-.81),
      forearmSupport:true,
      toePitch:.70
    });
  }
  if (!['squat','pushup','bridge','calf','reverse-lunge','split-squat'].includes(kind)) return null;
  const landmark=pushUp==='diamond23' || pushUp==='pushwide30' ? 3 : kind==='pushup' ? 1 : 2;
  const values=clip.frames.map(frame=>frame[landmark][1]);
  const low=Math.min(...values), high=Math.max(...values);
  if (high-low<1e-4) return null;

  const edge=approachFraction(clip);
  return (frame, phase, mirror) => {
    // Preserve the catalog's individual descent, pause, and recovery timing.
    const depth=MathUtils.clamp((frame[landmark][1]-low)/(high-low),0,1);
    if (kind==='reverse-lunge' || kind==='split-squat') {
      const reverse=kind==='reverse-lunge';
      const lead=mirror ? -1 : 1;
      const step=reverse ? MathUtils.smoothstep(Math.min(phase,1-phase),0,edge) : 1;
      const dip=reverse && (phase<edge || phase>1-edge) ? 0 : depth;
      const swing=reverse ? .096*4*step*(1-step) : 0;
      const rearPitch=.34*step;
      const foot=side=>{
        if (side===lead) return new Vector3(side*.167,feet[side].flat,.135);
        const [y,z]=feet[side].contact(rearPitch);
        return new Vector3(side*.167,y+swing,.135-.651*step+z);
      };
      return {
        hip:new Vector3(0,.958-.064*step-.315*dip,.112-(reverse?.27:.29)*step+.018*dip),
        trunk:new Vector3(0,1,reverse ? .065+.12*dip : .07+.047*dip),
        foot,
        knee:side=>step<.001 ? new Vector3(side*.167,.50,.22) : side===lead
          ? new Vector3(side*.174,.56-.22*dip,.31+.065*dip)
          : new Vector3(side*.17,.54-.30*dip,-.12-.14*step),
        toePitch:side=>side===lead ? 0 : rearPitch,
        contact:side=>side===lead || swing<.006,
        armsHanging:true
      };
    }
    if (kind==='bridge') {
      const lift=1-depth;
      const bridgeRise=/^bridge(?:_(?:pause|slow))?$/.test(clip.exerciseId ?? '') ? .265 : .31;
      const rise=bridgeRise*lift-.016;
      const run=Math.sqrt(.417**2-rise**2);
      return {
        hip:new Vector3(0,.157+rise,-.415+run),
        trunk:new Vector3(0,-rise,-run),
        foot:side=>new Vector3(side*.17,feet[side].flat,.47),
        knee:side=>new Vector3(side*.17,.59,.32),
        armsAlongFloor:true
      };
    }
    if (kind==='calf') {
      const lift=1-depth;
      const toePitch=.53*lift;
      const foot=side=>{
        const [y,z]=feet[side].contact(toePitch);
        return new Vector3(side*.17,y,z);
      };
      const left=foot(1), right=foot(-1);
      const heelY=(left.y+right.y)/2,heelZ=(left.z+right.z)/2;
      // Keep the body over the support and extend the ankle without a knee dip.
      return {
        hip:new Vector3(0,heelY+.891,heelZ+.012),
        trunk:new Vector3(0,1,.026),
        foot,
        knee:side=>new Vector3(side*.177,heelY+.49,heelZ+.08),
        armsHanging:true,
        toePitch
      };
    }
    if (kind==='squat') {
      const lean=.10+.46*depth;
      const hip=new Vector3(0,.958-.397*depth,-.026-.254*depth);
      return {
        hip,
        trunk:new Vector3(0,Math.cos(lean),Math.sin(lean)),
        foot:side=>new Vector3(side*.19,feet[side].flat,0),
        knee:side=>new Vector3(side*.205,.51-depth*.15,.18+.13*depth)
      };
    }
    const hipHeight=.401-.147*depth;
    const toePitch=pushUp==='close' ? .96 : .70;
    const supportY=side=>pushUp==='close' ? feet[side].contact(toePitch)[0]+.001 : feet[side].toe;
    const ankleY=(supportY(1)+supportY(-1))/2, ankleZ=-1.232;
    const extension=Math.min(feet[1].length,feet[-1].length)-.0035;
    const shift=pushUp==='archer' ? .028*depth : 0;
    const hipZ=ankleZ+Math.sqrt(extension**2-(hipHeight-ankleY)**2-(.022+shift)**2);
    const hip=new Vector3(shift,hipHeight,hipZ);
    const torsoLift=.115-.105*depth;
    const wrist=side=>{
      if (pushUp==='close') return new Vector3(side*.144,.052,.012);
      if (pushUp==='diamond23') return new Vector3(side*.058,.052,.012);
      if (pushUp==='pushwide30') return new Vector3(side*.316,.052,.012);
      if (pushUp==='staggered23') return new Vector3(side*.247,.052,.012+side*.094);
      if (pushUp==='archer') return new Vector3(side*.445,.052,.012);
      return new Vector3(side*.247,.052,.012);
    };
    return {
      hip,
      trunk:new Vector3(shift*4,torsoLift,Math.sqrt(.417*.417-torsoLift*torsoLift-(shift*4)**2)),
      foot:side=>new Vector3(side*.135,supportY(side),ankleZ),
      knee:side=>new Vector3(side*.154,.13,-.81),
      wrist,
      elbow:side=>new Vector3(
        (pushUp==='diamond23' || pushUp==='close') ? side*.185 : pushUp==='archer' ? side*.38+shift*.30 : side*(.27+.043*depth),
        .28-.037*depth,-.124-.14*depth),
      toePitch
    };
  };
}
