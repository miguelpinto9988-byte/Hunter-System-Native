package com.huntersystem.app

import android.graphics.Color
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import androidx.webkit.WebViewAssetLoader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

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

internal const val profilePrototypeUrl = "https://appassets.androidplatform.net/assets/profile-monarca/index.html"
private const val profileHost = "appassets.androidplatform.net"

/** Only the packaged profile document and its curated artwork are exposed to WebView. */
internal fun profileAllowedAssets(context: Context, legacy: JSONObject): Set<String> {
    val paths = context.assets.list("profile-monarca").orEmpty()
        .filter { it.substringAfterLast('.') in setOf("html", "js", "css", "ttf", "webp") }
        .mapTo(mutableSetOf()) { "/assets/profile-monarca/$it" }
    paths.addAll(listOf(
        "/assets/ui33/companion-tusk.webp",
        "/assets/ui33/companion-dokja.webp",
        "/assets/fonts/BarlowCondensed-SemiBold.ttf"
    ))
    for (path in listOf(legacy.s("scene"), legacy.s("figure"))) {
        if ((path.startsWith("/assets/ui33/") || path.startsWith("/assets/ui34/"))
            && !path.contains("..") && path.endsWith(".webp")) {
            paths.add(path)
        }
    }
    return paths
}

internal fun profileAssetLoader(context: Context): WebViewAssetLoader =
    WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .build()

internal fun profileAssetResponse(
    url: Uri, paths: Set<String>, assetLoader: WebViewAssetLoader
): WebResourceResponse {
    val path = url.encodedPath ?: return unavailableProfileResource()
    if (url.scheme != "https" || url.authority != profileHost || url.query != null ||
        path !in paths) return unavailableProfileResource()
    return assetLoader.shouldInterceptRequest(url) ?: unavailableProfileResource()
}

@Composable
internal fun ProfilePrototype(
    ui: NativeUi,
    registration: ProfileRegistration,
    handlesBack: Boolean,
    onSupplement: (String) -> Unit
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var saved by remember { mutableStateOf(false) }
    var pageReady by remember { mutableStateOf(false) }
    var loadIssue by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    // Prefer a native profile on first launch: some Android WebView renderers crash
    // before onRenderProcessGone can deliver a callback.
    var nativeFallback by remember { mutableStateOf(true) }
    var visualRecovery by remember { mutableStateOf(false) }
    val avatar = ui.pendingAvatar ?: ui.store.state.s("avatar")

    BackHandler(enabled = handlesBack && ui.stack.isEmpty()) {
        webView?.evaluateJavascript("window.profileBack?.()", null)
    }
    LaunchedEffect(attempt, pageReady, loadIssue) {
        if (!pageReady && loadIssue == null) {
            delay(12000)
            if (!pageReady && loadIssue == null)
                loadIssue = "El perfil no respondió. Comprueba que Android System WebView esté actualizado."
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (loadIssue == null && !nativeFallback) key(attempt) {
            AndroidView(
                factory = { context ->
                    WebView(context).also { view ->
                        val currentAttempt = attempt
                        val initial = registration.initialData()
                        val allowed = profileAllowedAssets(context, initial.o("legacyArtwork"))
                        val loader = profileAssetLoader(context)
                        view.setBackgroundColor(android.graphics.Color.rgb(4, 10, 20))
                        view.visibility = View.INVISIBLE // Keep WebView offscreen until rendered DOM is verified.
                        view.settings.apply {
                            javaScriptEnabled = true
                            allowFileAccess = false
                            allowContentAccess = false
                            javaScriptCanOpenWindowsAutomatically = false
                            setSupportMultipleWindows(false)
                            domStorageEnabled = false
                        }
                        view.webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                                if (message.messageLevel() == ConsoleMessage.MessageLevel.ERROR)
                                    Log.e("HunterProfile", "JavaScript ${message.sourceId()}:${message.lineNumber()}: ${message.message()}")
                                return true
                            }
                        }
                        view.webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView, request: WebResourceRequest
                            ): Boolean = request.url.toString() != profilePrototypeUrl

                            override fun shouldInterceptRequest(
                                view: WebView, request: WebResourceRequest
                            ): WebResourceResponse = profileAssetResponse(request.url, allowed, loader)

                            override fun onReceivedError(
                                view: WebView, request: WebResourceRequest, error: WebResourceError
                            ) {
                                if (request.isForMainFrame) view.post {
                                    if (currentAttempt == attempt)
                                        loadIssue = "No se pudo cargar el perfil local (WebView ${error.errorCode})."
                                }
                            }

                            override fun onReceivedHttpError(
                                view: WebView, request: WebResourceRequest,
                                errorResponse: WebResourceResponse
                            ) {
                                if (request.isForMainFrame) view.post {
                                    if (currentAttempt == attempt)
                                        loadIssue = "No se encontró la página del perfil en la APK (HTTP ${errorResponse.statusCode})."
                                }
                            }

                            override fun onRenderProcessGone(
                                view: WebView, detail: RenderProcessGoneDetail
                            ): Boolean {
                                // Renderer crashes can leave an empty WebView surface.
                                // The Compose form does not need the WebView process.
                                if (currentAttempt == attempt) {
                                    nativeFallback = true
                                    visualRecovery = true
                                    loadIssue = null
                                }
                                return true
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                if (url != profilePrototypeUrl || currentAttempt != attempt) return
                                val currentAvatar = ui.pendingAvatar ?: ui.store.state.s("avatar")
                                view.evaluateJavascript(
                                    "window.profileAvatar?.(${JSONObject.quote(currentAvatar)})", null
                                )
                                // A loaded empty HTML shell is not a successful profile screen.
                                view.evaluateJavascript(
                                    "(() => { const a=document.querySelector('#app'); return !!(a && a.childElementCount > 0 && a.innerText && a.innerText.trim().length > 30 && a.getBoundingClientRect().height > 40) })()"
                                ) { result ->
                                    if (currentAttempt == attempt && loadIssue == null) {
                                        if (result == "true") pageReady = true
                                        else loadIssue = "El perfil se abrió, pero JavaScript no generó la interfaz."
                                    }
                                }
                            }
                        }
                        view.addJavascriptInterface(ProfileNativeActions(initial.toString(), view) { action, payload ->
                            when (action) {
                                "photo" -> if (!saved) ui.chooseAvatar()
                                "exit" -> if (registration.editing) ui.closeProfile()
                                "enter" -> if (saved) ui.profileSaved()
                                "health" -> if (!saved) onSupplement(action)
                                "settings" -> if (registration.editing && !saved) onSupplement(action)
                                "submit" -> if (!saved) {
                                    val result = runCatching {
                                        registration.submit(JSONObject(payload))
                                        saved = true
                                    }
                                    val message = result.exceptionOrNull()?.message
                                        ?: if (result.isFailure) "No se pudo guardar el perfil." else ""
                                    view.evaluateJavascript(
                                        "window.profileResult(${JSONObject.quote(message)})", null
                                    )
                                }
                            }
                        }, "HunterProfile")
                        webView = view
                        view.loadUrl(profilePrototypeUrl)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = { view ->
                    if (webView === view) webView = null
                    runCatching { view.removeJavascriptInterface("HunterProfile") }
                    runCatching { view.stopLoading() }
                    runCatching { view.destroy() }
                },
                update = { view ->
                    view.visibility = if (pageReady && loadIssue == null) View.VISIBLE else View.INVISIBLE
                    if (pageReady && loadIssue == null) view.evaluateJavascript(
                        "window.profileAvatar?.(${JSONObject.quote(avatar)})", null
                    )
                }
            )
        }

        if (nativeFallback) {
            ProfileNativeFallback(ui, registration, visualRecovery, onRetry = {
                nativeFallback = false
                pageReady = false
                loadIssue = null
                attempt++
            })
        } else if (!pageReady || loadIssue != null) {
            Column(
                Modifier.fillMaxSize()
                    .background(ComposeColor(0xFF040A14))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (loadIssue == null) {
                    CircularProgressIndicator(color = ComposeColor(0xFF25C8FF))
                    Spacer(Modifier.height(20.dp))
                    Text("Cargando perfil del cazador...", color = ComposeColor.White)
                    Spacer(Modifier.height(12.dp))
                    Text("Hunter System 43.12.1", color = ComposeColor(0xFFAEC6DF))
                } else {
                    Text("NO SE PUDO ABRIR EL PERFIL", color = ComposeColor(0xFFF3B0B8))
                    Spacer(Modifier.height(12.dp))
                    Text(loadIssue!!, color = ComposeColor.White)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = {
                        pageReady = false
                        loadIssue = null
                        attempt++
                    }) { Text("Reintentar") }
                }
            }
        }
    }
    // Separate Android dialog window: visible even if a WebView uses a hardware layer
    // above the Compose content on a particular device.
    if (loadIssue != null && !nativeFallback) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Error al iniciar el perfil") },
            text = { Text(loadIssue ?: "No se pudo cargar el perfil local.") },
            confirmButton = {
                TextButton(onClick = {
                    pageReady = false
                    loadIssue = null
                    attempt++
                }) { Text("Reintentar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    nativeFallback = true
                    visualRecovery = true
                    loadIssue = null
                }) { Text("Abrir perfil nativo") }
            }
        )
    }
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
