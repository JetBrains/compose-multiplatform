plugins {
    id("kotlin-multiplatform")
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
                    //useFirefox() // js tests run benchmarks only in Chrome for now
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

val printBenchmarkResults by tasks.registering {
    doLast {
        val report = buildDir.resolve("reports/tests/jsTest/classes/BenchmarkTests.html").readText()
        val stdout = "#.*;".toRegex().findAll(report).map { it.value }.firstOrNull()

        val benchmarks = stdout?.split(";")?.mapNotNull {
            if (it.isEmpty()) {
                null
            } else {
                val b = it.split(":")
                val testName = b[0].replace("#", "")

                // reported testName contains also some info about browser in '[]'
                val reportedTestName = "${testName}\\[.*\\]".toRegex().find(report)?.value

                val benchmarkMs = b[1].toDouble()

                reportedTestName to benchmarkMs
            }
        }?.toMap()

        println("##teamcity[testSuiteStarted name='BenchmarkTests']")
        benchmarks?.forEach {
            if (it.key != null) {
                // TeamCity messages need to escape '[' and ']' using '|'
                val testName = it.key!!
                    .replace("[", "|[")
                    .replace("]", "|]")
                    .replace("ChromeHeadless(\\d*\\.)*\\d*".toRegex(), "ChromeHeadless")

                println("##teamcity[testStarted name='$testName']")
                println("##teamcity[testMetadata name='benchmark avg' type='number' value='${it.value}']")
                println("##teamcity[testFinished name='$testName']")
            }
        }
        println("##teamcity[testSuiteFinished name='BenchmarkTests']")
    }
}

tasks.named("jsTest") { finalizedBy(printBenchmarkResults) }