plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.library")
    id("org.jetbrains.compose")
}

kotlin {
    android()
    ios()
    iosSimulatorArm64()
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }
    js(IR) {
        moduleName = project.name
        browser {
            commonWebpackConfig {
                outputFileName = "$moduleName.js"
            }
        }
        binaries.executable()
    }

    cocoapods {
        framework {
            summary = "Shared code for Ballast app"
            homepage = "http://"
            baseName = project.name
            isStatic = true
        }
        ios.deploymentTarget = "11"
        version = "1.11.3"
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":feature:router"))
                implementation(project(":feature:counter"))
                implementation(project(":feature:home"))
                implementation(compose.animation)
                implementation(compose.animationGraphics)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.materialIconsExtended)
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(libs.ballast.core)
                implementation(libs.ballast.navigation)
            }
        }
        val androidMain by getting
        val iosMain by getting {
            dependsOn(commonMain)
        }
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

android {
    namespace = "com.adrianwitaszak.ballastsharedui.${project.name}"
    compileSdk = 33
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

compose {
    desktop {
        application {
            mainClass = "MainKt"
            nativeDistributions {
                targetFormats(
                    org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                    org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                    org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
                )
                packageName = "Ballast"
                packageVersion = "1.0.0"
            }
        }
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}
