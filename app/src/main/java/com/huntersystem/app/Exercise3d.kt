package com.huntersystem.app

import android.content.Context
import java.io.ByteArrayInputStream
import android.view.MotionEvent
import android.view.View
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

internal fun exercise3dClip(id: String): String? = when (id) {
    "performance-db-step-up" -> "dumbbells"
    "stepup" -> "bodyweight"
    "stepup_slow" -> "slow"
    "stepup_pause" -> "pause"
    else -> null
}

/** Sample the same pose interpolation used by the native demonstration. */
internal fun projectExercise3d(motions: List<ExerciseMotion>, exerciseId: String? = null): ByteArray =
    arr(motions.map { motion ->
        val framing = ExerciseFraming.from(motion)
        val count = if (motion.staticHold) 1 else (motion.duration * 24).toInt().coerceIn(12, 240)
        val phases = List(count) { it.toFloat() / count }
        val poses = phases.map(motion::at)
        val equipment = phases.zip(poses).map { (phase, pose) ->
            framing.equipment.frame(pose, phase)
        }
        obj(
            "duration" to motion.duration,
            "exerciseId" to exerciseId,
            "view" to motion.view.name,
            "posture" to motion.posture.name,
            "facing" to motion.facing,
            "hold" to motion.staticHold,
            "suspended" to motion.suspended,
            "handSupport" to motion.handSupport,
            "planted" to when (exerciseId) {
                "squat", "squat_slow", "squat_pause" -> "squat"
                "pushup", "pushup_slow", "pushup_pause" -> "pushup"
                "plank", "plank_easy", "plank_short" -> "plank"
                "mountain", "mountain_easy", "mountain_short" -> "mountain"
                "bridge", "bridge_slow", "bridge_pause", "bridgehold", "bridge-control18" -> "bridge"
                "calf", "calf_slow", "calf_pause", "calfhold", "calf-control18" -> "calf"
                "reverselunge", "reverselunge_slow", "reverselunge_pause" -> "reverse-lunge"
                "split", "split_slow", "split_pause", "dbsplit", "dbsplit_slow", "dbsplit_pause",
                "splitpause", "split-control18", "split-15-19" -> "split-squat"
                else -> null
            },
            "gear" to arr(motion.apparatus.map { arr(listOf(it.gear.name, it.mount.name)) }),
            "scale" to (.417f / (framing.unit * 165f)),
            "times" to arr(phases),
            "frames" to arr(poses.map { pose -> arr(pose.map { arr(listOf(it.x, it.y)) }) }),
            "props" to arr(framing.equipment.authoredSupports.map {
                arr(listOf(it.kind, it.bounds.left, it.bounds.top, it.bounds.right, it.bounds.bottom))
            }),
            "equipment" to arr(equipment.map { frame ->
                obj(
                    "sprites" to arr(frame.sprites.map {
                        arr(listOf(it.asset, it.center.x, it.center.y, it.size.width, it.size.height,
                            it.angle, it.foreground))
                    }),
                    "links" to arr(frame.links.map {
                        arr(listOf(it.start.x, it.start.y, it.end.x, it.end.y,
                            it.stroke.width * framing.unit, it.bend?.x, it.bend?.y, it.stroke.name))
                    })
                )
            }),
        )
    }).toString().toByteArray(Charsets.UTF_8)

@Composable
internal fun InteractiveExercise3d(
    clip: String,
    projection: ByteArray?,
    play: Boolean,
    mirror: Boolean,
    reset: Int,
    onReady: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val owner = LocalLifecycleOwner.current
    var foreground by remember(owner) {
        mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    var view by remember { mutableStateOf<ExerciseWebView?>(null) }
    DisposableEffect(owner, view) {
        val observer = LifecycleEventObserver { _, _ ->
            foreground = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (foreground) view?.onResume() else view?.onPause()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    AndroidView<View>(
        modifier = modifier,
        factory = { context ->
            try {
                ExerciseWebView(context, clip, projection, onReady).also { view = it }
            } catch (_: RuntimeException) {
                View(context).also { placeholder -> placeholder.post { onReady(false) } }
            }
        },
        onRelease = { (it as? ExerciseWebView)?.release() },
        update = { (it as? ExerciseWebView)?.playback(play && foreground, mirror, reset) }
    )
}

private class ExerciseWebView(
    context: Context,
    clip: String,
    projection: ByteArray?,
    private val onReady: (Boolean) -> Unit
) : WebView(context) {
    private data class Playback(val play: Boolean, val mirror: Boolean, val reset: Int)

    private var state = Playback(false, false, 0)
    private var sent: Playback? = null
    private var ready = false
    private var disposed = false
    private var checks = 0
    private var failed = false
    private val loadTimeout = Runnable { if (!ready) useFallback() }

    private val probe = object : Runnable {
        override fun run() {
            if (disposed || failed) return
            evaluateJavascript("document.getElementById('viewport')?.dataset.ready") { result ->
                if (disposed || failed) return@evaluateJavascript
                when {
                    result == "\"true\"" -> {
                        ready = true
                        removeCallbacks(loadTimeout)
                        alpha = 1f
                        sendPlayback()
                        onReady(true)
                    }
                    result == "\"failed\"" || ++checks >= 80 -> useFallback()
                    else -> postDelayed(this, 125)
                }
            }
        }
    }

    init {
        alpha = 0f
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        settings.javaScriptEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = true

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                val uri = request.url
                val asset = uri.lastPathSegment
                if (uri.scheme == "https" && uri.host == HOST && asset != null) {
                    if (uri.path == "/motion.json" && projection != null) {
                        return WebResourceResponse("application/json", "UTF-8", ByteArrayInputStream(projection))
                    }
                    if (uri.path == "/equipment/$asset" && equipmentAsset.matches(asset)) {
                        try {
                            return WebResourceResponse("image/svg+xml", "UTF-8",
                                context.assets.open("exercise-actor/equipment/$asset"))
                        } catch (_: java.io.FileNotFoundException) {
                            // Optional images can be absent on a given build.
                        }
                    }
                }
                val mime = when (asset) {
                    "index.html" -> "text/html"
                    "step-up.js" -> "application/javascript"
                    "step-up.glb" -> "model/gltf-binary"
                    else -> null
                }
                if (uri.scheme == "https" && uri.host == HOST && mime != null) {
                    return WebResourceResponse(
                        mime,
                        if (asset == "step-up.glb") null else "UTF-8",
                        context.assets.open("exercise-3d/$asset")
                    )
                }
                return WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", emptyMap(), null)
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                useFallback()
                return true
            }
        }
        loadUrl("https://$HOST/index.html?clip=$clip")
        post(probe)
        postDelayed(loadTimeout, 12_000)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean = ready && super.onTouchEvent(event)

    fun playback(play: Boolean, mirror: Boolean, reset: Int) {
        state = Playback(play, mirror, reset)
        sendPlayback()
    }

    private fun sendPlayback() {
        if (!ready || disposed || state == sent) return
        val (play, mirror, reset) = state
        sent = state
        evaluateJavascript("window.hunterPlayback($play,$mirror,$reset)", null)
    }

    private fun useFallback() {
        if (disposed || failed) return
        failed = true
        ready = false
        visibility = GONE
        removeCallbacks(probe)
        removeCallbacks(loadTimeout)
        onReady(false)
    }

    fun release() {
        if (disposed) return
        disposed = true
        removeCallbacks(probe)
        removeCallbacks(loadTimeout)
        destroy()
    }

    private companion object {
        val equipmentAsset = Regex("[a-z0-9-]{1,64}\\.svg")
        const val HOST = "exercise3d.hunter.invalid"
    }
}
