package com.huntersystem.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Fusiona la identidad Eclipse 41 con la pantalla Monarca 42 sin cambiar datos de entrenamiento. */
@Composable fun FusionEclipsePortal(){
    val c=LocalHunter.current
    Box(Modifier.fillMaxWidth().height(158.dp).clip(RoundedCornerShape(24.dp)).background(c.panel)){
        AssetImage("eclipse/portal.webp",Modifier.fillMaxSize(),description="Portal Eclipse")
        Box(Modifier.fillMaxSize().background(
            Brush.horizontalGradient(listOf(c.bg.copy(alpha=.96f),c.bg.copy(alpha=.79f),Color.Transparent))
        ))
        Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(.70f).padding(16.dp),
            verticalArrangement=Arrangement.spacedBy(4.dp)){
            Eyebrow("ECLIPSE × MONARCA")
            Heading("Tu próxima evolución",size=23)
            HText("Visuales nativos en Compose y Canvas",size=12,color=c.text)
        }
        // Reliquia geométrica 3D del diseño Eclipse, manejable mediante gestos.
        RelicScene(Modifier.align(Alignment.CenterEnd).size(116.dp),interactive=true)
    }
}
