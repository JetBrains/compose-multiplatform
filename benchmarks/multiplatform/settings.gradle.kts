pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral {
            url = uri("https://cache-redirector.jetbrains.com/maven-central")
        }
        gradlePluginPortal()
        maven("https://packages.jetbrains.team/maven/p/cmp/dev")
        google()
    }
}

rootProject.name = "compose-benchmarks"

dependencyResolutionManagement {
    versionCatalogs{
        create("libs") {
            // Override Kotlin and Compose versions with properties
            providers.run {
                with(gradleProperty("kotlin.version")) {
                    if (isPresent) version("kotlin", get())
                }
                with(gradleProperty("compose.version")) {
                    if (isPresent) version("compose-multiplatform", get())
                }
            }
        }
    }
}

include(":compose-scene-api")

/**
 * Selects the appropriate compose-scene-impl module based on the Compose Multiplatform version.
 *
 * - compose-scene-impl-1: for versions before compose-multiplatform-core#3012
 *   Uses CanvasLayersComposeScene and ComposeScene.render(canvas, nanoTime)
 *
 * - compose-scene-impl-2: for versions after compose-multiplatform-core#3012
 *   Uses FrameRecomposer + ComposeScene.measureAndLayout() + ComposeScene.draw(canvas)
 *
 * The breaking change (PR #3012) was introduced between dev builds 4213 and 4221
 * of 1.12.0-alpha02, so dev builds before dev4221 still use the old API.
 * 1.12.0-alpha01 also uses the old API (before the PR).
 *
 * The selected module is included as ":compose-scene-impl" so that the benchmarks module
 * always depends on ":compose-scene-impl" regardless of which actual module is used.
 */
fun resolveComposeVersion(): String {
    // Check Gradle property override first, then fall back to libs.versions.toml
    val override = providers.gradleProperty("compose.version")
    if (override.isPresent) return override.get()

    val tomlFile = file("gradle/libs.versions.toml")
    val versionRegex = Regex("""compose-multiplatform\s*=\s*"(.+?)"""")
    tomlFile.useLines { lines ->
        for (line in lines) {
            val match = versionRegex.find(line)
            if (match != null) return match.groupValues[1]
        }
    }
    error("Could not find compose-multiplatform version in gradle/libs.versions.toml")
}

fun isAfterPR3012(version: String): Boolean {
    // Version format examples:
    //   "1.11.1"
    //   "1.12.0-alpha01"              (before PR #3012)
    //   "1.12.0-alpha02+dev4213"      (before PR #3012)
    //   "1.12.0-alpha02+dev4221"      (after PR #3012)
    //   "1.12.0-alpha02+dev4236"
    //   "1.12.0-beta01"
    //   "1.12.0"

    val parts = version.split(".")
    if (parts.size < 2) return false
    val major = parts[0].toIntOrNull() ?: return false
    val minor = parts[1].toIntOrNull() ?: return false

    // Versions before 1.12 definitely use the old API
    if (major < 1 || (major == 1 && minor < 12)) return false
    // Versions after 1.12.x definitely use the new API
    if (major > 1 || (major == 1 && minor > 12)) return true

    // major == 1, minor == 12: check pre-release tag and dev build number.
    // The PR #3012 landed between dev4213 and dev4221 of 1.12.0-alpha02.
    // 1.12.0-alpha01 still uses the old API.

    // Extract pre-release tag (e.g. "alpha01", "alpha02", "beta01") if present
    val preReleaseRegex = Regex("""-(alpha|beta|rc)(\d+)""")
    val preReleaseMatch = preReleaseRegex.find(version)
    if (preReleaseMatch != null) {
        val preReleaseType = preReleaseMatch.groupValues[1]  // "alpha", "beta", or "rc"
        val preReleaseNum = preReleaseMatch.groupValues[2].toIntOrNull() ?: 0

        // alpha01 and earlier are before the PR
        if (preReleaseType == "alpha" && preReleaseNum < 2) return false

        // beta and rc are after the PR
        if (preReleaseType != "alpha") return true

        // alpha02+: check dev build number
        val devBuildRegex = Regex("""\+dev(\d+)""")
        val devMatch = devBuildRegex.find(version)
        if (devMatch != null) {
            val devBuild = devMatch.groupValues[1].toIntOrNull() ?: return true
            return devBuild >= 4221
        }

        // alpha02 without +devNNNN suffix — released alpha02 includes PR #3012
        return true
    }

    // No pre-release tag means a final release (e.g. "1.12.0") — includes PR #3012
    return true
}

val composeVersion = resolveComposeVersion()
val implDir = if (isAfterPR3012(composeVersion)) "compose-scene-impl-2" else "compose-scene-impl-1"

include(":compose-scene-impl")
project(":compose-scene-impl").projectDir = file(implDir)

include(":benchmarks")
