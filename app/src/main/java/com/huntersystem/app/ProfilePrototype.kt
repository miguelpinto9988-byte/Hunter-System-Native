package com.huntersystem.app

import android.graphics.Color
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject
import java.io.ByteArrayInputStream

internal const val profilePrototypeUrl="https://profile.hunter.invalid/profile-monarca/index.html"
private const val profileHost="profile.hunter.invalid"

@Composable
internal fun ProfilePrototype(
 ui:NativeUi,
 registration:ProfileRegistration,
 handlesBack:Boolean,
 onSupplement:(String)->Unit
) {
 var webView by remember { mutableStateOf<WebView?>(null) }
 var saved by remember { mutableStateOf(false) }
 val avatar=ui.pendingAvatar?:ui.store.state.s("avatar")
 BackHandler(enabled=handlesBack&&ui.stack.isEmpty()) {
  webView?.evaluateJavascript("window.profileBack?.()",null)
 }
 AndroidView(
  factory={ context ->
   WebView(context).also { view ->
    val initial=registration.initialData()
    val localAssets=context.assets.list("profile-monarca").orEmpty()
     .filterNot { it.endsWith(".txt") }.map { "/profile-monarca/$it" }.toMutableSet()
    localAssets+=listOf("/ui33/companion-tusk.webp","/ui33/companion-dokja.webp",
     "/fonts/BarlowCondensed-SemiBold.ttf")
    val legacy=initial.o("legacyArtwork")
    localAssets+=listOf(legacy.s("scene"),legacy.s("figure"))
    view.setBackgroundColor(Color.rgb(4,10,20))
    view.settings.apply {
     javaScriptEnabled=true
     allowFileAccess=false
     allowContentAccess=false
     javaScriptCanOpenWindowsAutomatically=false
     setSupportMultipleWindows(false)
     domStorageEnabled=false
    }
    view.webViewClient=object:WebViewClient(){
     override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest)=true

     override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest):WebResourceResponse {
      val uri=request.url
      val path=uri.encodedPath
      if(uri.scheme!="https"||uri.authority!=profileHost||request.method!="GET"||path !in localAssets||
       (request.isForMainFrame&&uri.toString()!=profilePrototypeUrl))
       return unavailableProfileResource()
      val mime=when(path?.substringAfterLast('.')){
       "html"->"text/html"
       "css"->"text/css"
       "js"->"application/javascript"
       "ttf"->"font/ttf"
       "webp"->"image/webp"
       else->return unavailableProfileResource()
      }
      return runCatching {
       WebResourceResponse(mime,if(mime.startsWith("text/")||mime=="application/javascript")"UTF-8" else null,
        context.assets.open(path!!.removePrefix("/")))
      }.getOrElse { unavailableProfileResource() }
     }

     override fun onPageFinished(view:WebView,url:String) {
      if(url==profilePrototypeUrl) {
       val currentAvatar=ui.pendingAvatar?:ui.store.state.s("avatar")
       view.evaluateJavascript("window.profileAvatar?.(${JSONObject.quote(currentAvatar)})",null)
      }
     }
    }
    view.addJavascriptInterface(ProfileNativeActions(initial.toString(),view) { action,payload ->
     when(action){
      "photo"->if(!saved)ui.chooseAvatar()
      "exit"->if(registration.editing)ui.closeProfile()
      "enter"->if(saved)ui.profileSaved()
      "health"->if(!saved)onSupplement(action)
      "settings"->if(registration.editing&&!saved)onSupplement(action)
      "submit"->if(!saved){
       val result=runCatching { registration.submit(JSONObject(payload));saved=true }
       val message=result.exceptionOrNull()?.message?:if(result.isFailure)"No se pudo guardar el perfil." else ""
       view.evaluateJavascript("window.profileResult(${JSONObject.quote(message)})",null)
      }
     }
    },"HunterProfile")
    view.loadUrl(profilePrototypeUrl)
    webView=view
   }
  },
  modifier=Modifier.fillMaxSize(),
  onRelease={ view ->
   view.removeJavascriptInterface("HunterProfile")
   view.stopLoading()
   view.destroy()
  },
  update={ view -> view.evaluateJavascript("window.profileAvatar?.(${JSONObject.quote(avatar)})",null) }
 )
}

// The bridge is attached only to the packaged document. Persistent changes run on the UI thread.
internal class ProfileNativeActions(
 private val initialData:String,
 private val view:WebView,
 private val handle:(String,String)->Unit
) {
 @JavascriptInterface fun initial()=initialData
 @JavascriptInterface fun action(action:String){view.post { if(view.isAttachedToWindow)handle(action,"") }}
 @JavascriptInterface fun submit(payload:String){view.post { if(view.isAttachedToWindow)handle("submit",payload) }}
}

private fun unavailableProfileResource()=WebResourceResponse(
 "text/plain","UTF-8",404,"Not Found",emptyMap(),ByteArrayInputStream(ByteArray(0))
)
