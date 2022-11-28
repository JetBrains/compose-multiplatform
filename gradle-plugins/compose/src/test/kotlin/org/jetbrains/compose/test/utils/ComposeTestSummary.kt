/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test.utils

import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.io.File
import java.io.Writer

class ComposeTestSummary : TestExecutionListener {
    private val summaryFile = TestProperties.summaryFile
    private val isEnabled = summaryFile != null
    private val startNanoTime = hashMapOf<TestIdentifier, Long>()
    private val results = arrayListOf<TestResult>()

    override fun executionStarted(testIdentifier: TestIdentifier) {
        if (isEnabled && testIdentifier.isTest) {
            startNanoTime[testIdentifier] = System.nanoTime()
        }
    }

    override fun executionSkipped(testIdentifier: TestIdentifier, reason: String?) {
        if (isEnabled && testIdentifier.isTest) {
            addTestResult(testIdentifier, TestResult.Status.Skipped, durationMs = null)
        }
    }

    override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
        if (isEnabled && testIdentifier.isTest) {
            val durationMs = (System.nanoTime() - startNanoTime[testIdentifier]!!) / 1_000_000
            val status = when (testExecutionResult.status!!) {
                TestExecutionResult.Status.SUCCESSFUL -> TestResult.Status.Successful
                TestExecutionResult.Status.ABORTED -> TestResult.Status.Aborted
                TestExecutionResult.Status.FAILED ->
                    TestResult.Status.Failed(
                        testExecutionResult.throwable.orElse(null)
                    )
            }
            addTestResult(testIdentifier, status, durationMs = durationMs)
        }
    }

    override fun testPlanExecutionFinished(testPlan: TestPlan) {
        if (isEnabled) {
            MarkdownSummary.write(results, summaryFile!!)
        }
    }

    private fun addTestResult(
        identifier: TestIdentifier,
        status: TestResult.Status,
        durationMs: Long?
    ) {
        val result = TestResult(
            testCase = identifier.displayName,
            testClass = (identifier.source.get() as? MethodSource)?.className ?: "",
            status = status,
            durationMs = durationMs
        )
        results.add(result)
    }
}

internal data class TestResult(
    val testCase: String,
    val testClass: String,
    val durationMs: Long?,
    val status: Status
) {
    sealed class Status {
        object Successful : Status()
        object Aborted : Status()
        object Skipped : Status()
        class Failed(val exception: Throwable?) : Status()
    }

    val displayName: String
        get() = "${testClass.substringAfterLast(".")}.$testCase"
}

internal object MarkdownSummary {
    fun write(testResults: List<TestResult>, file: File) {
        file.parentFile.mkdirs()
        file.bufferedWriter().use { writer ->
            writer.writeSummary(testResults)
        }
    }

    private fun Writer.writeSummary(testResults: List<TestResult>) {
        writeLn()
        writeLn("|Status|Test case|Duration|")
        writeLn("|---|---|---:|")

        for (result in testResults) {
            val status = when (result.status) {
                is TestResult.Status.Successful -> ":white_check_mark:"
                is TestResult.Status.Aborted -> ":fast_forward:"
                is TestResult.Status.Failed -> ":x:"
                is TestResult.Status.Skipped -> ":fast_forward:"
            }
            writeLn("|$status|${result.displayName}|${result.durationMs ?: 0} ms|")
        }

        val failedTests = testResults.filter { it.status is TestResult.Status.Failed }
        if (failedTests.isEmpty()) return

        writeLn("#### ${failedTests.size} failed tests")
        for (failedTest in failedTests) {
            withDetails(failedTest.displayName) {
                withHtmlTag("samp") {
                    val exception = (failedTest.status as TestResult.Status.Failed).exception
                    val stacktrace = exception?.stackTraceToString() ?: ""
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