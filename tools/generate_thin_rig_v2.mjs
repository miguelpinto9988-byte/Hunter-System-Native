#!/usr/bin/env node
// Regenerador paramétrico 3D para la figura delgada original de Monarca Eclipse.
// Sin paquetes npm: solamente Node.js. SVG resultante reproducible sin WebView.
import fs from "node:fs";
import path from "node:path";

const FRAME_COUNT = 25;
const DURATION = 4.8;
const PI = Math.PI;
const VIEWS = { front:"squat_front.svg", side:"squat_side.svg", top:"pushup_top.svg" };
const palette = {
  background:"#101f2a", stage:"#152e3d", edge:"#243d50",
  shirt:"#dceaf0", shirtPanel:"#b6d6e5", far:"#94aeba",
  dark:"#35495a", skin:"#edc5ad", joints:"#7ce2e9"
};
const v=(x,y,z)=>[x,y,z], add=(a,b)=>a.map((n,i)=>n+b[i]),
  sub=(a,b)=>a.map((n,i)=>n-b[i]), mul=(a,s)=>a.map(n=>n*s),
  dot=(a,b)=>a.reduce((s,n,i)=>s+n*b[i],0),
  len=a=>Math.sqrt(dot(a,a)), dist=(a,b)=>len(sub(a,b)),
  unit=a=>mul(a,1/len(a));
function twoBoneIK(origin,target,upper,lower,bend){
  const dir=sub(target,origin), d=len(dir);
  if (!(d < upper+lower && d > Math.abs(upper-lower))) throw Error("IK unreachable "+d);
  const u=unit(dir), e=unit(sub(bend,mul(u,dot(u,bend))));
  const along=(upper*upper-lower*lower+d*d)/(2*d);
  return add(origin,add(mul(u,along),mul(e,Math.sqrt(upper*upper-along*along))));
}
function pose(view,t){
  const depth=(1-Math.cos(2*PI*t))/2, p={};
  if(view!=="top"){
    const y=226-92*depth,z=-12-32*depth,a=.11+.22*depth;
    const shY=y+124*Math.cos(a),shZ=z+124*Math.sin(a);
    p.pelvis=v(0,y,z);p.chest=v(0,shY,shZ);
    p.neck=v(0,shY+10*Math.cos(a),shZ+10*Math.sin(a));
    p.head=v(0,shY+62*Math.cos(a),shZ+62*Math.sin(a));
    for(const [side,sign] of [["L",-1],["R",1]]){
      p["hip"+side]=v(sign*38,y,z);
      p["ankle"+side]=v(sign*44,0,12);
      p["toe"+side]=v(sign*44,0,54);
      p["knee"+side]=twoBoneIK(p["hip"+side],p["ankle"+side],116,114,v(sign*.13,0,1));
      p["shoulder"+side]=v(sign*51,shY,shZ);
      p["wrist"+side]=v(sign*58,shY-(112-65*depth),shZ+22+110*depth);
      p["elbow"+side]=twoBoneIK(p["shoulder"+side],p["wrist"+side],82,80,v(sign,0,.2));
    }
  } else {
    const y=94-49*depth, z=-187+Math.sqrt(308*308-y*y);
    p.chest=v(0,y,z);p.pelvis=v(0,y*.38,-187+(z+187)*.38);
    p.head=v(0,y+10,z+49);p.neck=v(0,y,z+25);
    for(const [side,sign] of [["L",-1],["R",1]]){
      p["shoulder"+side]=v(sign*51,y,z);
      p["hip"+side]=v(sign*31,y*.38,-187+(z+187)*.38);
      p["ankle"+side]=v(sign*26,0,-187);p["toe"+side]=v(sign*26,0,-215);
      p["knee"+side]=add(p["ankle"+side],mul(sub(p["hip"+side],p["ankle"+side]),.52));
      p["wrist"+side]=v(sign*100,0,147);
      p["elbow"+side]=twoBoneIK(p["shoulder"+side],p["wrist"+side],85,80,v(sign,0,.38));
    }
  }
  return p;
}
const bones = [
  ["hipL","kneeL"],["kneeL","ankleL"],["hipR","kneeR"],["kneeR","ankleR"],
  ["shoulderL","elbowL"],["elbowL","wristL"],["shoulderR","elbowR"],["elbowR","wristR"],
  ["shoulderL","hipL"],["shoulderR","hipR"],["pelvis","chest"],["chest","neck"],["neck","head"]
];
const fmt=n=>{const s=Number(n.toFixed(2));return Object.is(s,-0)?"0":String(s)};
const point=a=>a.map(fmt).join(" ");
function project(view,p){
  return view==="front" ? [360+1.22*p[0],665-1.25*p[1]]
    :view==="side" ? [340+1.48*p[2],665-1.25*p[1]]
    :[360+1.45*p[0],401-1.12*p[2]];
}
function segment(view,a,b,width){
  const A=project(view,a),B=project(view,b),dx=B[0]-A[0],dy=B[1]-A[1],L=Math.hypot(dx,dy)||1;
  const n=[-dy/L,dx/L],p=[];
  for(const [center,sign,size] of [[A,1,width*.5],[B,1,width*.42],[B,-1,width*.42],[A,-1,width*.5]]){
    p.push([center[0]+sign*n[0]*size,center[1]+sign*n[1]*size]);
  }
  return "M "+point(p[0])+" L "+point(p[1])+" L "+point(p[2])+" L "+point(p[3])+" Z";
}
function polygon(view,points){
  return "M "+points.map(p=>point(project(view,p))).join(" L ")+" Z";
}
const TIMES=Array.from({length:FRAME_COUNT},(_,i)=>fmt(i/(FRAME_COUNT-1))).join(";");
function animate(attr,values){
  return '<animate attributeName="'+attr+'" values="'+values.join(";")+'" keyTimes="'+TIMES+
    '" dur="'+DURATION+'s" repeatCount="indefinite" calcMode="linear"/>';
}
function sourceFrames(view){
  const frames=Array.from({length:FRAME_COUNT},(_,i)=>pose(view,i/(FRAME_COUNT-1)));
  for(const [a,b] of bones){
    const lengths=frames.map(p=>dist(p[a],p[b]));
    if(Math.max(...lengths)-Math.min(...lengths)>1e-8) throw Error("bone drift "+a+" "+b);
  }
  const anchors=view==="top"?["wristL","wristR","ankleL","ankleR"]:["ankleL","ankleR"];
  for(const a of anchors) for(const f of frames) if(dist(f[a],frames[0][a])>1e-8) throw Error("foot/hand drift "+a);
  return frames;
}
function create(view){
  const frames=sourceFrames(view), p=frames[0], title=view==="front"?"Sentadilla frontal":view==="side"?"Sentadilla lateral":"Flexiones desde arriba";
  const out=['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 720 800" role="img"'+
    ' data-renderer="thin-rig-v2" data-legacy-reference="media2d" data-view="'+view+
    '" data-kinematic-space="3d" data-frames="'+FRAME_COUNT+'" aria-label="'+title+' · rig vectorial de dos capas">'];
  out.push('<title>'+title+' · figura delgada original mejorada</title>');
  out.push('<desc>Modelo 3D proyectado y estilizado, no evaluación clínica.</desc>');
  out.push('<rect width="720" height="800" rx="26" fill="'+palette.background+'"/>'+
    '<circle cx="360" cy="370" r="268" fill="'+palette.stage+'"/>'+
    '<circle cx="360" cy="370" r="216" fill="none" stroke="#284352"/>'+
    '<text x="360" y="77" text-anchor="middle" fill="'+palette.shirt+'" font-size="21" font-family="sans-serif">'+
      title.toUpperCase()+'</text>');
  if(view!=="top")out.push('<path d="M100 665H620" stroke="#47606e" stroke-width="2"/>');
  function shape(id,color,stroke,width,from){
    const values=frames.map(from);
    out.push('<path id="'+id+'" d="'+values[0]+'" fill="'+color+'" stroke="'+stroke+
      '" stroke-width="'+width+'" stroke-linejoin="round">'+animate("d",values)+'</path>');
  }
  function circle(id,joint,r,color,stem){
    const xs=frames.map(q=>fmt(project(view,q[joint])[0])),
      ys=frames.map(q=>fmt(project(view,q[joint])[1]));
    out.push('<circle id="'+id+'" cx="'+xs[0]+'" cy="'+ys[0]+'" r="'+r+'" fill="'+color+
      '" stroke="'+palette.edge+'" stroke-width="1.6"'+(stem?' data-xyz="'+
        frames.map(q=>q[joint].map(fmt).join(",")).join("|")+'"':'')+'>'+
        animate("cx",xs)+animate("cy",ys)+'</circle>');
  }
  out.push('<g id="layer-aesthetic" data-layer="appearance" data-style-source="media2d">');
  for(const side of ["L","R"]){
    const fill=side==="L"?palette.far:palette.shirt;
    shape("a-thigh-"+side,palette.dark,palette.background,1.8,q=>segment(view,q["hip"+side],q["knee"+side],20));
    shape("a-calf-"+side,fill,palette.background,1.8,q=>segment(view,q["knee"+side],q["ankle"+side],17));
  }
  shape("a-torso",palette.shirt,palette.edge,2.5,q=>polygon(view,[q.shoulderL,q.shoulderR,q.hipR,q.hipL]));
  shape("a-shirt-panel",palette.shirtPanel,"none",0,q=>polygon(view,[
    add(q.shoulderL,v(5,0,0)),add(q.shoulderR,v(-5,0,0)),
    add(q.hipR,v(-4,8,0)),add(q.hipL,v(4,8,0))]));
  shape("a-shorts",palette.edge,palette.background,2,q=>polygon(view,[
    q.hipL,q.hipR,add(q.hipR,mul(sub(q.kneeR,q.hipR),.19)),
    add(q.hipL,mul(sub(q.kneeL,q.hipL),.19))]));
  for(const side of ["L","R"]){
    const color=side==="L"?palette.far:palette.shirt;
    shape("a-upperarm-"+side,color,palette.edge,1.8,q=>segment(view,q["shoulder"+side],q["elbow"+side],16));
    shape("a-forearm-"+side,color,palette.edge,1.8,q=>segment(view,q["elbow"+side],q["wrist"+side],13));
    circle("a-elbow"+side,"elbow"+side,8,color,false);
    circle("a-knee"+side,"knee"+side,9,color,false);
    circle("a-hand-"+side,"wrist"+side,9,palette.skin,false);
    const foot=project(view,p["toe"+side]);
    out.push('<ellipse id="a-shoe-'+side+'" cx="'+fmt(foot[0])+'" cy="'+fmt(foot[1])+
      '" rx="16" ry="11" fill="'+palette.edge+'" stroke="'+palette.joints+'" stroke-width="2"/>');
  }
  circle("a-head","head",25,palette.skin,false);
  shape("a-hair",palette.edge,"none",0,q=>{
    const [x,y]=project(view,q.head);
    return "M "+point([x-22,y-4])+" Q "+point([x-25,y-34])+" "+point([x+4,y-28])+
      " Q "+point([x+24,y-29])+" "+point([x+22,y-5])+" L "+point([x+7,y-18])+" Z";
  });
  out.push('</g>');
  out.push('<g id="layer-skeleton" data-layer="rig" opacity=".30" stroke="'+palette.joints+'">');
  for(const [a,b] of bones){
    const A=project(view,p[a]),B=project(view,p[b]);
    let line='<line data-link="'+a+':'+b+'" x1="'+fmt(A[0])+'" y1="'+fmt(A[1])+
      '" x2="'+fmt(B[0])+'" y2="'+fmt(B[1])+'" stroke-width="2">';
    for(const [attr,j,coord] of [["x1",a,0],["y1",a,1],["x2",b,0],["y2",b,1]])
      line+=animate(attr,frames.map(q=>fmt(project(view,q[j])[coord])));
    out.push(line+'</line>');
  }
  for(const j of Object.keys(p))circle("j-"+j,j,5,palette.joints,true);
  out.push('</g>');
  if(view==="top")out.push('<g id="depth-indicator"><text x="575" y="590" fill="'+palette.shirt+
    '" font-family="sans-serif" font-size="14">ALTURA</text><rect x="580" y="605" width="18" height="90" fill="'+palette.edge+
    '"/><rect x="580" y="605" width="18" height="90" fill="'+palette.joints+'">'+
    '<animate attributeName="height" values="90;40;90" dur="4.8s" repeatCount="indefinite"/></rect></g>');
  out.push('<text x="360" y="774" text-anchor="middle" fill="#b0c6d1" font-size="12" font-family="sans-serif">MONARCA · FIGURA DELGADA · NO EVALUACIÓN CLÍNICA</text></svg>');
  return out.join("\n");
}
const i=process.argv.indexOf("--output");
const outputDir=i>=0?process.argv[i+1]:path.resolve("app/src/main/assets/media2d/rig-v2");
if(!outputDir)throw Error("Missing --output directory");
fs.mkdirSync(outputDir,{recursive:true});
for(const [view,file] of Object.entries(VIEWS)){
  const filePath=path.join(outputDir,file);
  const svg=create(view);
  fs.writeFileSync(filePath,svg,"utf8");
  process.stdout.write(file+" · "+FRAME_COUNT+" 3D frames · "+Buffer.byteLength(svg)+" bytes\n");
}
