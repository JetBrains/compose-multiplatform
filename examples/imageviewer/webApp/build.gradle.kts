import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.DefaultIncrementalSyncTask
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

val rootDirPath = project.rootDir.path

kotlin {
    js {
        outputModuleName = "imageviewer"
        browser {
            commonWebpackConfig {
                outputFileName = "imageviewer.js"
            }
        }
        binaries.executable()
        useEsModules()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "imageviewer"
        browser {
             // TODO: uncomment when https://youtrack.jetbrains.com/issue/KT-68614 is fixed (it doesn't work with configuration cache)
//            commonWebpackConfig {
//                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
//                    static = (static ?: mutableListOf()).apply {
//                        // Serve sources to debug inside browser
//                        add(rootDirPath)
//                        add(rootDirPath + "/shared/")
//                        add(rootDirPath + "/webApp/")
//                    }
//                }
//            }
        }
        binaries.executable()
    }

    sourceSets {
        val webMain by creating {
            dependsOn(commonMain.get())
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
            dependsOn(webMain)
        }
        val wasmJsMain by getting {
            dependsOn(webMain)
        }
    }
}
