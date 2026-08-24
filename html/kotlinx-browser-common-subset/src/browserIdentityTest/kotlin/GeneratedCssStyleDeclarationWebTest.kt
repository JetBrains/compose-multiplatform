/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies CSS browser identity and inline-style members.
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package kotlinx.browser.dom.css.identity

import kotlinx.browser.document
import kotlinx.browser.dom.HTMLElement as PortableHTMLElement
import kotlinx.browser.dom.css.CSS as PortableCSS
import kotlinx.browser.dom.css.CSSStyleDeclaration as PortableCSSStyleDeclaration
import org.w3c.dom.HTMLElement as BrowserHTMLElement
import org.w3c.dom.css.CSS as BrowserCSS
import org.w3c.dom.css.CSSStyleDeclaration as BrowserCSSStyleDeclaration
import kotlin.js.unsafeCast
import kotlin.test.Test
import kotlin.test.assertTrue

private fun browserStyleAsPortable(value: BrowserCSSStyleDeclaration): PortableCSSStyleDeclaration = value

private fun portableStyleAsBrowser(value: PortableCSSStyleDeclaration): BrowserCSSStyleDeclaration = value

private fun portableElementAsBrowser(value: PortableHTMLElement): BrowserHTMLElement = value

private fun usePortableInlineStyle(element: PortableHTMLElement): BrowserCSSStyleDeclaration {
    val style = element.style
    style.color = "rebeccapurple"
    style.setProperty("background-color", "black")
    return style
}

private fun portableEscapeUsesTheBrowserCompanion(identifier: String): Boolean =
    PortableCSS.escape(identifier) == BrowserCSS.escape(identifier)

class GeneratedCssStyleDeclarationWebTest {
    @Test
    fun cssAliasesAndMembersAreCallable() {
        val element = document.createElement("div").unsafeCast<PortableHTMLElement>()

        browserStyleAsPortable(element.style)
        portableStyleAsBrowser(element.style)
        portableElementAsBrowser(element)
        usePortableInlineStyle(element)
        assertTrue(portableEscapeUsesTheBrowserCompanion("portable:id"))
    }
}
