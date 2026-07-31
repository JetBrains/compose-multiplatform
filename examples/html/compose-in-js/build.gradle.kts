plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

group = "me.user"
version = "1.0"

repositories {
    google()
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/cmp/dev")
}


kotlin {
    js(IR) {
        browser {
            testTask {
                testLogging.showStandardStreams = true
                useKarma {
                    useChromeHeadless()
                    useFirefox()
                }
            }
        }
        binaries.executable()
    }
    sourceSets {
        val composeVersion = property("compose.version") as String
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.compose.html:html-core:$composeVersion")
                implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

