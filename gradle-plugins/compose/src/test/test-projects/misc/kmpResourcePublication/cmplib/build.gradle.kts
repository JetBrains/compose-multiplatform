plugins {
    id("org.jetbrains.compose")
    kotlin("multiplatform")
    id("maven-publish")
    id("com.android.library")
}

group = "me.sample.library"
version = "1.0"
publishing {
    repositories {
        maven {
            url = uri(rootProject.projectDir.resolve("my-mvn"))
        }
    }
}

kotlin {
    androidTarget { publishLibraryVariants("release") }
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    js { browser() }
    wasmJs { browser() }

    sourceSets {
        all {
            languageSettings {
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
            }
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.material3)
            implementation(compose.components.resources)
        }
    }
}

android {
    namespace = "me.sample.library"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose {
    kotlinCompilerPlugin.set(dependencies.compiler.forKotlin("COMPOSE_COMPILER_PLUGIN_PLACEHOLDER"))
    kotlinCompilerPluginArgs.add("suppressKotlinVersionCompatibilityCheck=KOTLIN_VERSION_PLACEHOLDER")
}

compose.resources {
    publicResClass = true
    resourceProjectId = "me.sample.library.resources"
}