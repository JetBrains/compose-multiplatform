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

package androidx.ui.integration.test.core

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.shift
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class SimpleRadioButton2TestCase : BaseSimpleRadioButtonTestCase() {
    @Composable
    override fun emitContent() {
        val padding = (48.dp - getInnerSize().value) / 2
        Box(
            Modifier.preferredSize(48.dp)
                .border(BorderStroke(1.dp, Color.Cyan), CircleShape)
                .background(
                    color = Color.Cyan,
                    shape = (PaddingShape(padding, CircleShape))
                )
        )
    }
}

private data class PaddingShape(val padding: Dp, val shape: Shape) : Shape {
    override fun createOutline(size: Size, density: Density): Outline {
        val twoPaddings = with(density) { (padding * 2).toPx() }
        val sizeMinusPaddings = Size(size.width - twoPaddings, size.height - twoPaddings)
        val rawResult = shape.createOutline(sizeMinusPaddings, density)
        return rawResult.offset(twoPaddings / 2)
    }
}

private fun Outline.offset(size: Float): Outline {
    val offset = Offset(size, size)
    return when (this) {
        is Outline.Rectangle -> Outline.Rectangle(rect.shift(offset))
        is Outline.Rounded -> Outline.Rounded(roundRect.shift(offset))
        is Outline.Generic -> Outline.Generic(Path().apply {
            addPath(path)
            shift(offset)
        })
    }
}
