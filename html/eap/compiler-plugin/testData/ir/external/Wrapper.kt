/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package external

import androidx.compose.runtime.Composable
import kotlinx.browser.dom.HTMLElement
import org.jetbrains.compose.web.dom.*

const val CARD_CLASS = "card"

// Card is a <section> wrapper, compiled without the HTML plugin.
// Its annotation lets us optimize calls and supported attrs without reading its body.
// This is a promise from the external definition. The plugin cannot verify it.
// Unsupported attrs keep the original call.
@Composable
@LowerableHtmlTag("section")
fun Card(attrs: AttrBuilderContext<HTMLElement>? = null, content: ContentBuilder<HTMLElement>? = null) {
    TagElement<HTMLElement>(tagName = "section", applyAttrs = attrs, content = content)
}
