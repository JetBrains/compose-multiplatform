import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("maven-publish")
    id("com.android.library")
}

val composeVersion = extra["compose.version"] as String

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
    macosX64()
    macosArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
                implementation("org.jetbrains.compose.foundation:foundation:$composeVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
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
                implementation("org.jetbrains.compose.ui:ui-test-junit4:$composeVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.6.4")
            }
        }
        val androidMain by getting {
            dependsOn(jvmAndAndroidMain)
            dependsOn(commonButJSMain)
        }
        val androidTest by getting {
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
        val jsMain by getting {
            dependsOn(skikoMain)
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
        minSdk = 24
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
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
}

// TODO it seems that argument isn't applied to the common sourceSet. Figure out why
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}

configureMavenPublication(
    groupId = "org.jetbrains.compose.components",
    artifactId = "components-resources",
    name = "Resources for Compose JB"
)
