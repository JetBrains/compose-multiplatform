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
package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Rect

/**
 * Creates a new instance of [android.graphics.Rect] with the same bounds
 * specified in the given [Rect]
 */
fun Rect.toAndroidRect(): android.graphics.Rect {
    return android.graphics.Rect(
        left.toInt(),
        top.toInt(),
        right.toInt(),
        bottom.toInt()
    )
}

/**
 * Creates a new instance of [android.graphics.RectF] with the same bounds
 * specified in the given [Rect]
 */
fun Rect.toAndroidRectF(): android.graphics.RectF {
    return android.graphics.RectF(
        left,
        top,
        right,
        bottom
    )
}

/**
 * Creates a new instance of [androidx.compose.ui.geometry.Rect] with the same bounds
 * specified in the given [android.graphics.Rect]
 */
fun android.graphics.Rect.toComposeRect(): androidx.compose.ui.geometry.Rect =
    androidx.compose.ui.geometry.Rect(
        this.left.toFloat(),
        this.top.toFloat(),
        this.right.toFloat(),
        this.bottom.toFloat()
    )