import org.jetbrains.compose.gradle.standardConf

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}


kotlin {
    js(IR) {
        browser {
            testTask {
                useKarma {
                    standardConf()
                    // useChromeHeadless()
                    // useFirefox()
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
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(project(":html-test-utils"))
                implementation(kotlin("test-js"))
            }
        }
    }
}

val BENCHMARKS_PATH = "reports/tests/jsTest/classes/org.jetbrains.compose.web.tests.benchmarks.BenchmarkTests.html"

val printBenchmarkResults by tasks.registering {
    doLast {
        val report = layout.buildDirectory.file(BENCHMARKS_PATH).get().asFile.readText()
        val stdout = "#.*;".toRegex().findAll(report).map { it.value }.firstOrNull()

        val benchmarks = stdout?.split(";")?.mapNotNull {
            if (it.isEmpty()) {
                null
            } else {
                val b = it.split(":")
                val testName = b[0].replace("#", "")
                val benchmarkMs = b[1].toInt()

                testName to benchmarkMs
            }
        }?.toMap()

        benchmarks?.forEach {
            // TeamCity messages need to escape '[' and ']' using '|'
            val testName = it.key
                .replace("[", "|[")
                .replace("]", "|]")
            println("##teamcity[buildStatisticValue key='benchmark_$testName' value='${it.value}']")
        }
    }
}

tasks.named("jsTest") { finalizedBy(printBenchmarkResults) }
