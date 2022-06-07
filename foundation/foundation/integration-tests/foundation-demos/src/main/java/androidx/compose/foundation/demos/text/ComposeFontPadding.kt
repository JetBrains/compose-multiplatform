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

package androidx.compose.foundation.demos.text

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Preview
@Composable
fun TextFontPaddingDemo() {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        FontPaddingRow("ABCDEfgHIjKgpvyzgpvyzgpvyzgpvyz")
        FontPaddingRow("مرحبا" + "ဪไန််မ့်၇ဤဩဦနိမြသကိမ့်" + "مرحبا" + "ဪไန််မ့်၇ဤဩဦနိမြသကိမ့်")
        CenteredInContainerRow()
        CenterInCircleRow()
        MultiStyleText()
        InlineContent()
        Configuration()
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun Configuration() {
    val fontSize = 72.sp
    val width = with(LocalDensity.current) { fontSize.toDp() } * 5
    val padding = Modifier.padding(8.dp)
    val latinText = "ABCDEfgHIjKgpvyzgpvyzgpvyzgpvyz"
    val tallText = "ဪไန််မ့်၇ဤဩဦနိမြသကိမ့်ဪไန််မ့်၇ဤဩဦနိမြသကိမ့်"
    val style = TextStyle(fontSize = fontSize)
    Row(padding.horizontalScroll(rememberScrollState())) {
        for (text in arrayOf(latinText, tallText)) {
            Box {
                Column(padding.width(width)) {
                    @Suppress("DEPRECATION")
                    Text(
                        text,
                        style = style.copy(
                            color = Color.Red,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
                Column(padding.width(width)) {
                    @Suppress("DEPRECATION")
                    Text(
                        text,
                        style = style.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = true)
                        )
                    )
                }
            }
            Spacer(padding)
        }
    }
}

@Composable
private fun FontPaddingRow(text: String) {
    val padding = Modifier.padding(8.dp)
    Row(padding.horizontalScroll(rememberScrollState())) {
        for (overflow in arrayOf(TextOverflow.Clip, TextOverflow.Ellipsis, TextOverflow.Visible)) {
            Column(padding) {
                SecondTagLine(tag = "TextOverflow.$overflow")
                Spacer(padding)
                FontPaddingColumn(text, overflow)
            }
        }
    }
}

@Composable
private fun FontPaddingColumn(text: String, overflow: TextOverflow) {
    val fontSize = fontSize8
    val width = with(LocalDensity.current) { fontSize.toDp() } * 5
    val widthWodifier = Modifier.width(width)
    Column {
        SecondTagLine(tag = "no-softwrap,~5chars width")
        SelectionContainer {
            Text(
                text,
                style = TextStyle(fontSize = fontSize),
                softWrap = false,
                maxLines = 1,
                modifier = widthWodifier,
                overflow = overflow
            )
        }

        SecondTagLine(tag = "maxLines=2,~5chars width")

        SelectionContainer {
            Text(
                text,
                style = TextStyle(fontSize = fontSize),
                modifier = widthWodifier,
                maxLines = 2,
                overflow = overflow
            )
        }
    }
}

@Composable
private fun CenteredInContainerRow() {
    Row(Modifier.padding(16.dp).horizontalScroll(rememberScrollState())) {
        CenteredInContainerColumn("Abcdefgh")
        Spacer(Modifier.width(16.dp))
        CenteredInContainerColumn("ဤဩဦနိမြသ")
    }
}

@Composable
private fun CenteredInContainerColumn(text: String) {
    val fontSize = fontSize8
    val height = with(LocalDensity.current) { fontSize.toDp() } * 3
    Column(
        modifier = Modifier.background(Color.DarkGray).height(height),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            style = TextStyle(fontSize = fontSize, color = Color.White)
        )
    }
}

@Composable
private fun CenterInCircleRow() {
    Row(Modifier.padding(16.dp).horizontalScroll(rememberScrollState())) {
        CenteredInCircle("1")
        Spacer(Modifier.width(16.dp))
        CenteredInCircle("ဩ")
        Spacer(Modifier.width(16.dp))
        CenteredInCircle("y")
        Spacer(Modifier.width(16.dp))
        CenteredInCircle("Ay")
    }
}

@Composable
private fun CenteredInCircle(text: String) {
    val fontSize = fontSize8
    val size = with(LocalDensity.current) { fontSize.toDp() } * 3
    Box(
        modifier = Modifier.clip(CircleShape).background(Color.Red).size(size),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(fontSize = fontSize, color = Color.White)
        )
    }
}

@Composable
private fun MultiStyleText() {
    val fontSize = fontSize8
    Row(Modifier.padding(16.dp).horizontalScroll(rememberScrollState())) {
        val shorterTallChar = buildAnnotatedString {
            append("a")
            // half the size of original size
            // as a tall script should not be extending the height since now it is shorter
            withStyle(SpanStyle(fontSize = fontSize / 2)) {
                append("ဩ")
            }
        }
        Text(text = shorterTallChar.toString(), style = TextStyle(fontSize = fontSize))
        Spacer(Modifier.padding(16.dp))

        Text(text = shorterTallChar, style = TextStyle(fontSize = fontSize))
        Spacer(Modifier.padding(16.dp))

        val tallerTallChar = buildAnnotatedString {
            append("a")
            withStyle(SpanStyle(fontSize = fontSize * 3)) {
                append("ဩ")
            }
        }
        Text(text = tallerTallChar, style = TextStyle(fontSize = fontSize))
    }
}

@Composable
private fun InlineContent() {
    Row(Modifier.padding(16.dp).horizontalScroll(rememberScrollState())) {
        // tall char larger than inline content
        TextWithInlineContent(
            tallCharSize = 1.5.em,
            inlineContentSize = 1.em,
            placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline
        )
        Spacer(Modifier.padding(16.dp))

        // inline content larger than tall char
        TextWithInlineContent(
            tallCharSize = 1.em,
            inlineContentSize = 2.em,
            PlaceholderVerticalAlign.AboveBaseline
        )
        Spacer(Modifier.padding(16.dp))

        // inline content same as tall char
        TextWithInlineContent(
            tallCharSize = 1.em,
            inlineContentSize = 1.em,
            PlaceholderVerticalAlign.AboveBaseline
        )
    }
}

@Composable
private fun TextWithInlineContent(
    tallCharSize: TextUnit,
    inlineContentSize: TextUnit,
    placeholderVerticalAlign: PlaceholderVerticalAlign
) {
    val fontSize = fontSize10
    val myId = "inlineContent"
    val smallerShape = buildAnnotatedString {
        append("a")
        appendInlineContent(myId, " ")
        withStyle(SpanStyle(fontSize = tallCharSize)) {
            append("ဩ")
        }
    }

    val inlineContent = InlineTextContent(
        Placeholder(
            width = inlineContentSize,
            height = inlineContentSize,
            placeholderVerticalAlign = placeholderVerticalAlign
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .clip(CircleShape)
                .background(color = Color.Red)
        )
    }

    val inlineContentMap = mapOf(Pair(myId, inlineContent))

    Text(
        modifier = Modifier.background(Color.LightGray),
        text = smallerShape,
        style = TextStyle(fontSize = fontSize),
        inlineContent = inlineContentMap
    )
}
