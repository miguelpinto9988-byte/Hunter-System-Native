// Layout and interaction from the approved Monarca profile prototype.
const identityBackdrop="identity.webp";
const backdrop="training.webp";
const customizationBackdrop="appearance.webp";
const activationBackdrop="activation.webp";
const identityAvatar="avatar.webp";
const images={"Igris": "igris.webp", "Beru": "beru.webp", "Tank": "tank.webp", "Iron": "iron.webp", "scenes": ["scenes-1.webp", "scenes-2.webp", "scenes-3.webp", "scenes-4.webp", "scenes-5.webp"], "figures": ["figures-1.webp", "figures-2.webp", "figures-3.webp", "figures-4.webp", "figures-5.webp"], "Tusk": "../ui33/companion-tusk.webp", "Dokja": "../ui33/companion-dokja.webp"};
const labels={gender:'Género',exp:'Nivel',goal:'Objetivo principal',mode:'Modalidad',missions:'Cantidad de misiones',rest:'Descanso preferido',format:'Organización de la rutina',time:'Tiempo disponible',theme:'Tema del sistema',edge:'Color del borde',figure:'Silueta del cazador',scene:'Fondo de la tarjeta'};
const options={gender:['No especificado','Masculino','Femenino'],exp:['Básico','Intermedio','Avanzado'],goal:['Mejorar mi bienestar general','Desarrollar fuerza funcional','Mejorar mi resistencia','Mejorar mi movilidad','Crear un hábito de actividad física'],mode:['Misiones diarias','Rutinas'],missions:['1 misión',...Array.from({length:6},(_,i)=>(i+2)+' misiones')],rest:['30 segundos','60 segundos','90 segundos'],format:['Sesiones por rondas','Sesiones por series'],time:[10,15,20,30,45,60].map(x=>x+' minutos'),theme:['Blanco','Negro'],edge:['Azul','Morado','Rosa','Verde','Rojo','Blanco'],figure:['Sung Jinwoo','Cha Hae-In','Kim Dokja','Yoo Joonghyuk','Yoo Sangah'],scene:['Ruinas arcanas','Niebla violeta','Obsidiana','Aurora dorada','Bosque ancestral']};
const edges=['#258FE6','#9A68F2','#ED72B8','#34BC91','#E65260','#F2F5FF'];
const categories=[['Tren superior','↑','#258FE6'],['Tren central','◆','#9A68F2'],['Tren inferior','↓','#34BC91'],['Movilidad','≈','#168887'],['Cardio','♥','#D8732D'],['Descanso','–','#62738B']];
const dayNames=['Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo'];
const allies=['Igris','Beru','Tank','Tusk','Iron','Dokja'];
const equipment=['Peso corporal / sin equipo','Colchoneta o superficie acolchada estable','Pared o apoyo vertical firme','Silla estable','Mancuernas','Gimnasio'];
const gym=['Poleas','Máquinas guiadas','Banco','Barras','Bicicleta estática','Máquina de remo','Elíptica'];
const selections={name:'',age:'',height:'',weight:'',custom:'',weights:'',photo:'',companion:0,confirmed:false};
Object.keys(options).forEach(k=>selections[k]=0);
Object.assign(selections,{missions:2,rest:1,time:3});
const dayInitials="LMXJVSD";
const week=Array(7).fill(5), equipped=new Set([0]), gymSelected=new Set();
let step=1, editing=false, slide=0, error='';
const app=document.querySelector('#app');
const html=s=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const native=window.HunterProfile;
const nativeFields={name:'name',age:'age',height:'height',weight:'weight',custom:'finalGoal',weights:'dumbbellWeights'};
const nativeOptions={
 gender:['gender',['unspecified','male','female']], exp:['difficulty',['basic','intermediate','advanced']],
 goal:['objective',['general','strength','endurance','mobility','habit']], mode:['trainingMode',['challenges','routines']],
 missions:['dailyMissions',['1','2','3','4','5','6','7']], rest:['restSeconds',['30','60','90']],
 format:['blockMode22',['rounds','exercise']], time:['minutes',['10','15','20','30','45','60']],
 theme:['theme',['light','dark']], edge:['outlineColor',['ocean','purple','pink','green','red','white']],
 figure:['collection',['hunter-male','hunter-female','reader','regressor','reader-female']],
 scene:['scene',['templo-del-ocaso','biblioteca-lunar','trono-volcanico','reino-de-las-nubes','jardin-sagrado']],
 companion:['companionCharacter',['knight','insect','bear','tusk','warrior','scientist']]
};
const basicGear=['body','mat','wall','chair','db','gym'];
const gymGear=['gym_cable','gym_machine','gym_bench','gym_barbell','gym_bike','gym_rower','gym_elliptical'];
let pending=false;
if(native){
 const saved=JSON.parse(native.initial());
 editing=saved.editing;
 selections.photo=saved.photo;
 for(const [key,field] of Object.entries(nativeFields))selections[key]=saved.fields[field]??'';
 for(const [key,[field,ids]] of Object.entries(nativeOptions)){
  const id=saved.fields[field];
  let index=ids.indexOf(id);
  if(index<0&&['goal','edge','scene','figure'].includes(key)){
   index=ids.push(id)-1;
   options[key].push(saved.legacyLabels[key]);
   if(key==='edge')edges.push(saved.legacyArtwork.edge);
   if(key==='scene')images.scenes.push(saved.legacyArtwork.scene);
   if(key==='figure')images.figures.push(saved.legacyArtwork.figure);
  }
  if(index>=0)selections[key]=index;
 }
 week.splice(0,7,...saved.week);
 for(const [ids,set] of [[basicGear,equipped],[gymGear,gymSelected]]){
  set.clear();
  ids.forEach((id,i)=>{if(saved.gear[id])set.add(i);});
 }
}
const value=k=>options[k][selections[k]];
function submitProfile(){
 if(!native){changeStep(5);return;}
 const fields={};
 for(const [key,field] of Object.entries(nativeFields))fields[field]=selections[key];
 for(const [key,[field,ids]] of Object.entries(nativeOptions))fields[field]=ids[selections[key]];
 const gear={};
 basicGear.forEach((id,i)=>gear[id]=equipped.has(i));
 gymGear.forEach((id,i)=>gear[id]=gymSelected.has(i));
 pending=true;
 app.querySelector('[data-do=next]').disabled=true;
 native.submit(JSON.stringify({fields,week,gear,confirmed:selections.confirmed}));
}
window.profileResult=message=>{
 pending=false;
 if(!message){changeStep(5);return;}
 error=message;render();
 app.querySelector('[role=alert]')?.scrollIntoView({block:'nearest'});
};
window.profileAvatar=photo=>{
 if(photo!==selections.photo){selections.photo=photo;render();}
};
window.profileBack=()=>{
 const dialog=app.querySelector('dialog[open]');
 if(dialog){dialog.close();return;}
 if(app.querySelector('.inline-options')){closeInline(true);return;}
 if(pending)return;
 if(step===5)native?.action('enter');
 else if(step>1)changeStep(step-1);
 else if(editing)native?.action('exit');
};
const iconPaths={"person": "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM4 21a8 6 0 0 1 16 0", "calendar": "M4 6h16v15H4zM4 11h16M8 3v5m8-5v5m-8 7h2m4 0h2", "ruler": "M7 3h10v18H7zM7 7h5M7 12h3M7 17h5", "weight": "M4 21 6 8h12l2 13ZM9 8a3 3 0 1 1 6 0", "camera": "M3 7h5l2-3h4l2 3h5v13H3zM12 16a3 3 0 1 0 0-6 3 3 0 0 0 0 6z", "shield": "M12 3 4 6v5c0 5 3 8 8 10 5-2 8-5 8-10V6zM9 12l2 2 4-4", "bars": "M4 20h4v-5H4zM10 20h4v-9h-4zM16 20h4V5h-4z", "crown": "m3 7 5 5 4-7 4 7 5-5-2 12H5z", "heart": "M12 20 4 12C-1 6 7 2 12 8c5-6 13-2 8 4z", "dumbbell": "M3 10v4m4-7v10m10-10v10m4-7v4M7 12h10", "pulse": "M3 13h4l3-6 4 12 3-6h4", "move": "M17 5a2 2 0 1 0 0-.01M5 19l5-4 2-5m-7 1 4-3 4 1 4 4h4m-11 2 5 5", "check": "M5 4h14v16H5zm3 8 3 3 5-6", "flag": "M5 21V4m0 1 5-2 4 2 5-2v10l-5 2-4-2-5 2", "clock": "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Zm0-14v6l4 2", "cycle": "M18 8a7 7 0 1 0 1 8m0-10v5h-5", "pen": "m4 20 4-1L20 7l-3-3L5 16zM14 7l3 3", "frame": "M4 9V4h5m6 0h5v5m0 6v5h-5m-6 0H4v-5", "landscape": "M3 5h18v14H3zM4 17l5-5 4 4 3-7 4 8", "moon": "M13 3a9 9 0 1 0 8 12 8 8 0 0 1-8-12Z", "mat": "M4 18h14a3 3 0 1 0-3-3V6H7a3 3 0 0 0-3 3zM15 15a3 3 0 0 0 3 3", "wall": "M3 5h18v14H3zM3 10h18M3 14h18M9 5v5m6 0v4m-6 0v5", "chair": "M7 4h10v8H7zM5 16h14M7 12v9m10-9v9", "gym": "M3 9 12 3l9 6M5 8v12h14V8m-9 12v-7h4v7", "bike": "M4 17a3 3 0 1 0 0-.01m13 .01a3 3 0 1 0 0-.01M7 17l4-7 4 7H7m2-7h6m2-3 2 10", "arrow": "m9 5 7 7-7 7", "back": "m15 5-7 7 7 7"};
const iconTones={person:'sapphire',calendar:'violet',ruler:'jade',weight:'ruby',camera:'gold',shield:'jade',bars:'sapphire',crown:'gold',heart:'rose',dumbbell:'copper',pulse:'ruby',move:'turquoise',check:'jade',flag:'sapphire',clock:'amber',cycle:'turquoise',pen:'jade',frame:'gold',landscape:'jade',moon:'silver',mat:'violet',wall:'copper',chair:'amber',gym:'sapphire',bike:'turquoise'};
const artIcon=(name,tone=iconTones[name]||'')=>`<svg class="ui-icon ${tone||iconTones[name]||""}" viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.65" stroke-linecap="round" stroke-linejoin="round"><path d="${iconPaths[name]}"/></svg>`;
const fieldArt={name:'person',age:'calendar',height:'ruler',weight:'weight',weights:'weight'};
const tileArt={gender:'person',exp:'bars',goal:'heart',missions:'flag',rest:'clock',format:'cycle',time:'clock',theme:'moon',edge:'frame',figure:'person',scene:'landscape'};
const dayArt=['dumbbell','shield','move','cycle','pulse','moon'];
const gearArt={basic:['move','mat','wall','chair','dumbbell','gym'],gym:['cycle','gym','chair','dumbbell','bike','move','pulse']};
const optionArt={goal:['heart','dumbbell','pulse','move','calendar'],rest:['clock','clock','clock'],format:['cycle','bars'],time:Array(6).fill('clock'),missions:Array(7).fill('flag'),gender:Array(3).fill('person'),theme:['theme','moon'],edge:Array(6).fill('frame')};
optionArt.theme[0]='frame';
const tile=k=>`<button type="button" class="tile" data-picker="${k}" ${step===2&&["missions","time","rest","format","goal"].includes(k)?'aria-expanded="false"':'aria-haspopup="dialog"'}><strong>${artIcon(tileArt[k],k==="figure"?"violet":"")} ${labels[k]}</strong><small>${k==='figure'?'Elegir retrato':value(k)}</small><b aria-hidden="true">⌄</b></button>`;
const field=(k,label,placeholder='',type='text',extra='')=>`<label class="field"><span class="field-label">${artIcon(fieldArt[k],({age:"violet",height:"jade",weight:"ruby"})[k]||"")} ${label}</span><input data-field="${k}" type="${type}" value="${html(selections[k])}" placeholder="${placeholder}" ${extra}></label>`;
const check=(name,i,group,selected)=>`<label><input type="checkbox" data-gear="${group}" value="${i}" ${selected.has(i)?'checked':''}>${artIcon(gearArt[group][i])}<span>${name}</span></label>`;
function identity(){return `<div class="identity-fields">
 ${field('name','Nombre del cazador','Escribe tu nombre','text','maxlength="40" required')}
 ${field('age','Edad en años','Ingresa tu edad','number','min="0" step="1" required')}
 ${field('height','Estatura (cm) · Opcional','Ej. 175','number','min="0" step="any"')}
 ${field('weight','Peso (kg) · Opcional','Ej. 70','number','min="0" step="any"')}
 <div class="gender-block"><div class="field-label">${artIcon('person','violet')} Género</div><div class="gender-choices" role="group" aria-label="Género">
 ${[1,2,0].map(i=>`<button type="button" data-gender="${i}" aria-pressed="${selections.gender===i}" class="${selections.gender===i?'selection':''}">${artIcon(['shield','person','heart'][i],['silver','sapphire','rose'][i])}<span>${options.gender[i]}</span></button>`).join('')}
 </div></div>
 <div class="photo-row"><img class="portrait" alt="Avatar del cazador" src="${html(selections.photo||identityAvatar)}"><input type="file" id="photo" accept="image/*" hidden><button data-do="photo">${artIcon('camera','gold')} Elegir fotografía</button></div>
 <p class="identity-note">${artIcon('check','silver')} El nombre y la edad son obligatorios. La edad no limita el acceso a los ejercicios.</p>
 </div>`;}
function training(){return `<h2><svg class="ink-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zm0-5a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm0-4 7-7"/></svg> Nivel</h2><div class="level-row">${options.exp.map((label,i)=>`<button aria-pressed="${selections.exp===i}" data-level="${i}" class="${selections.exp===i?'selection':''}">${artIcon(['shield','bars','crown'][i],i===2?'gold':'')} ${label}</button>`).join('')}</div><h2>${artIcon('heart','violet')} Objetivo principal</h2>${tile('goal')}<h2>${artIcon('pen','jade')} Objetivo personalizado</h2><label class="field"><textarea aria-label="Objetivo personalizado" data-field="custom" placeholder="Describe tu objetivo con tus propias palabras">${html(selections.custom)}</textarea></label><h2><svg class="ink-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5 21V4m0 1 5-2 4 2 5-2v10l-5 2-4-2-5 2"/></svg> Modalidad del sistema</h2><div class="grid"><div class="grid mode-options" style="grid-column:1/-1">${options.mode.map((label,i)=>`<button data-mode="${i}" aria-pressed="${i===selections.mode}" class="${i===selections.mode?'selection':''}">${artIcon(i===0?'flag':'calendar')} ${label}</button>`).join('')}</div>${selections.mode===0?tile('missions'):tile('rest')}${selections.mode===1?tile('format')+tile('time'):''}</div>${selections.mode===0?'<p>Las misiones pueden incluir actividades ligeras, aprendizaje y descansos.</p>':''}<h2><svg class="ink-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 6h16v15H4zM4 11h16M8 3v5m8-5v5M8 15h2m4 0h2"/></svg> Planificación semanal</h2><p>Toca un día para recorrer las categorías en el orden de la leyenda.</p><div class="days" role="group" aria-label="Días de la semana">${dayNames.map((day,i)=>{const [name,icon,color]=categories[week[i]];return `<button data-day="${i}" aria-label="${day}: ${name}" style="--category:${color}"><strong>${dayInitials[i]}</strong><span aria-hidden="true">${artIcon(dayArt[week[i]])}</span></button>`}).join('')}</div><p class="day-update" aria-live="polite">${week.every(day=>day===5)?'Los siete días están en Descanso.':week.map((day,i)=>day===5?'':dayNames[i]+' · '+categories[day][0]).filter(Boolean).slice(-1)[0]}</p><div class="legend">${categories.map(([name,icon,color])=>`<span style="--category:${color}"><i>${artIcon(dayArt[categories.findIndex(c=>c[0]===name)])}</i>${name}</span>`).join('')}</div><h2><svg class="ink-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 10v4m4-7v10m10-10v10m4-7v4M7 12h10"/></svg> Equipamiento disponible</h2><div class="checks">${equipment.map((n,i)=>check(n,i,'basic',equipped)).join('')}</div>${equipped.has(4)?field('weights','Pesos de las mancuernas disponibles (kg), separados por comas','2, 4, 6'):''}${equipped.has(5)?`<div class="grid checks">${gym.map((n,i)=>check(n,i,'gym',gymSelected)).join('')}</div>`:''}`;}
function appearance(){return `<h2><svg class="ink-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 13a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM4 21a8 5 0 0 1 16 0"/></svg> Acompañante del sistema</h2><p>Desliza para ver todos los acompañantes.</p><div class="carousel" aria-label="Acompañantes">${allies.map((n,i)=>`<button data-companion="${i}" aria-label="Acompañante ${i+1}" aria-pressed="${selections.companion===i}" class="${selections.companion===i?'selection':''}">${images[n]?`<img alt="" src="${images[n]}">`:'<span class="placeholder" aria-hidden="true">◆</span>'}</button>`).join('')}</div><h2><svg class="ink-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 4H4v5m11-5h5v5m0 6v5h-5m-6 0H4v-5"/></svg> Apariencia</h2><div class="grid">${tile('theme')}${tile('edge')}${tile('figure')}${tile('scene')}</div><h2><svg class="ink-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 13a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM4 21a8 5 0 0 1 16 0"/></svg> Vista previa del cazador</h2><div class="preview" style="background-image:url(${images.scenes[selections.scene]})"><img alt="" src="${images.figures[selections.figure]}"><div class="preview-content"><strong>◆ SISTEMA MONARCA</strong><span class="preview-title">${html(selections.name||'Cazador')}</span><span class="preview-meta">${value('theme')} · ${value('edge')}<br>${value('scene')}</span></div>${selections.companion!==0?`<img class="companion-mark" alt="" src="${images[allies[selections.companion]]}">`:''}</div>`;}
function review(){
 const summary=(title,lines)=>`<details><summary>${artIcon(title==='Entrenamiento'?'dumbbell':title==='Personalización'?'frame':'person','gold')} ${title}</summary>${lines.map(line=>`<p>${html(line)}</p>`).join('')}</details>`;
 const activeGear=[...equipped].map(i=>equipment[i]);
 if(equipped.has(4)&&selections.weights.trim())activeGear.push(selections.weights+' kg');
 if(equipped.has(5))activeGear.push(...[...gymSelected].map(i=>gym[i]));
 const trainingValues=[
  'Nivel · '+value('exp'), value('goal'), selections.custom||'Objetivo personalizado sin indicar',
  value('mode')+' · '+(selections.mode===0?value('missions'):[value('rest'),value('format'),value('time')].join(' · ')),
  ...(selections.mode===0?['Descanso configurado · '+value('rest')]:[]),
  dayNames.map((day,i)=>day+' · '+categories[week[i]][0]).join(' | '),
  activeGear.join(', ')||'Sin equipo declarado'
 ];
 return `<h2><svg class="ink-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M6 3h12v18H6zM9 12l2 2 4-4"/></svg> Verificación del registro</h2>${summary('Identidad del cazador',[selections.name,selections.age+' años','Estatura · '+(selections.height?selections.height+' cm':'Sin indicar'),'Peso · '+(selections.weight?selections.weight+' kg':'Sin indicar'),value('gender')])}${summary('Entrenamiento',trainingValues)}${summary('Personalización',[value('theme'),value('edge'),'Acompañante elegido','Silueta elegida',value('scene')])}<label class="consent"><input type="checkbox" data-confirm ${selections.confirmed?'checked':''}>He revisado mi información y deseo activar mi perfil de cazador.</label>`;}
const titles=['Identidad del cazador','Entrenamiento','Personalización','Activación del sistema','Sistema activado','Inicio'];
function render(){
 app.dataset.dark=String(selections.theme===1); app.dataset.cardTheme=String(selections.theme);app.dataset.step=String(step);
 app.style.setProperty('--accent',selections.edge===5&&selections.theme===0?'#526582':edges[selections.edge]);
 const body=[identity,training,appearance,review,()=>`<div class="done"><i>◇</i><h2>${editing?'Cambios guardados':'Registro completado'}</h2><button class="primary" data-do="enter">ENTRAR AL SISTEMA</button></div>`,()=>`<h2>${html(selections.name)}, tu sistema está listo</h2><button data-do="edit">Editar perfil</button>`][step-1]();
 const scroll=app.querySelector('.frame-scroll')?.scrollTop||0;
 app.innerHTML=`<div class="wordmark"><span aria-hidden="true">✦</span><strong>Hunter System</strong><small>MONARCA ECLIPSE</small></div><div class="frame-body"><div class="frame-scroll"><header><div class='art'></div><div class="step-tag">${step<=4?"PASO "+step+" DE 4":""}</div><b><i>◆</i> HUNTER SYSTEM</b><h1>${titles[step-1]}</h1>${step<=4?`<div class="meter">${Array.from({length:4},(_,i)=>`<span class="${i+1===step?'on':''}">${i+1}</span>`).join('')}</div>`:''}</header><section>${body}${error?`<p class="error" role="alert">${html(error)}</p>`:''}</section></div>${step<=4?`<footer>${step>1?'<button data-do="back">‹ ATRÁS</button>':''}<button class="primary" data-do="next" ${step===4&&!selections.confirmed?'disabled':''}>${step<4?'SIGUIENTE ›':editing?'GUARDAR CAMBIOS':'ACTIVAR PERFIL'}</button></footer>`:''}</div><dialog aria-labelledby="picker-title"></dialog>`;
 app.querySelector('.frame-scroll').scrollTop=scroll;
 const section=app.querySelector('section');
 if(native&&step===2)section.insertAdjacentHTML('beforeend','<div class="profile-extras"><button data-do="health">Indicaciones y restricciones</button></div>');
 if(native&&editing&&step===3)section.insertAdjacentHTML('beforeend','<div class="profile-extras"><button data-do="settings">Ajustes de la aplicación</button></div>');
 if(step===2||step===3){
  let module;
  [...section.children].forEach(node=>{
   if(node.tagName==='H2'){
    module=document.createElement('div');module.className='module';
    section.insertBefore(module,node);
   }
   if(module)module.append(node);
  });
 }
 const track=app.querySelector('.carousel');if(track)track.scrollLeft=slide; const backgroundByStep=[identityBackdrop,backdrop,customizationBackdrop,activationBackdrop];app.style.backgroundImage='url('+backgroundByStep[Math.min(step,4)-1]+')';
}
function changeStep(target){if(pending)return;selections.confirmed=false;app.querySelector('.frame-scroll').scrollTop=0;step=target;error='';render();}
function closeInline(restoreFocus=false){
 const options=app.querySelector('.inline-options');
 if(!options)return;
 const trigger=options.previousElementSibling;
 options.remove();trigger.setAttribute('aria-expanded','false');trigger.removeAttribute('aria-controls');
 if(restoreFocus)trigger.focus({preventScroll:true});
}
app.addEventListener('keydown',e=>{if(e.key==='Escape'&&app.querySelector('.inline-options')){e.preventDefault();closeInline(true);}});
function picker(key,button){const dialog=app.querySelector('dialog');dialog.innerHTML=`<h2 id="picker-title">${labels[key]}</h2><div class="grid pick">${options[key].map((item,i)=>`<button data-choice="${i}" data-key="${key}" aria-label="${key==='figure'?'Silueta '+(i+1):item}" aria-pressed="${selections[key]===i}" class="${selections[key]===i?'selection':''}">${key==='scene'||key==='figure'?'<img alt="" src="'+images[key==='scene'?'scenes':'figures'][i]+'">':''}${key==='figure'?'':((optionArt[key]?.[i]?artIcon(optionArt[key][i]):'')+(selections[key]===i?'✓ ':'')+item)}${key==='edge'?' · '+edges[i]:''}</button>`).join('')}</div><button class="cancel" data-do="close">Cancelar</button>`;dialog.addEventListener('close',()=>button.focus(),{once:true});if(step===2&&['missions','time','rest','format','goal'].includes(key)){
 const existing=app.querySelector('.inline-options');
 const same=existing?.previousElementSibling===button;
 closeInline();
 if(same)return;
 const options=document.createElement('div');
 options.className='inline-options';
 options.id='training-options';
 options.setAttribute('role','group');
 options.setAttribute('aria-label',labels[key]);
 options.append(dialog.querySelector('.pick'));
 button.after(options);
 button.setAttribute('aria-expanded','true');
 button.setAttribute('aria-controls',options.id);
 options.querySelector('[aria-pressed="true"]')?.focus({preventScroll:true});
 return;
 }else dialog.showModal();dialog.querySelector('.selection')?.focus();}
app.addEventListener('input',event=>{const input=event.target;if(input.dataset.field)selections[input.dataset.field]=input.value;});
app.addEventListener('change',event=>{
 const input=event.target;
 if(input.matches('[data-confirm]')){selections.confirmed=input.checked;app.querySelector('[data-do=next]').disabled=!input.checked;return;}
 if(input.dataset.gear){const set=input.dataset.gear==='basic'?equipped:gymSelected;const item=Number(input.value);input.checked?set.add(item):set.delete(item);if(input.dataset.gear==='basic'&&item>=4){render();app.querySelector(`[data-gear=basic][value="${item}"]`).focus({preventScroll:true});}}
 if(!native&&input.id==='photo'&&input.files[0]){const reader=new FileReader();reader.onload=()=>{selections.photo=reader.result;render();};reader.readAsDataURL(input.files[0]);}
});
app.addEventListener('click',event=>{
 const b=event.target.closest('button');if(!b)return;
 if('gender' in b.dataset){selections.gender=Number(b.dataset.gender);render();app.querySelector('[data-gender="'+selections.gender+'"]').focus({preventScroll:true});return;} if('level' in b.dataset){selections.exp=Number(b.dataset.level);render();app.querySelector('[data-level="'+selections.exp+'"]').focus({preventScroll:true});return;} if('mode' in b.dataset){selections.mode=Number(b.dataset.mode); render();app.querySelector(`[data-mode="${selections.mode}"]`).focus({preventScroll:true});return;}
 if(b.dataset.picker){picker(b.dataset.picker,b);return;}
 if('choice'in b.dataset){const {key,choice}=b.dataset;selections[key]=Number(choice);slide=app.querySelector('.carousel')?.scrollLeft||slide;render();app.querySelector(`[data-picker="${key}"]`)?.focus({preventScroll:true});return;}
 if('day'in b.dataset){const i=Number(b.dataset.day);week[i]=(week[i]+1)%categories.length;const [label,icon,color]=categories[week[i]];b.style.setProperty('--category',color);b.querySelector('span').innerHTML=artIcon(dayArt[week[i]]);b.setAttribute('aria-label',dayNames[i]+': '+label); app.querySelector('.day-update').textContent=dayNames[i]+' · '+label;return;}
 if('companion'in b.dataset){slide=app.querySelector('.carousel').scrollLeft;selections.companion=Number(b.dataset.companion);render();app.querySelector(`[data-companion="${selections.companion}"]`).focus({preventScroll:true});return;}
 switch(b.dataset.do){
 case 'next':
  if(step===1&&(!selections.name.trim()||selections.age===''||!Number.isInteger(Number(selections.age))||Number(selections.age)<0)){error='Escribe tu nombre y una edad válida en años.';render();app.querySelector(!selections.name.trim()?'[data-field=name]':'[data-field=age]').focus();return;}
  if(step===1&&[selections.height,selections.weight].some(v=>v!==''&&!(Number(v)>0))){error='Revisa la estatura y el peso.';render();return;}
  if(step===2&&equipped.has(4)&&selections.weights.split(',').map(x=>x.trim()).filter(Boolean).some(x=>!Number.isFinite(Number(x))||Number(x)<=0)){error='Indica pesos positivos separados por comas.';render();return;}
  if(step===4){if(selections.confirmed&&!pending)submitProfile();return;}
  if(step===3)selections.confirmed=false;
  changeStep(step+1);break;
 case 'back':changeStep(step-1);break;
 case 'enter':if(native)native.action('enter');else changeStep(6);break;
 case 'edit':editing=true;changeStep(1);break;
 case 'photo':if(native)native.action('photo');else app.querySelector('#photo').click();break;
 case 'health':case 'settings':native?.action(b.dataset.do);break;
 case 'close':app.querySelector('dialog').close();break;
 }
});
render();
