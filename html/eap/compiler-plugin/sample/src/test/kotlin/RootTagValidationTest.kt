/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import org.jetbrains.compose.web.composeHtmlToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame

class RootTagValidationTest {
    @Test
    fun invalidMarkedRootFailsDuringOrdinaryRendering() {
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString { InvalidMarkedRootPage() }
        }
    }

    @Test
    fun noscriptKeepsTheOrdinaryRenderingPath() {
        val html = composeHtmlToString { NoscriptRootPage() }
        assertEquals("<noscript>fallback</noscript>", html)
        assertNotSame(html, composeHtmlToString { NoscriptRootPage() })
    }

    @Test
    fun nestedNoscriptStillRejectsItsEmbeddedEndTag() {
        assertFailsWith<IllegalArgumentException> {
            composeHtmlToString { NestedNoscriptRootPage() }
        }
    }
}
