plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.gms)
    alias(libs.plugins.firebase.crashlytics)
}

kotlin {
    jvmToolchain(21)
}


android {
    namespace = "com.fluxzen.babybeam"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fluxzen.babybeam"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

dependencies {
    implementation(libs.ui.design)
    
    // Core & Compose Bundle
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.material)
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)
    
    // Signaling
    implementation(libs.play.services.nearby)
    implementation(libs.gson)
    
    // AI & Streaming
    implementation(libs.mediapipe.tasks.audio)
    implementation(libs.webrtc.android)
    
    // Navigation 3 & Adaptive UI
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.material3.windowsize)
    
    // DI (Hilt Bundle)
    implementation(libs.bundles.hilt)
    ksp(libs.hilt.android.compiler)
    
    // Testing (Bundles & Junit5 Runtime)
    testImplementation(libs.bundles.test.unit)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    androidTestImplementation(libs.bundles.test.android)
}

