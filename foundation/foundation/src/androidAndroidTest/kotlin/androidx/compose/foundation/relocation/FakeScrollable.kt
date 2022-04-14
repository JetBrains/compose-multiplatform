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

package androidx.compose.foundation.relocation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * Returns a [bringIntoViewResponder] modifier that implements [BringIntoViewResponder] by
 * offsetting the local rect by [parentOffset] and handling the request by calling
 * [onBringIntoView]. Note that [onBringIntoView] will not be called if [parentOffset] is zero,
 * since that means the scrollable doesn't actually need to scroll anything to satisfy the
 * request.
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.fakeScrollable(
    parentOffset: Offset = Offset.Zero,
    onBringIntoView: suspend (Rect) -> Unit
): Modifier = bringIntoViewResponder(
    object : BringIntoViewResponder {
        override fun calculateRectForParent(localRect: Rect): Rect =
            localRect.translate(parentOffset)

        override suspend fun bringChildIntoView(localRect: Rect) {
            onBringIntoView(localRect)
        }
    })
    .wrapContentSize(align = Alignment.TopStart, unbounded = true)