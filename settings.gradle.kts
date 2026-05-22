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
        
        if (isCI) {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/${System.getenv("GITHUB_REPOSITORY") ?: "ophilar/BabyLink"}")
                credentials {
                    username = System.getenv("GPR_USER")
                    password = System.getenv("GPR_TOKEN")
                }
            }
        }
        
        maven { url = uri("https://jitpack.io") }
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
    logger.error("FluxZenShared directory not found at $fluxZenDir. Composite build required. Set 'fluxzen.dir' in local.properties.")
}
