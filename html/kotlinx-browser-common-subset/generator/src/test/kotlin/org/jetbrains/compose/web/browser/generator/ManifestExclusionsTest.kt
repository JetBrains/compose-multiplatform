/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies coverage boundaries and validation for explicit API exclusions.
package org.jetbrains.compose.web.browser.generator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ManifestExclusionsTest {
    private fun exclusions(vararg lines: String) = ManifestExclusions.parse(lines.toList())

    /** Classifier entries cover that classifier and its declarations. */
    @Test
    fun aClassifierCoversItsOwnDeclarations() {
        val exclusions = exclusions("unsupported-canvas org.w3c.dom.CanvasGradient")

        assertEquals("unsupported-canvas", exclusions.reasonFor("org.w3c.dom.CanvasGradient"))
        assertEquals("unsupported-canvas", exclusions.reasonFor("org.w3c.dom.CanvasGradient#fun addColorStop(offset: Double, color: String)"))
        assertEquals("unsupported-canvas", exclusions.reasonFor("org.w3c.dom.CanvasGradient#constructor()"))
        assertEquals("unsupported-canvas", exclusions.reasonFor("org.w3c.dom.CanvasGradient#factory"))
        assertEquals("unsupported-canvas", exclusions.reasonFor("org.w3c.dom.CanvasGradient.Companion#val NONE"))
    }

    /** Coverage follows declaration boundaries rather than string prefixes. */
    @Test
    fun aClassifierDoesNotCoverASimilarlyNamedOne() {
        val exclusions = exclusions("unsupported-node org.w3c.dom.Node")

        assertNull(exclusions.reasonFor("org.w3c.dom.NodeList"))
        assertNull(exclusions.reasonFor("org.w3c.dom.NodeList#fun item(index: Int)"))
        assertNull(exclusions.reasonFor("org.w3c.dom.NodeIterator"))
    }

    /** The most specific matching entry determines the reason. */
    @Test
    fun theMostSpecificEntryDecides() {
        val exclusions = exclusions(
            "unsupported-canvas org.w3c.dom.Path2D",
            "target-declaration-mismatch org.w3c.dom.Path2D#constructor(paths: JsArray<Path2D>)",
        )

        assertEquals(
            "target-declaration-mismatch",
            exclusions.reasonFor("org.w3c.dom.Path2D#constructor(paths: JsArray<Path2D>)"),
        )
        assertEquals("unsupported-canvas", exclusions.reasonFor("org.w3c.dom.Path2D#fun closePath()"))
    }

    /** Entries that cover no declaration are stale. */
    @Test
    fun anEntryThatCoversNothingIsReportedAsStale() {
        val exclusions = exclusions(
            "unsupported-worker org.w3c.dom.Worker",
            "unsupported-worker org.w3c.dom.SharedWorker",
        )

        exclusions.reasonFor("org.w3c.dom.Worker#fun terminate()")

        assertEquals(listOf("org.w3c.dom.SharedWorker"), exclusions.unused)
    }

    @Test
    fun aLineWithoutBothAReasonAndASubjectIsRejected() {
        assertFailsWith<IllegalArgumentException> { exclusions("org.w3c.dom.Worker") }
        assertFailsWith<IllegalArgumentException> { exclusions("unsupported-worker ") }
    }

    /** Conflicting reasons for one subject are rejected. */
    @Test
    fun oneSubjectWithTwoReasonsIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            exclusions("unsupported-worker org.w3c.dom.Worker", "unsupported-canvas org.w3c.dom.Worker")
        }
        exclusions("unsupported-worker org.w3c.dom.Worker", "unsupported-worker org.w3c.dom.Worker")
    }
}
