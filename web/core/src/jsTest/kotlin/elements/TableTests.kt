/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
            <table style="">
                <caption style="">CaptionText</caption>
                <colgroup style="">
                    <col style="">
                    <col style="">
                    <col style="">
                </colgroup>
                <thead style="">
                    <tr style="">
                        <th style=""></th>
                        <th style=""></th>
                        <th colspan="2" style="">First</th>
                    </tr>
                    <tr style="">
                        <th style=""></th>
                        <th style=""></th>
                        <th scope="col" style="">A</th>
                        <th scope="col" style="">B</th>
                    </tr>
                </thead>
                <tbody style="">
                    <tr style="">
                        <th scope="row" rowspan="2" style="">Rows</th>
                        <th scope="row" style="">1</th>
                        <td style="">30</td>
                        <td style="">40</td>
                    </tr>
                    <tr style="">
                        <th scope="row" style="">2</th>
                        <td style="">10</td>
                        <td style="">20</td>
                    </tr>
                </tbody>
                <tfoot style="">
                    <tr style="">
                        <th scope="row" style="">Totals</th>
                        <th style=""></th>
                        <td style="">40</td>
                        <td style="">60</td>
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
