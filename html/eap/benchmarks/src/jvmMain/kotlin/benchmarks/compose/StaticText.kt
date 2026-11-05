/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package benchmarks.compose

import androidx.compose.runtime.Composable
import benchmarks.model.BAVARIA_FOO
import benchmarks.model.BAVARIA_BAR
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*

// Adapted from preact-render-to-string benchmarks.


@Composable
fun StaticText() {
    Div {
        repeat(1_000) {
            Div {
                Span(attrs = {
                    classes("foo")
                    attr("data-testid", "foo")
                }) { Text(BAVARIA_FOO) }
                Span(attrs = {
                    classes("bar")
                    attr("data-testid", "bar")
                }) { Text(BAVARIA_BAR) }
            }
        }
    }
}
