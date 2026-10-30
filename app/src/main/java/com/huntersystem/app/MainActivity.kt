package com.huntersystem.app

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class MainActivity:ComponentActivity(){
 private var ui:NativeUi?=null
 private val createBackup=registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->if(uri!=null)lifecycleScope.launch{runCatching{withContext(Dispatchers.IO){contentResolver.openOutputStream(uri,"wt")!!.use{it.write(ui!!.store.export().toByteArray())}}}.onSuccess{toast("Copia guardada")}.onFailure{toast("No se pudo exportar: ${it.message}")}}}
 private val openBackup=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)lifecycleScope.launch{runCatching{withContext(Dispatchers.IO){contentResolver.openInputStream(uri)!!.use{JSONObject(readBounded(it,40*1024*1024).toString(Charsets.UTF_8))}.let{ui!!.store.validateBackup(it)}}}.onSuccess{ui?.open("confirm-import",it)}.onFailure{toast("No se pudo leer la copia: ${it.message}")}}}
 private val pickAvatar=registerForActivityResult(ActivityResultContracts.GetContent()){uri->if(uri!=null)lifecycleScope.launch{runCatching{withContext(Dispatchers.IO){val bytes=contentResolver.openInputStream(uri)!!.use{readBounded(it,20*1024*1024)};val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds);require(bounds.outWidth>0&&bounds.outHeight>0);val sample=BitmapFactory.Options().apply{inSampleSize=(maxOf(bounds.outWidth,bounds.outHeight)/1024).coerceAtLeast(1)};val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size,sample)?:error("Imagen inválida");val scale=512f/maxOf(bitmap.width,bitmap.height);val result=if(scale<1)Bitmap.createScaledBitmap(bitmap,(bitmap.width*scale).toInt(),(bitmap.height*scale).toInt(),true) else bitmap;val out=ByteArrayOutputStream();result.compress(Bitmap.CompressFormat.PNG,100,out);if(result!==bitmap)result.recycle();bitmap.recycle();"data:image/png;base64,"+Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP)}}.onSuccess{ui?.pendingAvatar=it}.onFailure{toast("No se pudo abrir la imagen")}}}
 private val notifyPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(!granted)toast("Las alarmas necesitan permiso de notificaciones para mostrar sus avisos")}
 private fun toast(message:String){Toast.makeText(this,message,Toast.LENGTH_LONG).show()}
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  setContent{var repository by remember{mutableStateOf<NativeStore?>(null)};var error by remember{mutableStateOf<String?>(null)};LaunchedEffect(Unit){try{val s=withContext(Dispatchers.IO){HunterRepository.get(this@MainActivity).also{if(it.readError==null)it.refreshWeek()}};val controller=NativeUi(s);controller.message={toast(it)};controller.export={createBackup.launch("Hunter-System-Backup-${today()}.json")};controller.import={openBackup.launch(arrayOf("application/json","text/plain","application/octet-stream"))};controller.chooseAvatar={pickAvatar.launch("image/*")};controller.requestNotifications={if(android.os.Build.VERSION.SDK_INT>=33)notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)};controller.openUrl={url->runCatching{val uri=Uri.parse(url);require(uri.scheme=="https");startActivity(Intent(Intent.ACTION_VIEW,uri))}.onFailure{toast("No se pudo abrir el enlace")}};savedInstanceState?.getString("tab")?.let{if(it=="profile")controller.openProfile() else if(it in MonarchNavigation.routes&&s.profile!=null)controller.tab=it};savedInstanceState?.getString("library")?.let{if(s.profile!=null)controller.library=it};savedInstanceState?.getString("top")?.let{controller.top=it};if(savedInstanceState?.getBoolean("profileOpen")==true)controller.openProfile();ui=controller;repository=s;if(intent?.action==HunterAlarms.OPEN)controller.open("reminders");if(s.readError==null)HunterAlarms.sync(s);if(s.readError==null)CreatorFeedNative.schedule(this@MainActivity,s.state.b("creatorAuto",true))}catch(e:Exception){error=e.message?:"No se pudo abrir Hunter System"}}
 val s=repository;MonarchMotionProvider { if(s!=null){HunterTheme(s){HunterRoot(ui!!)}}else MonarchBootScreen(error){recreate()} }
 }}
 override fun onNewIntent(intent:Intent){super.onNewIntent(intent);setIntent(intent);if(intent.action==HunterAlarms.OPEN)ui?.open("reminders")}
 override fun onResume(){super.onResume();ui?.let{if(it.store.readError==null)runCatching{HunterAlarms.sync(it.store)}}}
 override fun onSaveInstanceState(outState:Bundle){ui?.let{outState.putString("tab",it.tab);outState.putString("library",it.library);outState.putString("top",it.top);outState.putBoolean("profileOpen",it.profileEditorOpen)};super.onSaveInstanceState(outState)}
}
@Composable
fun HunterRoot(ui:NativeUi){
    val store=ui.store
    store.revision.intValue
    val hasProfile=store.profile!=null
    // El perfil se dibuja fuera de las pestañas tanto al crear como al editar.
    val profileStandalone=mustShowStandaloneProfile(
        profileExists=hasProfile,
        editorRequested=ui.profileEditorOpen,
        readError=store.readError!=null
    )
    BackHandler(
        ui.stack.isNotEmpty() ||
        (hasProfile && (profileStandalone || ui.library.isNotBlank() || ui.tab!="home"))
    ){ui.back()}
    Scaffold(
        containerColor=androidx.compose.ui.graphics.Color.Transparent,
        bottomBar={
            if(hasProfile && !profileStandalone && store.readError==null)
                MonarchBottomNavigation(ui)
        }
    ){insets->
        Box(Modifier.fillMaxSize().padding(insets)){
            HunterFlatBackground(Modifier.matchParentSize())
            when {
                store.readError!=null -> MonarchRecoveryScreen(ui,store.readError!!)
                profileStandalone -> ProfileScreen(ui)
                else -> {
                    val saveableScreens=rememberSaveableStateHolder()
                    Box(Modifier.fillMaxSize()){
                        MonarchScreenTransition(ui.tab,Modifier.fillMaxSize()){route->
                            saveableScreens.SaveableStateProvider(route){
                                when(route){
                                    "home"->HomeScreen(ui)
                                    "plan"->PlanScreen(ui)
                                    "objectives"->ObjectivesScreen(ui)
                                    "routines"->RoutinesScreen(ui)
                                    "book"->ExercisesScreen(ui)
                                    "stats"->ProgressScreen(ui)
                                    else->HomeScreen(ui)
                                }
                            }
                        }
                        FloatingCompanion(
                            ui,
                            Modifier.align(Alignment.BottomEnd)
                                .padding(end=14.dp,bottom=12.dp)
                        )
                    }
                }
            }
        }
    }
    if(ui.stack.isNotEmpty())OverlayRoot(ui,ui.stack.last())
}

@Composable fun BottomNav(ui:NativeUi)=MonarchBottomNavigation(ui)
/** Fachada de navegación: render nativo configurado exclusivamente en Perfil. */
@Composable fun FloatingCompanion(ui:NativeUi,modifier:Modifier=Modifier) {
    MonarchFloatingCompanion(ui,modifier)
}
