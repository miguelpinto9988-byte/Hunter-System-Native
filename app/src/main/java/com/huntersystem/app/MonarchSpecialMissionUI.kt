package com.huntersystem.app

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier

/** Los desafíos anteriores conservan sus resultados y pueden cerrarse desde el Panel. */
@Composable
internal fun LegacyMonarchMissionPanel(ui:NativeUi) {
    val store=ui.store
    store.revision.intValue
    val legacy=store.state.optJSONObject("monarchSpecialMission")?.takeIf { it.s("status")=="active" }
    val planning=store.state.optJSONObject("monarcaPlanningV2")?:store.state.o("asterionV1")
    val participation=planning.a("monarchCompletion").objects()
    if(legacy==null&&participation.isEmpty())return
    var expanded by rememberSaveable { mutableStateOf(false) }
    HPanel(decor=false,padding=12){
        HButton(if(expanded) "Ocultar desafíos anteriores" else "Ver desafíos anteriores",Modifier.fillMaxWidth()){
            expanded=!expanded
        }
        if(expanded){
            legacy?.let { run ->
                val mission=runCatching { MonarchMissionCatalog.fromRun(run) }.getOrNull()
                Heading(mission?.title?:"Desafío anterior",size=22)
                val counts=run.o("counts").entries()
                if(counts.isEmpty()) HText("Sin resultados declarados.")
                for((id,count) in counts) HText("${store.catalog.byId[id]?.name?:id} · $count")
                HButton("Cerrar desafío anterior",Modifier.fillMaxWidth()){
                    ui.attempt {
                        val now=System.currentTimeMillis()
                        val status=MonarchMissionCatalog.status(run,now)
                        run.put("status",if(status=="expired") "expired" else "stopped").put("finishedAt",now)
                        store.save()
                    }
                }
            }
            for(row in participation.asReversed()){
                val id=row.s("id")
                HText("${row.s("date")} · ${store.catalog.byId[id]?.name?:id} · Participación declarada",size=14)
            }
        }
    }
}
