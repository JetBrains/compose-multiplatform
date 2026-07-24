pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral {
            url = uri("https://cache-redirector.jetbrains.com/maven-central")
        }
        gradlePluginPortal()
        maven("https://packages.jetbrains.team/maven/p/cmp/dev")
        google {
            url = uri("https://cache-redirector.jetbrains.com/dl.google.com/dl/android/maven2")
        }
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
 * Semver version parser, taken from compose-multiplatform-core:
 * buildSrc/public/src/main/kotlin/androidx/build/Version.kt
 *
 * Supports format: major.minor.patch[-preRelease][+buildMetadata]
 * Examples: "1.11.1", "1.12.0-alpha02+dev4221", "1.12.0-beta01", "1.12.0"
 */
data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String? = null,
    val buildMetadata: String? = null,
) : Comparable<Version> {
    override fun compareTo(other: Version) = compareValuesBy(
        this, other,
        { it.major },
        { it.minor },
        { it.patch },
        { it.preRelease == null },  // no pre-release (stable) sorts after pre-release
        { it.preRelease },          // lexicographic ordering (alpha < beta < rc)
    )

    companion object {
        // Semver regex from https://semver.org
        private val SEMVER_REGEX = Regex(
            """^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)""" +
            """(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?""" +
            """(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$"""
        )

        fun parse(versionString: String): Version {
            val match = SEMVER_REGEX.matchEntire(versionString)
                ?: error("Cannot parse version: $versionString")
            return Version(
                major = match.groupValues[1].toInt(),
                minor = match.groupValues[2].toInt(),
                patch = match.groupValues[3].toInt(),
                preRelease = match.groupValues[4].ifEmpty { null },
                buildMetadata = match.groupValues[5].ifEmpty { null },
            )
        }
    }
}

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

// The PR #3012 landed between dev4213 and dev4221 of 1.12.0-alpha02.
// buildMetadata ("+devNNNN") is not part of semver comparison, so we handle it explicitly.
val PR_3012_VERSION_BARRIER = Version.parse("1.12.0-alpha02")
val PR_3012_MIN_DEV_BUILD = 4221

fun isAfterPR3012(version: Version): Boolean {
    if (version > PR_3012_VERSION_BARRIER) return true
    if (version < PR_3012_VERSION_BARRIER) return false
    // Exact match on 1.12.0-alpha02: check dev build number
    val devBuild = version.buildMetadata?.removePrefix("dev")?.toIntOrNull()
    // No +devNNNN suffix means released alpha02 which includes PR #3012
    return devBuild == null || devBuild >= PR_3012_MIN_DEV_BUILD
}

val composeVersion = Version.parse(resolveComposeVersion())
val implDir = if (isAfterPR3012(composeVersion)) "compose-scene-impl-2" else "compose-scene-impl-1"

include(":compose-scene-impl")
project(":compose-scene-impl").projectDir = file(implDir)

include(":benchmarks")
