plugins {
    id("com.android.library")
    id("dev.icerock.mobile.multiplatform.android-manifest")
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

android {
    compileSdk = 31

    defaultConfig {
        minSdk = 21
        targetSdk = 31
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    android()
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }
    js(IR) {
        browser()
    }

    val composeUIKitArm64 = iosArm64("composeUIKitArm64")
    val composeUIKitSimulatorArm64 = iosArm64("composeUIKitSimulatorArm64")
    val composeUIKitX64 = iosX64("composeUIKitX64")
    configure(listOf(composeUIKitArm64, composeUIKitSimulatorArm64, composeUIKitX64)) {
        binaries {
            executable {
                entryPoint = "main"
                freeCompilerArgs += listOf(
                    "-linker-option", "-framework", "-linker-option", "Metal",
                    "-linker-option", "-framework", "-linker-option", "CoreText",
                    "-linker-option", "-framework", "-linker-option", "CoreGraphics"
                )
                // TODO: the current compose binary surprises LLVM, so disable checks for now.
                freeCompilerArgs += "-Xdisable-phases=VerifyBitcode"
            }
        }
        compilations.getByName("main") {
            val uiViewProtocol by cinterops.creating {
                defFile(project.file("src/composeUIKitMain/cinterop/UIViewProtocol.def"))
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0-native-mt") {
                    isForce = true
                }
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val composeUiMain by creating {
            dependsOn(commonMain)
            dependencies {
                api(compose.ui)
                api(compose.foundation)
                api(compose.material)
            }
        }
        val androidMain by getting {
            dependsOn(composeUiMain)
            dependencies {
                api("androidx.appcompat:appcompat:1.2.0")
                api("androidx.core:core-ktx:1.3.1")
                implementation("androidx.compose.ui:ui-tooling:1.0.5")
                implementation("androidx.navigation:navigation-compose:2.4.0-rc01")
            }
        }
        val jvmMain by getting {
            dependsOn(composeUiMain)
            dependencies {
                api(compose.preview)
                implementation(compose.desktop.currentOs)
            }
        }
        val jvmTest by getting
        val jsMain by getting {
            dependencies {
                implementation(compose.web.core)
            }
        }

        // compose uikit
        val composeUIKitMain by creating {
            dependsOn(commonMain)
        }
        val composeUIKitArm64Main by getting {
            dependsOn(composeUIKitMain)
        }
        val composeUIKitSimulatorArm64Main by getting {
            dependsOn(composeUIKitMain)
        }
        val composeUIKitX64Main by getting {
            dependsOn(composeUIKitMain)
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
}
