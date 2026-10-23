/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.*

@Composable
fun CompilerHydrationContent(count: Int, increment: () -> Unit) {
    Div {
        Span(attrs = { id("compiler-title") }) {
            Text("Compiler <HTML> & hydration")
            Text("")
            Text(" ready")
        }
        ConstantTable()
    }
    Button(attrs = { onClick { increment() } }) { Text("Increment") }
    Span(attrs = { id("compiler-count") }) { Text("Count: $count") }
}

@Composable
fun ConstantTable() {
    Table(attrs = { id("constant-table") }) {
        Caption {
            val label = "Results"
            Text(label)
        }
        Colgroup { Col(attrs = { attr("span", "2") }) }
        Thead { Tr { Th { Text("Name") } } }
        Tbody {
            Text("\n")
            repeat(2) {
                Tr {
                    Td(attrs = { attr("title", "<&\"") }) {
                        Text("a<&")
                        Text("")
                        Text("b")
                    }
                    Td { Table { Tbody { Tr { Td { Text("nested") } } } } }
                }
            }
        }
        Tfoot { Tr { Td { Text("Total") } } }
    }
}

@Composable
fun NestedConstantTable() {
    Div {
        Text("before")
        Table { Tbody { Tr { Td { Text("cell") } } } }
        Text("after")
    }
}

@Composable
fun EmptyConstantTable() {
    Table {
        Caption()
        Colgroup()
        Thead()
        Tbody()
        Tfoot()
    }
}
