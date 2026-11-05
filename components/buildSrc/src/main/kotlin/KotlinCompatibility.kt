import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

/**
 * Keeps published libraries compatible with older Kotlin compilers.
 * more info: https://youtrack.jetbrains.com/issue/CMP-10881
 */
fun Project.configureKotlinCompatibility() {
    val currentVersion = providers.gradleProperty("kotlin.version").get()
    val currentKotlin = KotlinVersion.fromVersion(
        currentVersion.split('.').take(2).joinToString(".")
    )

    val minSupportedVersion = providers.gradleProperty("kotlin.supported.version").get()
    val minKotlin = KotlinVersion.fromVersion(
        minSupportedVersion.split('.').take(2).joinToString(".")
    )

    extensions.configure<KotlinMultiplatformExtension> {
        coreLibrariesVersion = minSupportedVersion
        compilerOptions {
            languageVersion.set(minKotlin)
            apiVersion.set(minKotlin)
        }

        targets.withType<KotlinJsIrTarget>().configureEach {
            if (platformType == KotlinPlatformType.wasm) {
                // for the new wasm memory API
                compilerOptions {
                    languageVersion.set(currentKotlin)
                    apiVersion.set(currentKotlin)
                }
            }

            // JS and Wasm require a stdlib matching the compiler's ABI.
            compilations.matching { it.name == "main" }.configureEach {
                defaultSourceSet.dependencies {
                    api("org.jetbrains.kotlin:kotlin-stdlib:$currentVersion")
                }
            }
        }
    }
}
