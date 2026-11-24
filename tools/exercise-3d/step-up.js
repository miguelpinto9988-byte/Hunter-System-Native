import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { RoundedBoxGeometry } from 'three/addons/geometries/RoundedBoxGeometry.js';

import { playbackDuration } from './planted-motion.js';
import { projectedMotion } from './projected-motion.js';

const viewport = document.querySelector('#viewport');
const variant = new URLSearchParams(location.search).get('clip');
const sourceDuration = 6.4;
const returnStart = 3.65;
const holdPose = 2.2;
const holdDuration = 1;

function sourceTime(time) {
  if (variant === 'pause') {
    if (time < holdPose) return time;
    return Math.max(holdPose, time - holdDuration);
  }
  if (variant === 'slow' && time >= returnStart) {
    return returnStart + (time - returnStart) * (sourceDuration - returnStart) / 3;
  }
  return time;
}

const stepDuration = variant === 'pause' ? sourceDuration + holdDuration
  : variant === 'slow' ? returnStart + 3 : sourceDuration;

async function startViewer() {
  const tracks = variant === 'projected' ? await fetch('./motion.json').then(response => response.json()) : null;
  const duration = tracks ? tracks.reduce((total,clip) => total+playbackDuration(clip),0) : stepDuration;
  const renderer = new THREE.WebGLRenderer({ antialias: true });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 1.6));
  renderer.shadowMap.enabled = true;
  renderer.shadowMap.type = THREE.PCFSoftShadowMap;
  renderer.toneMapping = THREE.ACESFilmicToneMapping;
  renderer.toneMappingExposure = 1.15;
  renderer.domElement.setAttribute('aria-label', 'Demostración tridimensional del ejercicio');
  viewport.prepend(renderer.domElement);

  const scene = new THREE.Scene();
  scene.fog = new THREE.Fog('#0e222b', 4.5, 15);
  const backdrop = document.createElement('canvas');
  backdrop.width = 512;
  backdrop.height = 512;
  const backdropPaint = backdrop.getContext('2d');
  const halo = backdropPaint.createRadialGradient(318, 130, 12, 318, 130, 380);
  halo.addColorStop(0, '#285966');
  halo.addColorStop(.44, '#1b3947');
  halo.addColorStop(1, '#0b1923');
  backdropPaint.fillStyle = halo;
  backdropPaint.fillRect(0, 0, 512, 512);
  scene.background = new THREE.CanvasTexture(backdrop);
  scene.background.colorSpace = THREE.SRGBColorSpace;
  const camera = new THREE.PerspectiveCamera(38, 1, 0.03, 75);
  const controls = new OrbitControls(camera, renderer.domElement);
  controls.enableDamping = true;
  controls.enablePan = false;
  controls.minDistance = 2.15;
  controls.maxDistance = 7;
  controls.maxPolarAngle = Math.PI * .89;
  const bodyTarget = new THREE.Vector3(0, .96, -.04);
  controls.target.copy(bodyTarget);
  camera.position.set(3.33, 1.43, .18);
  controls.update();

  scene.add(new THREE.HemisphereLight('#d8edff', '#718080', 1.81));
  const key = new THREE.DirectionalLight('#fff0e5', 2.60);
  key.position.set(3.4, 5, 2.7);
  key.castShadow = true;
  key.shadow.mapSize.set(1024, 1024);
  key.shadow.radius = 3.5;
  key.shadow.camera.left = -2.3;
  key.shadow.camera.right = 2.3;
  key.shadow.camera.top = 2.5;
  key.shadow.camera.bottom = -2.5;
  key.shadow.normalBias = 0.014;
  key.shadow.bias = -.00012;
  scene.add(key);
  const rim = new THREE.DirectionalLight('#a6dce0', 1.46);
  rim.position.set(-1.6, 2.9, -3.1);
  scene.add(rim);

  const material = (color, roughness = .72, metalness = 0) =>
    new THREE.MeshStandardMaterial({ color, roughness, metalness });
  const rubber = material('#26323b', .89);
  const boxBody = material('#283e4b');
  const boxTop = material('#3d5360');
  const boxTrim = material('#a6f883');
  const contactMaterial = new THREE.MeshBasicMaterial({ color: '#a1f5af', transparent: true, opacity: .78 });

  function mesh(geometry, surface, parent = scene) {
    const result = new THREE.Mesh(geometry, surface);
    result.castShadow = true;
    result.receiveShadow = true;
    parent.add(result);
    return result;
  }
  const fill = new THREE.DirectionalLight('#e0ebf1', 1.04);
  fill.position.set(-2, 2.5, 4);
  scene.add(fill);

  const floorSurface = material('#16232d');
  floorSurface.transparent = true;
  floorSurface.onBeforeCompile = shader => {
    shader.fragmentShader = shader.fragmentShader.replace(
      '#include <fog_fragment>',
      '#include <fog_fragment>\n gl_FragColor.a *= 1.0 - smoothstep(5.0, 14.0, vFogDepth);'
    );
  };
  const floor = mesh(new THREE.PlaneGeometry(100, 100), floorSurface);
  floor.rotation.x = -Math.PI / 2;
  floor.castShadow = false;
  const grid = new THREE.GridHelper(14, 42, '#3b5968', '#263f4a');
  grid.material.transparent = true;
  grid.material.opacity = .46;
  grid.position.y = .002;
  scene.add(grid);

  const roundedBox = (w, h, d, radius, surface, parent) =>
    mesh(new RoundedBoxGeometry(w, h, d, 3, radius), surface, parent);

  const stepStage = new THREE.Group();
  scene.add(stepStage);
  // A soft near-field shadow grounds the platform independently of the key light.
  const occlusion = document.createElement('canvas');
  occlusion.width = occlusion.height = 128;
  const shadowPaint = occlusion.getContext('2d');
  const fade = shadowPaint.createRadialGradient(64, 64, 22, 64, 64, 64);
  fade.addColorStop(0, 'rgba(2, 10, 15, .7)');
  fade.addColorStop(.66, 'rgba(2, 10, 15, .38)');
  fade.addColorStop(1, 'rgba(2, 10, 15, 0)');
  shadowPaint.fillStyle = fade;
  shadowPaint.fillRect(0, 0, 128, 128);
  const platformShadow = new THREE.Mesh(new THREE.PlaneGeometry(1.48, 1.48),
    new THREE.MeshBasicMaterial({ map: new THREE.CanvasTexture(occlusion),
      transparent: true, depthWrite: false }));
  platformShadow.rotation.x = -Math.PI / 2;
  platformShadow.position.set(0, .003, .31);
  platformShadow.renderOrder = 3;
  stepStage.add(platformShadow);

  const platformTop = .30;
  roundedBox(.79, .278, .83, .055, boxBody, stepStage).position.set(0, .139, .31);
  roundedBox(.798, .046, .838, .016, boxTrim, stepStage).position.set(0, .246, .31);
  roundedBox(.80, .072, .84, .027, boxTop, stepStage).position.set(0, .264, .31);
  for (const side of [-1, 1]) {
    for (const end of [-1, 1]) {
      roundedBox(.16, .041, .135, .012, rubber, stepStage).position.set(side * .267, .0205, .31 + end * .306);
    }
  }

  const textureCanvas = document.createElement('canvas');
  textureCanvas.width = 192;
  textureCanvas.height = 128;
  const grain = textureCanvas.getContext('2d');
  grain.fillStyle = '#3d5260';
  grain.fillRect(0, 0, 192, 128);
  for (let y = 3; y < 128; y += 5) {
    for (let x = 3; x < 192; x += 5) {
      grain.fillStyle = '#30434f';
      grain.fillRect(x, y + 1, 3, 1);
      grain.fillStyle = (x + y) % 3 ? '#4c6372' : '#455c69';
      grain.fillRect(x + 1, y, 2, 1);
    }
  }
  const treadTexture = new THREE.CanvasTexture(textureCanvas);
  treadTexture.colorSpace = THREE.SRGBColorSpace;
  treadTexture.anisotropy = Math.min(8, renderer.capabilities.getMaxAnisotropy());
  const tread = mesh(new THREE.PlaneGeometry(.73, .77), new THREE.MeshStandardMaterial({
    map: treadTexture, bumpMap: treadTexture, bumpScale: .006, roughness: .89
  }), stepStage);
  tread.rotation.x = -Math.PI / 2;
  tread.position.set(0, platformTop + .0007, .31);
  tread.castShadow = false;

  // Thin inlay follows the raised surface without interrupting the grip.
  const inlay = new THREE.MeshBasicMaterial({ color: '#a2cdbd', transparent: true,
    opacity: .55, depthWrite: false });
  function platformMark(x,z,width,length) {
    const mark = new THREE.Mesh(new THREE.PlaneGeometry(width,length),inlay);
    mark.rotation.x = -Math.PI / 2;
    mark.position.set(x,platformTop+.0015,z);
    stepStage.add(mark);
  }
  for (const sign of [-1,1]) {
    for (const end of [-1,1]) {
      platformMark(sign*.324,.31+end*.341,.070,.005);
      platformMark(sign*.357,.31+end*.307,.005,.073);
    }
  }
  for (const z of [.05,.15,.25,.35,.45,.55]) platformMark(0,z,.004,.033);


  const contact = new THREE.Group();
  stepStage.add(contact);
  const supportRing = mesh(new THREE.RingGeometry(.85, .98, 48), contactMaterial, contact);
  supportRing.rotation.x = -Math.PI / 2;
  supportRing.scale.set(.101, .171, 1);
  supportRing.castShadow = false;
  contact.position.y = platformTop + .002;

  const { scene: athlete, animations } = await new GLTFLoader().loadAsync('./step-up.glb');
  const clipsByLeg = new Map(animations.map(clip => [clip.name, clip]));
  if (!clipsByLeg.has('Izquierda') || !clipsByLeg.has('Derecha')) {
    throw new Error('Faltan las animaciones de una o ambas piernas.');
  }
  const contoured = new Set();
  function outlineMaterial(material) {
    if (contoured.has(material)) return;
    contoured.add(material);
    if (!/^(Tejido|Piel|Cabello|Goma|Suela|Malla)/.test(material.name)) return;
    material.onBeforeCompile = shader => {
      shader.fragmentShader = shader.fragmentShader.replace('#include <opaque_fragment>', `
        float silhouette = pow(1.0 - clamp(dot(normalize(nonPerturbedNormal),
          normalize(vViewPosition)), 0.0, 1.0), 3.5);
        float rimDirection = smoothstep(-0.45, 0.80, nonPerturbedNormal.y - nonPerturbedNormal.x * 0.35);
        outgoingLight += vec3(0.045, 0.088, 0.095) * silhouette * rimDirection;
        #include <opaque_fragment>
      `);
    };
  }
  athlete.traverse(object => {
    if (object.isMesh) {
      const surfaces = Array.isArray(object.material) ? object.material : [object.material];
      surfaces.forEach(outlineMaterial);
      if (variant !== 'dumbbells' && /^(Mango|Pesa_hexagonal|Placa|Remache)/.test(object.name)) {
        object.visible = false;
        return;
      }
      object.castShadow = true;
      object.receiveShadow = object.name !== 'Rostro_esculpido';
      // Limbs move outside the bind-pose bounds during the step-up.
      object.frustumCulled = false;
    }
  });
  scene.add(athlete);
  const projected = tracks ? await projectedMotion(athlete,tracks,scene,controls,camera) : null;
  stepStage.visible = !projected;
  const mixer = new THREE.AnimationMixer(athlete);
  let mirrored = false;
  function chooseLeg() {
    mixer.stopAllAction();
    mixer.clipAction(clipsByLeg.get(mirrored ? 'Derecha' : 'Izquierda')).play();
  }
  if (!projected) chooseLeg();

  let current = 0;
  let playing = false;
  let lastReset;
  let previousTime;
  let redraw = true;

  function drawPose() {
    if (projected) {
      projected.draw(current,mirrored);
      return;
    }
    const poseTime = sourceTime(current);
    mixer.setTime(poseTime);
    contact.position.x = (mirrored ? -1 : 1) * .147;
    contact.position.z = .246;
    contact.visible = poseTime >= sourceDuration * .22 && poseTime <= sourceDuration * .81;
  }

  function resize() {
    const { width, height } = viewport.getBoundingClientRect();
    if (!width || !height) return;
    renderer.setSize(width, height);
    camera.aspect = width / height;
    // Fit the whole body even when the app is in a narrow pane.
    camera.zoom = Math.min(1, camera.aspect / .78);
    camera.updateProjectionMatrix();
    redraw = true;
  }
  new ResizeObserver(resize).observe(viewport);
  resize();
  drawPose();
  controls.update();
  renderer.render(scene, camera);

  window.hunterPlayback = (play, mirror, reset) => {
    if (lastReset !== reset) current = 0;
    lastReset = reset;
    if (mirrored !== mirror) {
      mirrored = mirror;
      if (projected) projected.beginMirror();
      else {
        chooseLeg();
        camera.position.x *= -1;
      }
    }
    playing = play;
    previousTime = undefined;
    redraw = true;
    drawPose();
  };
  viewport.dataset.ready = 'true';
  renderer.setAnimationLoop(now => {
    const elapsed = previousTime === undefined ? 0 : Math.min(.10, (now - previousTime) / 1000);
    previousTime = now;
    if (playing) {
      current = (current + elapsed) % duration;
      drawPose();
    }
    const moved = controls.update();
    if (playing || redraw || moved) renderer.render(scene, camera);
    redraw = false;
  });
}

startViewer().catch(error => {
  console.error(error);
  viewport.dataset.ready = 'failed';
});
