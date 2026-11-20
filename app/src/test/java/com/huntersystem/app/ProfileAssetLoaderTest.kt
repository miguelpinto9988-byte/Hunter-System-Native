package com.huntersystem.app

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProfileAssetLoaderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun profileHtmlScriptStylesAndArtworkAreAvailableOffline() {
        val loader = profileAssetLoader(context)
        val allowed = profileAllowedAssets(context, JSONObject())
        for (path in listOf(
            "/assets/profile-monarca/index.html",
            "/assets/profile-monarca/profile.js",
            "/assets/profile-monarca/profile.css",
            "/assets/profile-monarca/avatar.webp",
            "/assets/ui33/companion-tusk.webp",
            "/assets/fonts/BarlowCondensed-SemiBold.ttf"
        )) {
            assertTrue("Missing allowlist entry: $path", path in allowed)
            val response = profileAssetResponse(
                Uri.parse("https://appassets.androidplatform.net$path"), allowed, loader
            )
            response.data.use { assertTrue("Missing content: $path", it.read() != -1) }
        }
    }

    @Test fun refusesUntrustedHostsAndUnlistedFiles() {
        val loader = profileAssetLoader(context)
        val allowed = profileAllowedAssets(context, JSONObject())
        for (url in listOf(
            "https://example.org/assets/profile-monarca/index.html",
            "http://appassets.androidplatform.net/assets/profile-monarca/index.html",
            "https://appassets.androidplatform.net/assets/catalog.json",
            "https://appassets.androidplatform.net/assets/profile-monarca/unknown.js"
        )) {
            assertEquals(404, profileAssetResponse(Uri.parse(url), allowed, loader).statusCode)
        }
    }

    @Test fun legacyProfileArtworkUsesTheSameLocalOrigin() {
        val legacy = JSONObject()
            .put("scene", "/assets/ui34/saved-scene.webp")
            .put("figure", "/assets/ui33/saved-figure.webp")
        val allowed = profileAllowedAssets(context, legacy)
        assertTrue("/assets/ui34/saved-scene.webp" in allowed)
        assertTrue("/assets/ui33/saved-figure.webp" in allowed)
    }
}
