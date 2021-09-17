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

package androidx.compose.ui.graphics

import org.jetbrains.skia.VertexMode as SkVertexMode

fun VertexMode.toDesktopVertexMode(): SkVertexMode = when (this) {
    VertexMode.Triangles -> SkVertexMode.TRIANGLES
    VertexMode.TriangleStrip -> SkVertexMode.TRIANGLE_STRIP
    VertexMode.TriangleFan -> SkVertexMode.TRIANGLE_FAN
    else -> SkVertexMode.TRIANGLES
}
