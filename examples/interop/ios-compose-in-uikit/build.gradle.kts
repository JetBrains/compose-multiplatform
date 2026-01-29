plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    kotlin("multiplatform") apply false
    kotlin("plugin.compose") apply false
    id("org.jetbrains.compose") apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://packages.jetbrains.team/maven/p/cmp/dev")
    }
}
