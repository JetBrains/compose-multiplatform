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

package androidx.compose.ui.test.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.util.ClickableTestBox.defaultColor
import androidx.compose.ui.test.util.ClickableTestBox.defaultSize
import androidx.compose.ui.test.util.ClickableTestBox.defaultTag

object ClickableTestBox {
    const val defaultSize = 100.0f
    val defaultColor = Color.Yellow
    const val defaultTag = "ClickableTestBox"
}

@Composable
fun ClickableTestBox(
    modifier: Modifier = Modifier,
    width: Float = defaultSize,
    height: Float = defaultSize,
    color: Color = defaultColor,
    tag: String = defaultTag
) {
    with(LocalDensity.current) {
        Box(
            modifier = modifier.testTag(tag)
                .requiredSize(width.toDp(), height.toDp())
                .background(color)
        )
    }
}
