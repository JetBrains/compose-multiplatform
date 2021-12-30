pluginManagement {
    buildscript {
        repositories {
            mavenCentral()
            maven("https://maven.pkg.jetbrains.space/public/p/compose/internal")
            maven("https://maven.pkg.jetbrains.space/public/p/space/maven")
        }
        dependencies {
            val buildHelpersVersion = System.getProperty("BUILD_HELPERS_VERSION") ?: "0.1.16"
            classpath("org.jetbrains.compose.internal.build-helpers:publishing:$buildHelpersVersion")
        }
    }
}

if (System.getProperty("idea.active") != "true") {
    includeBuild("frameworks/support")
}
