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

package androidx.compose.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * <a href="https://material.io/components/cards" class="external" target="_blank">Material Design card</a>.
 *
 * Cards contain content and actions about a single subject.
 *
 * ![Cards image](https://developer.android.com/images/reference/androidx/compose/material/cards.png)
 *
 * @sample androidx.compose.material.samples.CardSample
 *
 * @param modifier Modifier to be applied to the layout of the card.
 * @param shape Defines the surface's shape as well its shadow. A shadow is only
 *  displayed if the [elevation] is greater than zero.
 * @param backgroundColor The background color.
 * @param contentColor The preferred content color provided by this Surface to its children.
 * Defaults to either the matching content color for [backgroundColor], or if [backgroundColor]
 * is not a color from the theme, this will keep the same value set above this Surface.
 * @param border Optional border to draw on top of the card
 * @param elevation The z-coordinate at which to place this surface. This controls
 *  the size of the shadow below the surface.
 */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    border: BorderStroke? = null,
    elevation: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = backgroundColor,
        contentColor = contentColor,
        elevation = elevation,
        border = border,
        content = content
    )
}
