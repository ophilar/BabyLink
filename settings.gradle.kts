pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// 1. Explicitly resolve FluxZenShared based on environment
val isCI = System.getenv("GITHUB_ACTIONS") == "true"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        
        // Remove github repo to let it fallback to jitpack/local if possible. Wait, Memory said:
        // "The Gradle build may currently fail to resolve the `com.fluxzen:ui-design` dependency due to it missing from remote repositories, which can prevent successful test execution."
        
        maven { url = uri("https://jitpack.io") }
        mavenLocal()
    }
}

rootProject.name = "BabyBeam"
include(":app")

if (!isCI) {
    val localProperties = java.util.Properties().apply {
        val localFile = file("local.properties")
        if (localFile.exists()) {
            localFile.inputStream().use { load(it) }
        }
    }
    val fluxZenDir = localProperties.getProperty("fluxzen.dir")?.let { file(it) }
    
    if (fluxZenDir != null && fluxZenDir.exists()) {
        includeBuild(fluxZenDir) {
            dependencySubstitution {
                substitute(module("com.fluxzen:ui-design")).using(project(":ui-design"))
                substitute(module("com.fluxzen:firebase-auth")).using(project(":firebase-auth"))
            }
        }
    } else {
        logger.warn("FluxZenShared directory not found. Local composite build disabled. Set 'fluxzen.dir' in local.properties.")
    }
}
