/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.dom.HTMLElement
import kotlinx.browser.dom.HTMLInputElement
import kotlinx.browser.dom.HTMLTextAreaElement
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.CheckboxInput
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.RadioInput
import org.jetbrains.compose.web.dom.TextArea
import org.jetbrains.compose.web.dom.TextInput
import org.jetbrains.compose.web.internal.runtime.browserDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class FormStateStringRenderingBrowserTest {
    @Test
    fun clientRenderedTextareaDefaultsPreserveNewlinesAndUserEdits() = MainScope().promise {
        val root = browserDocument.createElement("div") as HTMLElement
        val value = "first\nsecond & <third>"
        var default by mutableStateOf(value)
        val composition = renderComposable(root) { TextArea { defaultValue(default) } }
        try {
            val textarea = root.firstElementChild as HTMLTextAreaElement
            assertEquals(value, textarea.value)
            assertEquals(value, textarea.textContent)
            textarea.value = "typed"
            default = "new\ndefault"
            delay(100.milliseconds)
            assertEquals("typed", textarea.value)
            assertEquals("new\ndefault", textarea.textContent)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun controlledInputOverridesHydrateWithConflictingDefaults() {
        val content: @Composable () -> Unit = {
            TextInput("controlled") { defaultValue("default") }
            CheckboxInput(false) { defaultChecked() }
            RadioInput(false) { attr("checked", "false") }
        }
        for (strict in listOf(false, true)) {
            val root = browserDocument.createElement("div") as HTMLElement
            root.innerHTML = composeHtmlToString(content = content)
            val input = root.firstElementChild as HTMLInputElement
            val composition = hydrateComposable(root, validateStrictly = strict, onHydrationMismatch = { throw it }) {
                content()
            }
            try {
                assertSame(input, root.firstElementChild)
                assertEquals("controlled", input.value)
                assertFalse((root.children.item(1) as HTMLInputElement).checked)
                assertFalse((root.children.item(2) as HTMLInputElement).checked)
            } finally {
                composition.dispose()
            }
        }
    }

    @Test
    fun controlledInputStateCanChangeDuringHydrationWithDefaults() {
        for (strict in listOf(false, true)) {
            for ((value, checked) in listOf("client" to true, "" to false)) {
                val root = browserDocument.createElement("div") as HTMLElement
                root.innerHTML = composeHtmlToString {
                    TextInput("server") { defaultValue("default") }
                    CheckboxInput(!checked) { defaultChecked() }
                    RadioInput(!checked) { defaultChecked() }
                }
                val inputs = (0..2).map { root.children.item(it) as HTMLInputElement }
                val composition = hydrateComposable(root, validateStrictly = strict, onHydrationMismatch = { throw it }) {
                    TextInput(value) { defaultValue("default") }
                    CheckboxInput(checked) { defaultChecked() }
                    RadioInput(checked) { defaultChecked() }
                }
                try {
                    inputs.forEachIndexed { index, input -> assertSame(input, root.children.item(index)) }
                    assertEquals(value, inputs[0].value)
                    assertEquals(checked, inputs[1].checked)
                    assertEquals(checked, inputs[2].checked)
                } finally {
                    composition.dispose()
                }
            }
        }
    }

    @Test
    fun controlledInputStateDoesNotSuppressOtherAttributeMismatches() {
        val root = browserDocument.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            TextInput("server") {
                defaultValue("default")
                attr("data-kind", "server")
            }
        }
        assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, validateStrictly = true, onHydrationMismatch = { throw it }) {
                TextInput("client") {
                    defaultValue("default")
                    attr("data-kind", "client")
                }
            }
        }
    }

    @Test
    fun parsedInputsRestoreValuesAndCheckedState() {
        val root = browserDocument.createElement("div") as HTMLElement
        val value = "Tom & <Jerry> \"'é😀"
        root.innerHTML = composeHtmlToString {
            TextInput(value)
            CheckboxInput(true)
            CheckboxInput(false) { defaultChecked() }
            RadioInput(true)
            RadioInput(false) { defaultChecked() }
        }
        val inputs = root.querySelectorAll("input")
        assertEquals(value, (inputs.item(0) as HTMLInputElement).value)
        for (index in 1..4) {
            val input = inputs.item(index) as HTMLInputElement
            val checked = index % 2 == 1
            assertEquals(checked, input.checked)
            assertEquals(checked, input.hasAttribute("checked"))
        }
    }

    @Test
    fun parsedTextareasRestoreEscapedValuesAndLeadingNewlines() {
        val values = listOf("", "hello", "\nhello", "\n\nhello", "& </textarea><script>é😀\"")
        for (value in values) {
            val root = browserDocument.createElement("div") as HTMLElement
            root.innerHTML = composeHtmlToString {
                TextArea(value)
                TextArea { defaultValue(value) }
                TextArea { value(value) }
            }
            assertEquals(3, root.children.length)
            for (index in 0..2) {
                val textarea = root.children.item(index) as HTMLTextAreaElement
                assertEquals(value, textarea.value)
                assertEquals(value, textarea.textContent)
                assertFalse(textarea.hasAttribute("value"))
            }
        }
    }

    @Test
    fun serializedTextareaDefaultsHydrateWithTheirExistingElements() {
        for (strict in listOf(false, true)) {
            val value = "\nDefault & </textarea>é😀"
            val root = browserDocument.createElement("div") as HTMLElement
            root.innerHTML = composeHtmlToString { TextArea { defaultValue(value) } }
            val textarea = root.firstElementChild as HTMLTextAreaElement
            val composition = hydrateComposable(root, validateStrictly = strict, onHydrationMismatch = { throw it }) {
                TextArea { defaultValue(value) }
            }
            try {
                assertSame(textarea, root.firstElementChild)
                assertEquals(value, textarea.value)
            } finally {
                composition.dispose()
            }
        }
    }

    @Test
    fun emptyControlledTextareaValueOverridesSerializedServerDefault() {
        val root = browserDocument.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString { TextArea { defaultValue("server default") } }
        val textarea = root.firstElementChild as HTMLTextAreaElement
        val composition = hydrateComposable(root, validateStrictly = true, onHydrationMismatch = { throw it }) {
            TextArea("")
        }
        try {
            assertSame(textarea, root.firstElementChild)
            assertEquals("", textarea.value)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun unexpectedTextareaElementChildrenStillFailHydration() {
        val root = browserDocument.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString { TextArea("server") }
        val textarea = root.firstElementChild as HTMLTextAreaElement
        textarea.appendChild(browserDocument.createElement("span"))
        assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root, validateStrictly = true, onHydrationMismatch = { throw it }) {
                TextArea("client")
            }
        }
        assertEquals("server", textarea.value)
    }

    @Test
    fun uncontrolledDefaultsStillPopulateParsedInputs() {
        val root = browserDocument.createElement("div") as HTMLElement
        root.innerHTML = composeHtmlToString {
            Input(InputType.Text) { defaultValue("default") }
            Input(InputType.Checkbox) { defaultChecked() }
        }
        assertEquals("default", (root.firstElementChild as HTMLInputElement).value)
        assertTrue((root.lastElementChild as HTMLInputElement).checked)
    }
}
