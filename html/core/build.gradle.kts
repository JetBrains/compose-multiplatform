import org.jetbrains.compose.gradle.standardConf
import org.gradle.api.tasks.JavaExec

val generatedSsrHydrationFixtures = layout.buildDirectory.dir("generated/ssrHydrationFixtures")

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}


kotlin {
    jvm()
    js(IR) {
        browser() {
            testTask {
                useKarma {
                    standardConf()
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(libs.kotlinx.coroutines.core)
                api(project(":kotlinx-browser-common-subset"))
                implementation(project(":internal-html-core-runtime"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val jsMain by getting {
            languageSettings {
                optIn("org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi")
            }
            dependencies {
                api(project(":internal-html-core-runtime"))
            }
        }

        val jsTest by getting {
            resources.srcDir(generatedSsrHydrationFixtures)
            languageSettings {
                optIn("org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi")
                optIn("org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi")
            }
            dependencies {
                implementation(project(":html-test-utils"))
                implementation(kotlin("test-js"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

val jvmTestCompilation = kotlin.targets.getByName("jvm").compilations.getByName("test")
val ssrHydrationFixture = generatedSsrHydrationFixtures.map { it.file("ssr-hydration.html") }
val generateSsrHydrationFixture = tasks.register<JavaExec>("generateSsrHydrationFixture") {
    group = "verification"
    description = "Generates JVM-rendered HTML for the Kotlin/JS hydration test."
    dependsOn(jvmTestCompilation.compileTaskProvider)
    mainClass.set("org.jetbrains.compose.web.SsrHydrationFixtureGenerator")
    classpath(jvmTestCompilation.output.allOutputs)
    classpath(jvmTestCompilation.runtimeDependencyFiles)
    args(ssrHydrationFixture.get().asFile.absolutePath)
    outputs.file(ssrHydrationFixture)
}

tasks.named("jsTestProcessResources") {
    dependsOn(generateSsrHydrationFixture)
}
