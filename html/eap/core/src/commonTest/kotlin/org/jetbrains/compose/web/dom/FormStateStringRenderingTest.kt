/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.Recomposer
import kotlinx.browser.dom.HTMLInputElement
import kotlinx.browser.dom.HTMLTextAreaElement
import kotlinx.coroutines.Dispatchers
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.setInputValue
import org.jetbrains.compose.web.attributes.setTextAreaValue
import org.jetbrains.compose.web.LocalHtmlValidationMode
import org.jetbrains.compose.web.htmlValidationMode
import org.jetbrains.compose.web.composeHtmlString
import org.jetbrains.compose.web.composeHtmlToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FormStateStringRenderingTest {
    @Test
    fun serializesControlledInputValues() {
        assertRenders(
            "<input type=\"text\" value=\"Tom &amp; &lt;Jerry&gt; &quot;'&#10;&#9;&#13;é😀\">" +
                "<input type=\"number\" value=\"12.5\">" +
                "<input type=\"range\" step=\"1\" value=\"42\">" +
                "<input type=\"text\" value=\"\">",
        ) {
            TextInput("Tom & <Jerry> \"'\n\t\ré😀")
            NumberInput(12.5)
            RangeInput(42)
            TextInput()
        }
    }

    @Test
    fun controlledInputValuesOverrideDefaultsInEitherOrder() {
        assertRenders("<input type=\"text\" value=\"controlled\"><input type=\"text\" value=\"\">") {
            Input(InputType.Text) {
                defaultValue("default")
                value("first")
                value("controlled")
                defaultValue("later default")
            }
            Input(InputType.Text) {
                value("")
                defaultValue("default")
            }
        }
    }

    @Test
    fun serializesCheckboxAndRadioCheckedState() {
        assertRenders(
            "<input type=\"checkbox\" checked><input type=\"checkbox\">" +
                "<input type=\"radio\" checked><input type=\"radio\">",
        ) {
            CheckboxInput(true)
            CheckboxInput(false)
            RadioInput(true)
            RadioInput(false)
        }
    }

    @Test
    fun controlledCheckedStateOverridesDefaultsAndUsesLastUpdate() {
        assertRenders("<input type=\"checkbox\"><input type=\"radio\" checked><input type=\"checkbox\" checked>") {
            Input(InputType.Checkbox) {
                checked(true)
                checked(false)
                defaultChecked()
            }
            Input(InputType.Radio) {
                attr("checked", "false")
                checked(false)
                checked(true)
            }
            Input(InputType.Checkbox) { defaultChecked() }
        }
    }

    @Test
    fun serializesTextareaValuesAndDefaultsAsEscapedText() {
        assertRenders(
            "<textarea>\n\nTom &amp; &lt;/textarea&gt; \"é😀&#13;</textarea>" +
                "<textarea>default &amp; &lt;text&gt;</textarea>" +
                "<textarea>attribute value</textarea><textarea></textarea>",
        ) {
            TextArea("\nTom & </textarea> \"é😀\r")
            TextArea { defaultValue("default & <text>") }
            TextArea { value("attribute value") }
            TextArea("")
        }
    }

    @Test
    fun controlledTextareaValueOverridesDefaultsInEitherOrder() {
        assertRenders("<textarea>controlled</textarea><textarea></textarea><textarea>last default</textarea>") {
            TextArea("controlled") { defaultValue("default") }
            TextArea {
                value("first")
                defaultValue("default")
                value("")
                defaultValue("later default")
            }
            TextArea {
                defaultValue("first default")
                defaultValue("last default")
            }
        }
    }

    @Test
    fun textareaValueReplacesChildTextWhileChildrenStillCompose() {
        var composed = 0
        assertRenders("<textarea>value</textarea><span>after</span>") {
            TagElement<HTMLTextAreaElement>("textarea", { prop(setTextAreaValue, "value") }) {
                composed++
                Text("child")
            }
            Span { Text("after") }
        }
        assertEquals(16, composed)
    }

    @Test
    fun arbitraryPropertyCallbacksAreNotInvokedOrSerialized() {
        assertRenders("<input type=\"text\"><textarea></textarea>") {
            Input(InputType.Text) {
                prop({ _: HTMLInputElement, _: String -> error("DOM callback must not execute") }, "ignored")
            }
            TextArea {
                prop({ _: HTMLTextAreaElement, _: String -> error("DOM callback must not execute") }, "ignored")
            }
        }
    }

    @Test
    fun formPropertiesDoNotAffectOtherTagsOrNamespaces() {
        assertRenders("<div></div><input></input>") {
            Div({ prop(setInputValue, "ignored") }) {}
            TagElementNS<HTMLInputElement>("input", "urn:example", {
                prop(setInputValue, "ignored")
            }, null)
        }
    }

    @Test
    fun keyedRendersClearFormStateBetweenRequests() {
        for (includeState in listOf(true, false, true, false)) {
            val content: @Composable () -> Unit = {
                Input(InputType.Text) { if (includeState) value("value") }
                Input(InputType.Checkbox) { if (includeState) checked(true) }
                TextArea { if (includeState) defaultValue("default") }
            }
            assertEquals(
                composeHtmlToString(content = content),
                composeHtmlToString(key = "form-state-reset", content = content),
            )
        }
    }

    @Test
    fun formStateRejectsNulCharacters() {
        for (content in listOf<@Composable () -> Unit>(
            { TextInput("a\u0000b") },
            { TextArea("a\u0000b") },
            { TextArea { defaultValue("a\u0000b") } },
        )) {
            assertFailsWith<IllegalArgumentException> { composeHtmlToString(content = content) }
        }
    }

    private fun assertRenders(expected: String, content: @Composable () -> Unit) {
        for (hydratable in listOf(false, true)) {
            for (strict in listOf(false, true)) {
                assertEquals(expected, composeHtmlToString(hydratable, validateStrictly = strict, content = content))
                assertEquals(expected, composeHtmlToString(hydratable, "form-state", strict, content))
                val chunks = StringBuilder()
                val context: @Composable () -> Unit = {
                    CompositionLocalProvider(
                        LocalHtmlValidationMode provides htmlValidationMode(strict),
                    ) { content() }
                }
                composeHtmlString(hydratable, chunkSink = { chunks.append(it) }, chunkSize = 1, content = context)
                assertEquals(expected, chunks.toString())
                assertEquals(expected, renderTree(hydratable, context))
            }
        }
    }

    private fun renderTree(hydratable: Boolean, content: @Composable () -> Unit): String {
        val root = StringHtmlElementNode.root()
        val recomposer = Recomposer(Dispatchers.Default)
        val composition = ControlledComposition(StringHtmlApplier(StringHtmlNodeWrapper(root)), recomposer)
        try {
            composition.setContent {
                CompositionLocalProvider(LocalComposeHtmlContext provides StringComposeHtmlContext) { content() }
            }
            return root.toHtmlString(hydratable)
        } finally {
            composition.dispose()
            recomposer.close()
        }
    }
}
