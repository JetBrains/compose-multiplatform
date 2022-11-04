/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.testing.Test as GradleTestTask
import org.gradle.api.tasks.testing.TestDescriptor as GradleTestDescriptor
import org.gradle.api.tasks.testing.TestResult as GradleTestResult
import org.gradle.kotlin.dsl.KotlinClosure2
import java.io.File
import java.io.Writer

fun Project.configureGithubTestsSummary(file: File) {
    val summaryService = gradle.sharedServices.registerIfAbsent(
        "compose.build.services.github.summary",
        SummaryService::class.java
    ) {
        parameters.summaryFile.set(file)
    }

    allprojects {
        tasks.withType(GradleTestTask::class.java).configureEach {
            val taskPath = this.path
            doFirst {
                summaryService.get()
            }
            afterTest { test, result ->
                SummaryService.add(taskPath, TestResult(test, result))
            }
            usesService(summaryService)
        }
    }
}

abstract class SummaryService : BuildService<SummaryService.Params>, AutoCloseable {
    interface Params : BuildServiceParameters {
        val summaryFile: Property<File>
    }

    override fun close() {
        parameters.summaryFile.get().writer().buffered().use { writer ->
            withTaskTestResults {
                SummaryWriter.writeFailedTestsSummary(writer, it)
            }
        }
        clearStaticState()
    }

    internal companion object {
        private val taskTestsResults = hashMapOf<String, MutableList<TestResult>>()

        @Synchronized
        fun withTaskTestResults(fn: (Map<String, List<TestResult>>) -> Unit) {
            fn(taskTestsResults)
        }

        @Synchronized
        fun add(taskPath: String, result: TestResult) {
            val taskResults = taskTestsResults.getOrPut(taskPath) { arrayListOf() }
            taskResults.add(result)
        }

        @Synchronized
        fun clearStaticState() {
            taskTestsResults.clear()
        }
    }
}

private object SummaryWriter {
    fun writeFailedTestsSummary(writer: Writer, taskResults: Map<String, List<TestResult>>) {
        for ((task, tests) in taskResults.entries.sortedBy { it.key }) {
            writer.writeLn("### $task")

            writer.writeTaskSummary(task, tests)
            writer.writeFailedTestTaskSummary(task, tests)
        }
    }

    private fun Writer.writeTaskSummary(task: String, tests: List<TestResult>) {
        writeLn()
        writeLn("|Status|Test case|Duration|")
        writeLn("|---|---|---:|")

        for (test in tests) {
            val status = when (test.resultType) {
                GradleTestResult.ResultType.SUCCESS -> ":white_check_mark:"
                GradleTestResult.ResultType.FAILURE -> ":x:"
                GradleTestResult.ResultType.SKIPPED -> ":heavy_minus_sign:"
            }
            val suiteShortName = test.suite.split(".").last()
            val testName = test.testCase
            writeLn("|$status|${test.displayName}|${test.durationMs} ms|")
        }
    }

    private fun Writer.writeFailedTestTaskSummary(task: String, tests: List<TestResult>) {
        val failedTests = tests.filter { it.resultType == GradleTestResult.ResultType.FAILURE }
        if (failedTests.isEmpty()) return

        writeLn("#### ${failedTests.size} failed tests")
        for (failedTest in failedTests) {
            withDetails("${failedTest.displayName}") {
                withHtmlTag("samp") {
                    val stacktrace = failedTest.exception?.stackTraceToString() ?: ""
                    write(stacktrace.replace("\n", "<br/>"))
                }
            }
            writeLn()
        }
    }

    private inline fun Writer.withDetails(summary: String, details: Writer.() -> Unit) {
        withHtmlTag("details") {
            withHtmlTag("summary") {
                write(summary)
            }

            details()
        }
    }

    private inline fun Writer.withHtmlTag(tag: String, fn: Writer.() -> Unit) {
        writeLn("<$tag>")
        fn()
        writeLn("</$tag>")
    }

    private fun Writer.writeLn(str: String = "") {
        write(str)
        write("\n")
    }
}

internal data class TestResult(
    val testCase: String,
    val suite: String,
    val durationMs: Long,
    val resultType: GradleTestResult.ResultType,
    val exception: Throwable?
) {
    constructor(test: GradleTestDescriptor, result: GradleTestResult) : this(
        testCase = test.displayName,
        suite = test.className ?: "",
        durationMs = result.endTime - result.startTime,
        resultType = result.resultType,
        exception = result.exception
    )

    val displayName: String
        get() = "${suite.substringAfterLast(".")}.$testCase"
}

private inline fun <A, B> gradleClosure(crossinline fn: (A, B) -> Unit): KotlinClosure2<A, B, Void> =
    KotlinClosure2<A, B, Void>({ a, b ->
        fn(a, b)
        null
    })

private inline fun GradleTestTask.afterTest(crossinline fn: (GradleTestDescriptor, GradleTestResult) -> Unit): Unit {
    afterTest(gradleClosure(fn))
}

