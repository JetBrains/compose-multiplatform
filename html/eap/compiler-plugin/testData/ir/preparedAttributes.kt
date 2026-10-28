/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.Composable
import kotlinx.browser.dom.HTMLDivElement
import org.jetbrains.compose.web.dom.*

@Composable
fun PreparedAttributes(first: ContentBuilder<HTMLDivElement>?, second: ContentBuilder<HTMLDivElement>?) {
    Div(attrs = { classes("card"); attr("title", "A & B") }, content = first)
    Div(attrs = { classes("card"); attr("title", "A & B") }, content = second)
}
