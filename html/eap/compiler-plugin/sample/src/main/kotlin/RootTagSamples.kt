/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

import androidx.compose.runtime.Composable
import kotlinx.browser.dom.HTMLElement
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.LowerableHtmlTag
import org.jetbrains.compose.web.dom.TagElement
import org.jetbrains.compose.web.dom.Text

@Composable
fun InvalidMarkedRootPage() {
    InvalidRoot { Text("text") }
}

@Composable
fun NoscriptRootPage() {
    Noscript { Text("fallback") }
}

@Composable
fun NestedNoscriptRootPage() {
    Noscript { Noscript { Text("nested") } }
}

@Composable
@LowerableHtmlTag("bad tag")
private fun InvalidRoot(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) {
    TagElement("bad tag", attrs, content)
}

@Composable
@LowerableHtmlTag("noscript")
private fun Noscript(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null,
) {
    TagElement("noscript", attrs, content)
}
