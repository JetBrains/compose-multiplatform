import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("maven-publish")
    id("com.android.library")
}

val composeVersion = extra["compose.version"] as String
val coroutinesVersion = extra["kotlinx.coroutines.version"] as String

// TODO: remove this once coroutines are rebuilt with kotlin 1.9.21 and published
configurations.all {
    val isWasm = this.name.contains("wasm", true)
    resolutionStrategy.eachDependency {
        if (requested.module.group == "org.jetbrains.kotlinx" &&
            requested.module.name.contains("kotlinx-coroutines", true)
        ) {
            if (!isWasm) useVersion("1.7.2")
        }

        if (requested.version == "0.22.0-wasm2") {
            useVersion("0.23.1")
        }
    }
}

kotlin {
    jvm("desktop")
    android {
        publishLibraryVariants("release")
    }
    ios()
    iosSimulatorArm64()
    js(IR) {
        browser()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    macosX64()
    macosArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
                implementation(kotlin("test"))
            }
        }
        val commonButJSMain by creating {
            dependsOn(commonMain)
        }
        val skikoMain by creating {
            dependsOn(commonMain)
        }
        val jvmAndAndroidMain by creating {
            dependsOn(commonMain)
        }
        val nativeMain by creating {
            dependsOn(commonMain)
        }
        val desktopMain by getting {
            dependsOn(skikoMain)
            dependsOn(jvmAndAndroidMain)
            dependsOn(commonButJSMain)
        }
        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)

                @OptIn(ExperimentalComposeLibrary::class)
                implementation(compose.uiTestJUnit4)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:$coroutinesVersion")
            }
        }
        val androidMain by getting {
            dependsOn(jvmAndAndroidMain)
            dependsOn(commonButJSMain)
        }
        val androidUnitTest by getting {
            dependencies {

            }
        }
        val iosMain by getting {
            dependsOn(skikoMain)
            dependsOn(commonButJSMain)
            dependsOn(nativeMain)
        }
        val iosTest by getting
        val iosSimulatorArm64Main by getting
        iosSimulatorArm64Main.dependsOn(iosMain)
        val iosSimulatorArm64Test by getting
        iosSimulatorArm64Test.dependsOn(iosTest)

        val jsAndWasmMain by creating {
            dependsOn(skikoMain)
        }
        val jsMain by getting {
            dependsOn(jsAndWasmMain)
        }
        val wasmJsMain by getting {
            dependsOn(jsAndWasmMain)
        }

        val macosMain by creating {
            dependsOn(skikoMain)
            dependsOn(commonButJSMain)
            dependsOn(nativeMain)
        }
        val macosX64Main by getting {
            dependsOn(macosMain)
        }
        val macosArm64Main by getting {
            dependsOn(macosMain)
        }
    }
}

android {
    compileSdk = 33
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 21
        targetSdk = 33
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    testOptions {
        managedDevices {
            devices {
                maybeCreate<com.android.build.api.dsl.ManagedVirtualDevice>("pixel5").apply {
                    device = "Pixel 5"
                    apiLevel = 31
                    systemImageSource = "aosp"
                }
            }
        }
    }
}

dependencies {
    //Android integration tests
    testImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest:1.3.1")
    androidTestImplementation("androidx.compose.ui:ui-test:1.3.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.3.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
}

// TODO it seems that argument isn't applied to the common sourceSet. Figure out why
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}

project.tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs += "-opt-in=kotlinx.cinterop.ExperimentalForeignApi"
        freeCompilerArgs += "-opt-in=kotlin.experimental.ExperimentalNativeApi"
    }
}

configureMavenPublication(
    groupId = "org.jetbrains.compose.components",
    artifactId = "components-resources",
    name = "Resources for Compose JB"
)

project.tasks.configureEach {
    if (name == "compileJsAndWasmMainKotlinMetadata") {
        enabled = false
    }
}

project.tasks.findByName("publishJsPublicationToComposeRepoRepository")
    ?.dependsOn("publishWasmJsPublicationToComposeRepoRepository")

project.tasks.findByName("publishJsPublicationToMavenLocal")
    ?.dependsOn("publishWasmJsPublicationToMavenLocal")