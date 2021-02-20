import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform") // kotlin("jvm") doesn't work well in IDEA/AndroidStudio (https://github.com/JetBrains/compose-jb/issues/22)
    id("org.jetbrains.compose")
}

kotlin {
    jvm {
        withJava()
    }

    sourceSets {
        named("jvmMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(project(":common:utils"))
                implementation(project(":common:database"))
                implementation(project(":common:root"))
                implementation(project(":common:compose-ui"))
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
                implementation(Deps.ArkIvanov.MVIKotlin.mvikotlin)
                implementation(Deps.ArkIvanov.MVIKotlin.mvikotlinMain)
                implementation(Deps.Badoo.Reaktive.reaktive)
                implementation(Deps.Badoo.Reaktive.coroutinesInterop)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "example.todo.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ComoseDesktopTodo"
            modules("java.sql")

            windows {
                menuGroup = "Compose Examples"
                // see https://wixtoolset.org/documentation/manual/v3/howtos/general/generate_guids.html
                upgradeUuid = "BF9CDA6A-1391-46D5-9ED5-383D6E68CCEB"
            }
        }
    }
}
