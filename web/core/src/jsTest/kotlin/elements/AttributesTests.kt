package org.jetbrains.compose.web.core.tests

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.attributes.AttrsBuilder
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.forId
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import kotlin.test.Test
import kotlin.test.assertEquals

class AttributesTests {

    @Test
    fun copyFromStyleBuilderCopiesCorrectly() {
        val copyFromStyleBuilder = StyleBuilderImpl().apply {
            property("color", "red")
            property("height", 100.px)

            variable("var1", 100)
            variable("var2", 100.px)
        }

        val copyToStyleBuilder = StyleBuilderImpl().apply {
            copyFrom(copyFromStyleBuilder)
        }

        assertEquals(copyFromStyleBuilder, copyToStyleBuilder)
    }

    @Test
    fun copyFromAttrsBuilderCopiesCorrectly() {
        val attrsBuilderCopyFrom = AttrsBuilder<HTMLElement>().apply {
            id("id1")
            classes("a b c")
            attr("title", "customTitle")

            prop<HTMLElement, String>({_, _ ->}, "Value")

            ref { onDispose {  } }
            style {
                width(500.px)
                backgroundColor("red")
            }

            onClick {  }
            onFocusIn {  }
            onMouseEnter {  }
        }

        val copyToAttrsBuilder = AttrsBuilder<HTMLElement>().apply {
            copyFrom(attrsBuilderCopyFrom)
        }

        assertEquals(attrsBuilderCopyFrom.attributesMap, copyToAttrsBuilder.attributesMap)
        assertEquals(attrsBuilderCopyFrom.styleBuilder, copyToAttrsBuilder.styleBuilder)
        assertEquals(attrsBuilderCopyFrom.refEffect, copyToAttrsBuilder.refEffect)
        assertEquals(attrsBuilderCopyFrom.propertyUpdates, copyToAttrsBuilder.propertyUpdates)
        assertEquals(attrsBuilderCopyFrom.collectListeners(), copyToAttrsBuilder.collectListeners())
    }

    @Test
    fun attrsBuilderCopyFromPreservesExistingAttrs() {
        val attrsBuilderCopyFrom = AttrsBuilder<HTMLElement>().apply {
            attr("title", "customTitle")
        }

        val copyToAttrsBuilder = AttrsBuilder<HTMLElement>().apply {
            id("id1")
            onClick {  }
            style {
                width(100.px)
            }

            copyFrom(attrsBuilderCopyFrom)
        }

        assertEquals("id1", copyToAttrsBuilder.attributesMap["id"])
        assertEquals(StyleBuilderImpl().apply { width(100.px) }, copyToAttrsBuilder.styleBuilder)

        val listeners = copyToAttrsBuilder.collectListeners()
        assertEquals(1, listeners.size)
        assertEquals("click", listeners[0].event)
    }

    @Test
    fun attrsBuilderCopyFromOverridesSameAttrs() {
        val attrsBuilderCopyFrom = AttrsBuilder<HTMLElement>().apply {
            attr("title", "customTitleNew")
        }

        val copyToAttrsBuilder = AttrsBuilder<HTMLElement>().apply {
            attr("title", "customTitleOld")
        }
        assertEquals("customTitleOld", copyToAttrsBuilder.attributesMap["title"])

        copyToAttrsBuilder.copyFrom(attrsBuilderCopyFrom)
        assertEquals("customTitleNew", copyToAttrsBuilder.attributesMap["title"])
    }

    @Test
    fun labelForIdAttrAppliedProperly() = runTest {

        composition {
            Label(forId = "l1") { Text("label") }
        }

        assertEquals(
            expected = "<label for=\"l1\">label</label>",
            actual = root.innerHTML
        )
    }

    @Test
    fun labelForIdIsOptional() = runTest {

        composition {
            Label { Text("label") }
        }

        assertEquals(
            expected = "<label>label</label>",
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
            expected = "<label for=\"lb1\">label</label>",
            actual = root.innerHTML
        )
    }

    @Test
    fun buttonDisabledAttributeAddedOnlyWhenTrue() = runTest {
        var disabled by mutableStateOf(false)

        composition {
            Button(
                {
                    if (disabled) {
                        disabled()
                    }
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
            Div({
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
    fun multipleClassesCallsWithConditionsAddCorrectClasses() = runTest {
        val addClassD = mutableStateOf(true)
        composition {
            Div({
                classes("c")
                if (addClassD.value) {
                    classes("d")
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

    @Test
    fun attributesRecreated() = runTest {
        var flag by mutableStateOf(true)

        composition {
            Div({
                if (flag) {
                    attr("a", "aa")
                    attr("b", "bb")
                } else {
                    attr("b", "pp")
                    attr("c", "cc")
                }
            })
        }

        assertEquals("<div a=\"aa\" b=\"bb\"></div>", root.innerHTML)

        flag = false

        waitChanges()
        assertEquals("<div b=\"pp\" c=\"cc\"></div>", root.innerHTML)
    }
}
