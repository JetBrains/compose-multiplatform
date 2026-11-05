/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import org.jetbrains.compose.web.dom.*

@Composable
fun CallbackWriterScopes(
    values: List<String>,
    local: ProvidableCompositionLocal<String>,
    store: (@Composable () -> Unit) -> Unit,
) {
    Div {
        values.forEachIndexed { index, value ->
            if (value.isEmpty()) return@forEachIndexed
            Span { Text("$index:$value") }
            store { Span { Text("stored:$value") } }
            CompositionLocalProvider(local provides value) {
                values.forEach { nested ->
                    Span { Text(nested) }
                    Text(local.current)
                }
            }
        }
    }
}
