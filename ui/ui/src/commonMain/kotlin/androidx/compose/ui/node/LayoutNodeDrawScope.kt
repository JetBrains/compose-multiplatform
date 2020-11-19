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

package androidx.compose.ui.node

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

/**
 * [ContentDrawScope] implementation that extracts density and layout direction information
 * from the given LayoutNodeWrapper
 */
internal class LayoutNodeDrawScope(
    private val canvasDrawScope: CanvasDrawScope = CanvasDrawScope()
) : DrawScope by canvasDrawScope, ContentDrawScope {

    // NOTE, currently a single ComponentDrawScope is shared across composables
    // which done to allocate a single set of Paint objects and re-use them across
    // draw calls for all composables.
    // As a result there could be thread safety concerns here for multi-threaded drawing
    // scenarios, generally a single ComponentDrawScope should be shared for a particular thread
    private var wrapped: LayoutNodeWrapper? = null

    override fun drawContent() {
        drawIntoCanvas { canvas -> wrapped?.draw(canvas) }
    }

    internal inline fun draw(
        canvas: Canvas,
        size: Size,
        LayoutNodeWrapper: LayoutNodeWrapper,
        block: DrawScope.() -> Unit
    ) {
        val previousWrapper = wrapped
        wrapped = LayoutNodeWrapper
        canvasDrawScope.draw(
            LayoutNodeWrapper.measureScope,
            LayoutNodeWrapper.measureScope.layoutDirection,
            canvas,
            size,
            block
        )
        wrapped = previousWrapper
    }
}
