/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test.utils

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions
import java.io.File

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
    val log: String
        get() = result.output

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

internal fun String.checkContains(substring: String) {
    if (!contains(substring)) {
        throw AssertionError("String '$substring' is not found in text:\n$this")
    }
}

internal fun assertEqualTextFiles(actual: File, expected: File) {
    Assertions.assertEquals(
        expected.normalizedText(),
        actual.normalizedText(),
        "Content of '$expected' is not equal to content of '$actual'"
    )
}

internal fun assertNotEqualTextFiles(actual: File, expected: File) {
    Assertions.assertNotEquals(
        expected.normalizedText(),
        actual.normalizedText(),
        "Content of '$expected' is equal to content of '$actual'"
    )
}

private fun File.normalizedText() =
    readLines().joinToString("\n") { it.trim() }
