import * as THREE from 'three';

const models = new Set(['bench','wheel','fan','console','pedal','handle','platform','stepmill','bar','weight-stack','weight-plates','pulley']);

export const cardioGear = new Set([
  'BIKE','AIR_BIKE','RECUMBENT','ROWER','TREADMILL','ELLIPTICAL','STEP_MILL','SKI_ERG'
]);

const pedalSurfaceHeight=.283;
export const platformSurfaceHeight=.33;

export function cardioPart(asset) {
  if (!models.has(asset)) return null;
  const group = new THREE.Group();
  const dark = new THREE.MeshStandardMaterial({color:'#243741',roughness:.8});
  const metal = new THREE.MeshStandardMaterial({color:'#54717c',metalness:.26,roughness:.72});
  const accent = new THREE.MeshStandardMaterial({color:'#87baab',roughness:.75});
  const screen = new THREE.MeshStandardMaterial({color:'#426d78',roughness:.62});
  function add(geometry,material,x=0,y=0,z=0) {
    const mesh=new THREE.Mesh(geometry,material);
    mesh.position.set(x,y,z);
    group.add(mesh);
    mesh.castShadow=true;
    mesh.receiveShadow=true;
    return mesh;
  }
  function box(w,h,d,material,x=0,y=0,z=0) {
    return add(new THREE.BoxGeometry(w,h,d),material,x,y,z);
  }
  function disk(radius,depth,material) {
    const mesh=add(new THREE.CylinderGeometry(radius,radius,depth,32),material);
    mesh.rotation.x=Math.PI/2;
  }
  switch(asset) {
    case 'wheel':
    case 'fan':
      disk(.47,.22,dark);
      disk(.405,.245,metal);
      disk(.35,.257,dark);
      for(let i=0;i<8;i++) {
        const a=i*Math.PI/4;
        box(.055,.29,.27,metal,Math.sin(a)*.19,Math.cos(a)*.19).rotation.z=-a;
      }
      disk(.098,.31,accent);
      break;
    case 'console':
      box(.91,.75,.23,dark);
      box(.73,.54,.025,screen,0,.046,.13);
      box(.40,.038,.03,accent,0,-.28,.128);
      break;
    case 'stepmill':
      for(let i=0;i<5;i++) {
        box(.20,.22+i*.115,1,dark,-.40+i*.20,-.28+i*.057);
        box(.20,.033,1,metal,-.40+i*.20,-.158+i*.115);
      }
      break;
    case 'platform':
      box(1,.70,1,dark,0,-.12);
      box(.97,.1,.84,metal,0,platformSurfaceHeight-.05);
      for(const z of [-.46,.46]) box(1,.14,.08,accent,0,.19,z);
      break;
    case 'bench':
      box(.97,.55,.88,dark,0,.16);
      box(.67,.28,.66,metal,0,-.23);
      break;
    case 'pedal':
      box(.97,.49,.91,dark);
      for(const x of [-.27,0,.27]) box(.045,.060,.76,accent,x,pedalSurfaceHeight-.030);
      break;
    case 'weight-stack':
      for(const x of [-.43,.43]) box(.13,1,.80,dark,x);
      for(const y of [-.45,.45]) box(.98,.10,.84,dark,0,y);
      for(const x of [-.25,.25]) box(.045,.89,.08,metal,x,0,.15);
      box(.70,.45,.34,screen,0,-.20,-.18);
      break;
    case 'weight-plates':
      for(let i=0;i<4;i++) box(.87,.17,.76,metal,0,-.32+.215*i);
      box(.23,.14,.04,accent,0,.41,.39);
      break;
    case 'pulley':
      disk(.42,.22,dark);
      disk(.30,.255,metal);
      disk(.10,.31,accent);
      break;
    case 'handle':
    case 'bar':
      box(.96,.34,.48,metal);
      for(const x of [-.34,.34]) box(.26,.49,.64,dark,x);
      break;
  }
  return group;
}



export function cardioPedalSurface(item,view,scale,lane) {
  const [,x,y,,height]=item;
  const surface=view.map([x,y]);
  surface.x=pedalLane(lane);
  surface.y+=height*scale*pedalSurfaceHeight;
  return surface;
}

function pedalLane(lane) {
  return lane%2===0 ? .17 : -.17;
}

export function placeCardioPart(model,item,view,scale,lane,gear) {
  const [asset,x,y,w,h,angle] = item;
  model.position.copy(view.map([x,y]));
  const athleticLane = asset==='pedal' || asset==='handle';
  const depth = athleticLane ? .18 : asset==='platform'||asset==='stepmill' ? .67 :
    asset==='bench' ? .43 : asset==='console' ? .27 : asset==='weight-stack' ? .48 : asset==='weight-plates' ? .30 : asset==='pulley' ? .17 : .34;
  model.rotation.set(0,view.side ? -view.sign*Math.PI/2 : 0,0);
  // The tread, steps, and seat have a physical upper surface.
  model.rotateZ(-THREE.MathUtils.degToRad(angle));
  if (athleticLane) {
    if (view.side) model.position.x = asset==='pedal' ? pedalLane(lane) : (lane%2===0 ? 1 : -1)*(gear==='SKI_ERG' ? .15 : .17);
    else model.position.z = .05;
  }
  model.scale.set(w*scale,h*scale,depth);
}

export function buildCardioChassis(gear,clip,map) {
  const group=new THREE.Group();
  if (clip.view!=='SIDE') return group;
  const sprites=clip.equipment[0].sprites;
  const position=(...assets) => {
    const item=sprites.find(sprite=>assets.includes(sprite[0]));
    return item ? map([item[1],item[2]]) : null;
  };
  const front=position('fan','wheel','console','weight-stack');
  const seat=position('bench');
  const deck=position('platform','stepmill');
  if (!front) return group;
  const chassis=new THREE.MeshStandardMaterial({color:'#314956',roughness:.84});
  function part(width,height,length,x,y,z) {
    const mesh=new THREE.Mesh(new THREE.BoxGeometry(width,height,length),chassis);
    mesh.position.set(x,y,z);
    mesh.castShadow=mesh.receiveShadow=true;
    group.add(mesh);
  }
  function floorRail(a,b,width=.18) {
    part(width,.062,Math.abs(a-b)+.24,0,.031,(a+b)/2);
    for (const z of [a,b]) part(.59,.065,.11,0,.0325,z);
  }
  if (gear==='ROWER' && seat) {
    const rear=Math.min(seat.z-.56,front.z-.66);
    floorRail(rear,front.z);
    const railY=Math.max(.14,seat.y-.09);
    part(.11,.058,Math.abs(front.z-rear),0,railY,(front.z+rear)/2);
    for (const z of [rear+.06,front.z-.04])
      part(.12,railY,.12,0,railY/2,z);
  } else if (seat) {
    floorRail(seat.z-.28,front.z+.18);
    const seatPost=Math.max(.065,seat.y-.07);
    part(.11,seatPost,.13,0,seatPost/2,seat.z);
    const wheelPost=Math.max(.065,front.y-.08);
    part(.13,wheelPost,.14,0,wheelPost/2,front.z);
  } else if (deck) {
    const deckPart=sprites.find(sprite=>sprite[0]==='platform'||sprite[0]==='stepmill');
    const length=deckPart[3]*clip.scale;
    floorRail(deck.z-length*.38,deck.z+length*.38);
    const riser=Math.max(.10,front.y-.06);
    for(const x of [-.23,.23]) part(.060,riser,.084,x,riser/2,front.z);
  } else if (gear==='ELLIPTICAL') {
    floorRail(front.z-.63,front.z+.18);
    const riser=Math.max(.06,front.y-.045);
    part(.16,riser,.16,0,riser/2,front.z);
  } else if (gear==='SKI_ERG') {
    part(.62,.067,.46,0,.0335,front.z);
  }
  return group;
}
