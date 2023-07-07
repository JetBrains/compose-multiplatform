/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

/**
 * The benchmark test case for [Text] with ellipsis.
 */
class TextWithEllipsisTestCase(
    private val texts: List<String>,
    private val width: Dp,
    private val fontSize: TextUnit
) : LayeredComposeTestCase(), ToggleableTestCase {

    private val align = mutableStateOf(TextAlign.Left)

    @Composable
    override fun MeasuredContent() {
        val height = with(LocalDensity.current) { (fontSize * 3.5).toDp() }
        for (text in texts) {
            Text(
                text = text,
                textAlign = align.value,
                fontSize = fontSize,
                overflow = TextOverflow.Ellipsis,
                softWrap = true,
                modifier = Modifier.heightIn(max = height)
            )
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        Column(modifier = Modifier.width(width)) {
            content()
        }
    }

    override fun toggleState() {
        align.value = if (align.value == TextAlign.Left) TextAlign.Right else TextAlign.Left
    }
}