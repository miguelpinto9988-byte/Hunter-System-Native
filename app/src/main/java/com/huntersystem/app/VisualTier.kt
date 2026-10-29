package com.huntersystem.app

/**
 * Device-wide rendering and companion-dialogue preferences.
 * An image resolution budget does not upscale the source or change display pixels.
 * Old appearance flags remain readable for backups, but this is the only quality control.
 */
enum class VisualTier(
 val id:String,
 val label:String,
 val imageEdge:Int,
 val stars:Int,
 val animated:Boolean,
 val floatingCompanion:Boolean
) {
 MAX("max","Máxima calidad",1080,30,true,true),
 BALANCED("balanced","Equilibrado",1080,12,true,true),
 SAVER("saver","Ahorro de energía",720,4,false,false);
 companion object {
  fun from(id:String?):VisualTier=entries.firstOrNull{it.id==id}?:BALANCED
 }
}

fun visualTier(store:NativeStore):VisualTier =
 VisualTier.from(store.state.s("visualQuality","balanced"))
