package com.huntersystem.app

import org.json.JSONObject

enum class DialogueMoment { WELCOME, EXERCISE, SERIES, SURVEY, COMPLETED, GUIDE }
data class DialogueContext(
 val moment:DialogueMoment,
 val group:String="",
 val exercise:String="",
 val set:Int=0,
 val sets:Int=0,
 val target:Int=0,
 val seconds:Boolean=false,
 val cue:String="",
 val rpe:Int=0,
 val fatigue:Boolean=false
)

/**
 * Local, deterministic dialogue composer. Does not call an online model or invent
 * a measurement. Its input is the actual mission/exercise and saved RPE.
 */
object CompanionDialogue {
 private fun character(profile:JSONObject?)=companionKey(profile)
 private fun choose(date:String,context:DialogueContext,first:String,second:String):String {
  val seed=(date+context.moment.name+context.group+context.exercise).hashCode().ushr(1)
  return if(seed%2==0)first else second
 }
 private fun shortLine(id:String,moment:DialogueMoment):String=when(id){
  "igris"->when(moment){
   DialogueMoment.WELCOME->"Cazador, comenzamos con disciplina."
   DialogueMoment.EXERCISE->"Mantén el control de cada repetición."
   DialogueMoment.SERIES->"Registra la serie con precisión."
   DialogueMoment.SURVEY->"Evalúa el esfuerzo con sinceridad."
   DialogueMoment.COMPLETED->"Misión cumplida. Descansa."
   DialogueMoment.GUIDE->"La técnica es prioritaria."
  }
  "iron"->when(moment){
   DialogueMoment.WELCOME->"¡Vamos, equipo! A trabajar."
   DialogueMoment.EXERCISE->"¡Firme y con buena técnica!"
   DialogueMoment.SERIES->"¡Una serie más registrada!"
   DialogueMoment.SURVEY->"¡Cuéntame cuánto te exigió!"
   DialogueMoment.COMPLETED->"¡Buen trabajo, a recuperar!"
   DialogueMoment.GUIDE->"¡Avanza sin perder la forma!"
  }
  "tank"->when(moment){
   DialogueMoment.WELCOME->"Estoy aquí. Vamos a tu ritmo."
   DialogueMoment.EXERCISE->"Muévete con calma y estabilidad."
   DialogueMoment.SERIES->"Buen momento para revisar cómo estás."
   DialogueMoment.SURVEY->"Escucha tu cuerpo y responde."
   DialogueMoment.COMPLETED->"Ahora toca recuperarte."
   DialogueMoment.GUIDE->"Prioriza comodidad y control."
  }
  "beru"->when(moment){
   DialogueMoment.WELCOME->"¡El desafío nos reclama, cazador!"
   DialogueMoment.EXERCISE->"¡Concentra tu energía en el gesto!"
   DialogueMoment.SERIES->"¡La serie está en tus manos!"
   DialogueMoment.SURVEY->"¡Revela el esfuerzo de esta batalla!"
   DialogueMoment.COMPLETED->"¡Victoria! La recuperación también cuenta."
   DialogueMoment.GUIDE->"¡Poder con técnica, no con prisa!"
  }
  else->when(moment){
   DialogueMoment.WELCOME->"Revisemos qué toca hoy."
   DialogueMoment.EXERCISE->"Observa la secuencia y tu ejecución."
   DialogueMoment.SERIES->"Anotemos lo que hiciste realmente."
   DialogueMoment.SURVEY->"Tu valoración ayudará a revisar el plan."
   DialogueMoment.COMPLETED->"Sesión registrada. Revisaremos los datos."
   DialogueMoment.GUIDE->"Comparemos lo previsto con tus registros."
  }
 }
 fun message(profile:JSONObject?,tier:VisualTier,context:DialogueContext,date:String=today()):String {
  val id=character(profile)
  val short=shortLine(id,context.moment)
  if(tier==VisualTier.SAVER)return short
  val group=context.group.takeIf{it in GroupEvolution.families}?.let{GroupEvolution.label(it)}
  val difficulty=when(profile?.s("difficulty")){
   "basic"->"nivel básico";"intermediate"->"nivel intermedio";"advanced"->"nivel avanzado";else->"tu nivel actual"
  }
  val isStrain=context.fatigue||context.rpe>=8
  val exercise=context.exercise.take(110)
  val detail=when(context.moment){
   DialogueMoment.WELCOME -> if(group!=null)
    "Hoy corresponde "+group.lowercase()+", con actividades compatibles con "+difficulty+"."
    else "Revisa el calendario y elige el entrenamiento que tengas pendiente."
   DialogueMoment.EXERCISE -> {
    val dose=if(context.target>0)" El objetivo mostrado es "+context.target+
     (if(context.seconds)" segundos." else " repeticiones.") else ""
    if(exercise.isBlank())"Sigue las instrucciones de la sesión."+dose
    else "Ejercicio actual: "+exercise+"."+dose
   }
   DialogueMoment.SERIES -> {
    val order=if(context.set>0&&context.sets>0)"Serie "+context.set+" de "+context.sets+". " else ""
    order+(if(exercise.isNotBlank())"Estás registrando "+exercise+". " else "")+
     "Anota únicamente la cantidad y carga que realizaste."
   }
   DialogueMoment.SURVEY -> (if(group!=null)"Has trabajado "+group.lowercase()+". " else "")+
    "Valora del 1 al 10 cuánto te costó la sesión; este registro se utilizará para revisar la próxima sesión del mismo grupo."
   DialogueMoment.COMPLETED -> (if(group!=null)"Se registró "+group.lowercase()+". " else "")+
    (if(context.rpe in 1..10)"Tu esfuerzo declarado fue "+context.rpe+" de 10. " else "")+
    "La próxima planificación consultará ese dato y los registros anteriores."
   DialogueMoment.GUIDE -> (if(group!=null)"Tu grupo de referencia es "+group.lowercase()+". " else "")+
    "Consulta el ejercicio, tu nivel y los registros antes de valorar cambios."
  }
  val attention=when{
   isStrain->"Has registrado un esfuerzo elevado o fatiga; prioriza la recuperación y revisa cualquier molestia."
   context.moment==DialogueMoment.EXERCISE&&context.cue.isNotBlank()->
    "Punto técnico de la ficha: "+context.cue.take(155)+"."
   context.moment==DialogueMoment.SERIES->"No es necesario igualar una cifra prevista si la ejecución se deteriora."
   context.moment==DialogueMoment.SURVEY->"Responde según lo que sentiste, sin cambiar la valoración para obtener más dificultad."
   else->"Si aparece dolor nuevo o aumenta una molestia, detén la actividad y revisa tus restricciones."
  }
  val opening=when(id){
   "igris"->choose(date,context,"Orden y constancia.","El progreso exige control.")
   "iron"->choose(date,context,"¡Escudo firme, energía lista!","¡Vamos con determinación!")
   "tank"->choose(date,context,"Cuento contigo, paso a paso.","No hace falta apresurarse.")
   "beru"->choose(date,context,"¡Que el desafío sea digno!","¡Observemos tu avance, cazador!")
   else->choose(date,context,"Pensemos en el siguiente dato útil.","Podemos interpretar el contexto antes de avanzar.")
  }
  if(tier==VisualTier.BALANCED)return short+" "+detail+
   (if(isStrain)" "+attention else "")
  val closing=when(id){
   "igris"->"Mantén la ejecución deliberada y registra los resultados con precisión."
   "iron"->"¡El objetivo es entrenar bien y llegar preparado a la siguiente misión!"
   "tank"->"Tu ritmo y tu descanso también forman parte del progreso."
   "beru"->"¡La auténtica victoria consiste en dominar cada movimiento!"
   else->"Distingamos esfuerzo registrado de progreso demostrado; las próximas sesiones se compararán con el historial de ese grupo."
  }
  return listOf(opening,detail,attention,closing).joinToString(" ")
 }
}
