import org.jetbrains.compose.gradle.standardConf

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}


kotlin {
    js(IR) {
        browser() {
            testTask {
                useKarma {
                    standardConf()
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(project(":compose-compiler-integration-lib"))
                implementation(kotlin("stdlib-js"))
                implementation(compose.runtime)
                implementation(project(":html-core"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}


fun cloneTemplate(templateName: String, contentMain: String, contentLib: String): File {
    val tempDir = layout.buildDirectory.dir("temp/cloned-$templateName").get().asFile
    tempDir.deleteRecursively()
    tempDir.mkdirs()
    file("${projectDir.absolutePath}/main-template").copyRecursively(tempDir)
    // tempDir.deleteOnExit()
    File("$tempDir/src/commonMain/kotlin/Main.kt").printWriter().use { out ->
        out.println(contentMain)
    }
    File("$tempDir/lib/src/commonMain/kotlin/Lib.kt").printWriter().use { out ->
        out.println(contentLib)
    }
    return tempDir
}

private fun build(
    caseName: String,
    directory: File,
    failureExpected: Boolean = false,
    composeVersion: String,
    kotlinVersion: String,
    vararg buildCmd: String = arrayOf("build", "jsNodeDevelopmentRun")
) {
    val isWin = System.getProperty("os.name").startsWith("Win")
    val arguments = buildCmd.toMutableList().also {
        it.add("-Pcompose.version=$composeVersion")
        it.add("-Pkotlin.version=$kotlinVersion")
        it.add("--stacktrace")
        it.add("--info")
    }.toTypedArray()

    val gradlewFile = File(directory, if (isWin) "gradlew.bat" else "gradlew")

    println("[compose-compiler-integration] Working directory: ${directory.absolutePath}")

    if (!gradlewFile.exists()) {
        throw GradleException("gradlew not found in ${directory.absolutePath}. Please ensure the Gradle wrapper is present.")
    }

    if (!isWin) {
        if (!gradlewFile.canExecute()) {
            val isExecutable = gradlewFile.setExecutable(true)
            if (!isExecutable) {
                throw GradleException("Failed to make gradlew executable: ${gradlewFile.absolutePath}")
            }
        }
    }

    val command: List<String> = if (isWin) {
        listOf("cmd", "/c", "gradlew.bat") + arguments
    } else {
        listOf("./gradlew") + arguments
    }
    println("[compose-compiler-integration] Executing: ${command.joinToString(" ")}")

    val proc = try {
        ProcessBuilder(command)
            .directory(directory)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
    } catch (e: Exception) {
        throw GradleException("Failed to start Gradle process. Command: ${command.joinToString(" ")}", e)
    }

    val finished = proc.waitFor(10, TimeUnit.MINUTES)
    if (!finished) {
        proc.destroyForcibly()
        throw GradleException("Gradle process timed out for $caseName. Command: ${command.joinToString(" ")}")
    }

    if (proc.exitValue() != 0 && !failureExpected) {
        throw GradleException("Error compiling $caseName (exit code ${proc.exitValue()})")
    }

    if (failureExpected && proc.exitValue() == 0) {
        throw AssertionError("$caseName compilation did not fail!!!")
    }
}

data class RunChecksResult(
    val cases: Map<String, Throwable?>
) {
    val totalCount = cases.size
    val failedCount = cases.filter { it.value != null }.size
    val hasFailed = failedCount > 0

    fun printResults() {
        cases.forEach { (name, throwable) ->
            println(name + " : " + (throwable ?: "OK"))
        }
    }

    fun reportToTeamCity() {
        cases.forEach { (caseName, error) ->
            println("##teamcity[testStarted name='compileTestCase_$caseName']")
            if (error != null) {
                println("##teamcity[testFailed name='compileTestCase_$caseName']")
            }
            println("##teamcity[testFinished name='compileTestCase_$caseName']")
        }
    }
}

fun runCasesInDirectory(
    dir: File,
    filterPath: String,
    expectCompilationError: Boolean,
    composeVersion: String,
    kotlinVersion: String
): RunChecksResult {
    return dir.listFiles()!!.filter { it.absolutePath.contains(filterPath) }.mapIndexed { _, file ->
        println("Running check for ${file.name}, expectCompilationError = $expectCompilationError, composeVersion = $composeVersion")

        val contentLines = file.readLines()
        val startMainLineIx = contentLines.indexOf("// @Module:Main").let { ix ->
            if (ix == -1) 0 else ix + 1
        }

        val startLibLineIx = contentLines.indexOf("// @Module:Lib").let { ix ->
            if (ix == -1) contentLines.size else ix - 1
        }

        require(startMainLineIx < startLibLineIx) {
            "The convention is that @Module:Lib should go after @Module:Main"
        }

        val mainContent = contentLines.let { lines ->
            val endLineIx = if (startLibLineIx < lines.size) startLibLineIx - 1 else lines.lastIndex
            lines.slice(startMainLineIx..endLineIx).joinToString(separator = "\n")
        }

        val libContent = contentLines.let { lines ->
            if (startLibLineIx < lines.size) {
                lines.slice(startLibLineIx..lines.lastIndex)
            } else {
                emptyList()
            }.joinToString(separator = "\n")
        }

        val caseName = file.name
        val tmpDir = cloneTemplate(caseName, contentMain = mainContent, contentLib = libContent)

        caseName to kotlin.runCatching {
            build(
                caseName = caseName,
                directory = tmpDir,
                failureExpected = expectCompilationError,
                composeVersion = composeVersion,
                kotlinVersion = kotlinVersion
            )
        }.exceptionOrNull()

    }.let {
        RunChecksResult(it.toMap())
    }
}

tasks.register("checkComposeCases") {
    doLast {
        val filterCases = project.findProperty("FILTER_CASES")?.toString() ?: ""
        val composeVersion = project.findProperty("compose.version")?.toString() ?: "0.0.0-SNASPHOT"
        val kotlinVersion = kotlin.coreLibrariesVersion

        val expectedFailingCasesDir = File("${projectDir.absolutePath}/testcases/failing")
        val expectedFailingResult = runCasesInDirectory(
            dir = expectedFailingCasesDir,
            expectCompilationError = true,
            filterPath = filterCases,
            composeVersion = composeVersion,
            kotlinVersion = kotlinVersion
        )

        val passingCasesDir = File("${projectDir.absolutePath}/testcases/passing")
        val passingResult = runCasesInDirectory(
            dir = passingCasesDir,
            expectCompilationError = false,
            filterPath = filterCases,
            composeVersion = composeVersion,
            kotlinVersion = kotlinVersion
        )

        expectedFailingResult.printResults()
        expectedFailingResult.reportToTeamCity()

        passingResult.printResults()
        passingResult.reportToTeamCity()

        if (expectedFailingResult.hasFailed || passingResult.hasFailed) {
            error("There were failed cases. Check the logs above")
        }
    }
}
