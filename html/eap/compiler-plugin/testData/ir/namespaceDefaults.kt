/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

import androidx.compose.runtime.Composable
import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.dom.*

@Composable
fun HtmlNamespaceDefaults(value: String) {
    Div {
        Text(value)
        Span { Text(value) }
    }
    Pre(attrs = { id(value) }) { Text(value) }
}

@Composable
fun ExplicitForeignNamespace(value: String) {
    TagElementNS<Element>("svg", "http://www.w3.org/2000/svg", null) {
        Text(value)
        TagElementNS<Element>("g", "http://www.w3.org/2000/svg", null) { Text(value) }
    }
    TagElementNS<Element>("g", "http://www.w3.org/2000/svg", { id(value) }) { Text(value) }
}
