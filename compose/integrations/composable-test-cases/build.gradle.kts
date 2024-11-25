import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

group "com.example"
version "1.0-SNAPSHOT"

fun Project.disableYarnLockMismatchReport() {
    plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
        the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().apply {
            yarnLockMismatchReport = org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport.NONE
        }
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/") // to test with kotlin dev builds
        // mavenLocal()
    }

    afterEvaluate {
        tasks.withType<KotlinJsCompile>().configureEach {
            kotlinOptions.freeCompilerArgs += listOf(
                "-Xklib-enable-signature-clash-checks=false",
            )
        }

        tasks.withType<KotlinCompile<*>>().configureEach {
            kotlinOptions.freeCompilerArgs += "-Xpartial-linkage=disable"
        }
        //non-depracated?
//        tasks.withType<KotlinCompilationTask<KotlinJsCompilerOptions>>() {
//            compilerOptions {
//                freeCompilerArgs.add("-Xklib-enable-signature-clash-checks=false")
//            }
//        }
//
//        tasks.withType<KotlinCompilationTask<*>>() {
//            compilerOptions {
//                freeCompilerArgs.add("-Xpartial-linkage=disable")
//            }
//        }
    }
    disableYarnLockMismatchReport()
}

plugins {
    kotlin("multiplatform").apply(false)
//    alias(libs.plugins.multiplatform).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.multiplatform")
//    apply(project.libs.plugins.multiplatform) // TODO doesnt work
//    apply(libs.plugins.multiplatform) // TODO doesnt work
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
//    allprojects {
        configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
            jvm("desktop")
            applyDefaultHierarchyTemplate()
            js(IR) {
//                browser()
                 nodejs() // Commented to save a bit of CI time. Testing in a browser should be enough.
            }
            wasmJs {
                d8 {}
            }

            iosArm64()
            iosSimulatorArm64()
            iosX64()
            macosX64()
            macosArm64()
            // We use linux agents on CI. So it doesn't run the tests, but it builds the klib anyway which is time consuming.
            // if (project.isMingwX64Enabled) mingwX64()
            linuxX64()
        }

//
//            sourceSets {
//                val commonMain by getting {
//                    dependencies {
//                        implementation(project(":common"))
//                        implementation(getLibDependencyForMain(project))
//                    }
//                }
//                val commonTest by getting {
//                    dependencies { configureCommonTestDependencies() }
//                }
//            }
//        }
    }
}

fun KotlinSourceSet.libDependencyForMain1() {
        if (!project.name.endsWith("-main")) error("Unexpected main module name: ${project.name}")
        dependencies {
            implementation(project(":" + project.name.replace("-main", "-lib")))
        }
    }

//subprojects {
//    // This makes sure the function is accessible in subprojects
//    apply {
//        from(rootProject.file("build.gradle.kts"))
//    }
//}

//fun KotlinDependencyHandler.getLibDependencyForMain(project: Project): ProjectDependency {
//    if (project.name.endsWith("-main")) error("Unexpected main module name: ${project.name}")
//    return project(":" + project.name.replace("-main", "-lib"))
//}

println("kotlin version: ${libs.versions.kotlin.get()}")
