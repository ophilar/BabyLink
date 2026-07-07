import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.fluxzen.babybeam"
    compileSdk = 36
    
    kotlin {
        jvmToolchain(21)
    }

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
        isCoreLibraryDesugaringEnabled = true
    }
    
    buildFeatures {
        compose = true
    }
    
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { load(it) }
    }
}
val releaseDir = localProperties.getProperty("release.dir")

if (releaseDir != null) {
    tasks.register<Copy>("publishApk") {
        description = "Copies the generated APK to the OneDrive Releases folder."
        group = "publishing"

        doFirst {
            println("Publishing APKs to: $releaseDir")
        }

        from(layout.buildDirectory.dir("outputs/apk"))

        into(releaseDir)
        include("**/*.apk")
        
        eachFile {
            val buildType = relativePath.segments[0]
            path = "BabyBeam-${android.defaultConfig.versionName}-$buildType.apk"
        }
        includeEmptyDirs = false
    }

    tasks.matching { it.name.startsWith("assemble") }.configureEach {
        finalizedBy("publishApk")
    }
}

tasks.register("unitTestClasses") {
    description = "Compiles unit test sources."
    group = "verification"
    dependsOn(tasks.matching { it.name.startsWith("compile") && it.name.endsWith("UnitTestSources") })
}

tasks.register("androidTestClasses") {
    description = "Compiles android test sources."
    group = "verification"
    dependsOn(tasks.matching { it.name.startsWith("compile") && it.name.endsWith("AndroidTestSources") })
}

dependencies {
    implementation("com.fluxzen:ui-design:1.0.2")

    
    // Core & Compose Bundle
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.material)
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)
    
    // Signaling
    implementation(libs.play.services.nearby)
    implementation(libs.gson)
    
    // AI & Streaming
    implementation(libs.mediapipe.tasks.audio)
    implementation(libs.litert) // Enforce 16KB alignment
    implementation(libs.webrtc.android)
    
    // WorkManager (Fixes PendingIntent S+ crash)
    implementation(libs.androidx.work.runtime.ktx)
    
    // Navigation 3
    implementation(libs.bundles.navigation3)
    
    // DI (Hilt Bundle)
    implementation(libs.bundles.hilt)
    ksp(libs.hilt.android.compiler)
    
    // Testing
    testImplementation(libs.bundles.test.unit)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    androidTestImplementation(libs.bundles.test.android)
    coreLibraryDesugaring(libs.android.desugar.jdk)
}


val generateGoogleServicesJson by tasks.registering {
    val googleServicesFile = file("google-services.json")
    val apiKey = System.getenv("GOOGLE_SERVICES_API_KEY") ?: "mock-api-key"

    outputs.file(googleServicesFile)

    doLast {
        val jsonContent = """{
  "project_info": {
    "project_number": "123456789",
    "project_id": "mock-project-id",
    "storage_bucket": "mock-project-id.appspot.com"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:123456789:android:mockappid",
        "android_client_info": {
          "package_name": "com.fluxzen.babybeam"
        }
      },
      "oauth_client": [],
      "api_key": [
        {
          "current_key": "${apiKey}"
        }
      ],
      "services": {
        "appinvite_service": {
          "other_platform_oauth_client": []
        }
      }
    }
  ],
  "configuration_version": "1"
}"""
        googleServicesFile.writeText(jsonContent)
    }
}

tasks.matching { it.name.matches(Regex("process.*GoogleServices")) }.configureEach {
    dependsOn(generateGoogleServicesJson)
}
