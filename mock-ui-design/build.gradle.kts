plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.fluxzen.ui_design"
    compileSdk = 36
    defaultConfig { minSdk = 31 }
    buildFeatures { compose = true }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("javax.inject:javax.inject:1")
    implementation("com.google.android.gms:play-services-nearby:19.3.0")
    implementation("io.github.webrtc-sdk:android:144.7559.05")
}
