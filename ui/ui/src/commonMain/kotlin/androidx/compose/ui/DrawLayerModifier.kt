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

package androidx.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.DefaultCameraDistance
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer

// DrawLayerModifier.kt should be removed after we go through a deprecated/remove cycle
// for compose.
// This file is kept around to keep the old deprecated Modifier.drawLayer API. Once this API
// is removed this file can be deleted in favor of GraphicsLayerModifier in the
// androidx.compose.ui.graphics package

@Deprecated(
    "Use TransformOrigin in the androidx.compose.ui.graphics package instead",
    ReplaceWith(
        "androidx.compose.ui.graphics.TransformOrigin",
        "androidx.compose.ui.graphics"
    )
)
typealias TransformOrigin = androidx.compose.ui.graphics.TransformOrigin

/**
 * A [Modifier.Element] that makes content draw into a draw layer. The draw layer can be
 * invalidated separately from parents. A [drawLayer] should be used when the content
 * updates independently from anything above it to minimize the invalidated content.
 *
 * [drawLayer] can also be used to apply effects to content, such as scaling ([scaleX], [scaleY]),
 * rotation ([rotationX], [rotationY], [rotationZ]), opacity ([alpha]), shadow
 * ([shadowElevation], [shape]), and clipping ([clip], [shape]).
 *
 * If the layer parameters are backed by a [androidx.compose.runtime.State] or an animated value
 * prefer an overload with a lambda block on [GraphicsLayerScope] as reading a state inside the block
 * will only cause the layer properties update without triggering recomposition and relayout.
 *
 * @sample androidx.compose.ui.samples.ChangeOpacity
 *
 * @param scaleX see [GraphicsLayerScope.scaleX]
 * @param scaleY see [GraphicsLayerScope.scaleY]
 * @param alpha see [GraphicsLayerScope.alpha]
 * @param translationX see [GraphicsLayerScope.translationX]
 * @param translationY see [GraphicsLayerScope.translationY]
 * @param shadowElevation see [GraphicsLayerScope.shadowElevation]
 * @param rotationX see [GraphicsLayerScope.rotationX]
 * @param rotationY see [GraphicsLayerScope.rotationY]
 * @param rotationZ see [GraphicsLayerScope.rotationZ]
 * @param cameraDistance see [GraphicsLayerScope.cameraDistance]
 * @param transformOrigin see [GraphicsLayerScope.transformOrigin]
 * @param shape see [GraphicsLayerScope.shape]
 * @param clip see [GraphicsLayerScope.clip]
 */
@Suppress("DEPRECATION")
@Deprecated(
    "Use graphicsLayer instead",
    ReplaceWith(
        "graphicsLayer(scaleX, scaleY, alpha, translationX, translationY, " +
            "shadowElevation, rotationX, rotationY, rotationZ, cameraDistance, transformOrigin, " +
            "shape, clip)",
        "androidx.compose.ui"
    )
)
@Stable
fun Modifier.drawLayer(
    scaleX: Float = 1f,
    scaleY: Float = 1f,
    alpha: Float = 1f,
    translationX: Float = 0f,
    translationY: Float = 0f,
    shadowElevation: Float = 0f,
    rotationX: Float = 0f,
    rotationY: Float = 0f,
    rotationZ: Float = 0f,
    cameraDistance: Float = DefaultCameraDistance,
    transformOrigin: TransformOrigin = TransformOrigin.Center,
    shape: Shape = RectangleShape,
    clip: Boolean = false
) = graphicsLayer(
    scaleX,
    scaleY,
    alpha,
    translationX,
    translationY,
    shadowElevation,
    rotationX,
    rotationY,
    rotationZ,
    cameraDistance,
    transformOrigin,
    shape,
    clip
)