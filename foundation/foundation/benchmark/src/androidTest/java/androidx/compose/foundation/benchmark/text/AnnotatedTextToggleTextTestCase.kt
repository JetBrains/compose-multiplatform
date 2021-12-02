/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.benchmark.text

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.benchmark.RandomTextGenerator
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

class AnnotatedTextToggleTextTestCase(
    private val textGenerator: RandomTextGenerator,
    private val textLength: Int,
    private val textNumber: Int,
    private val width: Dp,
    private val fontSize: TextUnit
) : ComposeTestCase, ToggleableTestCase {

    private val texts = List(textNumber) {
        mutableStateOf(AnnotatedString(textGenerator.nextParagraph(length = textLength)))
    }

    @Composable
    override fun Content() {
        Column(
            modifier = Modifier.wrapContentSize(Alignment.Center).width(width)
                .verticalScroll(rememberScrollState())
        ) {
            for (text in texts) {
                Text(text = text.value, color = Color.Black, fontSize = fontSize)
            }
        }
    }

    override fun toggleState() {
        texts.forEach {
            it.value = AnnotatedString(textGenerator.nextParagraph(length = textLength))
        }
    }
}