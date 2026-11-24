import * as THREE from 'three';

const guidedGear=new Set(['SMITH','LEG_PRESS','GUIDED_SQUAT','LEG_LEVER','HIP_LEVER','ARM_LEVER','ASSISTED','TIBIALIS','RACK']);
const componentNames=new Set(['smith','roller','platform','handle','bar','plate','bench','pulley','weight-stack','weight-plates']);

export function guidedMachine(gear) {
  const names=gear.map(item=>item[0]);
  return {guided:names.some(name=>guidedGear.has(name)),smith:names.includes('SMITH'),rack:names.includes('RACK')};
}

export function isGuidedComponent(asset) {
  return componentNames.has(asset);
}

export function guidedComponent(asset,rack=false) {
  const group=new THREE.Group();
  const structure=new THREE.MeshStandardMaterial({color:'#304753',roughness:.77,metalness:.26});
  const steel=new THREE.MeshStandardMaterial({color:'#7c98a3',roughness:.47,metalness:.50});
  const pad=new THREE.MeshStandardMaterial({color:'#273742',roughness:.87});
  function add(geometry,material,x=0,y=0,z=0) {
    const mesh=new THREE.Mesh(geometry,material);
    mesh.position.set(x,y,z);
    mesh.castShadow=mesh.receiveShadow=true;
    group.add(mesh);
    return mesh;
  }
  function box(w,h,d,material,x=0,y=0,z=0) {
    return add(new THREE.BoxGeometry(w,h,d),material,x,y,z);
  }

  if (asset==='smith') {
    for(const x of [-.45,.45]){
      box(.093,1,.11,structure,x,0,-.21);
      if(!rack) box(.035,.94,.046,steel,x,0,0);
      box(.091,.055,.91,structure,x,-.474,.06);
      for(let i=0;i<8;i++) box(.072,.020,.075,rack ? steel : structure,x,-.36+i*.095,rack ? -.13 : .022);
    }
    box(1,.050,.12,structure,0,.474,-.21);
    box(1,.075,.12,structure,0,-.456,-.21);
  } else if (asset==='bar') {
    add(new THREE.CylinderGeometry(.14,.14,1,20),steel).rotation.z=Math.PI/2;
  } else if (asset==='roller') {
    add(new THREE.CylinderGeometry(.48,.48,.88,24),pad).rotation.z=Math.PI/2;
    for(const x of [-.46,.46])
      add(new THREE.CylinderGeometry(.19,.19,.07,16),steel,x).rotation.z=Math.PI/2;
  } else if (asset==='plate') {
    add(new THREE.CylinderGeometry(.46,.46,.30,28),pad).rotation.x=Math.PI/2;
    add(new THREE.CylinderGeometry(.14,.14,.35,16),steel).rotation.x=Math.PI/2;
  } else return null;

  return group;
}

export function placeGuidedComponent(model,item,view,scale) {
  const [asset,x,y,width,height,angle]=item;
  const center=view.map([x,y]);
  model.position.copy(center);
  model.rotation.set(0,0,0);
  if(asset==='smith'){
    model.scale.set(Math.max(1.40,width*scale),height*scale,.79);
    model.position.y=height*scale*.5;
  } else if(asset==='bar'){
    model.scale.set(Math.max(.80,width*scale),.105,.105);
  } else if(asset==='roller'){
    if (!view.side) model.rotateZ(-THREE.MathUtils.degToRad(angle));
    model.scale.set(view.side ? Math.max(.30,width*scale) : width*scale,height*scale,height*scale);
  } else {
    if(view.side) model.rotation.y=-view.sign*Math.PI/2;
    model.scale.set(width*scale,height*scale,.14);
  }
}
