@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.jetbrains.compose.gradle.standardConf
import org.jetbrains.compose.gradle.standardWasmConf
import org.gradle.api.tasks.JavaExec

val generatedSsrHydrationFixtures = layout.buildDirectory.dir("generated/ssrHydrationFixtures")

val kotlinxBrowserCommonSubsetVersion: String =
    providers.gradleProperty("compose.html.eap.kotlinx-browser-common-subset.version").get()

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
    wasmJs {
        browser() {
            testTask {
                useKarma {
                    standardWasmConf()
                }
            }
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(kotlin("stdlib-common"))
                api("org.jetbrains.compose.html:kotlinx-browser-common-subset:$kotlinxBrowserCommonSubsetVersion")
                api(project(":internal-html-core-runtime-eap"))
                api(project(":html-core-eap"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }

        val webTest by getting {
            resources.srcDir(generatedSsrHydrationFixtures)
            languageSettings {
                optIn("org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi")
            }
            dependencies {
                implementation(project(":html-test-utils"))
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

configurations.matching { it.name.contains("Test") }.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(project(":internal-html-core-runtime"))
            .using(project(":internal-html-core-runtime-eap"))
            .because("Compose HTML EAP tests must use the EAP runtime exclusively")
    }
}

val jvmTestCompilation = kotlin.targets.getByName("jvm").compilations.getByName("test")
val generateSsrHydrationFixture = tasks.register<JavaExec>("generateSsrHydrationFixture") {
    group = "verification"
    description = "Generates JVM-rendered SVG for the Kotlin/JS hydration tests."
    dependsOn(jvmTestCompilation.compileTaskProvider)
    mainClass.set("org.jetbrains.compose.web.core.tests.svg.SvgSsrHydrationFixtureGenerator")
    classpath(jvmTestCompilation.output.allOutputs)
    classpath(jvmTestCompilation.runtimeDependencyFiles)
    args(generatedSsrHydrationFixtures.get().asFile.absolutePath)
    outputs.dir(generatedSsrHydrationFixtures)
}

tasks.named("jsTestProcessResources") {
    dependsOn(generateSsrHydrationFixture)
}

tasks.named("wasmJsTestProcessResources") {
    dependsOn(generateSsrHydrationFixture)
}
