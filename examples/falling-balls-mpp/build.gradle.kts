import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

buildscript {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        mavenLocal()
    }
}

plugins {
    kotlin("multiplatform") version "1.6.0"
    //id("org.jetbrains.compose") version "1.1.0-beta04"
    id("org.jetbrains.compose") version "0.1.0-SNAPSHOT"
}

version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    mavenLocal()
}

dependencies {
    // TODO: this should not be needed eventyally.
    kotlinNativeCompilerPluginClasspath(files("/Users/jetbrains/.m2/repository/androidx/compose/compiler/compiler-hosted/1.1.0-beta04/compiler-hosted-1.1.0-beta04.jar"))
}

kotlin {
    jvm()
    js(IR) {
        browser()
        binaries.executable()
    }
    macosX64() {
        binaries { 
            executable {
                entryPoint = "androidx.compose.native.demo.main"
                freeCompilerArgs += listOf(
                    "-linker-option", "-framework", "-linker-option", "Metal"
                )
            }
        }
    }
    iosX64("uikitX64") {
        binaries.executable()
    }
    iosArm64("uikitArm64") {
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }

        val jvmMain by getting {
             dependencies {
                implementation(compose.desktop.currentOs)
             }
        }

        val jsMain by getting {
            dependencies {
                //implementation(compose.web.widgets)
                implementation(compose.runtime)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val nativeMain by creating {
        }
        val macosMain by creating {
            dependsOn(nativeMain)
        }
        val macosX64Main by getting {
            dependsOn(macosMain)
            dependencies {
                implementation("androidx.compose.runtime:runtime-macosx64:1.1.0-beta04")
                implementation("androidx.compose.ui:ui-macosx64:1.1.0-beta04")
                implementation("androidx.compose.ui:ui-graphics-macosx64:1.1.0-beta04")
                implementation("androidx.compose.ui:ui-text-macosx64:1.1.0-beta04")
                implementation("androidx.compose.ui:ui-util-macosx64:1.1.0-beta04")
                implementation("androidx.compose.ui:ui-geometry-macosx64:1.1.0-beta04")
                implementation("androidx.compose.ui:ui-unit-macosx64:1.1.0-beta04")
                implementation("androidx.compose.foundation:foundation-macosx64:1.1.0-beta04")
                implementation("androidx.compose.material:material-macosx64:1.1.0-beta04")

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0-RC")
                implementation("org.jetbrains.skiko:skiko:0.5.12")
            }
        }
        val uikitMain by creating {
            dependsOn(nativeMain)
        }
        val uikitX64Main by getting {
            dependsOn(uikitMain)
        }
        val uikitArm64Main by getting {
            dependsOn(uikitMain)
        }

    }
}

compose.desktop {
    application {
        mainClass = "org.jetbrains.compose.common.demo.AppKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ImageViewer"
            packageVersion = "1.0.0"

            modules("jdk.crypto.ec")

            val iconsRoot = project.file("../common/src/desktopMain/resources/images")
            macOS {
                iconFile.set(iconsRoot.resolve("icon-mac.icns"))
            }
            windows {
                iconFile.set(iconsRoot.resolve("icon-windows.ico"))
                menuGroup = "Compose Examples"
                // see https://wixtoolset.org/documentation/manual/v3/howtos/general/generate_guids.html
                upgradeUuid = "18159995-d967-4CD2-8885-77BFA97CFA9F"
            }
            linux {
                iconFile.set(iconsRoot.resolve("icon-linux.png"))
            }
        }
    }
}

// a temporary workaround for a bug in jsRun invocation - see https://youtrack.jetbrains.com/issue/KT-48273
afterEvaluate {
    rootProject.extensions.configure<NodeJsRootExtension> {
        versions.webpackDevServer.version = "4.0.0"
        versions.webpackCli.version = "4.9.0"
    }
}
