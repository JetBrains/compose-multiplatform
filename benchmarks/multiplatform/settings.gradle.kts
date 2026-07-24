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
 * - compose-scene-impl-1: baseline, for versions before compose-multiplatform-core#3012.
 *   Uses CanvasLayersComposeScene and ComposeScene.render(canvas, nanoTime)
 *
 * - compose-scene-impl-2: from compose-multiplatform-core#3012.
 *   Uses FrameRecomposer + ComposeScene.measureAndLayout() + ComposeScene.draw(canvas)
 *
 * - compose-scene-impl-3: from compose-multiplatform-core#3126.
 *   Same scene API as impl-2, plus registerSkikoComposeImplementation() before creating a scene
 *
 * A module is a self-contained copy of the previous one: the duplication is deliberate, so that
 * adapting to a new API never touches the modules older versions still build against.
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

data class SceneImplBarrier(val version: Version, val minDevBuild: Int, val moduleDir: String)

/**
 * Barriers ordered from oldest to newest; the newest one the resolved version has reached wins,
 * and [BASELINE_SCENE_IMPL_DIR] is used by versions that reached none of them.
 */
val SCENE_IMPL_BARRIERS = listOf(
    // ComposeScene.render(canvas, nanoTime) was split into FrameRecomposer.performFrame(),
    // ComposeScene.measureAndLayout() and ComposeScene.draw(canvas) between dev4213 and dev4221.
    // 1.12.0-alpha01 still uses the old API.
    SceneImplBarrier(Version.parse("1.12.0-alpha02"), 4221, "compose-scene-impl-2"),
    // The Skiko graphics and text implementation moved into the ui-skiko module and has to be
    // registered at runtime via registerSkikoComposeImplementation().
    SceneImplBarrier(Version.parse("1.12.10-alpha01"), 4534, "compose-scene-impl-3"),
)

val BASELINE_SCENE_IMPL_DIR = "compose-scene-impl-1"

fun Version.hasReached(barrier: SceneImplBarrier): Boolean {
    if (this > barrier.version) return true
    if (this < barrier.version) return false
    // Exact match on the barrier version: no +devNNNN suffix means the released version,
    // which includes the change.
    val devBuild = buildMetadata?.removePrefix("dev")?.toIntOrNull() ?: return true
    return devBuild >= barrier.minDevBuild
}

val composeVersion = Version.parse(resolveComposeVersion())
val implDir = SCENE_IMPL_BARRIERS.lastOrNull { composeVersion.hasReached(it) }?.moduleDir
    ?: BASELINE_SCENE_IMPL_DIR

include(":compose-scene-impl")
project(":compose-scene-impl").projectDir = file(implDir)

include(":benchmarks")
