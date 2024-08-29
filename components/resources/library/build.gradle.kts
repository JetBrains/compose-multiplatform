import kotlinx.validation.ExperimentalBCVApi
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("maven-publish")
    id("com.android.library")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

kotlin {
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

    applyDefaultHierarchyTemplate()
    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlin.experimental.ExperimentalNativeApi")
                optIn("org.jetbrains.compose.resources.InternalResourceApi")
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
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
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(compose.material3)
                @OptIn(ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
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
            }
        }
        val androidMain by getting {
            dependsOn(jvmAndAndroidMain)
            dependencies {
                //it will be called only in android instrumented tests where the library should be available
                compileOnly(libs.androidx.test.monitor)
            }
        }
        val androidInstrumentedTest by getting {
            dependsOn(jvmAndAndroidTest)
            dependencies {
                implementation(libs.androidx.test.core)
                implementation(libs.androidx.compose.ui.test)
                implementation(libs.androidx.compose.ui.test.manifest)
                implementation(libs.androidx.compose.ui.test.junit4)
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
        named("main") { manifest.srcFile("src/androidMain/AndroidManifest.xml") }
    }
}

configureMavenPublication(
    groupId = "org.jetbrains.compose.components",
    artifactId = "components-resources",
    name = "Resources for Compose JB"
)

apiValidation {
    @OptIn(ExperimentalBCVApi::class)
    klib { enabled = true }
    nonPublicMarkers.add("org.jetbrains.compose.resources.InternalResourceApi")
}

//utility task to generate CLDRPluralRuleLists.kt file by 'CLDRPluralRules/plurals.xml'
tasks.register<GeneratePluralRuleListsTask>("generatePluralRuleLists") {
    val projectDir = project.layout.projectDirectory
    pluralsFile = projectDir.file("CLDRPluralRules/plurals.xml")
    outputFile = projectDir.file("src/commonMain/kotlin/org/jetbrains/compose/resources/plural/CLDRPluralRuleLists.kt")
    samplesOutputFile = projectDir.file("src/commonTest/kotlin/org/jetbrains/compose/resources/CLDRPluralRuleLists.test.kt")
}

afterEvaluate {
    // TODO(o.k.): remove this after we refactor jsAndWasmMain source set in skiko to get rid of broken "common" js-interop
    tasks.configureEach {
        if (name == "compileWebMainKotlinMetadata") enabled = false
    }
}
