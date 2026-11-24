import * as THREE from 'three';
import { buildCardioChassis, cardioGear, cardioPart, cardioPedalSurface, placeCardioPart, platformSurfaceHeight } from './cardio-equipment.js';
import { approachFraction, deadBugExtension, playbackDuration, plantedMotion, reviewedGroundMotion } from './planted-motion.js';
import { guidedMachine, guidedComponent, isGuidedComponent, placeGuidedComponent } from './guided-equipment.js';
import { isStrengthWeight, strengthWeight } from './strength-equipment.js';
import { contactAt, travelContacts } from './travel-contacts.js';
import { gripBindings } from './grip-contacts.js';
import { verifiedLegTrajectories } from './leg-trajectories.js';
import { thirdBatchMotion } from './review-third-batch.js';
import { floorHands, reversePlankHands } from './floor-hands.js';
import { verifiedWristTrajectories } from './wrist-trajectories.js';
import { auditMotion } from './audit-motion.js';
import { phaseInterval, sampleEquipment, samplePose } from './pose-timing.js';

const freeLegGear=new Set(['DUMBBELL','KETTLEBELL','BARBELL','PLATE','MED_BALL','BAND']);
const freeArmGear=new Set(['DUMBBELL','KETTLEBELL','BARBELL','PLATE','MED_BALL']);
const up = new THREE.Vector3(0,1,0);
const reverseHeelPitch=-.22;
const transverse = new THREE.Vector3(1,0,0);
const forward = new THREE.Vector3(0,0,1);
const vec = () => new THREE.Vector3();
const rotation = () => new THREE.Quaternion();

export async function projectedMotion(athlete, clips, scene, controls, camera) {
  if (!clips.length || clips.some(clip => !clip.frames.length)) throw Error('No hay poses');
  const rig = new Map();
  athlete.updateMatrixWorld(true);
  athlete.traverse(bone => {
    if (!bone.isBone) return;
    const name = bone.name.replace('.','');
    rig.set(name, { bone, position: bone.getWorldPosition(vec()),
      rotation: bone.getWorldQuaternion(rotation()), localPosition: bone.position.clone() });
  });
  function data(name) {
    const value = rig.get(name);
    if (!value) throw Error('Falta el hueso ' + name);
    return value;
  }
  const torso = data('Torso');
  const head = data('Head');
  const lengths = ['L','R'].map(side => ({
    arm: data('Arm'+side), forearm: data('Forearm'+side), hand: data('Hand'+side),
    thigh: data('Thigh'+side), shin: data('Shin'+side), foot: data('Foot'+side)
  }));
  const audits=clips.map(auditMotion);
  const gaitTracks=clips.map((clip,i)=>travelContacts(clip,audits[i]));
  // These sources show both feet and their flight in a single unobstructed view.
  const jumpClips=clips.map(clip =>
    /^(?:(?:jumpjack|rope)(?:_(?:easy|short))?|lowhop|linehop|squatjump|skaterhop|home-hopforward30(?:-(?:hold|ecc)30)?|new46-(?:doublepogo|singlepogo|forwardhop|crosshop|splitjump|jumpropeside))$/.test(clip.exerciseId ?? '') &&
      (clip.view==='SIDE' || clip.view==='FRONT') && clip.posture==='UPRIGHT');
  const pedalClips=clips.map(clip=>clip.view==='SIDE' &&
    clip.equipment.every(({sprites})=>{
      const pedals=sprites.filter(([asset])=>asset==='pedal');
      return pedals.length===2 && pedals.every(item=>item[5]===0);
    }));
  const sourceLegs=clips.map((clip,i)=>verifiedLegTrajectories.has(clip.exerciseId) && audits[i].status==='traveling-feet' &&
    !gaitTracks[i] && !jumpClips[i] && !pedalClips[i] &&
    (clip.gear ?? []).every(([gear])=>freeLegGear.has(gear)));
  const reviewedGroundMotions=clips.map(reviewedGroundMotion);
  // Fixed-level stances checked against the technique references in the audit.
  const supportedSquats=clips.map(clip=>{
    if (!/^(?:chair(?:_(?:pause|slow))?|gym-(?:box-bb23|smith-squat23)(?:-(?:pause|tempo)23)?|gym-belt30(?:-(?:hold|ecc)30)?|home-(?:cyclistdb|squatheels)30(?:-(?:hold|ecc)30)?|gym-(?:hack23(?:-(?:pause|tempo)23)?|(?:vsquat|pendulum)30(?:-(?:hold|ecc)30)?)|new46-zercher)$/.test(clip.exerciseId ?? '') ||
        clip.posture!=='UPRIGHT' || !['SIDE','FRONT'].includes(clip.view)) return null;
    const points=[8,10].map(index=>clip.frames[0][index]);
    return clip.frames.every(frame=>points.every(([x,y],i)=>Math.hypot(x-frame[8+2*i][0],y-frame[8+2*i][1])*clip.scale<.004))
      ? points : null;
  });
  const shrimpSquats=clips.map(clip=>clip.view==='SIDE' && clip.posture==='UPRIGHT' &&
    /^home-shrimp23(?:-(?:pause|tempo)23)?$/.test(clip.exerciseId ?? '') &&
    clip.frames.every(frame=>Math.hypot(frame[8][0]-clip.frames[0][8][0],
      frame[8][1]-clip.frames[0][8][1])*clip.scale<.004));
  const inclineProps=clips.map(clip=>/^incline(?:_(?:pause|slow))?$/.test(clip.exerciseId ?? '') &&
    clip.view==='SIDE' ? clip.props.find(prop=>prop[0]==='table') : null);
  const secondBatch=clips.map(clip=>/^(row|bentrow|prone|reverse|curl|hammer|deadbug|dbcalf)(?:_(?:slow|pause))?$/.exec(clip.exerciseId ?? '')?.[1]);
  const thirdBatchIds=/^(?:bird|kickback|cat|sideleg|clam|benchpress|inclinebench|squeezepress|pullover|kneepush|sumo|dbsumo|lateralunge|sumodead|lateral|reversefly|wallslide|singlehinge|singlecalf|ankle|towelrow|wallsit|singlebridge|dbbridge)(?:_(?:slow|pause|short|easy))?$/;
  const footCalibration={};
  if (clips.some(clip=>clip.planted) || audits.some(audit=>audit.status==='anchors') || gaitTracks.some(Boolean) || jumpClips.some(Boolean) || pedalClips.some(Boolean) || sourceLegs.some(Boolean) || reviewedGroundMotions.some(Boolean) || supportedSquats.some(Boolean) || clips.some(reversePlankHands) || shrimpSquats.some(Boolean) || inclineProps.some(Boolean) || secondBatch.some(Boolean) || clips.some(clip=>thirdBatchIds.test(clip.exerciseId) || /^dblunge(?:_(?:slow|pause))?$/.test(clip.exerciseId ?? ''))) {
    const variablePitch=clips.some(clip=>['calf','reverse-lunge','split-squat'].includes(clip.planted) || reversePlankHands(clip) || /^(?:dbcalf|dblunge|singlecalf|ankle|bird|cat|kickback|closepush|lateralunge)(?:_(?:pause|slow|short|easy))?$/.test(clip.exerciseId ?? ''));
    const toeRotation=rotation().setFromAxisAngle(new THREE.Vector3(1,0,0),.70);
    for (const [i,limb] of lengths.entries()) {
      const side=i===0 ? 'L' : 'R';
      const relative=vec();
      let flat=Infinity,toe=Infinity;
      const sole=[];
      athlete.traverse(mesh=>{
        if (!mesh.isMesh) return;
        let ancestor=mesh;
        while (ancestor && !ancestor.name.startsWith('Zapatilla_'+side)) ancestor=ancestor.parent;
        if (!ancestor) return;
        const count=mesh.geometry.attributes.position.count;
        for (let v=0;v<count;v++) {
          mesh.getVertexPosition(v,relative);
          relative.applyMatrix4(mesh.matrixWorld).sub(limb.foot.position);
          if (variablePitch) sole.push([relative.y,relative.z]);
          flat=Math.min(flat,relative.y);
          toe=Math.min(toe,relative.applyQuaternion(toeRotation).y);
        }
      });
      footCalibration[i===0 ? 1 : -1]={
        flat:Number.isFinite(flat) ? -.001-flat : .105,
        toe:Number.isFinite(toe) ? -.001-toe : .178,
        length:limb.shin.localPosition.length()+limb.foot.localPosition.length(),
        upper:limb.shin.localPosition.length(),
        lower:limb.foot.localPosition.length(),
        contact(pitch) {
          const c=Math.cos(pitch),s=Math.sin(pitch);
          let floor=Infinity;
          for (const [y,z] of sole) floor=Math.min(floor,y*c-z*s);
          // Rotate about the ball of the foot in the sagittal plane.
          const pivotY=-.105, pivotZ=.128;
          return [Number.isFinite(floor) ? -.001-floor : .105+.12*s,
            pivotZ-(pivotY*s+pivotZ*c)];
        }
      };
    }
  }
  function setWorld(bone, position, orientation) {
    const parent = bone.bone.parent;
    if (position) bone.bone.position.copy(parent.worldToLocal(position.clone()));
    const inverse = parent.getWorldQuaternion(rotation()).invert();
    bone.bone.quaternion.copy(inverse.multiply(orientation));
    bone.bone.updateMatrixWorld(true);
  }
  function targetRotation(bone,to,from,axis) {
    if (to.lengthSq() < 1e-7) return bone.rotation.clone();
    to.normalize();
    // Rotate in the source-view plane before adding the out-of-plane correction.
    // The hinge axis stays stable when the limb passes through its antipodal pose.
    const a = from.clone().addScaledVector(axis,-from.dot(axis));
    const b = to.clone().addScaledVector(axis,-to.dot(axis));
    if (a.lengthSq()<1e-7 || b.lengthSq()<1e-7) {
      return rotation().setFromUnitVectors(from,to).multiply(bone.rotation);
    }
    a.normalize();
    b.normalize();
    const angle=Math.atan2(axis.dot(a.clone().cross(b)),a.dot(b));
    const plane=rotation().setFromAxisAngle(axis,angle);
    const correction=rotation().setFromUnitVectors(from.clone().applyQuaternion(plane),to);
    return correction.multiply(plane).multiply(bone.rotation);
  }
  function boneAim(bone,direction,axis) {
    const restDirection=up.clone().applyQuaternion(bone.rotation);
    setWorld(bone,null,targetRotation(bone,direction,restDirection,axis));
  }

  function kneePlaneRotation(bone,direction,normal) {
    const restDirection=up.clone().applyQuaternion(bone.rotation);
    const aimed=targetRotation(bone,direction,restDirection,transverse);
    const aligned=transverse.clone().applyQuaternion(bone.rotation.clone().invert()).applyQuaternion(aimed);
    aligned.addScaledVector(direction,-aligned.dot(direction)).normalize();
    const twist=Math.atan2(direction.dot(aligned.clone().cross(normal)),aligned.dot(normal));
    return rotation().setFromAxisAngle(direction,twist).multiply(aimed);
  }

  function solveLimb(hip,target,kneeHint,upper,lower) {
    const axis=target.clone().sub(hip);
    const distance=axis.length();
    if (distance<1e-5) return null;
    axis.divideScalar(distance);
    const reach=THREE.MathUtils.clamp(distance,Math.abs(upper-lower)+.002,upper+lower-.002);
    const along=(upper*upper-lower*lower+reach*reach)/(2*reach);
    const pole=kneeHint.clone().sub(hip);
    pole.addScaledVector(axis,-pole.dot(axis));
    if (pole.lengthSq()<1e-6) {
      pole.copy(forward).addScaledVector(axis,-forward.dot(axis));
      if (pole.lengthSq()<1e-6) pole.copy(up).addScaledVector(axis,-up.dot(axis));
    }
    const knee=hip.clone().addScaledVector(axis,along)
      .addScaledVector(pole.normalize(),Math.sqrt(Math.max(0,upper*upper-along*along)));
    return [knee.clone().sub(hip),hip.clone().addScaledVector(axis,reach).sub(knee)];
  }

  const strapGeometry = new THREE.CylinderGeometry(.009,.009,1,8);
  const ringGeometry = new THREE.TorusGeometry(.083,.012,12,36);
  const ringMaterial = new THREE.MeshStandardMaterial({color:'#c09663',roughness:.72});
  const strapMaterial = new THREE.MeshStandardMaterial({color:'#c4a27e',roughness:.85});
  const supportMaterial = new THREE.MeshStandardMaterial({color:'#45606b',roughness:.85});
  const markMaterial = new THREE.MeshStandardMaterial({color:'#8bbeb4',roughness:.82});
  const textures = new Map();
  const textureLoader = new THREE.TextureLoader();
  const names = new Set(clips.flatMap(clip =>
    clip.equipment.flatMap(frame => frame.sprites.map(sprite => sprite[0]))));
  await Promise.all([...names].map(async name => {
    if (!/^[a-z0-9-]+$/.test(name)) return;
    try {
      const texture = await textureLoader.loadAsync('./equipment/'+name+'.svg');
      texture.colorSpace = THREE.SRGBColorSpace;
      textures.set(name,texture);
    } catch {
      // Some older equipment assets are absent.
    }
  }));
  const equipment = new THREE.Group();
  scene.add(equipment);
  function contactShadows(group) {
    const canvas=document.createElement('canvas');
    canvas.width=canvas.height=64;
    const ctx=canvas.getContext('2d');
    const radial=ctx.createRadialGradient(32,32,3,32,32,32);
    radial.addColorStop(0,'rgba(0,8,13,.66)');
    radial.addColorStop(.45,'rgba(0,8,13,.42)');
    radial.addColorStop(1,'rgba(0,8,13,0)');
    ctx.fillStyle=radial;
    ctx.fillRect(0,0,64,64);
    const material=new THREE.MeshBasicMaterial({map:new THREE.CanvasTexture(canvas),
      transparent:true,depthWrite:false});
    return [0,1,2,3].map(index=>{
      const shoe=index<2;
      const shadow=new THREE.Mesh(new THREE.PlaneGeometry(shoe ? .24 : .16,shoe ? .39 : .22),material);
      shadow.rotation.x=-Math.PI/2;
      shadow.visible=false;
      shadow.renderOrder=4;
      group.add(shadow);
      return shadow;
    });
  }
  const projection = clips.map((clip,clipIndex) => {
    const side = clip.view === 'SIDE';
    const overhead = clip.view === 'OVERHEAD';
    const sign = clip.facing === 'left' ? -1 : 1;
    const batch=secondBatch[clipIndex];
    const proneRaise=batch==='prone' || batch==='reverse';
    const curl=batch==='curl' || batch==='hammer';
    const row=batch==='row' || batch==='bentrow';
    const sourceWrist=!batch && verifiedWristTrajectories.has(clip.exerciseId) && clip.posture==='UPRIGHT' && !overhead && !clip.planted &&
      !clip.suspended && !clip.handSupport && clip.props.every(([kind])=>kind==='mat') &&
      (clip.gear ?? []).every(([gear])=>freeArmGear.has(gear));
    const all = clip.frames.flat();
    const x0 = (Math.min(...all.map(p=>p[0]))+Math.max(...all.map(p=>p[0])))/2;
    const ground = Math.max(...clip.frames.flatMap(p=>[p[4][1],p[6][1],p[8][1],p[10][1]]));
    const jump=jumpClips[clipIndex];
    const jumpFloor=jump ? Math.max(...clip.frames.flatMap(p=>[p[8][1],p[10][1]])) : ground;
    const y0 = overhead ? (Math.min(...all.map(p=>p[1]))+Math.max(...all.map(p=>p[1])))/2 : jumpFloor;
    const clearance = clip.suspended ? .28 : 0;
    const floorTouch = clip.posture === 'SUPINE' || clip.posture === 'SIDE_LYING';
    const map = ([x,y]) => {
      const dx = (x-x0)*clip.scale;
      const dy = (y0-y)*clip.scale;
      if (overhead) return new THREE.Vector3(dx,.24,-dy);
      if (side) return new THREE.Vector3(0,dy+.085+clearance,dx*sign);
      return new THREE.Vector3(dx*(clip.view==='REAR' ? -1 : 1),dy+.085+clearance,0);
    };
    const firstMapsPositive = (a,b) =>
      side || clip.frames.reduce((total,p) => total+map(p[a]).x-map(p[b]).x,0) >= 0;
    const audit=audits[clipIndex];
    const gait=gaitTracks[clipIndex];
    let locked=null;
    const squatSupport=supportedSquats[clipIndex] ?? (row ? [clip.frames[0][8],clip.frames[0][10]] : null);
    const wedge=squatSupport && side && /^home-(?:cyclistdb|squatheels)30/.test(clip.exerciseId);
    const wedgePitch=wedge ? .29 : 0;
    const wedgeDepth=.42;
    const wedgeHeight=.022+Math.tan(wedgePitch)*wedgeDepth/2;
    const wedgeCenter=wedge ? (map(squatSupport[0]).z+map(squatSupport[1]).z)/2+.082 : 0;
    const platform=squatSupport && /^gym-(?:hack23|vsquat30|pendulum30)/.test(clip.exerciseId)
      ? clip.equipment[0].sprites.find(item=>item[0]==='platform' && item[5]===0)
      : null;
    const platformCenter=platform ? map(platform.slice(1,3)) : null;
    const platformTop=platform ? platformCenter.y+platform[4]*clip.scale*platformSurfaceHeight : 0;
    if ((audit.status==='anchors' || squatSupport) && !thirdBatchIds.test(clip.exerciseId)) {
      const neutral=(audit.neutral ?? clip.frames[0]).map(map);
      const firstLegPositive=firstMapsPositive(7,9);
      const feet=lengths.map((limb,i)=>{
        const sideSign=i===0 ? 1 : -1;
        const nativeIndex=(i===0)===firstLegPositive ? 0 : 1;
        const target=map((squatSupport ?? audit.points)[nativeIndex]);
        if (side) target.x=sideSign*.157;
        target.y=footCalibration[sideSign].flat;
        if (platform) {
          target.z=platformCenter.z-.078;
          target.y+=platformTop;
        }
        if (wedge) {
          target.x=sideSign*(clip.exerciseId.startsWith('home-cyclist') ? .104 : .157);
          target.z=wedgeCenter-.082;
          target.y=wedgeHeight+(.082*Math.tan(wedgePitch))+target.y/Math.cos(wedgePitch);
        }
        return {target,knee:nativeIndex===0 ? 7 : 9};
      });
      // Match the reference pose to the rig's limb lengths, then apply the same
      // height offset throughout the repetition so the authored timing stays intact.
      const neutralY=feet.map(({target,knee},i)=>{
        const thigh=neutral[knee].clone().sub(neutral[2]);
        if (!side) thigh.x-=(i===0 ? 1 : -1)*.157;
        const shin=neutral[knee+1].clone().sub(neutral[knee]);
        return target.y-thigh.normalize().y*lengths[i].shin.localPosition.length()
          -shin.normalize().y*lengths[i].foot.localPosition.length();
      });
      const externalGear=(clip.gear ?? []).some(([gear,mount])=>
        gear!=='DUMBBELL' || !['BOTH','NEAR','FAR'].includes(mount));
      const heightOffset=externalGear || squatSupport ? 0
        : THREE.MathUtils.clamp((neutralY[0]+neutralY[1])/2-neutral[2].y,-.15,.30);
      locked={feet,offset:heightOffset+(row ? .10 : wedge ? .053 : platformTop)};
    }
    const shrimpFoot=shrimpSquats[clipIndex]
      ? map(clip.frames[0][8]).setX(.157).setY(footCalibration[1].flat) : null;
    const inclineProp=inclineProps[clipIndex];
    let incline=null;
    if (inclineProp) {
      const [,left,top]=inclineProp;
      const edge=map([left,top]);
      const heights=clip.frames.map(frame=>frame[1][1]);
      const low=Math.min(...heights), range=Math.max(1,Math.max(...heights)-low);
      incline=source=>{
        const descent=(source[1][1]-low)/range;
        const angle=.95-.19*descent;
        const direction=new THREE.Vector3(0,Math.sin(angle),Math.cos(angle));
        const toeZ=edge.z-1.08;
        const ankleY=(footCalibration[1].toe+footCalibration[-1].toe)/2;
        const extension=Math.min(footCalibration[1].length,footCalibration[-1].length)-.004;
        return {
          hip:new THREE.Vector3(0,ankleY,toeZ).addScaledVector(direction,extension),
          trunk:direction.clone().multiplyScalar(.417),
          foot:side=>new THREE.Vector3(side*.157,footCalibration[side].toe,toeZ),
          knee:side=>new THREE.Vector3(side*.157,ankleY,toeZ).addScaledVector(direction,.42).addScaledVector(up,-.12),
          wrist:side=>new THREE.Vector3(side*.229,edge.y+.048,edge.z+.032),
          elbow:side=>new THREE.Vector3(side*.255,edge.y+.14,edge.z-.35),
          toePitch:.70
        };
      };
    }
    const group = new THREE.Group();
    const reviewedGround=reviewedGroundMotions[clipIndex];
    const reversePlank=reversePlankHands(clip);
    let reverseLift=null;
    if (reversePlank && clip.exerciseId.startsWith('home-reverse-plank-lift')) {
      const heights=clip.frames.map(frame=>frame[2][1]);
      const top=Math.min(...heights);
      reverseLift={top,range:Math.max(1,Math.max(...heights)-top)};
    }
    const thirdBatch=thirdBatchIds.test(clip.exerciseId) ? thirdBatchMotion(clip,map,footCalibration) : null;
    const wristAnchors=reviewedGround || thirdBatch ? null : floorHands(clip);
    const rowTable=batch==='row' ? clip.props.find(prop=>prop[0]==='table') : null;
    const rowWrist=rowTable ? map([clip.frames[0][6][0],rowTable[2]]).setX(-.215) : null;
    if (rowWrist) rowWrist.y+=.048;
    const raiseRange=proneRaise ? Math.max(1,...clip.frames.map(frame=>Math.abs(frame[4][1]-clip.frames[0][4][1]))) : 1;
    group.visible = false;
    equipment.add(group);
    if (wedge) {
      const slope=new THREE.Shape();
      const half=wedgeDepth/2;
      slope.moveTo(-half,0);
      slope.lineTo(half,0);
      slope.lineTo(half,.022);
      slope.lineTo(-half,wedgeHeight+Math.tan(wedgePitch)*half);
      slope.closePath();
      const board=new THREE.Mesh(new THREE.ExtrudeGeometry(slope,{depth:.56,bevelEnabled:false}),supportMaterial);
      board.rotation.y=-Math.PI/2;
      board.position.set(.28,0,wedgeCenter);
      board.castShadow=board.receiveShadow=true;
      group.add(board);
    }
    for (const [kind,left,top,right,bottom] of clip.props) {
      if (right <= left || bottom <= top || (wedge && kind==='step')) continue;
      const a = map([left,top]), b = map([right,bottom]);
      const span = Math.abs(right-left)*clip.scale;
      const height = overhead ? .025 : Math.max(.035,Math.abs(a.y-b.y));
      const mat = kind === 'mat';
      const width = side ? .58 : span;
      const depth = side ? span : overhead ? Math.max(.1,Math.abs(a.z-b.z)) : .55;
      const center = a.clone().lerp(b,.5);
      function supportBox(w,h,d,position,material) {
        const box = new THREE.Mesh(new THREE.BoxGeometry(w,h,d),material);
        box.position.copy(position);
        box.castShadow = true;
        box.receiveShadow = true;
        group.add(box);
      }
      if (kind === 'bench' && !overhead && height > .11) {
        // The measured top is where the athlete makes contact with the pad.
        const top = Math.max(a.y,b.y);
        const padHeight = Math.min(.08,height*.24);
        const pad = center.clone();
        pad.y = top-padHeight/2;
        supportBox(width,padHeight,depth,pad,supportMaterial);
        const legHeight = height-padHeight;
        for (const end of [-1,1]) {
          const leg = center.clone();
          leg.y = top-padHeight-legHeight/2;
          if (side) leg.z += end*depth*.33;
          else leg.x += end*width*.33;
          supportBox(side ? width*.76 : .065,legHeight,side ? .065 : depth*.76,leg,markMaterial);
        }
      } else if (kind==='wall' && clip.exerciseId?.startsWith('wallslide')) {
        center.z=-.17;
        supportBox(width,height,.055,center,supportMaterial);
      } else if (kind==='incline' && thirdBatch) {
        // The seat is level; the back pad rises in the opposite direction.
        const seat=map([clip.frames[0][2][0],top]);
        seat.y=a.y-.25;
        supportBox(.55,.065,.26,seat,supportMaterial);
        const back=seat.clone().add(new THREE.Vector3(0,.119,-.207));
        supportBox(.52,.065,.62,back,supportMaterial);
        group.children.at(-1).rotation.x=.52;
        const post=seat.clone().setY(Math.max(.08,seat.y*.47));
        supportBox(.16,Math.max(.14,seat.y*.93),.33,post,markMaterial);
      } else {
        if (mat || overhead) center.y=.012;
        supportBox(width,mat ? .022 : height,depth,center,mat ? markMaterial : supportMaterial);
      }
    }
    const rigGear = clip.gear ?? [];
    const machine = rigGear.find(([gear])=>cardioGear.has(gear))?.[0];
    if (machine) group.add(buildCardioChassis(machine,clip,map));
    const guided=guidedMachine(rigGear);
    const firstBar=clip.equipment[0].sprites.find(([asset])=>asset==='smith'||asset==='barbell');
    const smithAxis=guided.smith && firstBar ? map([firstBar[1],firstBar[2]]) : null;
    const tower=rigGear.some(([gear])=>gear==='CABLE'||gear==='SKI_ERG');
    const freeBar=rigGear.some(([name])=>name==='BARBELL') &&
      !rigGear.some(([name])=>name==='LANDMINE'||name==='SMITH'||name==='GUIDED_SQUAT');
    const freeWeights=new Map(rigGear.flatMap(([name,mount])=>
      name==='PLATE' ? [['plate',mount]] : name==='KETTLEBELL' ? [['kettlebell',mount]] : []));
    const sharedDumbbell=thirdBatch && rigGear.find(([gear,mount])=>gear==='DUMBBELL' && ['HIP','SHARED'].includes(mount))?.[1];
    const hasTowel=thirdBatch && rigGear.some(([gear])=>gear==='TOWEL');
    const hasRings = rigGear.some(([gear]) => gear === 'RINGS');
    const dumbbellMount = rigGear.find(([gear,mount]) => gear === 'DUMBBELL'
      && ['BOTH','NEAR','FAR'].includes(mount))?.[1];
    const rings = hasRings ? [0,1].map(i => {
      const wrist = i===0 ? 4 : 6;
      const ring = new THREE.Mesh(ringGeometry,ringMaterial);
      const strap = new THREE.Mesh(strapGeometry,strapMaterial);
      ring.castShadow = true;
      const anchor = map(clip.frames[0][wrist]);
      if (side) anchor.x = i===0 ? .22 : -.22;
      anchor.y = Math.max(...clip.frames.map(frame=>map(frame[wrist]).y))+.70;
      group.add(ring,strap);
      return {ring,strap,anchor};
    }) : [];
    const sprites = [];
    const links = [];
    const spriteCount = Math.max(...clip.equipment.map(frame=>frame.sprites.length));
    const linkCount = Math.max(...clip.equipment.map(frame=>frame.links.length));
    for (let i=0;i<spriteCount;i++) {
      const sprite = new THREE.Mesh(new THREE.PlaneGeometry(1,1),
        new THREE.MeshBasicMaterial({transparent:true,alphaTest:.05,side:THREE.DoubleSide}));
      group.add(sprite);
      sprites.push(sprite);
    }
    for (let i=0;i<linkCount;i++) {
      const link = new THREE.Mesh(new THREE.CylinderGeometry(1,1,1,8),
        new THREE.MeshBasicMaterial({color:'#b5cbd1'}));
      group.add(link);
      links.push(link);
    }
    return {side,overhead,sign,map,group,floorTouch,sprites,links,sourceWrist,wedgePitch,shrimpFoot,batch,proneRaise,curl,row,rowWrist,raiseRange,
      hinge:side ? new THREE.Vector3(1,0,0) : overhead ? up : forward,
      firstArmPositive:firstMapsPositive(3,5),firstLegPositive:firstMapsPositive(7,9),
      rings,dumbbellMount,sharedDumbbell,hasTowel,machine,tower,guided,smithAxis,freeBar,freeWeights,parts:new Map(), thirdBatch, planted:thirdBatch ?? incline ?? plantedMotion(clip,footCalibration,reviewedGround), reviewedGround,
      contact:thirdBatch || /^dblunge(?:_(?:slow|pause))?$/.test(clip.exerciseId ?? '') || clip.planted || batch==='dbcalf' || incline || reviewedGround || locked || gait || jump || wristAnchors ? contactShadows(group) : [],
      locked,gait,jump,incline,reversePlank,reverseLift,sourceLegs:sourceLegs[clipIndex],pedalSupport:machine && pedalClips[clipIndex],grips:gripBindings(clip), floorHands:wristAnchors?.map((p,i)=>p ? (reversePlank ? new THREE.Vector3(0,.047,-.456) : map(p)).setX((i===0)!==reversePlank ? .235 : -.235).setY(.047) : null),
      extent:Math.max(2.1,(Math.max(...all.map(p=>p[0]))-Math.min(...all.map(p=>p[0])))*clip.scale,
        (Math.max(...all.map(p=>p[1]))-Math.min(...all.map(p=>p[1])))*clip.scale),
      centerY: overhead ? .05 : Math.max(.60,map([0,Math.min(...all.map(p=>p[1]))]).y*.53)};

  });

  const gripAxis = new THREE.Vector3();
  const handDown = new THREE.Vector3(0,.045,0);
  const ringFacing = rotation().setFromAxisAngle(up,Math.PI/2);
  function placeStrap(strap,anchor,tip) {
    gripAxis.copy(anchor).sub(tip);
    strap.position.copy(anchor).lerp(tip,.5);
    strap.scale.y=gripAxis.length();
    strap.quaternion.setFromUnitVectors(up,gripAxis.normalize());
  }

  function positionRings(view) {
    view.rings.forEach(({ring,strap,anchor},i) => {
      const hand = lengths[i].hand.bone;
      const palm = hand.localToWorld(handDown.clone());
      const direction = anchor.clone().sub(palm).normalize();
      ring.position.copy(palm).addScaledVector(direction,.081);
      ring.quaternion.setFromUnitVectors(up,direction).multiply(ringFacing);
      placeStrap(strap,anchor,ring.position.clone().addScaledVector(direction,.083));
    });
  }
  function updateEquipment(clip,view,frame) {
    const near=view.firstArmPositive ? 0 : 1;
    const grip=index=>lengths[index].hand.bone.localToWorld(handDown.clone());
    function worldGrip(wrist) {
      return wrist==='both' ? grip(near).lerp(grip(1-near),.5) : grip(wrist==='near' ? near : 1-near);
    }
    for (const part of view.parts.values()) if (part) part.visible=false;
    if (view.sharedDumbbell) {
      if (!view.parts.has('shared-dumbbell')) {
        const weight=strengthWeight('dumbbell');
        view.parts.set('shared-dumbbell',weight);
        view.group.add(weight);
      }
      const weight=view.parts.get('shared-dumbbell');
      const left=grip(0),right=grip(1);
      weight.position.copy(left).lerp(right,.5);
      gripAxis.copy(left).sub(right);
      if (gripAxis.lengthSq()>1e-5) weight.quaternion.setFromUnitVectors(transverse,gripAxis.normalize());
      weight.visible=true;
    }
    if (view.hasTowel) {
      if (!view.parts.has('towel')) {
        const material=new THREE.MeshStandardMaterial({color:'#d8b291',roughness:1});
        const cloth=new THREE.Group();
        for(let i=0;i<2;i++) {
          const strip=new THREE.Mesh(new THREE.BoxGeometry(.106,1,.022),material);
          strip.castShadow=true;
          cloth.add(strip);
        }
        view.parts.set('towel',cloth);
        view.group.add(cloth);
      }
      const cloth=view.parts.get('towel');
      for (const [i,strip] of cloth.children.entries()) {
        const hand=grip(i);
        const anchor=lengths[i].foot.bone.getWorldPosition(vec()).add(new THREE.Vector3(0,-.015,.135));
        strip.position.copy(hand).lerp(anchor,.5);
        gripAxis.copy(anchor).sub(hand);
        strip.scale.y=gripAxis.length();
        strip.quaternion.setFromUnitVectors(up,gripAxis.normalize());
      }
      cloth.visible=true;
    }
    const lanes = new Map();
    view.sprites.forEach((mesh,i) => {
      const item = frame.sprites[i];
      if (item && (view.machine || (view.tower && ['weight-stack','pulley','weight-plates'].includes(item[0])) ||
          (view.guided.guided && isGuidedComponent(item[0])))) {
        const key=i+':'+item[0];
        if (!view.parts.has(key)) {
          const custom=view.guided.guided ? guidedComponent(item[0],view.guided.rack) : null;
          const part=custom ?? cardioPart(item[0]);
          if (custom) part.userData.guided=true;
          if (part) view.group.add(part);
          view.parts.set(key,part);
        }
        const part=view.parts.get(key);
        if (part) {
          const lane=lanes.get(item[0]) ?? 0;
          lanes.set(item[0],lane+1);
          part.visible=true;
          if (part.userData.guided) placeGuidedComponent(part,item,view,clip.scale);
          else placeCardioPart(part,item,view,clip.scale,lane,view.machine);
          mesh.visible=false;
          return;
        }
      }
      const [asset,x,y,width,height,angle,foreground]=item ?? [];
      const weightAllowed=isStrengthWeight(asset) && !view.overhead && (
        asset==='barbell' ? (view.freeBar || view.smithAxis) : view.freeWeights.has(asset)
      );
      if (weightAllowed) {
        const key=i+':'+asset;
        if (!view.parts.has(key)) {
          const weight=strengthWeight(asset);
          view.group.add(weight);
          view.parts.set(key,weight);
        }
        const weight=view.parts.get(key);
        weight.visible=true;
        weight.position.copy(view.map([x,y]));
        if (view.side && asset!=='barbell') weight.position.x=(foreground ? -.16 : .16)*view.sign;
        weight.rotation.set(0,0,0);
        if (asset!=='barbell' && view.side) weight.rotation.y=-view.sign*Math.PI/2;
        if (asset!=='barbell' || !view.side) weight.rotateZ(-THREE.MathUtils.degToRad(angle));
        const nearIndex=view.firstArmPositive ? 0 : 1;
        const grip=index=>lengths[index].hand.bone.localToWorld(handDown.clone());
        if (asset==='barbell') {
          const a=grip(0), b=grip(1), axis=a.clone().sub(b);
          if (view.smithAxis) {
            weight.position.x=view.smithAxis.x;
            weight.position.z=view.smithAxis.z;
          } else if (axis.lengthSq()>.025) {
            weight.position.copy(a).lerp(b,.5);
            weight.quaternion.setFromUnitVectors(new THREE.Vector3(1,0,0),axis.normalize());
          }
          const radius=THREE.MathUtils.clamp(height*clip.scale/.36,.55,1.15);
          weight.scale.set(view.side || view.smithAxis ? 1 : THREE.MathUtils.clamp(width*clip.scale/1.53,.56,1.2),radius,radius);
        } else {
          const mount=view.freeWeights.get(asset);
          const lane=lanes.get(asset) ?? 0;
          lanes.set(asset,lane+1);
          if (mount==='NEAR' || mount==='FAR' || mount==='BOTH' || mount==='SHARED') {
            const index=mount==='FAR' || (mount==='BOTH' && lane===0) ? 1-nearIndex : nearIndex;
            const center=grip(index);
            if (mount==='SHARED') center.lerp(grip(1-index),.5);
            weight.position.copy(center);
            if (asset==='kettlebell') weight.position.y-=.071;
          }
        }
        mesh.visible=false;
        return;
      }
      const duplicate = item && ((item[0]==='ring' && view.rings.length)
        || (item[0]==='towel' && view.hasTowel)
        || (item[0]==='dumbbell' && (view.dumbbellMount || view.sharedDumbbell)));
      const texture = item && !duplicate && textures.get(item[0]);
      mesh.visible=Boolean(texture);
      if (!texture) return;
      if (mesh.material.map !== texture) {
        mesh.material.map=texture;
        mesh.material.needsUpdate=true;
      }
      mesh.position.copy(view.map([x,y]));
      mesh.rotation.set(0,0,0);
      if (view.overhead) {
        mesh.rotation.x=-Math.PI/2;
        mesh.position.y=.08;
      } else if (view.side) {
        mesh.rotation.y=-view.sign*Math.PI/2;
        mesh.position.x=(foreground ? -.14 : .14)*view.sign;
      } else {
        mesh.position.z=foreground ? .15 : -.15;
        if (clip.view==='REAR') mesh.rotation.y=Math.PI;
      }
      mesh.rotateZ(-THREE.MathUtils.degToRad(angle));
      if (asset==='handle' && !view.machine) {
        const bound=view.grips?.handles[i];
        if (bound) mesh.position.copy(worldGrip(bound));
      }
      mesh.scale.set(width*clip.scale,height*clip.scale,1);
    });
    const ringSprites = frame.sprites.filter(item => item[0] === 'ring');
    view.links.forEach((mesh,i) => {
      const link = view.hasTowel ? null : frame.links[i];
      const ringStrap = link && view.rings.length && link[7]==='ROPE' &&
        ringSprites.some(item => Math.hypot(item[1]-link[2],item[2]-link[3])<2);
      mesh.visible=Boolean(link && !ringStrap);
      if (!mesh.visible) return;
      const [x1,y1,x2,y2,width]=link;
      const a=view.map([x1,y1]),b=view.map([x2,y2]);
      if ((view.machine || view.guided.guided) && view.side) {
        const targets=frame.sprites.filter(item=>item[0]==='pedal'||item[0]==='handle');
        const footCount=targets.filter(item=>item[0]==='pedal').length;
        for (const point of targets) {
          const [asset,x,y]=point;
          const ordinal=targets.filter((item,index)=>index<targets.indexOf(point)&&item[0]===asset).length;
          const lateral=(ordinal%2===0 ? 1 : -1)*.17;
          if (Math.hypot(x2-x,y2-y)<3) b.x=lateral;
          if (Math.hypot(x1-x,y1-y)<3) a.x=lateral;
        }
        if (!footCount && view.machine==='SKI_ERG') {
          // Paired cords come from opposite sides of the same head unit.
          a.x=b.x=(i%2===0 ? 1 : -1)*.15;
        }
      }
      const startGrip=view.grips?.links[2*i],endGrip=view.grips?.links[2*i+1];
      if (startGrip) a.copy(worldGrip(startGrip));
      if (endGrip) b.copy(worldGrip(endGrip));
      gripAxis.copy(b).sub(a);
      if (gripAxis.lengthSq()<1e-7) { mesh.visible=false; return; }
      const radius=Math.max(view.guided.guided && link[7]==='STEEL' ? .014 : .003,width*clip.scale/2);
      mesh.scale.set(radius,gripAxis.length(),radius);
      mesh.position.copy(a).lerp(b,.5);
      if (view.overhead && !startGrip && !endGrip) mesh.position.y=.09;
      mesh.quaternion.setFromUnitVectors(up,gripAxis.normalize());
      mesh.material.color.set(link[7]==='BAND' ? '#efb571' : '#b5cbd1');
    });
  }
  const weights = [];
  athlete.traverse(o => {
    if (o.isMesh && /^(Mango|Pesa_hexagonal|Placa|Remache)/.test(o.name)) weights.push({object:o,left:/_L(?:[_.0-9]|$)/.test(o.name)});
  });

  let visible = -1;
  function frame(index, phase, mirror) {
    const clip = clips[index];
    const view = projection[index];
    if (visible !== index) {
      if (visible !== -1) projection[visible].group.visible = false;
      view.group.visible = true;
      visible = index;
      weights.forEach(({object,left}) => {
        object.visible = view.dumbbellMount === 'BOTH' ||
          (left===view.firstArmPositive && view.dumbbellMount === 'NEAR') ||
          (left!==view.firstArmPositive && view.dumbbellMount === 'FAR');
        if (object.visible) object.castShadow=true;
      });
      controls.target.set(0,view.centerY,0);
      if(view.thirdBatch && clip.exerciseId?.startsWith('wallslide')) camera.position.set(-2.45,1.52,2.15);
      else if (view.thirdBatch && /^(?:benchpress|inclinebench|squeezepress|pullover)/.test(clip.exerciseId)) camera.position.set(-2.38,1.65,.96);
      else if (view.thirdBatch && /^(?:clam|sideleg)(?:_|$)/.test(clip.exerciseId)) camera.position.set(2.9,1.40,-1.08);
      else if (view.thirdBatch && /^lateralunge(?:_|$)/.test(clip.exerciseId)) camera.position.set(-1.75,1.54,3.22);
      else if (view.reviewedGround) camera.position.set(-2.45,1.82,1.65);
      else if (view.proneRaise) camera.position.set(-2.6,2.3,1.6);
      else if (view.batch==='deadbug') camera.position.set(-2.85,1.60,1.28);
      else if (view.overhead) camera.position.set(0,3.8,.7);
      else if (view.side) camera.position.set((view.sign === 1 ? -1 : 1)*(mirror ? -3.5 : 3.5),1.48,.06);
      else camera.position.set(0,1.39,clip.view==='REAR' ? -3.6 : 3.6);
      camera.position.sub(controls.target).multiplyScalar(Math.max(1,view.extent/2.5)).add(controls.target);
      controls.update();
    }
    athlete.position.y = 0;
    athlete.scale.x = 1;
    athlete.updateMatrixWorld(true);
    // Insert the step and return around the original descent and hold.
    const edge=approachFraction(clip);
    const motionPhase=THREE.MathUtils.clamp((phase-edge)/(1-2*edge),0,1-1e-7);
    const deadbug=view.batch==='deadbug';
    const secondSide=deadbug && motionPhase>=.5;
    const extension=deadbug ? deadBugExtension(clip,motionPhase) : 0;
    const interval=phaseInterval(clip,deadbug ? 0 : motionPhase);
    const source=samplePose(clip,interval);
    const p = source.map(view.map);
    const planted=view.planted?.(source,phase,mirror);
    const hip=planted ? planted.hip : p[2];
    const trunk=planted ? planted.trunk : p[1].clone().sub(hip);
    if (view.proneRaise) {
      hip.set(0,.126,.025);
      trunk.set(0,0,.417);
    }
    if (deadbug) {
      hip.y=.145;
      trunk.set(0,0,-.417);
    }
    if (trunk.lengthSq() < 1e-7) trunk.copy(up);
    const poseRotation=()=>{
      const pose=targetRotation(torso,trunk,up,view.thirdBatch ? transverse : view.hinge);
      if (planted?.roll) pose.premultiply(rotation().setFromAxisAngle(trunk.clone().normalize(),planted.roll));
      if (!planted && !view.overhead && clip.posture==='SUPINE') {
        const spine=trunk.clone().normalize();
        const roll=view.reversePlank ? 1 : 1-THREE.MathUtils.smoothstep(Math.abs(spine.y),.28,.70);
        pose.premultiply(rotation().setFromAxisAngle(spine,Math.PI*roll));
      }
      return pose;
    };
    if (view.reversePlank) {
      const drop=view.reverseLift
        ? (source[2][1]-view.reverseLift.top)/view.reverseLift.range : 0;
      const heel=(footCalibration[1].contact(reverseHeelPitch)[0]+footCalibration[-1].contact(reverseHeelPitch)[0])/2;
      const legLength=footCalibration[1].length-.025;
      let height=.39;
      // Match shoulder height to straight arms, while the planted heels
      // determine pelvis placement. The lowering phase hinges at the hips.
      for (let pass=0;pass<7;pass++) {
        const shoulderRise=THREE.MathUtils.clamp((height-heel)*.417/legLength+.11*drop,.03,.31);
        trunk.set(0,shoulderRise,-Math.sqrt(.417**2-shoulderRise**2));
        hip.set(0,height,.79-Math.sqrt(legLength**2-(height-heel)**2));
        if (pass===6) break;
        const orientation=poseRotation();
        const limits=lengths.map((limb,i)=>{
          const wrist=view.floorHands[i];
          const offset=limb.arm.position.clone().sub(torso.position).applyQuaternion(orientation);
          const reach=limb.forearm.localPosition.length()+limb.hand.localPosition.length()-.0021;
          const horizontal=Math.hypot(hip.x+offset.x-wrist.x,hip.z+offset.z-wrist.z);
          return wrist.y-offset.y+Math.sqrt(Math.max(0,reach**2-horizontal**2));
        });
        height=THREE.MathUtils.lerp(height,Math.min(...limits),.64);
      }
    }
    const bodyRotation=poseRotation();
    const hipRotation=view.reviewedGround==='archer'
      ? targetRotation(torso,trunk.clone().setX(0),up,view.hinge)
      : bodyRotation;
    if (view.locked) hip.y+=view.locked.offset;
    const gaitAnchors=view.gait?.map((track,i)=>{
      const contact=contactAt(track,interval);
      const side=i===0 ? 1 : -1;
      const target=p[i===0 ? 8 : 10].clone();
      target.x=side*.157;
      target.y=Math.max(0,target.y-.085)+footCalibration[side].flat;
      if (contact) {
        const fixed=view.map(contact.point);
        fixed.x=target.x;
        fixed.y=footCalibration[side].flat;
        target.lerp(fixed,contact.weight);
      }
      return {target,weight:contact?.weight ?? 0};
    });
    const jumpFeet=view.jump ? lengths.map((_,i)=>{
      const side=i===0 ? 1 : -1;
      const ankle=(i===0)===view.firstLegPositive ? 8 : 10;
      const target=p[ankle].clone();
      if (view.side) target.x=side*.157;
      const clearance=Math.max(0,target.y-.085);
      target.y=footCalibration[side].flat+clearance;
      return {target,grounded:clearance<.015};
    }) : null;
    const equipmentFrame=sampleEquipment(clip,interval);
    const pedals=view.pedalSupport ? equipmentFrame.sprites.filter(item=>item[0]==='pedal') : [];
    const pedalFeet=pedals.map((pedal,i)=>{
      const target=cardioPedalSurface(pedal,view,clip.scale,i);
      target.y+=footCalibration[i===0 ? 1 : -1].flat;
      // The pedal sits under the forefoot, ahead of the ankle joint.
      target.z-=.075;
      return target;
    });
    if (view.locked || view.shrimpFoot || gaitAnchors || jumpFeet || pedalFeet.length) {
      for (const [i,limb] of lengths.entries()) {
        const target=view.locked?.feet[i].target ?? gaitAnchors?.[i]?.target ?? jumpFeet?.[i]?.target ?? pedalFeet[i] ?? (i===0 ? view.shrimpFoot : null);
        const weight=target ? 1 : 0;
        if (!target || !weight) continue;
        const from=limb.thigh.position.clone().sub(torso.position).applyQuaternion(bodyRotation);
        const lateral=Math.hypot(hip.x+from.x-target.x,hip.z+from.z-target.z);
        const reach=limb.shin.localPosition.length()+limb.foot.localPosition.length()-.004;
        const limit=target.y-from.y+Math.sqrt(Math.max(0,reach*reach-lateral*lateral));
        hip.y=THREE.MathUtils.lerp(hip.y,Math.min(hip.y,limit),weight);
      }
    }
    if ((view.floorHands && !view.reversePlank) || view.reviewedGround || view.rowWrist || planted?.stabilizeHands) {
      // The arms constrain height, while the fixed toes constrain how far
      // forward the pelvis can sit at that height. Solve both together.
      const iterations=view.reviewedGround ? 5 : 1;
      for (let pass=0;pass<iterations;pass++) {
        if (view.reviewedGround) {
          let limit=Infinity;
          for (const [i,limb] of lengths.entries()) {
            const offset=limb.thigh.position.clone().sub(torso.position).applyQuaternion(hipRotation);
            const toe=planted.foot(i===0 ? 1 : -1);
            const transverse=Math.hypot(hip.x+offset.x-toe.x,hip.y+offset.y-toe.y);
            const reach=limb.shin.localPosition.length()+limb.foot.localPosition.length()-.0022;
            limit=Math.min(limit,toe.z-offset.z+Math.sqrt(Math.max(0,reach**2-transverse**2)));
          }
          hip.z=limit;
        }
        for (const [i,limb] of lengths.entries()) {
          if (planted?.stabilizeHands && planted.armContact && !planted.armContact(i===0 ? 1 : -1)) continue;
          const target=view.floorHands?.[i] ?? planted?.wrist?.(i===0 ? 1 : -1) ?? (i===1 ? view.rowWrist : null);
          if (!target) continue;
          const shoulder=limb.arm.position.clone().sub(torso.position).applyQuaternion(bodyRotation);
          const lateral=Math.hypot(hip.x+shoulder.x-target.x,hip.z+shoulder.z-target.z);
          const reach=limb.forearm.localPosition.length()+limb.hand.localPosition.length()-.004;
          hip.y=Math.min(hip.y,target.y-shoulder.y+Math.sqrt(Math.max(0,reach*reach-lateral*lateral)));
        }
      }
    }
    setWorld(torso,hip,bodyRotation);
    const headDirection = planted ? trunk.clone().addScaledVector(up,.055) : p[0].clone().sub(p[1]);
    const bindHeadUp = up.clone().applyQuaternion(head.rotation);
    const headOrientation = targetRotation(head,headDirection,bindHeadUp,view.hinge);
    if (deadbug) {
      // Keep the bind-pose face (+Z) aimed up while the head rests on the floor.
      headOrientation.copy(rotation().setFromAxisAngle(transverse,-Math.PI/2)).multiply(head.rotation);
    } else if (view.reviewedGround || view.proneRaise || planted?.stillHead) {
      headOrientation.copy(bodyRotation).multiply(torso.rotation.clone().invert()).multiply(head.rotation);
    }
    setWorld(head,null,headOrientation);
    let contactWeight=0;
    function support(bone,sourceY,sole,minimum) {
      const weight=1-THREE.MathUtils.smoothstep(sourceY,minimum,minimum+.08);
      if (!weight) return;
      const correction=sole-bone.getWorldPosition(vec()).y;
      contactWeight=Math.max(contactWeight,weight);
      supportCorrections.push([correction,weight]);
    }
    const supportCorrections=[];
    lengths.forEach((limb,index) => {
      const sideOffset = index===0 ? 1 : -1;
      const arm = (index===0)===view.firstArmPositive ? 3 : 5;
      const leg = (index===0)===view.firstLegPositive ? 7 : 9;
      const deadbugArm=deadbug && index===(secondSide ? 1 : 0);
      const deadbugLeg=deadbug && index===(secondSide ? 0 : 1);
      let upperArm = p[arm].clone().sub(p[1]);
      let forearm = p[arm+1].clone().sub(p[arm]);
      if (!view.side) {
        // The source shoulder landmark is shared by both arms.
        upperArm.x -= sideOffset*.195;
      } else upperArm.x += sideOffset*.028;
      const handAnchor=view.floorHands?.[index] ?? (index===1 ? view.rowWrist : null);
      if (handAnchor) {
        const shoulder=limb.arm.bone.getWorldPosition(vec());
        const elbow=p[arm].clone();
        if (view.rowWrist) elbow.x=shoulder.x;
        const solved=solveLimb(shoulder,handAnchor,elbow,
          limb.forearm.localPosition.length(),limb.hand.localPosition.length());
        if (solved) [upperArm,forearm]=solved;
      } else if (planted?.arm) {
        [upperArm,forearm]=planted.arm(sideOffset);
      } else if (planted?.armsHanging) {
        upperArm.set(sideOffset*.07,-1,0);
        forearm.set(sideOffset*.025,-1,.087);
      } else if (planted?.armsAlongFloor) {
        upperArm.set(sideOffset*.06,-.20,1);
        forearm.set(0,-.16,1);
      } else if (planted?.forearmSupport) {
        upperArm.set(0,-1,0);
        forearm.copy(forward);
      } else if (planted?.wrist) {
        const shoulder=limb.arm.bone.getWorldPosition(vec());
        const solved=solveLimb(shoulder,planted.wrist(sideOffset),planted.elbow(sideOffset),
          limb.forearm.localPosition.length(),limb.hand.localPosition.length());
        if (solved) [upperArm,forearm]=solved;
      }
      if (deadbug) {
        const angle=deadbugArm ? 1.43*extension : 0;
        upperArm.set(0,Math.cos(angle),-Math.sin(angle));
        forearm.copy(upperArm);
        if (deadbugArm) forearm.y+=.045*extension;
      } else if (view.curl) {
        upperArm.set(sideOffset*.04,-1,.04);
        const turn=Math.atan2(Math.abs(source[arm+1][0]-source[arm][0]),source[arm+1][1]-source[arm][1]);
        forearm.set(0,-Math.cos(turn),Math.sin(turn));
      } else if (view.row && !handAnchor) {
        upperArm.set(0,p[arm].y-p[1].y,p[arm].z-p[1].z);
        forearm.set(0,p[arm+1].y-p[arm].y,p[arm+1].z-p[arm].z);
      } else if (view.proneRaise) {
        const height=THREE.MathUtils.clamp(Math.abs(source[4][1]-clip.frames[0][4][1])/view.raiseRange,0,1);
        upperArm.set(sideOffset*Math.abs(source[arm][0]-source[1][0])*clip.scale,.01+.13*height,
          (source[1][1]-source[arm][1])*clip.scale);
        forearm.set(sideOffset*Math.abs(source[arm+1][0]-source[arm][0])*clip.scale,.005+.07*height,
          (source[arm][1]-source[arm+1][1])*clip.scale);
      }
      if (view.sourceWrist && !view.thirdBatch) {
        const shoulder=limb.arm.bone.getWorldPosition(vec());
        const elbow=p[arm].clone();
        const wrist=p[arm+1].clone();
        if (view.side) {
          // A side view does not encode depth. Keep each arm in its own lane.
          elbow.x=shoulder.x;
          wrist.x=shoulder.x;
        }
        const solved=solveLimb(shoulder,wrist,elbow,
          limb.forearm.localPosition.length(),limb.hand.localPosition.length());
        if (solved) [upperArm,forearm]=solved;
      }
      boneAim(limb.arm,upperArm,view.hinge);
      boneAim(limb.forearm,forearm,view.hinge);
      const handAim=handAnchor ? new THREE.Vector3(0,0,trunk.z<0 && !view.reversePlank ? -1 : 1)
        : (planted?.wrist && !view.thirdBatch || planted?.forearmSupport || planted?.armContact?.(sideOffset)) ? forward.clone() : forearm.clone();
      boneAim(limb.hand,handAim,view.hinge);
      if (view.batch==='hammer') {
        const axis=forearm.clone().normalize();
        setWorld(limb.hand,null,rotation().setFromAxisAngle(axis,sideOffset*Math.PI/2)
          .multiply(limb.hand.bone.getWorldQuaternion(rotation())));
      }
      const joint = limb.thigh.position.clone().sub(torso.position).applyQuaternion(hipRotation).add(hip);
      let upperLeg = p[leg].clone().sub(hip);
      let shin = p[leg+1].clone().sub(p[leg]);
      const pedal = pedalFeet[index];
      const authoredLeg=planted?.leg?.(sideOffset,joint);
      if (deadbug) {
        const angle=deadbugLeg ? 1.39*extension : 0;
        upperLeg.set(0,Math.cos(angle),Math.sin(angle));
        // Unfold the knee progressively as the thigh lowers.
        shin.set(0,deadbugLeg ? .047*extension : 0,1);
      } else if (view.proneRaise) {
        upperLeg.set(0,-.027,-1);
        shin.set(0,0,-1);
      } else if (authoredLeg) {
        [upperLeg,shin]=authoredLeg;
      } else if (planted) {
        const foot=planted.foot(sideOffset);
        const knee=view.reviewedGround
          ? joint.clone().lerp(foot,.5).add(new THREE.Vector3(0,-.1,0))
          : planted.knee(sideOffset,joint,foot);
        const solved=solveLimb(joint,foot,knee,
          limb.shin.localPosition.length(),limb.foot.localPosition.length());
        if (solved) [upperLeg,shin]=solved;
      } else if (view.locked) {
        const anchor=view.locked.feet[index];
        const hint=p[anchor.knee].clone();
        if (view.side && (view.row || /^(?:chair|goblet|hinge|wall)(?:_(?:pause|slow))?$/.test(clip.exerciseId ?? ''))) hint.x=joint.x;
        const solved=solveLimb(joint,anchor.target,hint,
          limb.shin.localPosition.length(),limb.foot.localPosition.length());
        if (solved) [upperLeg,shin]=solved;
      } else if (gaitAnchors || jumpFeet) {
        // The frontal source has no depth. Use a forward knee pole so the
        // bend does not flip when the foot moves across the camera plane.
        const kneeHint=jumpFeet && !view.side ? p[leg].clone().addScaledVector(forward,.20) : p[leg];
        const solved=solveLimb(joint,(gaitAnchors ?? jumpFeet)[index].target,kneeHint,
          limb.shin.localPosition.length(),limb.foot.localPosition.length());
        if (solved) [upperLeg,shin]=solved;
      } else if (view.shrimpFoot) {
        const ankle=index===0 ? view.shrimpFoot : p[leg+1].clone().setX(-.157)
          .setY(Math.max(footCalibration[-1].flat+.014,p[leg+1].y));
        const solved=solveLimb(joint,ankle,p[leg],
          limb.shin.localPosition.length(),limb.foot.localPosition.length());
        if (solved) [upperLeg,shin]=solved;
      } else if (view.reversePlank) {
        const start=clip.frames[0][leg+1];
        const ankle=p[leg+1].clone();
        ankle.x=-sideOffset*.154;
        ankle.z=.79+(source[leg+1][0]-start[0])*clip.scale;
        ankle.y=footCalibration[sideOffset].contact(reverseHeelPitch)[0]+Math.max(0,(start[1]-source[leg+1][1])*clip.scale);
        const solved=solveLimb(joint,ankle,joint.clone().lerp(ankle,.5).addScaledVector(up,.20),
          limb.shin.localPosition.length(),limb.foot.localPosition.length());
        if (solved) [upperLeg,shin]=solved;
      } else if (pedal) {
        const solved = solveLimb(joint,pedal,p[leg],
          limb.shin.localPosition.length(),limb.foot.localPosition.length());
        if (solved) [upperLeg,shin] = solved;
      }
      if (!view.side && !view.locked && !jumpFeet && !view.reviewedGround && !view.proneRaise && !view.thirdBatch) upperLeg.x -= sideOffset*.152;
      if (upperLeg.lengthSq() < 1e-7) upperLeg.set(0,-1,0);
      const restDirection = up.clone().applyQuaternion(limb.thigh.rotation);
      const legHinge=(view.reviewedGround || view.proneRaise || view.thirdBatch) ? transverse : view.hinge;
      const kneePlane=planted?.alignKneePlane ? shin.clone().cross(upperLeg) : null;
      if (kneePlane && kneePlane.lengthSq()>1e-8) {
        kneePlane.normalize();
        if (kneePlane.x<0) kneePlane.negate();
        setWorld(limb.thigh,joint,kneePlaneRotation(limb.thigh,upperLeg,kneePlane));
        setWorld(limb.shin,null,kneePlaneRotation(limb.shin,shin,kneePlane));
      } else {
        setWorld(limb.thigh,joint,targetRotation(limb.thigh,upperLeg,restDirection,legHinge));
        boneAim(limb.shin,shin,legHinge);
      }
      const shoe = limb.foot.rotation.clone();
      const toePitch=planted?.shoePitch?.(sideOffset) ?? (view.wedgePitch || (view.reversePlank ? reverseHeelPitch : typeof planted?.toePitch==='function' ? planted.toePitch(sideOffset) : planted?.toePitch));
      if (deadbug) {
        const anklePitch=deadbugLeg ? THREE.MathUtils.lerp(-.58,-.19,extension) : -.58;
        shoe.premultiply(rotation().setFromAxisAngle(transverse,anklePitch));
      } else if (view.proneRaise) {
        shoe.premultiply(rotation().setFromAxisAngle(transverse,Math.PI*.45));
      } else if (toePitch) {
        shoe.premultiply(rotation().setFromAxisAngle(new THREE.Vector3(1,0,0),toePitch));
      } else if (!planted && !view.reversePlank && (view.overhead || view.floorTouch)) {
        shoe.premultiply(rotation().setFromUnitVectors(forward,trunk.clone().normalize().negate()));
      }
      if (planted?.shoeRoll) shoe.premultiply(rotation().setFromAxisAngle(forward,planted.shoeRoll));
      if (planted?.shoeYaw) shoe.premultiply(rotation().setFromAxisAngle(up,planted.shoeYaw(sideOffset)));
      if (planted?.shoeLift) shoe.premultiply(rotation().setFromAxisAngle(transverse,planted.shoeLift(sideOffset)));
      setWorld(limb.foot,null,shoe);
      if (!planted && !view.locked && !view.gait && !jumpFeet && !view.floorHands && !view.shrimpFoot && view.batch!=='deadbug' && !pedal && !clip.suspended && !view.overhead)
        support(limb.foot.bone,p[leg+1].y,.10,.10);
      if (!planted && !view.floorHands && !clip.suspended && !view.overhead && clip.handSupport) {
        support(limb.hand.bone,p[arm+1].y,.035,.095);
        support(limb.forearm.bone,p[arm].y,.039,.095);
      }

    });
    if (supportCorrections.length) {
      // Fade out the correction as the support points leave the floor.
      const best=Math.max(...supportCorrections.map(([value,w])=>value*w-.28*(1-w)));
      athlete.position.y = THREE.MathUtils.clamp(best*contactWeight,-.24,.28);
    }
    athlete.updateMatrixWorld(true);
    if (view.sourceLegs) for (const [i,limb] of lengths.entries()) {
      const side=i===0 ? 1 : -1;
      const knee=(i===0)===view.firstLegPositive ? 7 : 9;
      const ankle=limb.foot.bone.getWorldPosition(vec());
      const target=p[knee+1].clone();
      if (view.side) target.x=ankle.x;
      const sole=footCalibration[side].flat;
      target.y=sole+Math.max(0,target.y-.085);
      const shift=target.clone().sub(ankle).clampLength(0,.14);
      target.copy(ankle).add(shift);
      target.y=Math.max(sole,target.y);
      const joint=limb.thigh.bone.getWorldPosition(vec());
      const solved=solveLimb(joint,target,limb.shin.bone.getWorldPosition(vec()),
        limb.shin.localPosition.length(),limb.foot.localPosition.length());
      if (!solved) continue;
      boneAim(limb.thigh,solved[0],view.hinge);
      boneAim(limb.shin,solved[1],view.hinge);
      setWorld(limb.foot,null,limb.foot.rotation.clone());
    }
    if (planted || view.locked || view.gait || jumpFeet) {
      for (const [i,limb] of lengths.entries()) {
        const foot=limb.foot.bone.getWorldPosition(vec());
        const ankleShadow=view.contact[i];
        const side=i===0 ? 1 : -1;
        const toePitch=typeof planted?.toePitch==='function' ? planted.toePitch(side) : planted?.toePitch;
        ankleShadow.visible=view.thirdBatch && !planted?.foot ? false : jumpFeet ? jumpFeet[i].grounded : gaitAnchors ? (gaitAnchors[i]?.weight ?? 0)>.85 : (planted?.contact?.(side) ?? true);
        ankleShadow.position.set(foot.x,.004,foot.z+(toePitch ? .06 : .034));
        const hand=limb.hand.bone.getWorldPosition(vec());
        const palmShadow=view.contact[i+2];
        palmShadow.visible=planted?.armContact?.(side) ?? Boolean(planted?.wrist || planted?.forearmSupport);
        palmShadow.position.set(hand.x,.004,hand.z+(planted?.forearmSupport ? -.07 : .028));
        if (planted?.forearmSupport) palmShadow.scale.y=1.6;
      }
    }
    if (view.floorHands) for (const [i,target] of view.floorHands.entries()) {
      const shadow=view.contact[i+2];
      shadow.visible=Boolean(target);
      if (target) shadow.position.set(target.x,.004,target.z+.027);
    }
    positionRings(view);
    updateEquipment(clip,view,equipmentFrame);
    // Reflection is applied to the whole scene, including the support.
    athlete.scale.x = ((!view.side || view.reviewedGround) && mirror) ? -1 : 1;
    equipment.scale.x = athlete.scale.x;
    athlete.updateMatrixWorld(true);
  }
  return {
    beginMirror() {
      if (visible !== -1) projection[visible].group.visible=false;
      visible = -1;
    },
    draw(seconds,mirror) {
      let index=0;
      while (index<clips.length-1 && seconds>=playbackDuration(clips[index])) seconds-=playbackDuration(clips[index++]);
      frame(index,seconds/playbackDuration(clips[index]),mirror);
    }
  };
}
