import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("maven-publish")
    id("com.android.library")
}

val composeVersion = extra["compose.version"] as String

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    targetHierarchy.default()
    jvm("desktop")
    androidTarget {
        publishLibraryVariants("release")
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    js {
        browser {
            testTask(Action {
                enabled = false
            })
        }
    }
    macosX64()
    macosArm64()

    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }

        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
                implementation("org.jetbrains.compose.foundation:foundation:$composeVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
                implementation(kotlin("test"))
            }
        }
        val commonButJSMain by creating {
            dependsOn(commonMain)
        }
        val commonButJSTest by creating {
            dependsOn(commonTest)
        }
        val skikoMain by creating {
            dependsOn(commonMain)
        }
        val skikoTest by creating {
            dependsOn(commonTest)
        }
        val jvmAndAndroidMain by creating {
            dependsOn(commonMain)
        }
        val jvmAndAndroidTest by creating {
            dependsOn(commonTest)
        }
        val desktopMain by getting {
            dependsOn(skikoMain)
            dependsOn(jvmAndAndroidMain)
            dependsOn(commonButJSMain)
        }
        val desktopTest by getting {
            dependsOn(skikoTest)
            dependsOn(jvmAndAndroidTest)
            dependsOn(commonButJSTest)
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.compose.ui:ui-test-junit4:$composeVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
            }
        }
        val androidMain by getting {
            dependsOn(jvmAndAndroidMain)
            dependsOn(commonButJSMain)
        }
        val androidInstrumentedTest by getting {
            dependsOn(commonTest)
            dependsOn(jvmAndAndroidTest)
            dependsOn(commonButJSTest)
        }
        val iosMain by getting {
            dependsOn(skikoMain)
            dependsOn(commonButJSMain)
        }
        val iosTest by getting {
            dependsOn(skikoTest)
            dependsOn(commonButJSTest)
        }
        val jsMain by getting {
            dependsOn(skikoMain)
        }
        val jsTest by getting {
            dependsOn(skikoTest)
        }
        val macosMain by getting {
            dependsOn(skikoMain)
            dependsOn(commonButJSMain)
        }
        val macosTest by getting {
            dependsOn(skikoTest)
            dependsOn(commonButJSTest)
        }
    }
}

android {
    compileSdk = 34
    namespace = "org.jetbrains.compose.components.resources"
    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    @Suppress("UnstableApiUsage")
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
    dependencies {
        //Android integration tests
        testImplementation("androidx.test:core:1.5.0")
        androidTestImplementation("androidx.compose.ui:ui-test-manifest:1.5.3")
        androidTestImplementation("androidx.compose.ui:ui-test:1.5.3")
        androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.3")
    }
}

configureMavenPublication(
    groupId = "org.jetbrains.compose.components",
    artifactId = "components-resources",
    name = "Resources for Compose JB"
)
