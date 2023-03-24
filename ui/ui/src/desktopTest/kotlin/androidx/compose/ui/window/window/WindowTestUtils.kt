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

package androidx.compose.ui.window.window

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import java.awt.GraphicsConfiguration
import java.awt.Insets


/**
 * Converts AWT [Insets] to a [DpSize] object with the sums on each axis.
 */
internal fun Insets.toSize(): DpSize {
    // The AWT coordinates are scaled, so they're Dp
    return DpSize(
        width = (left + right).dp,
        height = (top + bottom).dp
    )
}

/**
 * Returns the size of the screen, as a [DpSize] object.
 */
internal fun GraphicsConfiguration.screenSize(): DpSize {
    return bounds.let {
        // The AWT coordinates are scaled, so they're Dp
        DpSize(it.width.dp, it.height.dp)
    }
}
