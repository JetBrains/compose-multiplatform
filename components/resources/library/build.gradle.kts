import kotlinx.validation.ExperimentalBCVApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("maven-publish")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

kotlin {
    jvm("desktop")
    android {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        namespace = "org.jetbrains.compose.components.resources"
        compileSdk = 37
        minSdk = 23

        androidResources.enable = true

        withHostTest {
            isIncludeAndroidResources = true
        }

        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    iosArm64()
    iosSimulatorArm64()
    js {
        browser {
            testTask {
                useKarma { useChromeHeadless() }
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        compilations.getByName("test").compileTaskProvider.configure {
            // https://youtrack.jetbrains.com/issue/KT-69014
            compilerOptions.freeCompilerArgs.add("-Xwasm-enable-array-range-checks")
        }
        browser {
            testTask {
                useKarma { useChromeHeadless() }
            }
        }
        binaries.executable()
    }
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
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui.test)
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
                implementation(libs.compose.desktop.jvm)
            }
        }
        val androidMain by getting {
            dependsOn(jvmAndAndroidMain)
            dependencies {
                //it will be called only in android instrumented tests where the library should be available
                compileOnly(libs.androidx.test.monitor)
            }
        }
        val androidDeviceTest by getting {
            dependencies {
                implementation(libs.androidx.test.core)
                implementation(libs.androidx.compose.ui.test)
                implementation(libs.androidx.compose.ui.test.manifest)
                implementation(libs.androidx.compose.ui.test.junit4)
            }
        }
        val androidHostTest by getting {
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
        val webMain by getting {
            dependsOn(skikoMain)
            dependencies {
                 implementation(libs.kotlinx.browser)
            }
        }
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

tasks {
    val desktopTestProcessResources =
        named<ProcessResources>("desktopTestProcessResources")

    withType<Test> {
        dependsOn(desktopTestProcessResources)
        environment("RESOURCES_PATH", desktopTestProcessResources.map { it.destinationDir.absolutePath }.get())
    }
}