import java.net.URI
plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
 namespace = "com.huntersystem.app"; compileSdk = 35
 defaultConfig { applicationId = "com.huntersystem.app"; minSdk = 26; targetSdk = 35; versionCode = 106; versionName = "40.0-native" }
 signingConfigs { create("release") { val path = System.getenv("HUNTER_KEYSTORE"); if (path != null) { storeFile = file(path); storePassword = System.getenv("HUNTER_STORE_PASSWORD"); keyAlias = "hunter-system-release"; keyPassword = System.getenv("HUNTER_STORE_PASSWORD"); storeType = "PKCS12" } } }
 buildTypes { getByName("release") { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("release") } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 buildFeatures { compose = true }
 androidResources { noCompress += listOf("mp4", "webp", "wav") }
 testOptions { unitTests.isIncludeAndroidResources = true; unitTests.all { t ->
   t.systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
   listOf("http.proxyHost","http.proxyPort","https.proxyHost","https.proxyPort","javax.net.ssl.trustStore").forEach { key -> System.getProperty(key)?.let { t.systemProperty(key,it) } }
   t.systemProperty("maven.repo.local", rootProject.file("../dev-tools/maven-cache").absolutePath)
   t.maxHeapSize="1536m"
 } }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2025.04.01"))
 implementation("androidx.activity:activity-compose:1.10.1")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.foundation:foundation")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
 implementation("com.caverock:androidsvg-aar:1.4")
 testImplementation("junit:junit:4.13.2")
 testImplementation("org.json:json:20240303")
 testImplementation("org.robolectric:robolectric:4.14.1")
 testImplementation("androidx.test:core:1.6.1")
 testImplementation(platform("androidx.compose:compose-bom:2025.04.01"))
 testImplementation("androidx.compose.ui:ui-test-junit4")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
}
