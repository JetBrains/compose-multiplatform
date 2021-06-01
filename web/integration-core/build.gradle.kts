plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}


kotlin {
    jvm {
        tasks.named<Test>("jvmTest") {
            testLogging.showStandardStreams = true

            useJUnitPlatform()

            systemProperty(
                "COMPOSE_WEB_INTEGRATION_TESTS_DISTRIBUTION",
                File(buildDir, "developmentExecutable")
            )
        }
    }

    js(IR) {
        browser() {
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
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(project(":web-core"))
                implementation(kotlin("stdlib-common"))
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation(npm("highlight.js", "10.7.2"))
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation("org.slf4j:slf4j-api:1.7.30")
                implementation("org.slf4j:slf4j-simple:1.7.30")

                implementation("org.seleniumhq.selenium:selenium-java:3.141.59")

                implementation("io.ktor:ktor-server-netty:1.5.4")
                implementation("io.ktor:ktor-server-core:1.5.4")
                implementation("io.ktor:ktor-server-host-common:1.5.4")

                implementation("org.junit.jupiter:junit-jupiter-engine:5.7.1")
                implementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
            }
        }
    }
}

tasks.named<Test>("jvmTest") {
    dependsOn(tasks.named("jsBrowserDevelopmentWebpack"))
}
