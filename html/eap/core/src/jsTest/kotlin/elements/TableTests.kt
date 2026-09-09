package org.jetbrains.compose.web.core.tests

import org.jetbrains.compose.web.attributes.Scope
import org.jetbrains.compose.web.attributes.colspan
import org.jetbrains.compose.web.attributes.rowspan
import org.jetbrains.compose.web.attributes.scope
import org.jetbrains.compose.web.attributes.span
import org.jetbrains.compose.web.dom.Caption
import org.jetbrains.compose.web.dom.Col
import org.jetbrains.compose.web.dom.Colgroup
import org.jetbrains.compose.web.dom.Table
import org.jetbrains.compose.web.dom.Tbody
import org.jetbrains.compose.web.dom.Td
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Tfoot
import org.jetbrains.compose.web.dom.Th
import org.jetbrains.compose.web.dom.Thead
import org.jetbrains.compose.web.dom.Tr
import org.w3c.dom.HTMLElement
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.compose.web.testutils.*

class TableTests {

    @Test
    fun colAttributes() = runTest {
        composition {
            Col {
                span(2)
            }
        }

        assertEquals(
            expected = "2",
            actual = (root.firstChild!! as HTMLElement).getAttribute("span")
        )
    }

    @Test
    fun create() = runTest {
        composition {
            Table {
                Caption {
                    Text("CaptionText")
                }
                Colgroup {
                    Col()
                    Col()
                    Col()
                }
                Thead {
                    Tr {
                        Th { }
                        Th { }
                        Th(
                            {
                                colspan(2)
                            }
                        ) {
                            Text("First")
                        }
                    }
                    Tr {
                        Th { }
                        Th { }
                        Th(
                            {
                                scope(Scope.Col)
                            }
                        ) { Text("A") }
                        Th(
                            {
                                scope(Scope.Col)
                            }
                        ) { Text("B") }
                    }
                }
                Tbody {
                    Tr {
                        Th(
                            {
                                scope(Scope.Row)
                                rowspan(2)
                            }
                        ) {
                            Text("Rows")
                        }
                        Th(
                            {
                                scope(Scope.Row)
                            }
                        ) {
                            Text("1")
                        }
                        Td {
                            Text("30")
                        }
                        Td {
                            Text("40")
                        }
                    }
                    Tr {
                        Th(
                            {
                                scope(Scope.Row)
                            }
                        ) {
                            Text("2")
                        }
                        Td {
                            Text("10")
                        }
                        Td {
                            Text("20")
                        }
                    }
                }
                Tfoot {
                    Tr {
                        Th(
                            {
                                scope(Scope.Row)
                            }
                        ) {
                            Text("Totals")
                        }
                        Th { }
                        Td { Text("40") }
                        Td { Text("60") }
                    }
                }
            }
        }

        assertEquals(
            expected = """
            <table>
                <caption>CaptionText</caption>
                <colgroup>
                    <col>
                    <col>
                    <col>
                </colgroup>
                <thead>
                    <tr>
                        <th></th>
                        <th></th>
                        <th colspan="2">First</th>
                    </tr>
                    <tr>
                        <th></th>
                        <th></th>
                        <th scope="col">A</th>
                        <th scope="col">B</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <th scope="row" rowspan="2">Rows</th>
                        <th scope="row">1</th>
                        <td>30</td>
                        <td>40</td>
                    </tr>
                    <tr>
                        <th scope="row">2</th>
                        <td>10</td>
                        <td>20</td>
                    </tr>
                </tbody>
                <tfoot>
                    <tr>
                        <th scope="row">Totals</th>
                        <th></th>
                        <td>40</td>
                        <td>60</td>
                    </tr>
                </tfoot>
            </table>
            """.trimIndent()
                .replace("\n", "")
                .replace("\\s{2,}".toRegex(), ""),
            actual = root.innerHTML
        )
    }
}
