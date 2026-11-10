plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    kotlin("jvm") apply false
    kotlin("multiplatform") apply false
    kotlin("plugin.compose") apply false
    kotlin("android") apply false
    id("com.android.application") apply false
    id("com.android.library") apply false
    id("org.jetbrains.compose") apply false
}

subprojects {
    repositories {
        google()
        mavenCentral {
            url = uri("https://cache-redirector.jetbrains.com/maven-central")
        }
        maven("https://packages.jetbrains.team/maven/p/cmp/dev")
    }
}
