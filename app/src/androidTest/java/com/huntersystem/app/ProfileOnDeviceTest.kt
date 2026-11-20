package com.huntersystem.app

import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unlike Robolectric's URL-only smoke test, launches the actual Activity on Android
 * and executes JavaScript in the installed system WebView to verify the rendered DOM.
 */
@RunWith(AndroidJUnit4::class)
class ProfileOnDeviceTest {
    private fun findWebView(view: View): WebView? = when (view) {
        is WebView -> view
        is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull {
            findWebView(view.getChildAt(it))
        }
        else -> null
    }

    @Test
    fun freshInstallRendersActualProfileText() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val lastResult = AtomicReference("(no WebView yet)")
            val deadline = SystemClock.uptimeMillis() + 40_000L
            while (SystemClock.uptimeMillis() < deadline) {
                val latch = CountDownLatch(1)
                scenario.onActivity { activity ->
                    val web = findWebView(activity.window.decorView)
                    if (web != null) {
                        web.evaluateJavascript(
                            "(document.querySelector('#app')?.innerText || '').slice(0,400)"
                        ) { value ->
                            lastResult.set(value ?: "(null JS response)")
                            latch.countDown()
                        }
                    } else {
                        lastResult.set("(no WebView found)")
                        latch.countDown()
                    }
                }
                latch.await(1500, TimeUnit.MILLISECONDS)
                val result = lastResult.get()
                if (result.contains("Identidad del cazador") &&
                    result.contains("Nombre")) return
                // Some devices crash the WebView renderer. In that case the
                // native recovery profile must be rendered and usable instead.
                val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
                if (device.hasObject(By.textContains("PERFIL NATIVO DE RECUPERACIÓN"))) return
                Thread.sleep(500)
            }
            assertTrue(
                "The profile was not actually rendered by Android WebView: ${lastResult.get()}",
                false
            )
        }
    }
}
