package com.huntersystem.app

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale

/**
 * Biblioteca local de iconos bitmap PNG 128 x 128. Nunca genera iconos SVG
 * ni dibuja símbolos poligonales en tiempo de ejecución.
 */
private val iconKeys=setOf(
    "panel","missions","routines","exercises","progress","profile",
    "goals","academy","strength","cardio","mobility","book","level",
    "streak","achievement","timer","training","dumbbell","focus",
    "lightning","energy","shield","milestone","settings","trophy",
    "success","reminders","xp","goal_strength","goal_habit",
    "goal_read","warning","calendar","history","rest","coach",
    "focus","energy"
)

@Composable
fun HunterPngIcon(
    key:String, modifier:Modifier=Modifier, contentDescription:String?=null
){
    val chosen=key.takeIf{it in iconKeys} ?: "energy"
    Box(modifier, contentAlignment=Alignment.Center){
        AssetImage("hunter-png/icons/$chosen.png",Modifier.matchParentSize(),
            scale=ContentScale.Fit,description=contentDescription)
    }
}

/** Clave adaptativa basada en el contenido REAL de la rutina y sus categorías. */
fun hunterRoutinePngKey(title:String,category:String=""):String{
    val text=(title+" "+category).lowercase()
    return when{
        listOf("cardio","resistencia","correr","intervalo","aeróbic").any{it in text}->"cardio"
        listOf("movilidad","flexibilidad","estira","yoga","recupera").any{it in text}->"mobility"
        listOf("hábito","constancia","disciplina").any{it in text}->"goal_habit"
        listOf("brazo","pecho","fuerza","masa","muscular","peso","gimnasio","espalda").any{it in text}->"strength"
        listOf("descans","sueño").any{it in text}->"rest"
        else->"training"
    }
}

/** Clave de imagen que depende del título real de la meta, no de un estado inventado. */
fun hunterGoalPngKey(title:String):String{
    val x=title.lowercase()
    return when{
        listOf("leer","estudiar","libro","aprender","curso").any{it in x}->"goal_read"
        listOf("entren","muscul","fuerza","gimnas","peso").any{it in x}->"goal_strength"
        listOf("caminar","correr","cardio","resistencia").any{it in x}->"cardio"
        listOf("dormir","descans","meditar","hábito","agua","día","diario").any{it in x}->"goal_habit"
        else->"goals"
    }
}
