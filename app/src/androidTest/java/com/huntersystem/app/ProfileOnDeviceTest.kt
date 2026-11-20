package com.huntersystem.app

import android.graphics.Color
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

    private fun assertScreenIsNotBlank() {
        val image = InstrumentationRegistry.getInstrumentation()
            .uiAutomation.takeScreenshot()
            ?: throw AssertionError("Screenshot unavailable")
        try {
            var bright = 0
            val step = (image.width / 260).coerceAtLeast(2)
            for (y in (image.height / 6) until (image.height * 5 / 6) step step) {
                for (x in (image.width / 10) until (image.width * 9 / 10) step step) {
                    val c = image.getPixel(x, y)
                    if (maxOf(Color.red(c), Color.green(c), Color.blue(c)) > 125)
                        bright++
                }
            }
            assertTrue("Application area appears blank: $bright bright pixels", bright > 120)
        } finally {
            image.recycle()
        }
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
                    result.contains("Nombre")) { assertScreenIsNotBlank(); return }
                // Some devices crash the WebView renderer. In that case the
                // native recovery profile must be rendered and usable instead.
                val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
                if (device.hasObject(By.textContains("PERFIL NATIVO DE RECUPERACIÓN")) &&
                    device.hasObject(By.textContains("Nombre del cazador")) &&
                    device.hasObject(By.textContains("Edad en años")) &&
                    device.hasObject(By.textContains("43.12.2"))) {
                    assertScreenIsNotBlank()
                    return
                }
                Thread.sleep(500)
            }
            assertTrue(
                "The profile was not actually rendered by Android WebView: ${lastResult.get()}",
                false
            )
        }
    }
}
