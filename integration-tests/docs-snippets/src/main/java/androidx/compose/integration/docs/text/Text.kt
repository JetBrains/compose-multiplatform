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

// Ignore lint warnings in documentation snippets
@file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE", "PreviewMustBeTopLevelFunction")

package androidx.compose.integration.docs.text

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/text
 *
 * No action required if it's modified.
 */

private object SimpleTextSnippet {
    @Composable
    fun SimpleText() {
        Text("Hello World")
    }
}

private object StringResourceSnippet {
    @Composable
    fun StringResourceText() {
        Text(stringResource(R.string.hello_world))
    }
}

private object TextColorSnippet {
    @Composable
    fun BlueText() {
        Text("Hello World", color = Color.Blue)
    }
}

private object TextSizeSnippet {
    @Composable
    fun BigText() {
        Text("Hello World", fontSize = 30.sp)
    }
}

private object TextItalicSnippet {
    @Composable
    fun ItalicText() {
        Text("Hello World", fontStyle = FontStyle.Italic)
    }
}

private object TextBoldSnippet {
    @Composable
    fun BoldText() {
        Text("Hello World", fontWeight = FontWeight.Bold)
    }
}

private object TextAlignmentSnippet {
    @Preview(showBackground = true)
    @Composable
    fun CenterText() {
        Text(
            "Hello World", textAlign = TextAlign.Center,
            modifier = Modifier.width(150.dp)
        )
    }
}

private object TextMultipleFontsSnippet {
    @Composable
    fun DifferentFonts() {
        Column {
            Text("Hello World", fontFamily = FontFamily.Serif)
            Text("Hello World", fontFamily = FontFamily.SansSerif)
        }
    }
}

@Composable
private fun TextDefineFontFamilySnippet() {
    val firaSansFamily = FontFamily(
        Font(R.font.firasans_light, FontWeight.Light),
        Font(R.font.firasans_regular, FontWeight.Normal),
        Font(R.font.firasans_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.firasans_medium, FontWeight.Medium),
        Font(R.font.firasans_bold, FontWeight.Bold)
    )
}

/* NOTE:
 * Snippet in docs page simplifies the arguments, using "..." for everything before
 * the font values. If code in TextFontWeightSnippet changes, make the corresponding change
 * to this code, which is in the doc:
Column {
    Text(..., fontFamily = firaSansFamily, fontWeight = FontWeight.Light)
    Text(..., fontFamily = firaSansFamily, fontWeight = FontWeight.Normal)
    Text(
        ..., fontFamily = firaSansFamily, fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic
    )
    Text(..., fontFamily = firaSansFamily, fontWeight = FontWeight.Medium)
    Text(..., fontFamily = firaSansFamily, fontWeight = FontWeight.Bold)
}
 *
 */
@Composable
private fun TextFontWeightSnippet() {
    Column {
        Text(text = "test", fontFamily = firaSansFamily, fontWeight = FontWeight.Light)
        Text(text = "test", fontFamily = firaSansFamily, fontWeight = FontWeight.Normal)
        Text(
            text = "test", fontFamily = firaSansFamily, fontWeight = FontWeight.Normal,
            fontStyle = FontStyle.Italic
        )
        Text(text = "test", fontFamily = firaSansFamily, fontWeight = FontWeight.Medium)
        Text(text = "test", fontFamily = firaSansFamily, fontWeight = FontWeight.Bold)
    }
}

private object TextMultipleStylesSnippet {
    @Composable
    fun MultipleStylesInText() {
        Text(
            buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color.Blue)) {
                    append("H")
                }
                append("ello ")

                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.Red)) {
                    append("W")
                }
                append("orld")
            }
        )
    }
}

private object TextParagraphStyleSnippet {
    @Composable
    fun ParagraphStyle() {
        Text(
            buildAnnotatedString {
                withStyle(style = ParagraphStyle(lineHeight = 30.sp)) {
                    withStyle(style = SpanStyle(color = Color.Blue)) {
                        append("Hello\n")
                    }
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    ) {
                        append("World\n")
                    }
                    append("Compose")
                }
            }
        )
    }
}

private object TextMaxLinesSnippet {
    @Composable
    fun LongText() {
        Text("hello ".repeat(50), maxLines = 2)
    }
}

private object TextOverflowSnippet {
    @Composable
    fun OverflowedText() {
        Text("Hello Compose ".repeat(50), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

private object TextSelectableSnippet {
    @Composable
    fun SelectableText() {
        SelectionContainer {
            Text("This text is selectable")
        }
    }
}

private object TextPartiallySelectableSnippet {
    @Composable
    fun PartiallySelectableText() {
        SelectionContainer {
            Column {
                Text("This text is selectable")
                Text("This one too")
                Text("This one as well")
                DisableSelection {
                    Text("But not this one")
                    Text("Neither this one")
                }
                Text("But again, you can select this one")
                Text("And this one too")
            }
        }
    }
}

private object TextClickableSnippet {
    @Composable
    fun SimpleClickableText() {
        ClickableText(
            text = AnnotatedString("Click Me"),
            onClick = { offset ->
                Log.d("ClickableText", "$offset -th character is clicked.")
            }
        )
    }
}

private object TextClickableAnnotatedSnippet {
    @Composable
    fun AnnotatedClickableText() {
        val annotatedText = buildAnnotatedString {
            append("Click ")

            // We attach this *URL* annotation to the following content
            // until `pop()` is called
            pushStringAnnotation(
                tag = "URL",
                annotation = "https://developer.android.com"
            )
            withStyle(
                style = SpanStyle(
                    color = Color.Blue,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append("here")
            }

            pop()
        }

        ClickableText(
            text = annotatedText,
            onClick = { offset ->
                // We check if there is an *URL* annotation attached to the text
                // at the clicked position
                annotatedText.getStringAnnotations(
                    tag = "URL", start = offset,
                    end = offset
                )
                    .firstOrNull()?.let { annotation ->
                        // If yes, we log its value
                        Log.d("Clicked URL", annotation.item)
                    }
            }
        )
    }
}

private object TextTextFieldSnippet {
    @Composable
    fun SimpleFilledTextFieldSample() {
        var text by remember { mutableStateOf("Hello") }

        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Label") }
        )
    }
}

private object TextOutlinedTextFieldSnippet {
    @Composable
    fun SimpleOutlinedTextFieldSample() {
        var text by remember { mutableStateOf("") }

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Label") }
        )
    }
}

private object TextStylingTextFieldSnippet {
    @Composable
    fun StyledTextField() {
        var value by remember { mutableStateOf("Hello\nWorld\nInvisible") }

        TextField(
            value = value,
            onValueChange = { value = it },
            label = { Text("Enter text") },
            maxLines = 2,
            textStyle = TextStyle(color = Color.Blue, fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(20.dp)
        )
    }
}

private object TextFormattingTextFieldSnippet {
    @Composable
    fun PasswordTextField() {
        var password by rememberSaveable { mutableStateOf("") }

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Enter password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
    }
}

private object TextCleanInputSnippet {
    @Composable
    fun NoLeadingZeroes() {
        var input by rememberSaveable { mutableStateOf("") }
        TextField(
            value = input,
            onValueChange = { newText ->
                input = newText.trimStart { it == '0' }
            }
        )
    }
}

/*
 * Fakes needed for snippets to build:
 */

private object R {
    object string {
        const val hello_world = 1
    }

    object font {
        const val firasans_light = 1
        const val firasans_regular = 1
        const val firasans_italic = 1
        const val firasans_medium = 1
        const val firasans_bold = 1
    }
}

private val firaSansFamily = FontFamily()
