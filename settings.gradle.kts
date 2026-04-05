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
    }
}

rootProject.name = "BabyLink"
include(":app")

// Resolve FluxZenShared
val sharedRepoName = "FluxZenShared"
val rootDirFile = rootDir
val possibleLocations = mutableListOf<File>()

rootDirFile.parentFile?.let { p1 ->
    possibleLocations.add(p1.resolve(sharedRepoName))
    p1.parentFile?.let { p2 ->
        possibleLocations.add(p2.resolve(sharedRepoName))
    }
}

val sharedBuild = possibleLocations.find { it.exists() && it.isDirectory }

if (sharedBuild != null) {
    includeBuild(sharedBuild) {
        dependencySubstitution {
            substitute(module("com.fluxzen:ui-design")).using(project(":ui-design"))
            substitute(module("com.fluxzen:firebase-auth")).using(project(":firebase-auth"))
        }
    }
}
