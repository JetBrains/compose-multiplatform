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

package androidx.compose.ui.text.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalTextApi::class)
@Sampled
@Composable
fun DrawTextLayoutResultSample() {
    val textMeasurer = rememberTextMeasurer()
    var textLayoutResult by remember {
        mutableStateOf<TextLayoutResult?>(null)
    }

    Canvas(
        Modifier.fillMaxSize()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                // TextLayout can be done any time prior to its use in draw, including in a
                // background thread.
                // In this sample, text layout is done in compose layout. This way the layout call
                // can be restarted when async font loading completes due to the fact that
                // `.measure` call is executed in `.layout`.
                textLayoutResult = textMeasurer.measure(
                    text = AnnotatedString("Hello ".repeat(2)),
                    style = TextStyle(fontSize = 35.sp)
                )
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(0, 0)
                }
            }) {
        // This happens during draw phase.
        textLayoutResult?.let { drawText(it) }
    }
}
