package elements

import androidx.compose.web.attributes.Scope
import androidx.compose.web.attributes.colspan
import androidx.compose.web.attributes.rowspan
import androidx.compose.web.attributes.scope
import androidx.compose.web.attributes.span
import androidx.compose.web.elements.Caption
import androidx.compose.web.elements.Col
import androidx.compose.web.elements.Colgroup
import androidx.compose.web.elements.Table
import androidx.compose.web.elements.Tbody
import androidx.compose.web.elements.Td
import androidx.compose.web.elements.Text
import androidx.compose.web.elements.Tfoot
import androidx.compose.web.elements.Th
import androidx.compose.web.elements.Thead
import androidx.compose.web.elements.Tr
import org.w3c.dom.HTMLElement
import runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TableTests {

    @Test
    fun colAttributes() = runTest {
        composition {
            Col(
                attrs = {
                    span(2)
                }
            ) { }
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
                    Col { }
                    Col { }
                    Col { }
                }
                Thead {
                    Tr {
                        Th { }
                        Th { }
                        Th(
                            attrs = {
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
                            attrs = {
                                scope(Scope.Col)
                            }
                        ) { Text("A") }
                        Th(
                            attrs = {
                                scope(Scope.Col)
                            }
                        ) { Text("B") }
                    }
                }
                Tbody {
                    Tr {
                        Th(
                            attrs = {
                                scope(Scope.Row)
                                rowspan(2)
                            }
                        ) {
                            Text("Rows")
                        }
                        Th(
                            attrs = {
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
                            attrs = {
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
                            attrs = {
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
