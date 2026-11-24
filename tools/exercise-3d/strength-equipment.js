import * as THREE from 'three';

const weights = new Set(['barbell','plate','kettlebell']);
export const isStrengthWeight = asset => weights.has(asset);

export function strengthWeight(asset) {
  const group = new THREE.Group();
  const rubber = new THREE.MeshStandardMaterial({color:'#26333d',roughness:.84});
  const steel = new THREE.MeshStandardMaterial({color:'#849eaa',metalness:.52,roughness:.50});
  function mesh(geometry, material, x=0) {
    const object = new THREE.Mesh(geometry,material);
    object.position.x=x;
    object.castShadow=object.receiveShadow=true;
    group.add(object);
    return object;
  }
  if (asset==='barbell') {
    mesh(new THREE.CylinderGeometry(.015,.015,1.53,16),steel).rotation.z=Math.PI/2;
    for (const side of [-1,1]) {
      mesh(new THREE.CylinderGeometry(.18,.18,.069,32),rubber,side*.647).rotation.z=Math.PI/2;
      mesh(new THREE.CylinderGeometry(.048,.048,.075,20),steel,side*.665).rotation.z=Math.PI/2;
    }
  } else if (asset==='dumbbell') {
    mesh(new THREE.CylinderGeometry(.015,.015,.25,14),steel).rotation.z=Math.PI/2;
    for(const side of [-1,1]) {
      mesh(new THREE.CylinderGeometry(.065,.065,.065,6),rubber,side*.118).rotation.z=Math.PI/2;
    }
  } else if (asset==='plate') {
    mesh(new THREE.TorusGeometry(.115,.055,12,36),rubber);
    mesh(new THREE.TorusGeometry(.044,.012,10,24),steel);
    for (let i=0;i<3;i++) {
      const grip=mesh(new THREE.BoxGeometry(.12,.042,.045),rubber);
      const angle=i*2*Math.PI/3;
      grip.position.set(.075*Math.cos(angle),.075*Math.sin(angle),0);
      grip.rotation.z=angle;
    }
  } else {
    const bell=mesh(new THREE.SphereGeometry(.092,24,16),rubber);
    bell.position.y=-.035;
    bell.scale.y=.82;
    const handle=mesh(new THREE.TorusGeometry(.057,.013,10,28,Math.PI),steel);
    handle.position.y=.042;
  }
  return group;
}
