import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

//group "com.example"
//version "1.0-SNAPSHOT"

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

        maven("https://packages.jetbrains.team/maven/p/kt/dev")
        maven("https://redirector.kotlinlang.org/maven/dev")
        maven {
            url = uri("${rootDir}/build/maven-project")
        }
    }

    disableYarnLockMismatchReport()
}

plugins {
    kotlin("multiplatform").version(libs.versions.kotlin).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.multiplatform")

    configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
        jvm("desktop")
        applyDefaultHierarchyTemplate()
        js(IR) {
//                browser()
            nodejs() // Commented to save a bit of CI time. Testing in a browser should be enough.
        }
        @OptIn(ExperimentalWasmDsl::class)
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

        sourceSets {
            val commonMain by getting {
                val projectName = project.name
                dependencies {
                    if (projectName != "common") {
                        implementation(project(":common"))
                    }

                    if (projectName.endsWith("-main")) {
                        implementation(project(":" + projectName.replace("-main", "-lib")))
                    }
                }
            }
        }

        targets
            .filter { it.name != "desktop" } // Exclude JVM target
            .forEach { target ->
                target.compilations.all {
                    compileTaskProvider.configure{
                        compilerOptions {
                            freeCompilerArgs.add("-Xpartial-linkage=disable")
                        }
                    }
                }
            }
    }
}
