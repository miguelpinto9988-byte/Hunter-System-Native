package com.huntersystem.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/**
 * Monarca V43.12: bordes suaves en reposo y resaltados al pulsar o seleccionar.
 * El ancho cambia solo en la fase de dibujo: .8 dp -> 1.4 dp.
 */
@Composable
internal fun Modifier.monarchOutline(
 shape:Shape,
 active:Boolean=false,
 pressed:Boolean=false,
 focused:Boolean=false,
 enabled:Boolean=true
):Modifier{
 val c=LocalHunter.current
 val reduced=LocalMonarchMotion.current.reduced
 val emphasis=enabled && (active || pressed || focused)
 val target=when{
  !enabled->c.line.copy(alpha=.38f)
  emphasis->c.outlineAccent.copy(alpha=.96f)
  else->lerp(c.line,c.outlineAccent,.16f).copy(alpha=.68f)
 }
 val color by animateColorAsState(target,
  animationSpec=if(reduced)snap() else tween(170),label="monarca-outline-color")
 val width by animateDpAsState(if(emphasis)1.4.dp else .8.dp,
  animationSpec=if(reduced)snap() else tween(170),label="monarca-outline-width")
 return this.border(width,color,shape)
}

/**
 * Alternativa para tarjetas especiales que no utilizan HPanel.
 * Se conserva la accesibilidad y el efecto pulsado sin cambiar navegación.
 */
@Composable
internal fun Modifier.monarchPressable(
 shape:Shape,
 active:Boolean=false,
 role:Role=Role.Button,
 onClick:()->Unit
):Modifier{
 val interaction=remember{MutableInteractionSource()}
 val pressed by interaction.collectIsPressedAsState()
 return this.monarchOutline(shape,active=active,pressed=pressed)
  .clickable(interactionSource=interaction,indication=LocalIndication.current,
   role=role,onClick=onClick)
}
