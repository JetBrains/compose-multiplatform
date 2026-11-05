/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package benchmarks.compose

import androidx.compose.runtime.Composable
import benchmarks.model.TextData
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text


@Composable
fun DynamicText(data: TextData) {
    Div {
        data.items.forEach { item ->
            Div {
                Span(attrs = {
                    classes("foo")
                    attr("data-testid", "foo")
                }) { Text(item.foo) }
                Span(attrs = {
                    classes("bar")
                    attr("data-testid", "bar")
                }) { Text(item.bar) }
            }
        }
    }
}
