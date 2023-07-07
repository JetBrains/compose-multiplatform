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

package androidx.compose.ui.interop

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntRect
import platform.CoreGraphics.CGRectMake

internal fun Rect.toCGRect() =
    CGRectMake(left.toDouble(), top.toDouble(), size.width.toDouble(), size.height.toDouble())

internal operator fun IntRect.div(divider: Float) =
    Rect(left / divider, top / divider, right / divider, bottom / divider)
