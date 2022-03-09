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

package androidx.compose.ui.platform

import android.graphics.Outline
import android.graphics.RenderNode
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.CanvasHolder
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect

/**
 * RenderNode on Q+ devices, where it is officially supported.
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal class RenderNodeApi29(val ownerView: AndroidComposeView) : DeviceRenderNode {
    private val renderNode = RenderNode("Compose")

    private var internalRenderEffect: RenderEffect? = null

    override val uniqueId: Long get() = renderNode.uniqueId

    override val left: Int get() = renderNode.left
    override val top: Int get() = renderNode.top
    override val right: Int get() = renderNode.right
    override val bottom: Int get() = renderNode.bottom
    override val width: Int get() = renderNode.width
    override val height: Int get() = renderNode.height

    override var scaleX: Float
        get() = renderNode.scaleX
        set(value) {
            renderNode.scaleX = value
        }

    override var scaleY: Float
        get() = renderNode.scaleY
        set(value) {
            renderNode.scaleY = value
        }

    override var translationX: Float
        get() = renderNode.translationX
        set(value) {
            renderNode.translationX = value
        }

    override var translationY: Float
        get() = renderNode.translationY
        set(value) {
            renderNode.translationY = value
        }

    override var elevation: Float
        get() = renderNode.elevation
        set(value) {
            renderNode.elevation = value
        }

    override var ambientShadowColor: Int
        get() = renderNode.ambientShadowColor
        set(value) {
            renderNode.ambientShadowColor = value
        }

    override var spotShadowColor: Int
        get() = renderNode.spotShadowColor
        set(value) {
            renderNode.spotShadowColor = value
        }

    override var rotationZ: Float
        get() = renderNode.rotationZ
        set(value) {
            renderNode.rotationZ = value
        }

    override var rotationX: Float
        get() = renderNode.rotationX
        set(value) {
            renderNode.rotationX = value
        }

    override var rotationY: Float
        get() = renderNode.rotationY
        set(value) {
            renderNode.rotationY = value
        }

    override var cameraDistance: Float
        get() = renderNode.cameraDistance
        set(value) {
            renderNode.cameraDistance = value
        }

    override var pivotX: Float
        get() = renderNode.pivotX
        set(value) {
            renderNode.pivotX = value
        }

    override var pivotY: Float
        get() = renderNode.pivotY
        set(value) {
            renderNode.pivotY = value
        }

    override var clipToOutline: Boolean
        get() = renderNode.clipToOutline
        set(value) {
            renderNode.clipToOutline = value
        }

    override var clipToBounds: Boolean
        get() = renderNode.clipToBounds
        set(value) {
            renderNode.clipToBounds = value
        }

    override var alpha: Float
        get() = renderNode.alpha
        set(value) {
            renderNode.alpha = value
        }

    override var renderEffect: RenderEffect?
        get() = internalRenderEffect
        set(value) {
            internalRenderEffect = value
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                RenderNodeApi29VerificationHelper.setRenderEffect(renderNode, value)
            }
        }

    override val hasDisplayList: Boolean
        get() = renderNode.hasDisplayList()

    override fun setOutline(outline: Outline?) {
        renderNode.setOutline(outline)
    }

    override fun setPosition(left: Int, top: Int, right: Int, bottom: Int): Boolean {
        return renderNode.setPosition(left, top, right, bottom)
    }

    override fun offsetLeftAndRight(offset: Int) {
        renderNode.offsetLeftAndRight(offset)
    }

    override fun offsetTopAndBottom(offset: Int) {
        renderNode.offsetTopAndBottom(offset)
    }

    override fun record(
        canvasHolder: CanvasHolder,
        clipPath: Path?,
        drawBlock: (Canvas) -> Unit
    ) {
        canvasHolder.drawInto(renderNode.beginRecording()) {
            if (clipPath != null) {
                save()
                clipPath(clipPath)
            }
            drawBlock(this)
            if (clipPath != null) {
                restore()
            }
        }
        renderNode.endRecording()
    }

    override fun getMatrix(matrix: android.graphics.Matrix) {
        return renderNode.getMatrix(matrix)
    }

    override fun getInverseMatrix(matrix: android.graphics.Matrix) {
        return renderNode.getInverseMatrix(matrix)
    }

    override fun drawInto(canvas: android.graphics.Canvas) {
        canvas.drawRenderNode(renderNode)
    }

    override fun setHasOverlappingRendering(hasOverlappingRendering: Boolean): Boolean =
        renderNode.setHasOverlappingRendering(hasOverlappingRendering)

    override fun dumpRenderNodeData(): DeviceRenderNodeData =
        DeviceRenderNodeData(
            uniqueId = renderNode.uniqueId,
            left = renderNode.left,
            top = renderNode.top,
            right = renderNode.right,
            bottom = renderNode.bottom,
            width = renderNode.width,
            height = renderNode.height,
            scaleX = renderNode.scaleX,
            scaleY = renderNode.scaleY,
            translationX = renderNode.translationX,
            translationY = renderNode.translationY,
            elevation = renderNode.elevation,
            ambientShadowColor = renderNode.ambientShadowColor,
            spotShadowColor = renderNode.spotShadowColor,
            rotationZ = renderNode.rotationZ,
            rotationX = renderNode.rotationX,
            rotationY = renderNode.rotationY,
            cameraDistance = renderNode.cameraDistance,
            pivotX = renderNode.pivotX,
            pivotY = renderNode.pivotY,
            clipToOutline = renderNode.clipToOutline,
            clipToBounds = renderNode.clipToBounds,
            alpha = renderNode.alpha,
            renderEffect = internalRenderEffect
        )

    override fun discardDisplayList() {
        renderNode.discardDisplayList()
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private object RenderNodeApi29VerificationHelper {

    @androidx.annotation.DoNotInline
    fun setRenderEffect(renderNode: RenderNode, target: RenderEffect?) {
        renderNode.setRenderEffect(target?.asAndroidRenderEffect())
    }
}
