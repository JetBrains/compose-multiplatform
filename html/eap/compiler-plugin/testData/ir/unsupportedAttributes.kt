/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.Composable
import kotlinx.browser.dom.HTMLDivElement
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.dom.*

@Composable
fun UnsupportedAttributes(callback: AttrsScope<HTMLDivElement>.() -> Unit) {
    Div(attrs = {
        id("before")
        callback()
        attr("title", "after")
    }) {
        Span { Text("child") }
    }
}
