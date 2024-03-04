import internal.InternalComposeSupportPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile

group "com.example"
version "1.0-SNAPSHOT"

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/") // to test with kotlin dev builds
        // mavenLocal()
    }

    // Apply here for all subprojects instead of applying in each build.gradle.kts separately.
    // It applies the compiler plugin
    this.apply<InternalComposeSupportPlugin>()

    afterEvaluate {
        val pluginOptionPrefix = "plugin:androidx.compose.compiler.plugins.kotlin:"
        val project = this
        val kotlinVersion = project.properties["kotlin.version"] as? String

        project.tasks.withType(KotlinCompile::class.java).configureEach {
            kotlinOptions.apply {
                freeCompilerArgs +=
                    listOf(
                        "-P",
                        "${pluginOptionPrefix}suppressKotlinVersionCompatibilityCheck=$kotlinVersion"
                    )

            }
        }

        tasks.withType<KotlinJsCompile>().configureEach {
            kotlinOptions.freeCompilerArgs += listOf(
                "-Xklib-enable-signature-clash-checks=false",
            )
        }

        tasks.withType<KotlinCompile<*>>().configureEach {
            kotlinOptions.freeCompilerArgs += "-Xpartial-linkage=disable"
        }
    }
    disableYarnLockMismatchReport()
}

plugins {
    kotlin("multiplatform") apply false
}

fun Project.disableYarnLockMismatchReport() {
    plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
        the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().apply {
            yarnLockMismatchReport = org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport.NONE
        }
    }
}
