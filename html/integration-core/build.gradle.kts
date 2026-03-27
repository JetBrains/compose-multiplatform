import org.jetbrains.compose.gradle.standardConf

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

val integrationTestsEnabled: Boolean = project.properties.getValue("integrationTestsEnabled") == "true"

kotlin {
    if (integrationTestsEnabled) {
        jvm {
            tasks.named<Test>("jvmTest") {
                useJUnitPlatform()

                systemProperty(
                    "COMPOSE_WEB_INTEGRATION_TESTS_DISTRIBUTION",
                    layout.buildDirectory.dir("developmentExecutable").get().asFile
                )
            }
        }
    }

    js(IR) {
        browser() {
            testTask {
                useKarma {
                    standardConf()
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
                implementation(project(":html-core"))
                implementation(kotlin("stdlib-common"))
                implementation(libs.kotlinx.coroutines.core)
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

        if (integrationTestsEnabled) {
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
                    implementation("org.junit.jupiter:junit-jupiter-params:5.7.1")
                }
            }
        }
    }
}

if (integrationTestsEnabled) {
    tasks.named<Test>("jvmTest") {
        dependsOn(tasks.named("jsBrowserDevelopmentWebpack"))

        listOf(
            "webdriver.chrome.driver",
            "webdriver.gecko.driver",
        ).forEach {
            if (rootProject.hasProperty(it)) {
                println("${it} => ${rootProject.extensions.getByName(it)}")
                systemProperty(it, rootProject.extensions.getByName(it))
            }
        }

        listOf(
            "compose.web.tests.integration.withFirefox"
        ).forEach { propName ->
            if (project.hasProperty(propName)) {
                systemProperty(propName, "true")
            }
        }
    }
}

// a temporary workaround for a bug in jsRun invocation - see https://youtrack.jetbrains.com/issue/KT-48273
afterEvaluate {
    rootProject.extensions.configure<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension> {
        versions.webpackDevServer.version = "4.0.0"
        versions.webpackCli.version = "4.10.0"
    }
}
