/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package benchmarks.hydration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Table
import org.jetbrains.compose.web.dom.Tbody
import org.jetbrains.compose.web.dom.Td
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Tr

data class Hydrate1kState(val rows: List<Hydrate1kRow>)

data class Hydrate1kRow(val id: Int, val label: String)

private val adjectives = listOf(
    "pretty", "large", "big", "small", "tall", "short", "long", "handsome", "plain",
    "quaint", "clean", "elegant", "easy", "angry", "crazy", "helpful", "mushy", "odd",
    "unsightly", "adorable", "important", "inexpensive", "cheap", "expensive", "fancy",
)

private val colours = listOf(
    "red", "yellow", "blue", "green", "pink", "brown", "purple", "brown", "white", "black", "orange",
)

private val nouns = listOf(
    "table", "chair", "house", "bbq", "desk", "car", "pony", "cookie", "sandwich", "burger", "pizza",
    "mouse", "keyboard",
)

fun hydrate1kFixture(): Hydrate1kState = Hydrate1kState(
    rows = (1..1_000).map { id ->
        Hydrate1kRow(
            id = id,
            label = "${adjectives[(id * 17) % adjectives.size]} " +
                "${colours[(id * 31) % colours.size]} ${nouns[(id * 47) % nouns.size]}",
        )
    },
)

@Composable
fun Hydrate1kTable(initialState: Hydrate1kState) {
    var rows by remember(initialState) { mutableStateOf(initialState.rows) }
    var selectedRowId by remember { mutableStateOf<Int?>(null) }

    Div(attrs = { classes("container") }) {
        Table(attrs = { classes("table", "table-hover", "table-striped", "test-data") }) {
            Tbody {
                rows.forEach { row ->
                    key(row.id) {
                        Tr(attrs = {
                            if (selectedRowId == row.id) classes("danger")
                        }) {
                            Td(attrs = { classes("col-md-1") }) { Text(row.id.toString()) }
                            Td(attrs = { classes("col-md-4") }) {
                                A(attrs = {
                                    onClick { event ->
                                        event.preventDefault()
                                        selectedRowId = row.id
                                    }
                                }) {
                                    Text(row.label)
                                }
                            }
                            Td(attrs = { classes("col-md-1") }) {
                                A(attrs = {
                                    onClick { event ->
                                        event.preventDefault()
                                        rows = rows.filterNot { it.id == row.id }
                                    }
                                }) {
                                    Span(attrs = {
                                        classes("glyphicon", "glyphicon-remove")
                                        attr("aria-hidden", "true")
                                    })
                                }
                            }
                            Td(attrs = { classes("col-md-6") })
                        }
                    }
                }
            }
        }
        Span(attrs = {
            classes("preloadicon", "glyphicon", "glyphicon-remove")
            attr("aria-hidden", "true")
        })
    }
}
