/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.*

@Composable
fun DynamicAttributesAndText(selected: Boolean, effect: (String) -> String) {
    Div(attrs = {
        classes("card")
        id(effect("id"))
        if (selected) attr("aria-label", effect("label"))
        attr("title", "A & B")
    }) {
        Span { Text("before") }
        Text(effect("body"))
        Span { Text("after") }
    }
}

@Composable
fun DynamicTable(name: String) {
    Table {
        Tbody {
            Tr { Td { Text(name) } }
        }
    }
}
