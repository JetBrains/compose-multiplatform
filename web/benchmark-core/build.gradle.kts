plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
}


kotlin {
    js(IR) {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
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
                implementation(project(":web-core"))
                implementation(kotlin("stdlib-common"))
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(project(":test-utils"))
                implementation(kotlin("test-js"))
            }
        }
    }
}

val BENCHMARKS_PATH = "reports/tests/jsTest/classes/org.jetbrains.compose.web.tests.benchmarks.BenchmarkTests.html"

val printBenchmarkResults by tasks.registering {
    doLast {
        val report = buildDir.resolve(BENCHMARKS_PATH).readText()
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
