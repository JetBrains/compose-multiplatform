package org.jetbrains.compose.test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

internal fun <T> Collection<T>.checkContains(vararg elements: T) {
    val expectedElements = elements.toMutableSet()
    forEach { expectedElements.remove(it) }
    if (expectedElements.isNotEmpty()) {
        error("Expected elements are missing from the collection: [${expectedElements.joinToString(", ")}]")
    }
}

internal fun BuildResult.checks(fn: (BuildResultChecks) -> Unit) {
    fn(BuildResultChecks(this))
}

internal class BuildResultChecks(private val result: BuildResult) {
    fun logContains(substring: String) {
        if (!result.output.contains(substring)) {
            throw AssertionError("Test output does not contain the expected string: '$substring'")
        }
    }

    fun taskOutcome(task: String, expectedOutcome: TaskOutcome) {
        val actualOutcome = result.task(task)?.outcome
        if (actualOutcome != expectedOutcome) {
            throw AssertionError(
                """|Unexpected outcome for task '$task'
                   |Expected: $expectedOutcome
                   |Actual: $actualOutcome
            """.trimMargin())
        }
    }
}

internal fun BuildResult.checkOutputLogContains(substring: String) {
    if (output.contains(substring)) return

    println("Test output:")
    output.lineSequence().forEach {
        println("  > $it")
    }
    throw AssertionError("Test output does not contain the expected string: '$substring'")
}