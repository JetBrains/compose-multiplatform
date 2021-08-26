package org.jetbrains.compose.web.core.tests.elements

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.RadioGroup
import org.jetbrains.compose.web.dom.RadioInput
import org.jetbrains.compose.web.testutils.runTest
import org.w3c.dom.HTMLInputElement
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalComposeWebApi::class)
class RadioGroupTests {

    @Test
    fun canCreateRadioGroupsWithUniqueGeneratedGroupName() = runTest {
        composition {
            RadioGroup(checkedValue = null) {
                RadioInput(value = "v1", id = "id1")
                RadioInput(value = "v2", id = "id2")
            }

            RadioGroup(checkedValue = null) {
                RadioInput(value = "v2_1", id = "id2_1")
                RadioInput(value = "v2_2", id = "id2_2")
            }
        }

        assertEquals(
            "<div>" +
                    "<input type=\"radio\" id=\"id1\" name=\"\$compose\$generated\$radio\$group-0\" value=\"v1\">" +
                    "<input type=\"radio\" id=\"id2\" name=\"\$compose\$generated\$radio\$group-0\" value=\"v2\">" +
                    "<input type=\"radio\" id=\"id2_1\" name=\"\$compose\$generated\$radio\$group-1\" value=\"v2_1\">" +
                    "<input type=\"radio\" id=\"id2_2\" name=\"\$compose\$generated\$radio\$group-1\" value=\"v2_2\">" +
                    "</div>",
            root.outerHTML
        )

        repeat(4) {
            assertEquals(false, (root.childNodes.item(it) as HTMLInputElement).checked)
        }
    }

    @Test
    fun canCreateRadioGroupWithGivenGroupName() = runTest {
        composition {
            RadioGroup(checkedValue = null, name = "g1") {
                RadioInput(value = "v1", id = "id1")
                RadioInput(value = "v2", id = "id2")
            }
        }

        assertEquals(
            "<div>" +
                    "<input type=\"radio\" id=\"id1\" name=\"g1\" value=\"v1\">" +
                    "<input type=\"radio\" id=\"id2\" name=\"g1\" value=\"v2\">" +
                    "</div>",
            root.outerHTML
        )

        repeat(2) {
            assertEquals(false, (root.childNodes.item(it) as HTMLInputElement).checked)
        }
    }

    private enum class Rg1 {
        V1, V2, V3
    }

    @Test
    fun canCreateRadioGroupWithEnumValues() = runTest {
        val rgCheckedState by mutableStateOf<Rg1?>(null)
        composition {
            RadioGroup(checkedValue = rgCheckedState, name = "g1") {
                RadioInput(value = Rg1.V1)
                RadioInput(value = Rg1.V2)
                RadioInput(value = Rg1.V3)
            }
        }

        assertEquals(
            "<div>" +
                    "<input type=\"radio\" name=\"g1\" value=\"V1\">" +
                    "<input type=\"radio\" name=\"g1\" value=\"V2\">" +
                    "<input type=\"radio\" name=\"g1\" value=\"V3\">" +
                    "</div>",
            root.outerHTML
        )

        repeat(3) {
            assertEquals(false, (root.childNodes.item(it) as HTMLInputElement).checked)
        }
    }

    @Test
    fun radioGroupCheckedValueChanges() = runTest {
        var rgCheckedState by mutableStateOf<Rg1?>(null)

        composition {
            RadioGroup(checkedValue = rgCheckedState, name = "g1") {
                RadioInput(value = Rg1.V1)
                RadioInput(value = Rg1.V2)
                RadioInput(value = Rg1.V3)
            }
        }

        val expectedHtml = "<div>" +
                "<input type=\"radio\" name=\"g1\" value=\"V1\">" +
                "<input type=\"radio\" name=\"g1\" value=\"V2\">" +
                "<input type=\"radio\" name=\"g1\" value=\"V3\">" +
                "</div>"

        assertEquals(expectedHtml, root.outerHTML)
        repeat(3) {
            assertEquals(false, (root.childNodes.item(it) as HTMLInputElement).checked)
        }

        Rg1.values().forEachIndexed { index, rg ->
            rgCheckedState = rg
            waitForRecompositionComplete()

            assertEquals(expectedHtml, root.outerHTML)
            repeat(3) {
                assertEquals(it == index, (root.childNodes.item(it) as HTMLInputElement).checked)
            }
        }

        rgCheckedState = null
        waitForRecompositionComplete()

        assertEquals(expectedHtml, root.outerHTML)
        repeat(3) {
            assertEquals(false, (root.childNodes.item(it) as HTMLInputElement).checked)
        }
    }

    @Test
    fun radioGroupWithNotDirectChildrenRadioInputsWorksCorrectly() = runTest {
        var rgCheckedState by mutableStateOf<Rg1?>(null)

        composition {
            RadioGroup(checkedValue = rgCheckedState, name = "g1") {
                Div { RadioInput(value = Rg1.V1) }
                Div { RadioInput(value = Rg1.V2) }
                Div { RadioInput(value = Rg1.V3) }
            }
        }

        val expectedHtml = "<div>" +
                "<div><input type=\"radio\" name=\"g1\" value=\"V1\"></div>" +
                "<div><input type=\"radio\" name=\"g1\" value=\"V2\"></div>" +
                "<div><input type=\"radio\" name=\"g1\" value=\"V3\"></div>" +
                "</div>"

        assertEquals(expectedHtml, root.outerHTML)
        repeat(3) {
            assertEquals(false, (root.childNodes.item(it)!!.firstChild as HTMLInputElement).checked)
        }

        Rg1.values().forEachIndexed { index, rg ->
            rgCheckedState = rg
            waitForRecompositionComplete()

            assertEquals(expectedHtml, root.outerHTML)
            repeat(3) {
                assertEquals(it == index, (root.childNodes.item(it)!!.firstChild as HTMLInputElement).checked)
            }
        }

        rgCheckedState = null
        waitForRecompositionComplete()

        assertEquals(expectedHtml, root.outerHTML)
        repeat(3) {
            assertEquals(false, (root.childNodes.item(it)!!.firstChild as HTMLInputElement).checked)
        }
    }
}
