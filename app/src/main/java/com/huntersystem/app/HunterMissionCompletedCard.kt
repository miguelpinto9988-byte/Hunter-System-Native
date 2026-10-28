package com.huntersystem.app

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Tarjeta de recompensa puramente visual. Nunca marca una sesión completada:
 * únicamente aparece cuando el registro de usuario confirma `complete=true`.
 */
@Composable
fun HunterMissionCompletedCard(ui:NativeUi,date:String){
 val log=ui.store.logs.o(date)
 if(!log.b("complete")) return
 val c=LocalHunter.current
 HPanel(padding=18){
  Row(verticalAlignment=Alignment.CenterVertically,
      horizontalArrangement=Arrangement.spacedBy(14.dp)){
   HunterPngIcon("success",Modifier.size(66.dp),"Misión completada")
   Column(Modifier.weight(1f)){
    Eyebrow("RECOMPENSA DESBLOQUEADA")
    Heading("¡Misión completada!",size=27)
    HText("Tu sesión del $date figura como completada en el registro local.",
      size=14,color=c.muted)
   }
  }
  HButton("VER MIS ESTADÍSTICAS  →",Modifier.fillMaxWidth(),primary=true){
    ui.go("stats")
  }
 }
}
