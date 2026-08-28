/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies parsing and validation of the classifier selection policy.
package org.jetbrains.compose.web.browser.generator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SelectionPolicyTest {
    @Test
    fun inputClassifiersDefaultToEmitAndOverridesDefer() {
        val policy = SelectionPolicy.parse(
            listOf(
                "input org.w3c.dom.kt",
                "signature-only-package org.w3c.files",
                "defer future-canvas org.w3c.dom.CanvasGradient",
            ),
        )
        val inputs = setOf(
            "org.w3c.dom.Document",
            "org.w3c.dom.ParentNode",
            "org.w3c.dom.CanvasGradient",
        )

        assertEquals(
            setOf("org.w3c.dom.Document", "org.w3c.dom.ParentNode"),
            policy.emittedClassifiers(inputs),
        )
        assertEquals(setOf("org.w3c.files"), policy.signatureOnlyPackages)
        assertEquals(ClassifierDisposition.EMIT, policy.dispositionOf("org.w3c.dom.Document"))
        assertEquals(ClassifierDisposition.EMIT, policy.dispositionOf("org.w3c.dom.ParentNode"))
        assertEquals(ClassifierDisposition.DEFER, policy.dispositionOf("org.w3c.dom.CanvasGradient"))
        assertEquals(setOf("org.w3c.dom.CanvasGradient"), policy.excludedFromClosure)
        assertEquals(
            listOf("future-canvas org.w3c.dom.CanvasGradient"),
            policy.deferredExclusionLines,
        )
        assertEquals(emptyList(), policy.validationErrors(inputs))
    }

    @Test
    fun policyEntriesMustPointToTheRightSideOfTheInputBoundary() {
        val policy = SelectionPolicy.parse(
            listOf(
                "input org.w3c.dom.kt",
                "defer future-surface org.w3c.dom.MissingMixin",
            ),
        )

        assertEquals(
            listOf("defer classifier is not declared by an input file: org.w3c.dom.MissingMixin"),
            policy.validationErrors(setOf("org.w3c.dom.Document")),
        )
    }

    @Test
    fun duplicateClassifierDecisionsAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            SelectionPolicy.parse(
                listOf(
                    "input org.w3c.dom.kt",
                    "defer future-surface org.w3c.dom.ParentNode",
                    "defer future-surface org.w3c.dom.ParentNode",
                ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            SelectionPolicy.parse(
                listOf(
                    "input org.w3c.dom.kt",
                    "signature-only-package org.w3c.files",
                    "signature-only-package org.w3c.files",
                ),
            )
        }
    }

    @Test
    fun deferredClassifiersRequireAStructuredReason() {
        assertFailsWith<IllegalArgumentException> {
            SelectionPolicy.parse(
                listOf(
                    "input org.w3c.dom.kt",
                    "defer NOT_A_REASON org.w3c.dom.Document",
                ),
            )
        }
    }
}
