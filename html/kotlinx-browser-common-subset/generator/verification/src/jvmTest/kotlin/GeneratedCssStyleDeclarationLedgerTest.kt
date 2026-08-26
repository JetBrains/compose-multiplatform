/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies CSS promotion against the generated model ledger.
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class GeneratedCssStyleDeclarationLedgerTest {
    private val model = GeneratedModelReport.read()

    @Test
    fun everyCssClassifierComesFromTheInputFile() {
        val css = model.declarations.filter { it.name.startsWith("org.w3c.dom.css.") }

        assertEquals(18, css.size)
        assertEquals(setOf("input"), css.mapTo(mutableSetOf()) { it.origin })
    }

    @Test
    fun cssStyleDeclarationKeepsItsInlineStyleSurface() {
        val style = model.byName.getValue("org.w3c.dom.css.CSSStyleDeclaration")

        assertEquals(236, style.memberCount)
        assertContains(style.members, "var color: kotlin.String")
        assertContains(style.members, "var display: kotlin.String")
        assertContains(
            style.members,
            "fun setProperty(kotlin.String, kotlin.String, kotlin.String): kotlin.Unit",
        )
        assertContains(style.members, "fun getPropertyValue(kotlin.String): kotlin.String")
    }

    @Test
    fun cssEscapeIsAnEmittedCompanionFunction() {
        val css = model.byName.getValue("org.w3c.dom.css.CSS")

        assertEquals(1, css.memberCount)
        assertContains(css.members, "companion fun escape(kotlin.String): kotlin.String")
    }
}
