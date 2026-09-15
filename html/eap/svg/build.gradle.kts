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
        browser() {
            testTask {
                useKarma {
                    standardConf()
                }
            }
        }
    }

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

        val jsTest by getting {
            languageSettings {
                optIn("org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi")
            }
            dependencies {
                implementation(project(":html-test-utils"))
                implementation(kotlin("test-js"))
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

val jsTestCompilation =
    kotlin.targets.getByName("js").compilations.getByName("test") as KotlinJsCompilation
val jsTestProcessResources =
    tasks.named(jsTestCompilation.processResourcesTaskName, ProcessResources::class.java) {
        from(generatedSsrHydrationFixtures)
        dependsOn(generateSsrHydrationFixture)
    }

val jsBrowserTest = tasks.named<KotlinJsTest>("jsBrowserTest")
val copySsrHydrationFixtureToKjsTestResources =
    tasks.register<Copy>("copySsrHydrationFixtureToKjsTestResources") {
        dependsOn(jsBrowserTest.flatMap { it.inputFileProperty })
        from(jsTestProcessResources) {
            include("svg-ssr-hydration.html")
        }
        into(jsBrowserTest.flatMap { requireNotNull(it.testFramework).workingDir })
    }

jsBrowserTest.configure {
    dependsOn(copySsrHydrationFixtureToKjsTestResources)
}
