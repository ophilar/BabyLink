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
            val fallbackUser: String? = providers.gradleProperty("gpr.user").orNull
            val fallbackToken: String? = providers.gradleProperty("gpr.token").orNull
            val user = fallbackUser?.takeIf { it.isNotBlank() } ?: System.getenv("GPR_USER")?.takeIf { it.isNotBlank() }
            val token = fallbackToken?.takeIf { it.isNotBlank() } ?: System.getenv("GPR_TOKEN")?.takeIf { it.isNotBlank() }

            // Only add the GitHub Packages repository if we actually have a valid token to authenticate with.
            // Otherwise, let Gradle fall back to other repositories (like Jitpack) to avoid 401 Unauthorized errors.
            if (user != null && token != null && user.isNotBlank() && token.isNotBlank()) {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/ophilar/FluxZenShared")
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
