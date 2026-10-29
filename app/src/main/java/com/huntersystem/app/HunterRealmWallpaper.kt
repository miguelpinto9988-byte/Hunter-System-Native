package com.huntersystem.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Wallpapers originales para las pantallas reales de Hunter System.
 *
 * Utiliza solo PNG rasterizados, totalmente locales; sin SVG ni Canvas.
 * Se sitúa DETRÁS de Compose: nunca modifica clicks, datos, ni desplazamiento.
 * Los paisajes son estáticos, respetando de forma natural movimiento reducido.
 */
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
 * Fondo global uniforme: sin wallpapers ilustrados en ninguna ruta de la aplicación.
 * Los personajes solo aparecen en los componentes de selección y evolución.
 */
@Composable
fun HunterRealmWallpaper(route:String, modifier:Modifier=Modifier) {
    Box(modifier.background(Brush.verticalGradient(
        listOf(Color(0xFF050916),Color(0xFF0A1426),Color(0xFF050916))
    )))
}
