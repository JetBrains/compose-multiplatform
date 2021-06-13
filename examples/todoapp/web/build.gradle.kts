import org.jetbrains.compose.compose

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    js(IR) {
        browser {
            useCommonJs()
            binaries.executable()
        }
    }

    sourceSets {
        named("jsMain") {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.web.widgets)
                implementation(project(":common:utils"))
                implementation(project(":common:database"))
                implementation(project(":common:root"))
                implementation(project(":common:main"))
                implementation(project(":common:edit"))
                implementation(Deps.ArkIvanov.Decompose.decompose)
                implementation(Deps.ArkIvanov.MVIKotlin.mvikotlin)
                implementation(Deps.ArkIvanov.MVIKotlin.mvikotlinMain)
                implementation(npm("copy-webpack-plugin", "9.0.0"))
                implementation(npm("@material-ui/icons", "4.11.2"))
            }
        }
    }
}
