/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies CSS browser identity and inline-style members.
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package kotlinx.browser.dom.css.identity

import kotlinx.browser.document
import kotlinx.browser.dom.HTMLElement as CommonHTMLElement
import kotlinx.browser.dom.css.CSS as CommonCSS
import kotlinx.browser.dom.css.CSSStyleDeclaration as CommonCSSStyleDeclaration
import org.w3c.dom.HTMLElement as BrowserHTMLElement
import org.w3c.dom.css.CSS as BrowserCSS
import org.w3c.dom.css.CSSStyleDeclaration as BrowserCSSStyleDeclaration
import kotlin.js.unsafeCast
import kotlin.test.Test
import kotlin.test.assertTrue

private fun browserStyleAsCommon(value: BrowserCSSStyleDeclaration): CommonCSSStyleDeclaration = value

private fun commonStyleAsBrowser(value: CommonCSSStyleDeclaration): BrowserCSSStyleDeclaration = value

private fun commonElementAsBrowser(value: CommonHTMLElement): BrowserHTMLElement = value

private fun useCommonInlineStyle(element: CommonHTMLElement): BrowserCSSStyleDeclaration {
    val style = element.style
    style.color = "rebeccapurple"
    style.setProperty("background-color", "black")
    return style
}

private fun commonEscapeUsesTheBrowserCompanion(identifier: String): Boolean =
    CommonCSS.escape(identifier) == BrowserCSS.escape(identifier)

class GeneratedCssStyleDeclarationWebTest {
    @Test
    fun cssAliasesAndMembersAreCallable() {
        val element = document.createElement("div").unsafeCast<CommonHTMLElement>()

        browserStyleAsCommon(element.style)
        commonStyleAsBrowser(element.style)
        commonElementAsBrowser(element)
        useCommonInlineStyle(element)
        assertTrue(commonEscapeUsesTheBrowserCompanion("common:id"))
    }
}
