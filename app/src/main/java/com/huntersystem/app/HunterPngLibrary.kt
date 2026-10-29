package com.huntersystem.app

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/** Los identificadores del perfil y de la navegación se mantienen intactos. */
private val iconToSvg = mapOf(
    "panel" to "home",
    "missions" to "quest",
    "routines" to "calendar",
    "exercises" to "dumbbell",
    "progress" to "chart",
    "profile" to "profile",
    "menu" to "sliders",
    "reminders" to "bell",
    "back" to "back",
    "settings" to "gear",
    "strength" to "dumbbell",
    "academy" to "book",
    "trophy" to "trophy",
    "cardio" to "heart",
    "start" to "play",
    "pause" to "pause",
    "success" to "check",
    "available" to "check",
    "locked" to "lock",
    "warning" to "warning",
    "timer" to "clock",
    "retry" to "refresh",
    "progress_bar" to "chart",
    "milestone" to "star",
    "streak" to "fire",
    "level" to "trophy",
    "xp" to "lightning",
    "goals" to "target",
    "crystal" to "crystal",
    "rune" to "crystal",
    "monarch_seal" to "crown",
    "portal" to "portal",
    "chest" to "chest",
    "insignia" to "shield",
    "special" to "swords",
    "next" to "next",
    "close" to "close",
    "add" to "add",
    "remove" to "remove",
    "edit" to "edit",
    "book" to "book",
    "training" to "dumbbell",
    "dumbbell" to "dumbbell",
    "focus" to "target",
    "energy" to "lightning",
    "lightning" to "lightning",
    "shield" to "shield",
    "achievement" to "trophy",
    "goal_strength" to "dumbbell",
    "goal_habit" to "checklist",
    "goal_read" to "book",
    "calendar" to "calendar",
    "history" to "clock",
    "rest" to "rest",
    "coach" to "chat",
    "search" to "search",
    "export" to "upload",
    "import" to "download",
    "notifications" to "bell",
    "play" to "play",
    "complete" to "check",
    "delete" to "trash",
    "back_arrow" to "back",
    "goal_finish" to "star",
    "rank" to "crown",
    "stats" to "chart",
    "library" to "book",
    "monarch" to "crown",
    "programs" to "checklist",
    "mobility" to "leaf",
    "week_upper" to "week_upper",
    "week_core" to "week_core",
    "week_lower" to "week_lower",
    "week_mobility" to "week_mobility",
    "week_rest" to "week_rest",
    "week_cardio" to "week_cardio"
)

/**
 * Íconos SOLIDO MINIMALISTA como VectorDrawable nativo.
 * Nada de carga asíncrona, Canvas Bitmap ni errores ignorados: Android
 * resuelve, mide y pinta el recurso en cada fotograma, incluso sin conexión.
 * Los SVG originales transparentes se conservan como fuentes editables.
 */
internal val hunterNativeVectorResources:Map<String,Int> = mapOf(
    "add" to R.drawable.hunter_icon_add,
    "back" to R.drawable.hunter_icon_back,
    "bell" to R.drawable.hunter_icon_bell,
    "book" to R.drawable.hunter_icon_book,
    "calendar" to R.drawable.hunter_icon_calendar,
    "chart" to R.drawable.hunter_icon_chart,
    "chat" to R.drawable.hunter_icon_chat,
    "check" to R.drawable.hunter_icon_check,
    "checklist" to R.drawable.hunter_icon_checklist,
    "chest" to R.drawable.hunter_icon_chest,
    "clock" to R.drawable.hunter_icon_clock,
    "close" to R.drawable.hunter_icon_close,
    "crown" to R.drawable.hunter_icon_crown,
    "crystal" to R.drawable.hunter_icon_crystal,
    "download" to R.drawable.hunter_icon_download,
    "dumbbell" to R.drawable.hunter_icon_dumbbell,
    "edit" to R.drawable.hunter_icon_edit,
    "fire" to R.drawable.hunter_icon_fire,
    "gear" to R.drawable.hunter_icon_gear,
    "heart" to R.drawable.hunter_icon_heart,
    "home" to R.drawable.hunter_icon_home,
    "leaf" to R.drawable.hunter_icon_leaf,
    "lightning" to R.drawable.hunter_icon_lightning,
    "lock" to R.drawable.hunter_icon_lock,
    "next" to R.drawable.hunter_icon_next,
    "pause" to R.drawable.hunter_icon_pause,
    "play" to R.drawable.hunter_icon_play,
    "portal" to R.drawable.hunter_icon_portal,
    "profile" to R.drawable.hunter_icon_profile,
    "quest" to R.drawable.hunter_icon_quest,
    "refresh" to R.drawable.hunter_icon_refresh,
    "remove" to R.drawable.hunter_icon_remove,
    "rest" to R.drawable.hunter_icon_rest,
    "search" to R.drawable.hunter_icon_search,
    "shield" to R.drawable.hunter_icon_shield,
    "sliders" to R.drawable.hunter_icon_sliders,
    "star" to R.drawable.hunter_icon_star,
    "swords" to R.drawable.hunter_icon_swords,
    "target" to R.drawable.hunter_icon_target,
    "trash" to R.drawable.hunter_icon_trash,
    "trophy" to R.drawable.hunter_icon_trophy,
    "upload" to R.drawable.hunter_icon_upload,
    "warning" to R.drawable.hunter_icon_warning,
    "week_cardio" to R.drawable.hunter_icon_week_cardio,
    "week_core" to R.drawable.hunter_icon_week_core,
    "week_lower" to R.drawable.hunter_icon_week_lower,
    "week_mobility" to R.drawable.hunter_icon_week_mobility,
    "week_rest" to R.drawable.hunter_icon_week_rest,
    "week_upper" to R.drawable.hunter_icon_week_upper
)

internal fun hunterIconDrawableId(key:String):Int {
    val graphic=iconToSvg[key] ?: "lightning"
    return hunterNativeVectorResources[graphic] ?: R.drawable.hunter_icon_lightning
}

/** API pública histórica para no alterar las pantallas ni las rutinas. */
@Composable
fun HunterPngIcon(
    key:String,
    modifier:Modifier=Modifier,
    contentDescription:String?=null
){
    Image(
        painter=painterResource(id=hunterIconDrawableId(key)),
        contentDescription=contentDescription,
        modifier=modifier,
        contentScale=ContentScale.Fit
    )
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
