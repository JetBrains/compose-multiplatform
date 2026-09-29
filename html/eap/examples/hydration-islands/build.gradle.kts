@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.gradle.api.tasks.JavaExec

val renderedSite = layout.buildDirectory.dir("generated/site")
val renderedIndex = renderedSite.map { it.file("index.html") }
val kotlinxBrowserCommonSubsetVersion: String =
    providers.gradleProperty("compose.html.eap.kotlinx-browser-common-subset.version").get()
val composeVersion: String = providers.gradleProperty("compose.version").get()

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvm()
    js(IR) {
        browser {
            commonWebpackConfig { outputFileName = "app.js" }
        }
        binaries.executable()
    }
    wasmJs {
        browser {
            commonWebpackConfig { outputFileName = "app.js" }
        }
        binaries.executable()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
                implementation(project(":html-core-eap"))
                implementation(
                    "org.jetbrains.compose.html:kotlinx-browser-common-subset:" +
                        kotlinxBrowserCommonSubsetVersion
                )
            }
        }
        val webMain by getting {
            resources.srcDir(renderedSite)
        }
    }
}

val jvmMain = kotlin.targets.getByName("jvm").compilations.getByName("main")
val renderHydrationIslandsExample = tasks.register<JavaExec>("renderHydrationIslandsExample") {
    group = "application"
    description = "Renders the type playground and its three hydration islands."
    dependsOn(jvmMain.compileTaskProvider)
    mainClass.set("ServerKt")
    classpath(jvmMain.output.allOutputs)
    classpath(jvmMain.runtimeDependencyFiles)
    args(renderedIndex.get().asFile.absolutePath)
    outputs.file(renderedIndex)
}

listOf("js", "wasmJs").forEach { targetName ->
    tasks.named("${targetName}ProcessResources") {
        dependsOn(renderHydrationIslandsExample)
    }
}
