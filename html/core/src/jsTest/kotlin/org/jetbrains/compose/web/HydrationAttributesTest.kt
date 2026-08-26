package org.jetbrains.compose.web

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.MutationObserver
import org.w3c.dom.MutationObserverInit
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class HydrationAttributesTest {
    @Test
    fun booleanAttributeValuesAreComparedByPresence() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Button(attrs = { attr("disabled", "true") })
        }
        val button = root.firstChild as HTMLElement

        val composition = hydrateComposable(root) {
            Button(attrs = { attr("disabled", "true") })
        }

        try {
            assertSame(button, root.firstChild)
            assertEquals("", button.getAttribute("disabled"))
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun unrelatedServerAttributesAreTolerated() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Div(attrs = { attr("data-owned", "value") })
        }
        val div = root.firstChild as HTMLElement
        div.setAttribute("data-extension", "injected")
        div.classList.add("ext-injected")
        div.style.setProperty("outline", "1px solid red")
        val injectedOutline = div.style.getPropertyValue("outline")

        val composition = hydrateComposable(root) {
            Div(attrs = { attr("data-owned", "value") })
        }

        try {
            assertSame(div, root.firstChild)
            assertEquals("injected", div.getAttribute("data-extension"))
            assertTrue(div.classList.contains("ext-injected"))
            assertEquals(injectedOutline, div.style.getPropertyValue("outline"))
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun additionalServerClassTokensAreTolerated() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Div(attrs = { classes("compose-owned") })
            Div(attrs = { attr("class", "raw-owned") })
        }
        val structuredDiv = root.firstChild as HTMLElement
        val rawDiv = root.lastChild as HTMLElement
        structuredDiv.classList.add("injected")
        rawDiv.classList.add("injected")

        val composition = hydrateComposable(root) {
            Div(attrs = { classes("compose-owned") })
            Div(attrs = { attr("class", "raw-owned") })
        }

        try {
            assertSame(structuredDiv, root.firstChild)
            assertSame(rawDiv, root.lastChild)
            assertTrue(structuredDiv.classList.contains("compose-owned"))
            assertTrue(structuredDiv.classList.contains("injected"))
            assertTrue(rawDiv.classList.contains("raw-owned"))
            assertTrue(rawDiv.classList.contains("injected"))
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun failedHydrationDoesNotApplyElementMutations() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Div(attrs = {
                attr("data-kind", "server")
                classes("server")
                style { property("color", "red") }
            })
            Span()
        }
        val serverHtml = root.innerHTML
        var propertyUpdateCount = 0

        assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, onHydrationMismatch = { throw it }) {
                Div(attrs = {
                    attr("data-kind", "client")
                    classes("client")
                    style { property("color", "blue") }
                    prop(
                        update = { element: HTMLElement, value: String ->
                            propertyUpdateCount++
                            element.setAttribute("data-property", value)
                        },
                        value = "client",
                    )
                })
            }
        }

        assertEquals(0, propertyUpdateCount)
        assertEquals(serverHtml, root.innerHTML)
    }

    @Test
    fun attributeMismatchFallsBackToClientRendering() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Div(attrs = {
                attr("data-kind", "server")
                attr("data-stale", "server-only")
                classes("server")
                style { property("color", "red") }
            })
        }
        val serverDiv = root.firstChild as HTMLElement
        var propertyUpdateCount = 0
        var refObservedKind: String? = null
        var refObservedProperty: String? = null

        val composition = hydrateComposable(
            root = root,
            onHydrationMismatch = {},
        ) {
            Div(attrs = {
                attr("data-kind", "client")
                classes("client")
                style { property("color", "blue") }
                prop(
                    update = { element: HTMLElement, value: String ->
                        propertyUpdateCount++
                        element.setAttribute("data-property", value)
                    },
                    value = "client",
                )
                ref { element ->
                    refObservedKind = element.getAttribute("data-kind")
                    refObservedProperty = element.getAttribute("data-property")
                    onDispose { }
                }
            })
        }

        val clientDiv = root.firstChild as HTMLElement
        try {
            assertNotSame(serverDiv, clientDiv)
            assertEquals(1, propertyUpdateCount)
            assertEquals("client", clientDiv.getAttribute("data-kind"))
            assertEquals(null, clientDiv.getAttribute("data-stale"))
            assertEquals("client", clientDiv.getAttribute("class"))
            assertEquals("color: blue;", clientDiv.getAttribute("style"))
            assertEquals("client", clientDiv.getAttribute("data-property"))
            assertEquals("client", refObservedKind)
            assertEquals("client", refObservedProperty)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun controlledInputPropertyIsAppliedAfterAttributeVerification() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Input(InputType.Text) {
                defaultValue("default")
            }
        }
        val input = root.firstChild as HTMLInputElement
        input.value = "typed before hydration"

        val composition = hydrateComposable(root) {
            Input(InputType.Text) {
                defaultValue("default")
                value("controlled")
            }
        }

        try {
            assertEquals("default", input.getAttribute("value"))
            assertEquals("controlled", input.value)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun uncontrolledInputEditSurvivesHydration() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Input(InputType.Text) {
                defaultValue("default")
            }
        }
        val input = root.firstChild as HTMLInputElement
        input.value = "typed before hydration"

        val composition = hydrateComposable(root) {
            Input(InputType.Text) {
                defaultValue("default")
            }
        }

        try {
            assertEquals("default", input.getAttribute("value"))
            assertEquals("typed before hydration", input.value)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun uncomposedInputDefaultIsTolerated() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Input(InputType.Text) {
                defaultValue("server default")
            }
        }
        val input = root.firstChild as HTMLInputElement

        val composition = hydrateComposable(root) {
            Input(InputType.Text)
        }

        try {
            assertSame(input, root.firstChild)
            assertEquals("server default", input.getAttribute("value"))
            assertEquals("server default", input.value)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun controlledCheckedPropertyIsAppliedAfterAttributeVerification() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Input(InputType.Checkbox)
        }
        val input = root.firstChild as HTMLInputElement

        val composition = hydrateComposable(root) {
            Input(InputType.Checkbox) {
                checked(true)
            }
        }

        try {
            assertEquals(null, input.getAttribute("checked"))
            assertEquals(true, input.checked)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun uncomposedCheckedDefaultIsTolerated() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Input(InputType.Checkbox) {
                defaultChecked()
            }
        }
        val input = root.firstChild as HTMLInputElement

        val composition = hydrateComposable(root) {
            Input(InputType.Checkbox)
        }

        try {
            assertSame(input, root.firstChild)
            assertEquals("", input.getAttribute("checked"))
            assertEquals(true, input.checked)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun uncontrolledOptionSelectionSurvivesHydration() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Select {
                Option("first", attrs = { attr("selected", "") }) { Text("First") }
                Option("second") { Text("Second") }
            }
        }
        val select = root.firstChild as HTMLSelectElement
        select.value = "second"

        val composition = hydrateComposable(root) {
            Select {
                Option("first", attrs = { attr("selected", "") }) { Text("First") }
                Option("second") { Text("Second") }
            }
        }

        try {
            assertEquals("second", select.value)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun uncomposedSelectedDefaultIsTolerated() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Select {
                Option("first", attrs = { attr("selected", "") }) { Text("First") }
                Option("second") { Text("Second") }
            }
        }
        val select = root.firstChild as HTMLSelectElement
        val firstOption = select.firstChild as HTMLElement

        val composition = hydrateComposable(root) {
            Select {
                Option("first") { Text("First") }
                Option("second") { Text("Second") }
            }
        }

        try {
            assertSame(select, root.firstChild)
            assertEquals("", firstOption.getAttribute("selected"))
            assertEquals("first", select.value)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun differentClientSelectedDefaultIsAnAttributeMismatch() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Select {
                Option("first", attrs = { attr("selected", "") }) { Text("First") }
                Option("second") { Text("Second") }
            }
        }
        val select = root.firstChild as HTMLSelectElement

        val serverHtml = root.innerHTML

        assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, onHydrationMismatch = { throw it }) {
                Select {
                    Option("first") { Text("First") }
                    Option("second", attrs = { attr("selected", "") }) { Text("Second") }
                }
            }
        }

        assertEquals(serverHtml, root.innerHTML)
        assertEquals("first", select.value)
    }

    @Test
    fun controlledTextAreaPropertyWinsOverPreHydrationEdit() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            TextArea(value = "server")
        }
        val textArea = root.firstChild as HTMLTextAreaElement
        textArea.value = "typed before hydration"

        val composition = hydrateComposable(root) {
            TextArea(value = "controlled")
        }

        try {
            assertEquals("controlled", textArea.value)
        } finally {
            composition.dispose()
        }
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    @Test
    fun matchingAttributesClassesAndStylesAreNotRewritten() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Div(attrs = {
                attr("data-kind", "matching")
                classes("first", "second")
                style {
                    property("color", "red")
                    property("display", "block", important = true)
                    variable("accent", "orange")
                }
            })
        }
        val div = root.firstChild as HTMLElement
        val observer = MutationObserver { _, _ -> }
        observer.observe(
            div,
            MutationObserverInit(
                attributes = true,
                attributeFilter = arrayOf("class", "style", "data-kind"),
            ),
        )

        val composition = hydrateComposable(root) {
            Div(attrs = {
                attr("data-kind", "matching")
                classes("first", "second")
                style {
                    property("color", "red")
                    property("display", "block", important = true)
                    variable("accent", "orange")
                }
            })
        }

        try {
            assertEquals(0, observer.takeRecords().length)
        } finally {
            observer.disconnect()
            composition.dispose()
        }
    }

    @Test
    fun laterDefaultValueDoesNotOverwriteAPreHydrationEdit() = MainScope().promise {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Input(InputType.Text) {
                defaultValue("server default")
            }
        }
        val input = root.firstChild as HTMLInputElement
        input.value = "typed before hydration"
        var defaultValue by mutableStateOf("server default")

        val composition = hydrateComposable(root) {
            Input(InputType.Text) {
                defaultValue(defaultValue)
            }
        }

        try {
            assertEquals("server default", input.getAttribute("value"))
            assertEquals("typed before hydration", input.value)

            defaultValue = "later default"
            delay(100.milliseconds)

            assertEquals("later default", input.getAttribute("value"))
            assertEquals("typed before hydration", input.value)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun rawClassAndStyleAttributesKeepPrecedenceAfterHydration() = MainScope().promise {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Div(attrs = {
                classes("ignored-before")
                style { property("color", "red") }
                attr("class", "manual")
                attr("style", "display:none")
            })
        }
        val div = root.firstChild as HTMLElement
        var useOtherBuilderValues by mutableStateOf(false)

        val composition = hydrateComposable(root) {
            Div(attrs = {
                classes(if (useOtherBuilderValues) "ignored-after" else "ignored-before")
                style {
                    property("color", if (useOtherBuilderValues) "blue" else "red")
                }
                attr("class", "manual")
                attr("style", "display:none")
            })
        }

        try {
            assertSame(div, root.firstChild)
            useOtherBuilderValues = true
            delay(100.milliseconds)

            assertEquals("manual", div.getAttribute("class"))
            assertEquals("display:none", div.getAttribute("style"))
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun propertiesObserveFinalizedHydratedTextNodes() {
        val root = document.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Div {
                Text("first")
                Text("")
                Text("second")
            }
        }
        var observedChildren = emptyList<org.w3c.dom.Node>()

        val composition = hydrateComposable(root) {
            Div(attrs = {
                prop(
                    update = { element: HTMLElement, _: Unit ->
                        observedChildren = (0 until element.childNodes.length).mapNotNull {
                            element.childNodes.item(it)
                        }
                    },
                    value = Unit,
                )
            }) {
                Text("first")
                Text("")
                Text("second")
            }
        }

        try {
            assertEquals(3, observedChildren.size)
            assertTrue(observedChildren.all { node -> node is org.w3c.dom.Text })
        } finally {
            composition.dispose()
        }
    }
}
