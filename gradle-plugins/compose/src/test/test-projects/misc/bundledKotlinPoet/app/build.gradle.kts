plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("com.github.gmazzo.buildconfig")
}

group = "app.group"

kotlin {
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.material)
                implementation(compose.components.resources)
            }
        }
    }
}

buildConfig {
    buildConfigField(String::class.java, "str", "")
}
