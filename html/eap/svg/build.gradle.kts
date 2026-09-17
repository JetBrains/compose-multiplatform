@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.jetbrains.compose.gradle.standardConf
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest

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
        browser {
            testTask {
                useKarma {
                    standardConf()
                }
            }
        }
    }
    wasmJs {
        browser {
            testTask {
                useKarma {
                    standardConf()
                }
            }
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
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

        val webTest by getting {
            dependencies {
                implementation(project(":html-test-utils"))
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val jsTest by getting
        val wasmJsTest by getting

        listOf(webTest, jsTest, wasmJsTest).forEach {
            it.languageSettings {
                optIn("org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi")
                optIn("org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi")
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
    description = "Generates JVM-rendered SVG for the browser hydration tests."
    dependsOn(jvmTestCompilation.compileTaskProvider)
    mainClass.set("org.jetbrains.compose.web.core.tests.svg.SvgSsrHydrationFixtureGenerator")
    classpath(jvmTestCompilation.output.allOutputs)
    classpath(jvmTestCompilation.runtimeDependencyFiles)
    args(generatedSsrHydrationFixtures.get().asFile.absolutePath)
    outputs.dir(generatedSsrHydrationFixtures)
}

listOf("js", "wasmJs").forEach { targetName ->
    val testCompilation =
        kotlin.targets.getByName(targetName).compilations.getByName("test") as KotlinJsCompilation
    val processResources =
        tasks.named(testCompilation.processResourcesTaskName, ProcessResources::class.java) {
            from(generatedSsrHydrationFixtures)
            dependsOn(generateSsrHydrationFixture)
        }
    val browserTest = tasks.named<KotlinJsTest>("${targetName}BrowserTest")
    val copyFixture = tasks.register<Copy>(
        "copySsrHydrationFixtureTo${targetName.replaceFirstChar(Char::uppercaseChar)}TestResources"
    ) {
        dependsOn(browserTest.flatMap { it.inputFileProperty })
        from(processResources) {
            include("svg-ssr-hydration.html")
        }
        into(browserTest.flatMap { requireNotNull(it.testFramework).workingDir })
    }

    browserTest.configure {
        dependsOn(copyFixture)
    }
}
