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
import org.jetbrains.compose.web.dom.Text
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.compose.web.testutils.*
import org.w3c.dom.*
import kotlin.test.assertContains
import kotlin.test.assertTrue

class AttributesTests {

    @Test
    fun correctOrderOfApplyingClasses() = runTest {
        composition {
            Div(attrs = {
                prop<HTMLDivElement, String>({ element, v ->
                    assertEquals("c1 c2 c3 c4", element.classList.value)
                    element.classList.add(v)
                }, "classFromProperty1")

                classes("c1", "c2")
                classes("c3", "c4")
            }) {
                Text("test")
            }
        }

        with(nextChild()) {
            assertEquals("c1 c2 c3 c4 classFromProperty1", getAttribute("class"))
        }
    }

    @Test
    fun correctOrderOfApplyingClasses2() = runTest {
        composition {
            Div(attrs = {
                // attr rewrites the content of 'class'
                attr("class", "classSetFromAttr")

                prop<HTMLDivElement, String>({ element, v ->
                    assertEquals("classSetFromAttr", element.classList.value)
                    element.classList.add(v)
                }, "classFromProperty1")

                classes("c1", "c2")
                classes("c3", "c4")
            }) {
                Text("test")
            }
        }

        with(nextChild()) {
            assertEquals("classSetFromAttr classFromProperty1", getAttribute("class"))
        }
    }

    @Test
    fun attrClassVarargArrayList() = runTest {
        composition {
            Div(attrs = {
                classes("c1", "c2")
                classes(listOf("c3", "c4"))
                classes(classes = arrayOf("c5", "c6"))
            })
        }

        with(nextChild()) {
            assertEquals("c1 c2 c3 c4 c5 c6", getAttribute("class"))
        }
    }

    @Test
    fun attrClassOverridesClassesCall() = runTest {
        composition {
            Div(attrs = {
                // attr rewrites the content of 'class'
                attr("class", "classSetFromAttr")
                classes("c1")
            })
        }

        with(nextChild()) {
            assertEquals("classSetFromAttr", getAttribute("class"))
        }
    }

    @Test
    fun attrStyleOverridesStyleCall() = runTest {
        composition {
            Div(attrs = {
                // attr rewrites the content of 'style'
                attr("style", "color: red;")
                style {
                    color(Color.green)
                }
            })
        }

        with(nextChild()) {
            assertEquals("color: red;", getAttribute("style"))
        }
    }

    @Test
    fun propCanSeeAllAttrsSet() = runTest {
        val attrsCollectedInProp = mutableMapOf<String, String>()

        composition {
            Div(attrs = {
                attr("style", "color: red;")
                attr("class", "c1")
                prop<HTMLDivElement, Unit>({ e, _ ->
                    attrsCollectedInProp.putAll(
                        e.getAttributeNames().associateWith { e.getAttribute(it)!! }
                    )
                }, Unit)
            })
        }

        assertEquals("color: red;", attrsCollectedInProp["style"])
        assertEquals("c1", attrsCollectedInProp["class"])
        assertEquals(2, attrsCollectedInProp.size)
    }

    @Test
    fun copyFromStyleBuilderCopiesCorrectly() {
        val copyFromStyleBuilder = StyleScopeBuilder().apply {
            property("color", "red")
            property("height", 100.px)

            variable("var1", 100)
            variable("var2", 100.px)
        }

        val copyToStyleBuilder = StyleScopeBuilder().apply {
            copyFrom(copyFromStyleBuilder)
        }

        assertEquals(copyFromStyleBuilder, copyToStyleBuilder)
    }

    @Test
    fun copyFromAttrsBuilderCopiesCorrectly() {
        val attrsScopeCopyFrom = AttrsScopeBuilder<HTMLElement>().apply {
            id("id1")
            classes("a b c")
            attr("title", "customTitle")

            prop<HTMLElement, String>({ _, _ -> }, "Value")

            ref { onDispose { } }
            style {
                width(500.px)
                backgroundColor(Color.red)
            }

            onClick { }
            onFocusIn { }
            onMouseEnter { }
        }

        val copyToAttrsScope = AttrsScopeBuilder<HTMLElement>().apply {
            copyFrom(attrsScopeCopyFrom)
        }

        assertEquals(attrsScopeCopyFrom.attributesMap, copyToAttrsScope.attributesMap)
        assertEquals(attrsScopeCopyFrom.styleScope, copyToAttrsScope.styleScope)
        assertEquals(attrsScopeCopyFrom.refEffect, copyToAttrsScope.refEffect)
        assertEquals(attrsScopeCopyFrom.propertyUpdates, copyToAttrsScope.propertyUpdates)
        assertEquals(
            attrsScopeCopyFrom.eventsListenerScopeBuilder.collectListeners(),
            copyToAttrsScope.eventsListenerScopeBuilder.collectListeners()
        )
    }

    @Test
    fun attrsBuilderCopyFromPreservesExistingAttrs() {
        val attrsScopeCopyFrom = AttrsScopeBuilder<HTMLElement>().apply {
            attr("title", "customTitle")
        }

        val copyToAttrsScope = AttrsScopeBuilder<HTMLElement>().apply {
            id("id1")
            onClick { }
            style {
                width(100.px)
            }

            copyFrom(attrsScopeCopyFrom)
        }

        assertEquals("id1", copyToAttrsScope.attributesMap["id"])
        assertEquals(StyleScopeBuilder().apply { width(100.px) }, copyToAttrsScope.styleScope)

        val listeners = copyToAttrsScope.eventsListenerScopeBuilder.collectListeners()
        assertEquals(1, listeners.size)
        assertEquals("click", listeners[0].event)
    }

    @Test
    fun attrsBuilderCopyFromOverridesSameAttrs() {
        val attrsScopeCopyFrom = AttrsScopeBuilder<HTMLElement>().apply {
            attr("title", "customTitleNew")
        }

        val copyToAttrsScope = AttrsScopeBuilder<HTMLElement>().apply {
            attr("title", "customTitleOld")
        }
        assertEquals("customTitleOld", copyToAttrsScope.attributesMap["title"])

        copyToAttrsScope.copyFrom(attrsScopeCopyFrom)
        assertEquals("customTitleNew", copyToAttrsScope.attributesMap["title"])
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
                        div.innerText = "Text set using ref {}"
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
                ref { _ ->
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
            @Suppress("DEPRECATION")
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

        val child = nextChild()
        with(child) {
            val attrs = getAttributeNames().toList()
            assertEquals(2, attrs.size)
            assertTrue(attrs.containsAll(listOf("style", "class",)))

            assertEquals("button", tagName.lowercase())
            assertEquals("a", getAttribute("class"))
            assertEquals("color: red;", getAttribute("style"))
            assertEquals("Button", innerText)
        }

        hasValue = true
        waitForRecompositionComplete()

        with(child) {
            val attrs = getAttributeNames().toList()
            assertEquals(3, attrs.size)
            assertTrue(attrs.containsAll(listOf("style", "class", "value")))

            assertEquals("button", tagName.lowercase())
            assertEquals("a b", getAttribute("class"))
            assertEquals("buttonValue", getAttribute("value"))
            assertEquals("color: red;", getAttribute("style"))
            assertEquals("Button", innerText)
        }
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

    @Test
    fun canvasAttributeTest() = runTest {
        composition {
            Canvas({
                height(400)
                width(450)
            })
        }
        with(nextChild() as HTMLCanvasElement) {
            val attrsMap = getAttributeNames().associateWith { getAttribute(it) }
            assertEquals(2, attrsMap.size)
            assertEquals("450", attrsMap["width"])
            assertEquals("400", attrsMap["height"])
        }
    }
}
