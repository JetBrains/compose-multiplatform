/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies that clipboard declarations are generated as a complete input package.
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeneratedClipboardLedgerTest {
    private val model = GeneratedModelReport.read()

    @Test
    fun clipboardPackageCarriesItsPortableApi() {
        val clipboardEvent = model.byName.getValue("org.w3c.dom.clipboard.ClipboardEvent")
        val clipboardEventInit = model.byName.getValue("org.w3c.dom.clipboard.ClipboardEventInit")
        val clipboard = model.byName.getValue("org.w3c.dom.clipboard.Clipboard")

        assertEquals("input", clipboardEvent.origin)
        assertEquals("input", clipboardEventInit.origin)
        assertEquals("input", clipboard.origin)
        assertTrue("val clipboardData: kotlinx.browser.dom.DataTransfer?" in clipboardEvent.members)
        assertTrue("var clipboardData: kotlinx.browser.dom.DataTransfer?" in clipboardEventInit.members)
        assertTrue(clipboard.members.any { it.startsWith("fun readText()") })
        assertTrue(clipboard.members.any { it.startsWith("fun writeText(") })
    }
}
