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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "BabyBeam"
include(":app")

// 1. Explicitly resolve FluxZenShared based on environment
val isCI = System.getenv("GITHUB_ACTIONS") == "true"
val fluxZenDir = if (isCI) {
    file("FluxZenShared") // Submodule in CI
} else {
    val localProperties = java.util.Properties().apply {
        val localFile = file("local.properties")
        if (localFile.exists()) {
            localFile.inputStream().use { load(it) }
        }
    }
    localProperties.getProperty("fluxzen.dir")?.let { file(it) }
}

if (fluxZenDir != null && fluxZenDir.exists()) {
    includeBuild(fluxZenDir) {
        dependencySubstitution {
            substitute(module("com.fluxzen:ui-design")).using(project(":ui-design"))
            substitute(module("com.fluxzen:firebase-auth")).using(project(":firebase-auth"))
        }
    }
}
