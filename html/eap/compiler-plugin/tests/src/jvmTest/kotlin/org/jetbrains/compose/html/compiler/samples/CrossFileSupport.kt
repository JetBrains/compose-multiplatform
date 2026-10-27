/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler.samples

import androidx.compose.runtime.Composable
import kotlinx.browser.dom.HTMLDivElement
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div as HtmlDiv
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

val sharedContent: @Composable () -> Unit = { Span { Text("shared lambda") } }

@Composable
fun SharedPage() {
    Span { Text("shared function") }
}

const val CARD_CLASS = "card"
const val APP_NAME = "My <application> & friends"
const val ATTRIBUTE = "title"
const val COUNT = 6

object Tokens {
    const val SUFFIX = "!"
}

class Theme {
    companion object {
        const val ID = "main"
    }
}

// A same-named application annotation must not be mistaken for the compiler contract.
@Target(AnnotationTarget.FUNCTION)
private annotation class LowerableHtmlTag

var customTagCalls = 0
    private set

@Composable
fun UserDefinedPage() {
    Div(attrs = { id("custom") }) { Text("text") }
}

// Same name and signature as the built-in wrapper, but with an observable effect.
@Composable
@LowerableHtmlTag
private fun Div(
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    content: ContentBuilder<HTMLDivElement>? = null,
) {
    customTagCalls++
    HtmlDiv(attrs, content)
}
