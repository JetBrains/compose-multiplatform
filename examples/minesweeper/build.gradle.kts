import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.compose.compose

plugins {
    kotlin("multiplatform") version "1.5.31"
    id("org.jetbrains.compose") version "1.0.0-rc6"
}

version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

kotlin {
    jvm()
    js(IR) {
        browser()
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(compose.web.widgets)
                implementation(compose.runtime)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.foundation)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.github.veselovalex.minesweeper.MainKt"
    }
}

// a temporary workaround for a bug in jsRun invocation - see https://youtrack.jetbrains.com/issue/KT-48273
afterEvaluate {
    rootProject.extensions.configure<NodeJsRootExtension> {
        versions.webpack.version = "5.64.4"
        versions.webpackDevServer.version = "4.6.0"
        versions.webpackCli.version = "4.9.1"
    }
}
