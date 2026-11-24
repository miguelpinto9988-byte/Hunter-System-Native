import {Vector3,MathUtils} from 'three';

const v=(x,y,z)=>new Vector3(x,y,z);
const {lerp,clamp}=MathUtils;

export function thirdBatchMotion(clip,map,feet) {
  const id=(clip.exerciseId ?? '').replace(/_(?:slow|pause|short|easy)$/,'');
  const travel=(point,axis)=>{
    const values=clip.frames.map(frame=>frame[point][axis]);
    const a=values[0];
    const b=values.reduce((best,n)=>Math.abs(n-a)>Math.abs(best-a)?n:best,a);
    return (frame,phase)=>Math.abs(b-a)<.1
      ? Math.sin(Math.PI*phase)**2 : clamp((frame[point][axis]-a)/(b-a),0,1);
  };
  const flat=(side,z,x=.19)=>v(side*x,feet[side].flat,z);
  const stillHead=true;

  if (id==='bird' || id==='kickback' || id==='cat') {
    return (frame,phase)=>{
      const level=id==='cat' ? Math.sin(phase*2*Math.PI) : 0;
      const amount=id==='cat' ? 0 : Math.sin(2*Math.PI*phase)**2;
      const outgoing=phase<.5 ? 1 : -1;
      return {
        hip:v(0,.543+Math.min(0,.098*level),-.25), trunk:v(0,-.105*level,.405), stillHead,
        wrist:side=>v(side*.207,.047+(id==='bird'&&side===-outgoing ? .385*amount : 0),
          .175+(id==='bird'&&side===-outgoing ? .54*amount : 0)),
        elbow:side=>v(side*.225,.24,.115),
        armContact:side=>id!=='bird'||side===outgoing||amount<.025,
        leg:(side,joint)=>{
          const reach=(id!=='cat'&&side===outgoing)?amount:0;
          const contact=.078-joint.y;
          const behind=-Math.sqrt(Math.max(.0001,feet[side].upper**2-contact**2));
          const shinRise=feet[side].contact(.80)[0]-.078;
          return [v(0,lerp(contact,.055,reach),lerp(behind,-feet[side].upper,reach)),
            v(0,lerp(shinRise,.031,reach),-feet[side].lower)];
        },
        shoePitch:()=>.80, shoeYaw:()=>Math.PI,
        stabilizeHands:true
      };
    };
  }

  if (id==='clam') {
    const drive=travel(9,1);
    return (frame,phase)=>{
      const opening=.70*drive(frame,phase);
      return {
        hip:v(0,.204,0),trunk:v(0,0,.417),roll:Math.PI/2,stillHead,
        wrist:side=>side===1 ? v(.295,.056,.24) : v(.137,.055,.68),
        elbow:side=>side===1 ? v(.115,.218,.252) : v(.23,.058,.39),
        armContact:()=>false,
        foot:side=>v(.062,side===1?.170:.072,-.558),
        knee:(side,joint,heel)=>{
          const axis=heel.clone().sub(joint).normalize();
          const forward=v(1,0,0).addScaledVector(axis,-axis.x).normalize();
          const lift=v(0,1,0).addScaledVector(axis,-axis.y).addScaledVector(forward,-forward.y).normalize();
          const angle=side===1 ? opening : 0;
          return joint.clone().lerp(heel,.5)
            .addScaledVector(forward,Math.cos(angle))
            .addScaledVector(lift,Math.sin(angle));
        },
        shoeRoll:Math.PI/2
      };
    };
  }

  if (id==='sideleg') {
    const drive=travel(9,1);
    return (frame,phase)=>{
      const angle=.49*drive(frame,phase);
      return {
        hip:v(0,.204,0),trunk:v(0,0,.417),roll:Math.PI/2,stillHead,
        wrist:side=>side===1 ? v(.29,.056,.25) : v(.137,.055,.68),
        elbow:side=>side===1 ? v(.11,.22,.25) : v(.23,.058,.39),
        armContact:()=>false,
        foot:()=>v(-.08,.078,-.75),
        knee:()=>v(.31,.079,-.37),
        leg:side=>side===1
          ? [v(0,Math.sin(angle),-Math.cos(angle)),v(0,Math.sin(angle),-Math.cos(angle))]
          : null,
        shoeRoll:Math.PI/2,
        shoeYaw:()=>Math.PI/2,
        shoeLift:side=>side===1 ? angle : 0,
        contact:side=>side===-1
      };
    };
  }

  if (id==='benchpress' || id==='inclinebench' || id==='squeezepress' || id==='pullover') {
    const drive=travel(4,1);
    const prop=clip.props[0];
    const support=prop ? Math.max(.30,map([prop[1],prop[2]]).y-(id==='inclinebench'?.23:0)) : 0;
    const incline=id==='inclinebench'?.52:0;
    const hipZ=prop ? map([clip.frames[0][2][0],prop[2]]).z : 0;
    return (frame,phase)=>{
      const a=drive(frame,phase);
      return {
        hip:v(0,support+.15,hipZ), trunk:v(0,.417*Math.sin(incline),-.417*Math.cos(incline)), stillHead,
        foot:side=>flat(side,hipZ+(prop?.51:.61),.20),
        knee:side=>v(side*.20,support+.29,hipZ+.29),
        wrist:side=>{
          if(id==='pullover') return v(side*.084,.223+.46*Math.cos(1.31*a),hipZ-.37-.46*Math.sin(1.31*a));
          const narrow=id==='squeezepress';
          return v(side*(narrow ? .096 : lerp(.344,.225,a)),support+.415+.253*a+incline*.36,hipZ-.35-.018*a);
        },
        elbow:side=>v(side*(id==='pullover'?.075:.43),support+.162+incline*.32,hipZ-.218),
        armContact:()=>false
      };
    };
  }

  if (id==='kneepush') {
    const drive=travel(1,1);
    return (frame,phase)=>{
      const a=drive(frame,phase);
      return {
        hip:v(0,.338-.071*a,-.35+.043*a), trunk:v(0,.233-.123*a,.342+.056*a), stillHead,
        wrist:side=>v(side*.214,.048,.085), elbow:side=>v(side*.295,.20,-.205),
        leg:()=>[v(0,-.45+.16*a,-.86),v(0,.04,-1)], shoePitch:()=>.82, shoeYaw:()=>Math.PI,
        stabilizeHands:true
      };
    };
  }

  if (id==='lateralunge') {
    const standing=Math.min(feet[1].flat+feet[1].length,feet[-1].flat+feet[-1].length)-.011;
    const stride=.425;
    const hipsAside=.388;
    const hipsBack=.207;
    return (_,phase)=>{
      const step=MathUtils.smoothstep(phase,0,.22)*(1-MathUtils.smoothstep(phase,.78,1));
      const depth=MathUtils.smoothstep(phase,.22,.50)*(1-MathUtils.smoothstep(phase,.50,.78));
      const lift=.075*4*step*(1-step);
      const x=.117*step+(hipsAside-.117)*depth;
      const z=-hipsBack*depth;
      // Choose pelvis height from the planted leg's reach as the body shifts.
      const heightFor=(side,dx,raised)=>{
        const reach=feet[side].length-.014;
        return feet[side].flat+raised+Math.sqrt(reach**2-dx**2-z**2);
      };
      const height=Math.min(standing,heightFor(-1,x,0),heightFor(1,x-stride*step,lift))-.215*depth;
      const lean=.34*depth;
      const rearPitch=.12*depth;
      const [rearY,rearZ]=feet[-1].contact(rearPitch);
      return {
        hip:v(x,height,z),trunk:v(0,Math.cos(lean),Math.sin(lean)).multiplyScalar(.417),stillHead,
        foot:side=>side===1 ? v(.157+stride*step,feet[1].flat+lift,0) : v(-.157,rearY,rearZ),
        shoePitch:side=>side===-1 ? rearPitch : 0,
        knee:(_,joint,ankle)=>joint.clone().lerp(ankle,.5).add(v(0,0,.38)),
        alignKneePlane:true,
        arm:side=>[v(side*.16,-.24,1),v(-side*.70,.24,.48)],
        contact:side=>side===-1 || lift<.002
      };
    };
  }

  if (id==='sumo' || id==='dbsumo' || id==='sumodead') {
    const drive=travel(2,1);
    return (frame,phase)=>{
      const a=drive(frame,phase);
      const hinge=id==='sumodead';
      const width=.263;
      const lean=hinge ? .05+.78*a : .12+.34*a;
      return {
        hip:v(0,.965-(hinge?.235:.34)*a,-.025-.145*a),
        trunk:v(0,Math.cos(lean),Math.sin(lean)).multiplyScalar(.417), stillHead,
        foot:side=>flat(side,0,width),
        knee:side=>v(side*(width+.025),.60-.19*a,.255),
        wrist:hinge ? side=>v(side*.061,.934-.373*a,.245-.025*a)
          : id==='dbsumo' ? side=>v(side*.055,.98-.34*a,.296-.11*a) : undefined,
        elbow:hinge ? side=>v(side*.239,1.12-.294*a,.14)
          : id==='dbsumo' ? side=>v(side*.245,1.17-.36*a,.205-.09*a) : undefined,
        arm:id==='sumo' ? side=>[v(side*.16,-.24,1),v(-side*.70,.24,.48)] : undefined,
        armContact:hinge ? ()=>false : undefined,
        shoeYaw:side=>side*.23
      };
    };
  }

  if (id==='lateral' || id==='reversefly' || id==='wallslide') {
    const drive=travel(4,1);
    const fly=id==='reversefly';
    const wall=id==='wallslide';
    const upright=id==='lateral';
    const standingHeight=Math.min(...[1,-1].map(side=>feet[side].flat+feet[side].length))-.006;
    return (frame,phase)=>{
      const a=drive(frame,phase);
      const angle=(wall?.66:0)+(wall?.27:1.46)*a;
      const lean=fly?.93:upright?0:.02;
      return {
        hip:v(0,upright?standingHeight:.962,fly?-.175:0), trunk:v(0,Math.cos(lean),Math.sin(lean)).multiplyScalar(.417), stillHead,
        foot:side=>flat(side,0,upright?.157:.177),
        knee:side=>v(side*(upright?.157:.182),.52,fly?.15:.060),
        wrist:wall ? side=>v(side*(.38-.07*a),1.53+.35*a,-.117) : undefined,
        elbow:side=>v(side*(.43-.11*a),1.38+.19*a,-.117),
        arm:wall ? undefined : side=>[v(side*(.12+Math.sin(angle)),fly?-Math.cos(angle)*.94:-Math.cos(angle),fly?.16:.08),
             v(side*(.10+Math.sin(angle)),fly?-Math.cos(angle)*.94:-Math.cos(angle),fly?.28:.10)],
      };
    };
  }

  if (id==='singlehinge' || id==='singlecalf' || id==='ankle' || id==='towelrow') {
    const prop=clip.props[0];
    const top=prop ? map([prop[1],prop[2]]) : v(0,0,0);
    const drive=travel(2,1);
    return (frame,phase)=>{
      const a=drive(frame,phase);
      if(id==='singlehinge') {
        const angle=.06+.98*a;
        const supportZ=top.z-.32;
        const freeLeg=.82+.065*a;
        const hip=v(.04,.984-.06*a,supportZ-.045-.10*a);
        return {
          hip,trunk:v(0,Math.cos(angle),Math.sin(angle)).multiplyScalar(.417),stillHead,
          foot:side=>side===1 ? v(.20,feet[1].flat,supportZ)
            : v(-.125,hip.y-freeLeg*Math.cos(angle),hip.z-freeLeg*Math.sin(angle)),
          knee:(side,joint,ankle)=>joint.clone().lerp(ankle,.5).add(v(0,0,.25)),
          wrist:side=>side===-1 ? v(-.225,top.y+.252,top.z+.045) : v(.30,.74-.34*a,top.z-.37),
          elbow:side=>v(side*.23,.83,top.z-.27),armContact:side=>side===-1,
          contact:side=>side===1, shoePitch:side=>side===1 ? 0 : .13+.31*a
        };
      }
      if(id==='singlecalf') {
        const pitch=.42*a;
        const [y,z]=feet[1].contact(pitch);
        const supportZ=top.z-.55+z;
        return {
          hip:v(.038,y+.905,supportZ),trunk:v(.055,.417,.027),stillHead,
          foot:side=>side===1 ? v(.195,y,supportZ) : v(-.145,y+.29,supportZ-.34),
          knee:side=>side===1 ? v(.195,y+.45,supportZ+.09) : v(-.125,y+.48,supportZ+.12),
          wrist:side=>side===-1 ? v(-.16,1.10,top.z-.05) : v(.36,.79+y,top.z-.56),
          elbow:side=>v(side*.24,.91,top.z-.28),armContact:side=>side===-1,
          contact:side=>side===1,
          shoePitch:side=>side===1 ? pitch : .27
        };
      }
      if(id==='ankle') {
        const pitch=-.27*a;
        const [y,z]=feet[1].contact(pitch);
        return {
          hip:v(0,top.y+.113,top.z+.145),trunk:v(0,.417,.025),stillHead,
          foot:side=>side===1 ? v(.178,y,top.z+.59+z) : flat(side,top.z+.37,.178),
          knee:side=>v(side*.185,top.y+.09,top.z+.46),
          arm:side=>[v(side*.10,-.65,.72),v(-side*.10,-.13,1)],
          shoePitch:side=>side===1 ? pitch : 0
        };
      }
      return {
        hip:v(0,.131,-.43),trunk:v(0,.40,-.10),stillHead,
        foot:side=>flat(side,.43,.145), knee:side=>v(side*.152,.19,.04),
        wrist:side=>v(side*.145,.35,.001),elbow:side=>v(side*.258,.274,-.25),armContact:()=>false
      };
    };
  }

  if (id==='wallsit') {
    const wall=clip.props.find(prop=>prop[0]==='wall');
    const z=wall ? map([wall[3],wall[2]]).z+.108 : 0;
    return ()=>{
      return {
        hip:v(0,.545,z),trunk:v(0,.417,0),stillHead,
        foot:side=>flat(side,z+.424),
        knee:side=>v(side*.19,.54,z+.411),
        arm:side=>[v(side*.11,-1,.11),v(0,-.95,.22)]
      };
    };
  }

  if (id==='singlebridge' || id==='dbbridge') {
    const drive=travel(2,1);
    return (frame,phase)=>{
      const a=drive(frame,phase);
      const rise=.206*a-.015;
      const run=Math.sqrt(.417**2-rise**2);
      const hip=v(0,.145+rise,-.415+run);
      return {
        hip,trunk:v(0,-rise,-run),stillHead,
        foot:side=>id==='singlebridge'&&side===-1 ? v(-.16,.61+.14*a,.49) : flat(side,.465,.165),
        knee:side=>id==='singlebridge'&&side===-1 ? v(-.16,.58+.15*a,.255) : v(side*.165,.55,.335),
        wrist:id==='dbbridge' ? side=>v(side*.091,hip.y+.249,hip.z+.02) : undefined,
        elbow:side=>v(side*.315,.14,hip.z-.14),
        arm:id==='singlebridge'?side=>[v(side*.12,-.225,1),v(0,-.085,1)]:undefined,
        armContact:()=>false,
        shoePitch:side=>id==='singlebridge'&&side===-1?-.24:0
      };
    };
  }
  return null;
}
