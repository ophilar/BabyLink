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
        maven { url = uri("https://jitpack.io") }
        mavenLocal()
        if (System.getenv("GITHUB_ACTIONS") == "true") {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/ophilar/FluxZenShared")
                val user = System.getenv("GPR_USER")
                val token = System.getenv("GPR_TOKEN")
                if (user != null && token != null && user.isNotBlank() && token.isNotBlank()) {
                    credentials {
                        username = user
                        password = token
                    }
                }
            }
        }
    }
}

rootProject.name = "BabyBeam"
include(":app")

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
    logger.warn("FluxZenShared directory not found at $fluxZenDir. Composite build required. Set 'fluxzen.dir' in local.properties.")
}
