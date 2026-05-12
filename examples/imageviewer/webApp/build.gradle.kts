import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
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
                implementation(projects.shared)
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material)
                implementation(libs.components.resources)
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
