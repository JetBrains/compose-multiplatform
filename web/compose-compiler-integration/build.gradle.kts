fun cloneTemplate(templateName: String, contentMain: String, contentLib: String): File {
    val tempDir = file("${project.buildDir.absolutePath}/temp/cloned-$templateName")
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

fun build(
    caseName: String,
    directory: File,
    failureExpected: Boolean = false,
    vararg buildCmd: String = arrayOf("build", "jsNodeRun")
) {
    val isWin = System.getProperty("os.name").startsWith("Win")
    val procBuilder = if (isWin) {
        ProcessBuilder("gradlew.bat", *buildCmd)
    } else {
        ProcessBuilder("bash", "./gradlew", *buildCmd)
    }
    val proc = procBuilder
        .directory(directory)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    proc.waitFor(5, TimeUnit.MINUTES)

    //println(proc.inputStream.bufferedReader().readText())
    println(proc.errorStream.bufferedReader().readText())

    if (proc.exitValue() != 0 && !failureExpected) {
        throw GradleException("Error compiling $caseName")
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
}

fun runCasesInDirectory(dir: File, filterPath: String, expectCompilationError: Boolean): RunChecksResult {
    return dir.listFiles()!!.filter { it.absolutePath.contains(filterPath) }.mapIndexed { _, file ->
        println("Running check for ${file.name}, expectCompilationError = $expectCompilationError")

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
            build(caseName = caseName, directory = tmpDir, failureExpected = expectCompilationError)
        }.exceptionOrNull()

    }.let {
        RunChecksResult(it.toMap())
    }
}

tasks.register("checkComposeCases") {
    doLast {
        val filterCases = project.findProperty("FILTER_CASES")?.toString() ?: ""

        val expectedFailingCasesDir = File("${projectDir.absolutePath}/testcases/failing")
        val expectedFailingResult = runCasesInDirectory(
            dir = expectedFailingCasesDir,
            expectCompilationError = true,
            filterPath = filterCases
        )

        val passingCasesDir = File("${projectDir.absolutePath}/testcases/passing")
        val passingResult = runCasesInDirectory(
            dir = passingCasesDir,
            expectCompilationError = false,
            filterPath = filterCases
        )

        expectedFailingResult.printResults()
        passingResult.printResults()

        if (expectedFailingResult.hasFailed || passingResult.hasFailed) {
            error("There were failed cases. Check the logs above")
        }
    }
}
