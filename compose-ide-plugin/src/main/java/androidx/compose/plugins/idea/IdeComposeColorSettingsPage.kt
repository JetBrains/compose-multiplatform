/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.plugins.idea

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import org.jetbrains.kotlin.idea.highlighter.KotlinColorSettingsPage
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingColors
import javax.swing.Icon

// This class is used by AndroidStudio to allow the user to change the style of Compose attributes.
class IdeComposeColorSettingsPage : ColorSettingsPage {
    override fun getHighlighter(): SyntaxHighlighter {
        return KotlinColorSettingsPage().highlighter
    }

    override fun getAdditionalHighlightingTagToDescriptorMap(): MutableMap<String,
            TextAttributesKey> {
        val attributes = HashMap<String, TextAttributesKey>()
        attributes[IdeComposableAnnotator.COMPOSABLE_CALL_TEXT_ATTRIBUTES_NAME] =
            IdeComposableAnnotator.COMPOSABLE_CALL_TEXT_ATTRIBUTES_KEY
        attributes["ANNOTATION"] = KotlinHighlightingColors.ANNOTATION
        attributes["KEYWORD"] = KotlinHighlightingColors.KEYWORD
        attributes["FUNCTION_DECLARATION"] = KotlinHighlightingColors.FUNCTION_DECLARATION
        attributes["FUNCTION_PARAMETER"] = KotlinHighlightingColors.PARAMETER
        return attributes
    }

    override fun getIcon(): Icon? {
        return null
    }

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> {
        // TODO: this needs to be localized.
        return arrayOf(AttributesDescriptor("Calls to @Compose functions",
            IdeComposableAnnotator.COMPOSABLE_CALL_TEXT_ATTRIBUTES_KEY))
    }

    override fun getColorDescriptors(): Array<ColorDescriptor> {
        return emptyArray()
    }

    override fun getDisplayName(): String {
        // TODO: this needs to be localized.
        return "Compose"
    }

    override fun getDemoText(): String {
        return "<ANNOTATION>@Composable</ANNOTATION>\n" +
            "<KEYWORD>fun</KEYWORD> <FUNCTION_DECLARATION>Text</FUNCTION_DECLARATION>(" +
            "<FUNCTION_PARAMETER>text</FUNCTION_PARAMETER>: <FUNCTION_PARAMETER>String" +
            "</FUNCTION_PARAMETER>)\n" +
            "}\n" +
            "\n" +
            "<ANNOTATION>@Composable</ANNOTATION>\n" +
            "<KEYWORD>fun</KEYWORD> <FUNCTION_DECLARATION>Greeting</FUNCTION_DECLARATION>() {\n" +
            "    <ComposableCallTextAttributes>Text</ComposableCallTextAttributes>(" +
            "<FUNCTION_PARAMETER>\"Hello\"</FUNCTION_PARAMETER>)\n" +
            "}"
    }
}