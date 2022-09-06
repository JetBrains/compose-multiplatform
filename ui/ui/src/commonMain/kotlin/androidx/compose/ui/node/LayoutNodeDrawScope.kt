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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.toSize

/**
 * [ContentDrawScope] implementation that extracts density and layout direction information
 * from the given NodeCoordinator
 */
@OptIn(ExperimentalComposeUiApi::class)
internal class LayoutNodeDrawScope(
    private val canvasDrawScope: CanvasDrawScope = CanvasDrawScope()
) : DrawScope by canvasDrawScope, ContentDrawScope {

    // NOTE, currently a single ComponentDrawScope is shared across composables
    // which done to allocate a single set of Paint objects and re-use them across
    // draw calls for all composables.
    // As a result there could be thread safety concerns here for multi-threaded drawing
    // scenarios, generally a single ComponentDrawScope should be shared for a particular thread
    private var drawNode: DrawModifierNode? = null

    override fun drawContent() {
        drawIntoCanvas { canvas ->
            val drawNode = drawNode!!
            val nextDrawNode = drawNode.nextDrawNode()
            // NOTE(lmr): we only run performDraw directly on the node if the node's coordinator
            // is our own. This seems to work, but we should think about a cleaner way to dispatch
            // the draw pass as with the new modifier.node / coordinator structure this feels
            // somewhat error prone.
            if (nextDrawNode != null) {
                nextDrawNode.performDraw(canvas)
            } else {
                // TODO(lmr): this is needed in the case that the drawnode is also a measure node,
                //  but we should think about the right ways to handle this as this is very error
                //  prone i think
                val coordinator = drawNode.requireCoordinator(Nodes.Draw)
                val nextCoordinator = if (coordinator.tail === drawNode)
                    coordinator.wrapped!!
                else
                    coordinator
                nextCoordinator.performDraw(canvas)
            }
        }
    }

    // This is not thread safe
    fun DrawModifierNode.performDraw(canvas: Canvas) {
        val coordinator = requireCoordinator(Nodes.Draw)
        val size = coordinator.size.toSize()
        val drawScope = coordinator.layoutNode.mDrawScope
        drawScope.draw(canvas, size, coordinator, this)
    }

    internal fun draw(
        canvas: Canvas,
        size: Size,
        coordinator: NodeCoordinator,
        drawNode: DrawModifierNode,
    ) {
        val previousDrawNode = this.drawNode
        this.drawNode = drawNode
        canvasDrawScope.draw(
            coordinator,
            coordinator.layoutDirection,
            canvas,
            size
        ) {
            with(drawNode) {
                this@LayoutNodeDrawScope.draw()
            }
        }
        this.drawNode = previousDrawNode
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun DelegatableNode.nextDrawNode(): DrawModifierNode? {
    val drawMask = Nodes.Draw.mask
    val measureMask = Nodes.Layout.mask
    val child = node.child ?: return null
    if (child.aggregateChildKindSet and drawMask == 0) return null
    var next: Modifier.Node? = child
    while (next != null) {
        if (next.kindSet and measureMask != 0) return null
        if (next.kindSet and drawMask != 0) {
            return next as DrawModifierNode
        }
        next = next.child
    }
    return null
}