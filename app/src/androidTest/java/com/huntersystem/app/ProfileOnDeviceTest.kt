package com.huntersystem.app

import android.graphics.Color
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.UiDevice
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayOutputStream
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
                if (device.hasObject(By.text("PERFIL DEL CAZADOR")) &&
                    device.hasObject(By.textContains("Nombre del cazador")) &&
                    device.hasObject(By.textContains("Edad en años")) &&
                    device.hasObject(By.textContains("43.12.2"))) {
                    assertScreenIsNotBlank()
                    val fields = device.findObjects(By.clazz("android.widget.EditText"))
                    assertTrue("Missing native profile fields", fields.size >= 2)
                    fields[0].text = "Auditoría Android"
                    fields[1].text = "30"
                    val create = By.text("Crear perfil y abrir Hunter System")
                    repeat(8) {
                        if (!device.hasObject(create)) {
                            val x = device.displayWidth / 2
                            device.swipe(x, device.displayHeight * 3 / 4, x, device.displayHeight / 4, 24)
                            device.waitForIdle()
                        }
                    }
                    checkNotNull(device.findObject(create)) { "Missing profile activation button" }.click()
                    val exercises = By.text("Ejercicios")
                    assertTrue("Home navigation did not appear", device.wait(Until.hasObject(exercises), 20_000))
                    scenario.recreate()
                    checkNotNull(device.wait(Until.findObject(exercises), 20_000)).click()
                    assertTrue("Catalog did not load", device.wait(Until.hasObject(By.text("5000 fichas")), 20_000))
                    val exercise = By.descContains("Abrir ejercicio:")
                    val list = checkNotNull(device.findObject(By.res("exerciseCatalog")))
                    val bounds = list.visibleBounds
                    repeat(8) {
                        if (!device.wait(Until.hasObject(exercise), 500)) {
                            device.swipe(bounds.centerX(), bounds.top + bounds.height() * 4 / 5,
                                bounds.centerX(), bounds.top + bounds.height() / 3, 30)
                            device.waitForIdle()
                        }
                    }
                    if (!device.hasObject(exercise)) {
                        val dump = ByteArrayOutputStream()
                        device.dumpWindowHierarchy(dump)
                        error("Exercise card unavailable: ${dump.toString("UTF-8").takeLast(12000)}")
                    }
                    checkNotNull(device.findObject(exercise)).click()
                    assertTrue("The exercise detail did not open",
                        device.wait(Until.hasObject(By.text("DEMOSTRACIÓN")), 15_000))
                    val play = By.text("Reproducir demostración")
                    repeat(6) {
                        if (!device.wait(Until.hasObject(play), 750)) {
                            val x = device.displayWidth / 2
                            device.swipe(x, device.displayHeight * 7 / 10, x, device.displayHeight * 5 / 10, 30)
                            device.waitForIdle()
                        }
                    }
                    checkNotNull(device.findObject(play)) { "Missing demonstration playback" }.click()
                    assertTrue("Playback did not start", device.wait(Until.hasObject(By.text("Pausar demostración")), 10_000))
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
