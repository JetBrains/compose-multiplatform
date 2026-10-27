/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.testing

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GoldenFilesTest {
    @TempDir
    lateinit var directory: File

    private val golden: File get() = File(directory, "golden.txt")
    private val actual: File get() = File(directory, "golden.txt.actual")

    @Test
    fun missingGoldenFailsWithoutCreatingExpectation() {
        val failure = assertThrows(AssertionError::class.java) { GoldenFiles.assertMatches(golden, "new IR") }
        assertFalse(golden.exists())
        assertEquals("new IR\n", actual.readText())
        assertTrue(failure.message!!.contains(actual.absolutePath))
    }

    @Test
    fun mismatchPreservesGoldenAndReportsFirstDifference() {
        golden.writeText("old IR\n")
        val failure = assertThrows(AssertionError::class.java) { GoldenFiles.assertMatches(golden, "new IR") }
        assertEquals("old IR\n", golden.readText())
        assertEquals("new IR\n", actual.readText())
        assertTrue(failure.message!!.contains("line 1"))
    }

    @Test
    fun updateCreatesParentsAndReplacesExpectation() {
        val nested = File(directory, "nested/golden.txt")
        GoldenFiles.assertMatches(nested, "first", GoldenMode.UPDATE)
        GoldenFiles.assertMatches(nested, "second", GoldenMode.UPDATE)
        assertEquals("second\n", nested.readText())
    }

    @Test
    fun matchingOutputNormalizesLineEndingsAndRemovesStaleActual() {
        golden.writeText("a\r\nb\r\n")
        actual.writeText("stale")
        GoldenFiles.assertMatches(golden, "a\nb")
        assertFalse(actual.exists())
    }

    @Test
    fun indentationAndTrailingSpacesRemainSignificant() {
        golden.writeText("  a \n")
        assertThrows(AssertionError::class.java) { GoldenFiles.assertMatches(golden, "a\n") }
    }

    @Test
    fun emptyGoldenIsAnExpectationNotAnUpdateRequest() {
        golden.writeText("")
        assertThrows(AssertionError::class.java) { GoldenFiles.assertMatches(golden, "IR") }
        assertEquals("", golden.readText())
    }
}
