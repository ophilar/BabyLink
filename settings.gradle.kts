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
                url = uri("https://maven.pkg.github.com/ophilar/FluxZenShared")
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

include(":mock-ui-design")
includeBuild(".") {
    dependencySubstitution {
        substitute(module("com.fluxzen:ui-design")).using(project(":mock-ui-design"))
    }
}
