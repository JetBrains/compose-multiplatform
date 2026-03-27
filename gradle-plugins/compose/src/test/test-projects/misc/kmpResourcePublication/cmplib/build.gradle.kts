plugins {
    id("org.jetbrains.compose")
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("maven-publish")
    id("com.android.kotlin.multiplatform.library")
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
    androidLibrary {
        compileSdk = 35
        namespace = "me.sample.library"
        minSdk = 23
        androidResources.enable = true
    }
    jvm()
    iosArm64()
    iosSimulatorArm64()
    macosArm64()
    js { browser() }
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.runtime:runtime:COMPOSE_VERSION_PLACEHOLDER")
            implementation("org.jetbrains.compose.material3:material3:1.9.0")
            implementation("org.jetbrains.compose.components:components-resources:COMPOSE_VERSION_PLACEHOLDER")
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "me.sample.library.resources"
}