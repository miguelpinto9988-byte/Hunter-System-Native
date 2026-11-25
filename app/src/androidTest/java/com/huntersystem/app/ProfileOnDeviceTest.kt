package com.huntersystem.app

import android.graphics.Color
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileOnDeviceTest {
    @Test
    fun freshInstallActivatesNativeProfileAndPersistsAfterRecreation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        val themeName = InstrumentationRegistry.getArguments().getString("captureTheme", "Blanco")
        val mode = if (themeName == "Negro") "oscuro" else "claro"
        val folder = "/sdcard/Download/monarca134"
        device.executeShellCommand("mkdir -p $folder")
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            fun awaitText(label: String) = checkNotNull(device.wait(Until.findObject(By.text(label)), 60_000)) {
                "Missing Compose control: $label"
            }
            fun capture(name: String) {
                device.waitForIdle()
                Thread.sleep(500)
                val file = "$folder/$mode-$name.png"
                device.executeShellCommand("screencap -p $file")
                val size = device.executeShellCommand("stat -c %s $file").trim().toLongOrNull() ?: 0L
                assertTrue("PNG missing or invalid: $file ($size bytes)", size > 1024)
            }
            awaitText("PASO 1 DE 4")
            capture("perfil-1")
            val fields = device.findObjects(By.clazz("android.widget.EditText"))
            assertTrue("Missing identity fields", fields.size >= 2)
            fields[0].text = "Cuenta de prueba"
            fields[1].text = "30"
            awaitText("SIGUIENTE ›").click()
            awaitText("PASO 2 DE 4")
            capture("perfil-2")
            awaitText("SIGUIENTE ›").click()
            awaitText("PASO 3 DE 4")
            capture("perfil-3-inicial")
            val theme = checkNotNull(device.wait(Until.findObject(By.textContains("Tema del sistema")), 10_000))
            theme.click()
            awaitText(themeName).click()
            capture("perfil-3")
            awaitText("SIGUIENTE ›").click()
            awaitText("PASO 4 DE 4")
            capture("perfil-4")
            // The visible Text can expose its own Android accessibility node.
            // Check the effect of a physical tap on the disabled activation control.
            awaitText("ACTIVAR PERFIL").click()
            device.waitForIdle()
            assertTrue("Activation must not proceed without consent", device.hasObject(By.text("PASO 4 DE 4")))
            awaitText("He revisado mi información y deseo activar mi perfil de cazador.").click()
            awaitText("ACTIVAR PERFIL").click()
            awaitText("SISTEMA ACTIVADO")
            awaitText("ENTRAR AL SISTEMA").click()
            awaitText("Ejercicios")
            scenario.recreate()
            awaitText("Ejercicios")
            for ((label, slug) in listOf("Panel" to "panel", "Misiones" to "misiones",
                    "Rutinas" to "rutinas", "Ejercicios" to "ejercicios")) {
                val node = device.wait(Until.findObject(By.desc(label)), 5000)
                    ?: device.findObject(By.desc("$label, seleccionada"))
                    ?: device.wait(Until.findObject(By.text(label)), 10000)
                    ?: error("Missing navigation tab: $label")
                node.click()
                device.waitForIdle()
                Thread.sleep(700)
                capture("$slug-00")
                repeat(6) { index ->
                    device.swipe(device.displayWidth / 2, device.displayHeight * 72 / 100,
                        device.displayWidth / 2, device.displayHeight * 29 / 100, 38)
                    capture("$slug-0${index+1}")
                }
            }
        }
    }
}
