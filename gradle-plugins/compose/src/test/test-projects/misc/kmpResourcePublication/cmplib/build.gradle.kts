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
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()
    js { browser() }
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.material3)
            implementation(compose.components.resources)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "me.sample.library.resources"
}