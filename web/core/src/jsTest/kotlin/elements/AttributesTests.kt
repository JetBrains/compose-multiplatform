package org.jetbrains.compose.web.core.tests

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import kotlinx.dom.clear
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.compose.web.testutils.*
import org.w3c.dom.HTMLInputElement

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
                backgroundColor(Color.red)
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
        waitForChanges()

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
        waitForChanges()

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

        waitForChanges()
        assertEquals("<div b=\"pp\" c=\"cc\"></div>", root.innerHTML)
    }

    @Test
    fun canAccessRef() = runTest {
        var flag by mutableStateOf(true)

        composition {
            if (flag) {
                Div(attrs = {
                    ref { div ->
                        (div as HTMLDivElement).innerText = "Text set using ref {}"
                        onDispose {
                            div.innerText = ""
                        }
                    }
                })
            }
        }

        assertEquals("<div>Text set using ref {}</div>", root.innerHTML)

        flag = false
        waitForChanges()

        assertEquals("", root.innerHTML)
    }

    @Test
    fun refDisposed() = runTest {
        var flag by mutableStateOf(true)

        var disposed = false

        composition {
            if (flag) {
                Div(attrs = {
                    ref {
                        onDispose {
                            disposed = true
                        }
                    }
                })
            }
        }

        assertEquals("<div></div>", root.innerHTML)
        assertEquals(false, disposed)

        flag = false
        waitForChanges()

        assertEquals("", root.innerHTML)
        assertEquals(true, disposed)
    }

    @Test
    fun refInitializedOnlyOnce() = runTest {
        var counter by mutableStateOf(1)

        var refInitCounter = 0
        var refDisposeCounter = 0
        var attrsCallCounter = 0

        composition {
            val useCounterWithinRootRecomposeScope = counter
            Text("$useCounterWithinRootRecomposeScope")

            Div(attrs = {
                attrsCallCounter += 1
                ref { div ->
                    refInitCounter += 1
                    onDispose {
                        refDisposeCounter += 1
                    }
                }
            })
        }

        assertEquals("1<div></div>", root.innerHTML)
        assertEquals(1, refInitCounter)
        assertEquals(1, attrsCallCounter)
        assertEquals(0, refDisposeCounter)

        counter++
        waitForChanges()

        assertEquals("2<div></div>", root.innerHTML)
        assertEquals(1, refInitCounter)
        assertEquals(2, attrsCallCounter)
        assertEquals(0, refDisposeCounter)
    }

    @Test
    fun disposableRefEffectWithChangingKey() = runTest {
        var key by mutableStateOf(0)

        composition {
            val readKey = key // read key here to recompose an entire scope
            Div(
                attrs = {
                    id("id$readKey")
                }
            ) {
                DisposableRefEffect(readKey) {
                    val p = document.createElement("p").also { it.innerHTML = "Key=$readKey" }
                    it.appendChild(p)

                    onDispose {
                        it.clear()
                    }
                }
            }
        }

        assertEquals(
            expected = "<div><div id=\"id0\"><p>Key=0</p></div></div>",
            actual = root.outerHTML
        )

        key = 1
        waitForRecompositionComplete()

        assertEquals(
            expected = "<div><div id=\"id1\"><p>Key=1</p></div></div>",
            actual = root.outerHTML
        )
    }

    @Test // issue: https://github.com/JetBrains/compose-jb/issues/981
    fun attributesUpdateShouldNotCauseInlineStylesCleanUp() = runTest {
        var hasValue by mutableStateOf(false)

        composition {
            Button(attrs = {
                classes("a")
                style {
                    color(Color.red)
                }
                if (hasValue) {
                    classes("b")
                    value("buttonValue")
                }
            }) {
                Text("Button")
            }
        }

        assertEquals(
            expected = "<button class=\"a\" style=\"color: red;\">Button</button>",
            actual = nextChild().outerHTML
        )

        hasValue = true
        waitForRecompositionComplete()

        assertEquals(
            expected = "<button style=\"color: red;\" value=\"buttonValue\" class=\"a b\">Button</button>",
            actual = currentChild().outerHTML
        )
    }

    @Test
    fun inputMode() = runTest {
        @Composable
        fun TestInput(mode: InputMode) {
            Input(type = InputType.Text) {
                inputMode(mode)
            }
        }

        val mode = mutableStateOf<InputMode?>(null)
        composition {
            if (mode.value != null) TestInput(mode.value!!)
        }

        suspend fun check(setMode: InputMode, value: String) {
            mode.value = setMode
            waitForRecompositionComplete()
            assertEquals(
                value,
                (root.firstChild as HTMLInputElement).getAttribute("inputmode")
            )
        }

        check(InputMode.None, "none")
        check(InputMode.Text, "text")
        check(InputMode.Decimal, "decimal")
        check(InputMode.Numeric, "numeric")
        check(InputMode.Tel, "tel")
        check(InputMode.Search, "search")
        check(InputMode.Url, "url")
    }
}
