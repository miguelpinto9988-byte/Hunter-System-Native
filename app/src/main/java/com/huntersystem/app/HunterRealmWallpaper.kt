package com.huntersystem.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Compatibilidad de rutas con las pantallas existentes; no selecciona imágenes de fondo. */
internal fun hunterRealmArtRoute(route:String):String {
    if (!route.startsWith("overlay-")) return when(route){
        "home","plan","routines","book","stats","objectives","profile","recovery"->route
        else->"profile"
    }
    return when(route.removePrefix("overlay-")){
        "exercise","session","video","series","body"->"book"
        "mission","mission-calendar","confirm-finish","record","review"->"plan"
        "routine","assign","routine-editor","custom"->"routines"
        "evolution","history"->"stats"
        "goal-edit","delete-goal"->"objectives"
        "coach","creator","academy","creator-refresh","reminders"->"profile"
        "pain"->"recovery"
        else->"home"
    }
}

/**
 * Capa base intencionadamente plana en TODAS las rutas, incluidas ventanas
 * superpuestas. Nunca debe cargar fotografías, banners, escenas ni PNG.
 * Las ilustraciones de personajes son componentes independientes.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun HunterRealmWallpaper(route:String, modifier:Modifier=Modifier) {
    Box(modifier.background(Color(0xFF070E1A)))
}
