import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "composeApp"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(project.projectDir.path)
                    }
                }
            }
        }
        binaries.executable()
    }
    
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
    }
    
    jvm("desktop")
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        all {
            languageSettings {
                optIn("androidx.compose.material3.ExperimentalMaterial3Api")
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
            }
        }

        val commonMain by getting
        val jbMain by creating {
            dependsOn(commonMain)
        }
        val desktopMain by getting {
            dependsOn(jbMain)
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(jbMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
        val wasmJsMain by getting {
            dependsOn(jbMain)
        }

        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)

            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation("org.jetbrains.compose.material:material-icons-core:1.7.3")

            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.navigation.compose)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

android {
    namespace = "org.jetbrains.nav_cupcake"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "org.jetbrains.nav_cupcake"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    dependencies {
        debugImplementation(libs.compose.ui.tooling)
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.jetbrains.nav_cupcake"
            packageVersion = "1.0.0"
        }

        buildTypes.release.proguard {
            configurationFiles.from(project.file("rules.pro"))
        }
    }
}
