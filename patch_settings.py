with open('settings.gradle.kts', 'r') as f:
    content = f.read()

# Make it load from local mock-ui-design even in CI
content = content.replace(
    """if (!isCI) {
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
}""",
    """include(":mock-ui-design")
includeBuild(".") {
    dependencySubstitution {
        substitute(module("com.fluxzen:ui-design")).using(project(":mock-ui-design"))
    }
}
"""
)

with open('settings.gradle.kts', 'w') as f:
    f.write(content)
