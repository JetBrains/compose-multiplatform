/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package benchmarks.compose

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*


@Composable
fun DeepStack() {
    Div { repeat(10) { DeepStackBranch(1_000) } }
}

@Composable
private fun DeepStackBranch(depth: Int) {
    if (depth == 0) {
        Div { Span(attrs = {
            classes("foo")
            attr("data-testid", "stack")
        }) { Text("deep stack") } }
    } else {
        Div { DeepStackBranch(depth - 1) }
    }
}
