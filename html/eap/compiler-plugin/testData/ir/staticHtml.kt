/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.*

private const val CARD = "card"

@Composable
fun StaticHtml() {
    Div(attrs = {
        classes(CARD)
        attr("title", "A & \"B\"")
    }) {
        Span { Text("<&" + "Hello>") }
        Br()
    }
}
