group "com.example"
version "1.0-SNAPSHOT"

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://packages.jetbrains.team/maven/p/cmp/dev")
    }
}

plugins {
    kotlin("multiplatform") apply false
    id("org.jetbrains.compose") apply false
}