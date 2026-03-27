plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

repositories {
    google()
    mavenLocal()
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/cmp/dev")
}

kotlin {
    js(IR) {
        nodejs {}
        browser() {}
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(compose.runtime)
                implementation(project(":html-core"))
            }
        }
    }
}
