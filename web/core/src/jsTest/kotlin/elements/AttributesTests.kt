package elements

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.web.attributes.disabled
import androidx.compose.web.attributes.forId
import androidx.compose.web.elements.Button
import androidx.compose.web.elements.Div
import androidx.compose.web.elements.Label
import androidx.compose.web.elements.Text
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AttributesTests {

    @Test
    fun labelForIdAttrAppliedProperly() = runTest {

        composition {
            Label(forId = "l1") { Text("label") }
        }

        assertEquals(
            expected = "<label for=\"l1\" style=\"\">label</label>",
            actual = root.innerHTML
        )
    }

    @Test
    fun labelForIdIsOptional() = runTest {

        composition {
            Label { Text("label") }
        }

        assertEquals(
            expected = "<label style=\"\">label</label>",
            actual = root.innerHTML
        )
    }

    @Test
    fun labelForIdIsAppliedFromAttrs() = runTest {

        composition {
            Label(
                attrs = {
                    forId("lb1")
                }
            ) {
                Text("label")
            }
        }

        assertEquals(
            expected = "<label for=\"lb1\" style=\"\">label</label>",
            actual = root.innerHTML
        )
    }

    @Test
    fun buttonDisabledAttributeAddedOnlyWhenTrue() = runTest {
        var disabled by mutableStateOf(false)

        composition {
            Button(
                attrs = {
                    disabled(disabled)
                }
            ) {}
        }

        val btn = root.firstChild as HTMLButtonElement
        assertEquals(null, btn.getAttribute("disabled"))

        disabled = true
        waitChanges()

        assertEquals("", btn.getAttribute("disabled"))
    }

    @Test
    fun multipleClassesCallsAddMoreClasses() = runTest {
        composition {
            Div(attrs = {
                classes("a", "b")
                classes("c", "d")
            }) {}
        }

        val div = root.firstChild as HTMLDivElement
        assertEquals(
            expected = "a b c d",
            actual = div.getAttribute("class")
        )
    }

    @Test
    fun multipleClassesBuildersCallsAddMoreClasses() = runTest {
        composition {
            Div(attrs = {
                classes("a", "b")
                classes {
                    +"c"
                    +"d"
                }
            }) {}
        }

        val div = root.firstChild as HTMLDivElement
        assertEquals(
            expected = "a b c d",
            actual = div.getAttribute("class")
        )
    }


    @Test
    fun multipleClassesBuildersCallsAddMoreClasses2() = runTest {
        composition {
            Div(attrs = {
                classes {
                    +"c"
                    +"d"
                }
                classes("a", "b")
            }) {}
        }

        val div = root.firstChild as HTMLDivElement
        assertEquals(
            expected = "c d a b",
            actual = div.getAttribute("class")
        )
    }

    @Test
    fun multipleClassesBuildersCallsWithConditionsAddCorrectClasses() = runTest {
        val addClassD = mutableStateOf(true)
        composition {
            Div(attrs = {
                classes {
                    +"c"
                    if (addClassD.value) +"d"
                }
                classes("a", "b")
            }) {}
        }

        val div = root.firstChild as HTMLDivElement
        assertEquals(
            expected = "c d a b",
            actual = div.getAttribute("class")
        )

        addClassD.value = false
        waitChanges()

        assertEquals(
            expected = "c a b",
            actual = div.getAttribute("class")
        )
    }
}