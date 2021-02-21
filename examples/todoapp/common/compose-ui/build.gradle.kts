plugins {
    id("multiplatform-compose-setup")
    id("android-setup")
}

kotlin {
    sourceSets {
        named("commonMain") {
            dependencies {
                implementation(project(":common:main"))
                implementation(project(":common:edit"))
                implementation(project(":common:root"))
                implementation(Deps.ArkIvanov.Decompose.decompose)
                implementation(Deps.ArkIvanov.Decompose.extensionsCompose) {
                    // TODO remove when we will not be using 0.0.0-unmerged version
                    exclude(group = "org.jetbrains.compose.desktop")
                    exclude(group = "org.jetbrains.compose.animation")
                    exclude(group = "org.jetbrains.compose.foundation")
                    exclude(group = "org.jetbrains.compose.material")
                    exclude(group = "org.jetbrains.compose.runtime")
                    exclude(group = "org.jetbrains.compose.ui")
                }
            }
        }
    }
}
