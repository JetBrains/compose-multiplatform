/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.demos.text

import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

@Composable
fun TextOverflowedSelectionDemo() {
    var overflow by remember { mutableStateOf(TextOverflow.Clip) }
    val context = LocalContext.current
    val clipboardManager = remember(context) {
        context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    }
    var copiedText by remember { mutableStateOf("") }

    DisposableEffect(clipboardManager) {
        val listener = ClipboardManager.OnPrimaryClipChangedListener {
            copiedText = clipboardManager.read()
        }
        clipboardManager.addPrimaryClipChangedListener(listener)
        onDispose {
            clipboardManager.removePrimaryClipChangedListener(listener)
        }
    }

    SelectionContainer {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = overflow == TextOverflow.Clip,
                    onClick = { overflow = TextOverflow.Clip })
                Text(text = "Clip")
                Spacer(modifier = Modifier.width(8.dp))
                RadioButton(
                    selected = overflow == TextOverflow.Ellipsis,
                    onClick = { overflow = TextOverflow.Ellipsis })
                Text(text = "Ellipsis")
            }
            DisableSelection {
                Text(text = "Softwrap false, no maxLines")
            }
            OverflowToggleText(
                text = loremIpsum(Language.Latin, wordCount = 50),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Green),
                softWrap = false,
                overflow = overflow
            )
            DisableSelection {
                Text(text = "Softwrap true, maxLines 1, in a row")
            }
            Row {
                Box(modifier = Modifier.weight(1f), propagateMinConstraints = false) {
                    OverflowToggleText(
                        text = loremIpsum(Language.Latin, wordCount = 50),
                        modifier = Modifier
                            .background(Color.Green),
                        overflow = overflow,
                        maxLines = 1
                    )
                }
                Box(modifier = Modifier.weight(1f), propagateMinConstraints = false) {
                    OverflowToggleText(
                        text = loremIpsum(Language.Latin, wordCount = 50),
                        modifier = Modifier
                            .background(Color.Green),
                        overflow = overflow,
                        maxLines = 1
                    )
                }
            }
            DisableSelection {
                Text(text = "Softwrap true, height constrained, in a row")
            }
            Row {
                Box(modifier = Modifier.weight(1f), propagateMinConstraints = false) {
                    OverflowToggleText(
                        text = loremIpsum(Language.Latin, wordCount = 50),
                        modifier = Modifier
                            .background(Color.Green)
                            .heightIn(max = 36.dp),
                        overflow = overflow
                    )
                }
                Box(modifier = Modifier.weight(1f), propagateMinConstraints = false) {
                    OverflowToggleText(
                        text = loremIpsum(Language.Latin, wordCount = 50),
                        modifier = Modifier
                            .background(Color.Green)
                            .heightIn(max = 36.dp),
                        overflow = overflow
                    )
                }
            }
            DisableSelection {
                Text(text = "Softwrap true, maxLines 1, half width")
            }
            OverflowToggleText(
                text = loremIpsum(Language.Latin, wordCount = 50),
                modifier = Modifier
                    .background(Color.Green)
                    .fillMaxWidth(0.5f),
                overflow = overflow,
                maxLines = 1
            )
            DisableSelection {
                Text(text = "Softwrap true, maxLines 1")
            }
            OverflowToggleText(
                text = loremIpsum(Language.Latin, wordCount = 50),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Red),
                maxLines = 1,
                overflow = overflow
            )

            DisableSelection {
                Text(
                    text = "BiDi, softwrap true, maxLines 1",
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            OverflowToggleText(
                text = loremIpsum(
                    Language.Latin,
                    wordCount = 3
                ) + loremIpsum(Language.Arabic, wordCount = 20),
                modifier = Modifier
                    .fillMaxWidth(),
                maxLines = 1,
                overflow = overflow
            )

            DisableSelection {
                Text(text = "Copied Text", modifier = Modifier.padding(top = 16.dp))
            }
            TextField(value = copiedText, onValueChange = {}, modifier = Modifier.fillMaxWidth())
        }
    }
}

fun ClipboardManager.read(): String {
    return primaryClip?.getItemAt(0)?.text.toString()
}

@Composable
private fun OverflowToggleText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    var toggleOverflow by remember(overflow) { mutableStateOf(overflow) }
    Text(
        text = text,
        modifier = modifier.clickable {
            toggleOverflow = when (toggleOverflow) {
                TextOverflow.Clip -> TextOverflow.Ellipsis
                TextOverflow.Ellipsis -> TextOverflow.Visible
                TextOverflow.Visible -> TextOverflow.Clip
                else -> TextOverflow.Clip
            }
        },
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = toggleOverflow,
        softWrap = if (toggleOverflow == TextOverflow.Visible) true else softWrap,
        maxLines = if (toggleOverflow == TextOverflow.Visible) Int.MAX_VALUE else maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}