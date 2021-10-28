pluginManagement {
    buildscript {
        repositories {
            mavenCentral()
            maven("https://maven.pkg.jetbrains.space/public/p/compose/internal")
            maven("https://maven.pkg.jetbrains.space/public/p/space/maven")
        }
        dependencies {
            classpath("org.jetbrains.compose.internal.build-helpers:publishing:0.0.2")
        }
    }
}

includeBuild("frameworks/support")