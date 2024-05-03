import org.jetbrains.kotlin.gradle.targets.js.ir.DefaultIncrementalSyncTask
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

val copyJsResources = tasks.create("copyJsResourcesWorkaround", Copy::class.java) {
    from(project(":shared").file("src/commonMain/composeResources"))
    into("build/processedResources/js/main")
}

tasks.withType<DefaultIncrementalSyncTask> {
    dependsOn(copyJsResources)
}

val copyWasmResources = tasks.create("copyWasmResourcesWorkaround", Copy::class.java) {
    from(project(":shared").file("src/commonMain/composeResources"))
    into("build/processedResources/wasmJs/main")
}

afterEvaluate {
    project.tasks.getByName("jsProcessResources").finalizedBy(copyJsResources)
    project.tasks.getByName("wasmJsProcessResources").finalizedBy(copyWasmResources)
    project.tasks.getByName("wasmJsDevelopmentExecutableCompileSync").dependsOn(copyWasmResources)
}

val rootDirPath = project.rootDir.path

kotlin {
    js {
        moduleName = "imageviewer"
        browser {
            commonWebpackConfig {
                outputFileName = "imageviewer.js"
            }
        }
        binaries.executable()
        useEsModules()
    }

    wasmJs {
        moduleName = "imageviewer"
        browser {
            commonWebpackConfig {
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(rootDirPath)
                        add(rootDirPath + "/shared/")
                        add(rootDirPath + "/webApp/")
                    }
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val jsWasmMain by creating {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.material)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)
            }
        }
        val jsMain by getting {
            dependsOn(jsWasmMain)
        }
        val wasmJsMain by getting {
            dependsOn(jsWasmMain)
        }
    }
}

compose.experimental {
    web.application {}
}