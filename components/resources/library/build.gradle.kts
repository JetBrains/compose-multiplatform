import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

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
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            testTask(Action {
                useKarma {
                    useChromeHeadless()
                    useConfigDirectory(project.projectDir.resolve("karma.config.d").resolve("wasm"))
                }
            })
        }
        binaries.executable()
    }
    macosX64()
    macosArm64()

    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlin.experimental.ExperimentalNativeApi")
            }
        }

        //          common
        //       ┌────┴────┐
        //    skiko       blocking
        //      │      ┌─────┴────────┐
        //  ┌───┴───┬──│────────┐     │
        //  │      native       │ jvmAndAndroid
        //  │    ┌───┴───┐      │   ┌───┴───┐
        // web   ios    macos   desktop    android

        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
                implementation(kotlin("test"))
            }
        }
        val blockingMain by creating {
            dependsOn(commonMain)
        }
        val blockingTest by creating {
            dependsOn(commonTest)
        }
        val skikoMain by creating {
            dependsOn(commonMain)
        }
        val skikoTest by creating {
            dependsOn(commonTest)
        }
        val jvmAndAndroidMain by creating {
            dependsOn(blockingMain)
            dependencies {
                implementation(compose.material3)
            }
        }
        val jvmAndAndroidTest by creating {
            dependsOn(blockingTest)
        }
        val desktopMain by getting {
            dependsOn(skikoMain)
            dependsOn(jvmAndAndroidMain)
        }
        val desktopTest by getting {
            dependsOn(skikoTest)
            dependsOn(jvmAndAndroidTest)
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.desktop.uiTestJUnit4)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
        val androidMain by getting {
            dependsOn(jvmAndAndroidMain)
        }
        val androidInstrumentedTest by getting {
            dependsOn(jvmAndAndroidTest)
            dependencies {
                implementation("androidx.test:core:1.5.0")
                implementation("androidx.compose.ui:ui-test-manifest:1.5.4")
                implementation("androidx.compose.ui:ui-test:1.5.4")
                implementation("androidx.compose.ui:ui-test-junit4:1.5.4")
            }
        }
        val androidUnitTest by getting {
            dependsOn(jvmAndAndroidTest)
        }
        val nativeMain by getting {
            dependsOn(skikoMain)
            dependsOn(blockingMain)
        }
        val nativeTest by getting {
            dependsOn(skikoTest)
            dependsOn(blockingTest)
        }
        val webMain by creating {
            dependsOn(skikoMain)
        }
        val jsMain by getting {
            dependsOn(webMain)
        }
        val wasmJsMain by getting {
            dependsOn(webMain)
        }
        val webTest by creating {
            dependsOn(skikoTest)
        }
        val jsTest by getting {
            dependsOn(webTest)
        }
        val wasmJsTest by getting {
            dependsOn(webTest)
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
    sourceSets {
        val commonTestResources = "src/commonTest/resources"
        named("androidTest") {
            resources.srcDir(commonTestResources)
            assets.srcDir("src/androidInstrumentedTest/assets")
        }
        named("test") { resources.srcDir(commonTestResources) }
    }
}

configureMavenPublication(
    groupId = "org.jetbrains.compose.components",
    artifactId = "components-resources",
    name = "Resources for Compose JB"
)

// adding it here to make sure skiko is unpacked and available in web tests
compose.experimental {
    web.application {}
}

afterEvaluate {
    // TODO(o.k.): remove this after we refactor jsAndWasmMain source set in skiko to get rid of broken "common" js-interop
    tasks.configureEach {
        if (name == "compileWebMainKotlinMetadata") enabled = false
    }
}
