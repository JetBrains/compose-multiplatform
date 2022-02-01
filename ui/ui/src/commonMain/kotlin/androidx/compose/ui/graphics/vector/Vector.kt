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

package androidx.compose.ui.graphics.vector

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Size.Companion.Unspecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import kotlin.math.ceil

const val DefaultGroupName = ""
const val DefaultRotation = 0.0f
const val DefaultPivotX = 0.0f
const val DefaultPivotY = 0.0f
const val DefaultScaleX = 1.0f
const val DefaultScaleY = 1.0f
const val DefaultTranslationX = 0.0f
const val DefaultTranslationY = 0.0f

val EmptyPath = emptyList<PathNode>()

inline fun PathData(block: PathBuilder.() -> Unit): List<PathNode> =
    with(PathBuilder()) {
        block()
        getNodes()
    }

const val DefaultPathName = ""
const val DefaultStrokeLineWidth = 0.0f
const val DefaultStrokeLineMiter = 4.0f
const val DefaultTrimPathStart = 0.0f
const val DefaultTrimPathEnd = 1.0f
const val DefaultTrimPathOffset = 0.0f

val DefaultStrokeLineCap = StrokeCap.Butt
val DefaultStrokeLineJoin = StrokeJoin.Miter
val DefaultTintBlendMode = BlendMode.SrcIn
val DefaultTintColor = Color.Transparent
val DefaultFillType = PathFillType.NonZero

fun addPathNodes(pathStr: String?): List<PathNode> =
    if (pathStr == null) {
        EmptyPath
    } else {
        PathParser().parsePathString(pathStr).toNodes()
    }

sealed class VNode {

    /**
     * Callback invoked whenever the node in the vector tree is modified in a way that would
     * change the output of the Vector
     */
    internal open var invalidateListener: (() -> Unit)? = null

    fun invalidate() {
        invalidateListener?.invoke()
    }

    abstract fun DrawScope.draw()
}

internal class VectorComponent : VNode() {

    val root = GroupComponent().apply {
        pivotX = 0.0f
        pivotY = 0.0f
        invalidateListener = {
            doInvalidate()
        }
    }

    var name: String
        get() = root.name
        set(value) {
            root.name = value
        }

    private fun doInvalidate() {
        isDirty = true
        invalidateCallback.invoke()
    }

    private var isDirty: Boolean = true

    private val cacheDrawScope = DrawCache()

    internal var invalidateCallback = {}

    internal var intrinsicColorFilter: ColorFilter? by mutableStateOf(null)

    var viewportWidth: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                doInvalidate()
            }
        }

    var viewportHeight: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                doInvalidate()
            }
        }

    private var previousDrawSize: Size = Unspecified

    /**
     * Cached lambda used to avoid allocating the lambda on each draw invocation
     */
    private val drawVectorBlock: DrawScope.() -> Unit = {
        with(root) { draw() }
    }

    fun DrawScope.draw(alpha: Float, colorFilter: ColorFilter?) {
        val targetColorFilter = if (colorFilter != null) {
            colorFilter
        } else {
            intrinsicColorFilter
        }
        // If the content of the vector has changed, or we are drawing a different size
        // update the cached image to ensure we are scaling the vector appropriately
        if (isDirty || previousDrawSize != size) {
            root.scaleX = size.width / viewportWidth
            root.scaleY = size.height / viewportHeight
            cacheDrawScope.drawCachedImage(
                IntSize(ceil(size.width).toInt(), ceil(size.height).toInt()),
                this@draw,
                layoutDirection,
                drawVectorBlock
            )
            isDirty = false
            previousDrawSize = size
        }
        cacheDrawScope.drawInto(this, alpha, targetColorFilter)
    }

    override fun DrawScope.draw() {
        draw(1.0f, null)
    }

    override fun toString(): String {
        return buildString {
            append("Params: ")
            append("\tname: ").append(name).append("\n")
            append("\tviewportWidth: ").append(viewportWidth).append("\n")
            append("\tviewportHeight: ").append(viewportHeight).append("\n")
        }
    }
}

internal class PathComponent : VNode() {

    var name: String = DefaultPathName
        set(value) {
            field = value
            invalidate()
        }

    var fill: Brush? = null
        set(value) {
            field = value
            invalidate()
        }

    var fillAlpha: Float = 1.0f
        set(value) {
            field = value
            invalidate()
        }

    var pathData: List<PathNode> = EmptyPath
        set(value) {
            field = value
            isPathDirty = true
            invalidate()
        }

    var pathFillType: PathFillType = DefaultFillType
        set(value) {
            field = value
            renderPath.fillType = value
            invalidate()
        }

    var strokeAlpha: Float = 1.0f
        set(value) {
            field = value
            invalidate()
        }

    var strokeLineWidth: Float = DefaultStrokeLineWidth
        set(value) {
            field = value
            invalidate()
        }

    var stroke: Brush? = null
        set(value) {
            field = value
            invalidate()
        }

    var strokeLineCap: StrokeCap = DefaultStrokeLineCap
        set(value) {
            field = value
            isStrokeDirty = true
            invalidate()
        }

    var strokeLineJoin: StrokeJoin = DefaultStrokeLineJoin
        set(value) {
            field = value
            isStrokeDirty = true
            invalidate()
        }

    var strokeLineMiter: Float = DefaultStrokeLineMiter
        set(value) {
            field = value
            isStrokeDirty = true
            invalidate()
        }

    var trimPathStart: Float = DefaultTrimPathStart
        set(value) {
            if (field != value) {
                field = value
                isTrimPathDirty = true
                invalidate()
            }
        }

    var trimPathEnd: Float = DefaultTrimPathEnd
        set(value) {
            if (field != value) {
                field = value
                isTrimPathDirty = true
                invalidate()
            }
        }

    var trimPathOffset: Float = DefaultTrimPathOffset
        set(value) {
            if (field != value) {
                field = value
                isTrimPathDirty = true
                invalidate()
            }
        }

    private var isPathDirty = true
    private var isStrokeDirty = true
    private var isTrimPathDirty = true

    private var strokeStyle: Stroke? = null

    private val path = Path()

    private val renderPath = Path()

    private val pathMeasure: PathMeasure by lazy(LazyThreadSafetyMode.NONE) { PathMeasure() }

    private val parser = PathParser()

    private fun updatePath() {
        parser.clear()
        path.reset()
        parser.addPathNodes(pathData).toPath(path)
        updateRenderPath()
    }

    private fun updateRenderPath() {
        renderPath.reset()
        if (trimPathStart == DefaultTrimPathStart && trimPathEnd == DefaultTrimPathEnd) {
            renderPath.addPath(path)
        } else {
            pathMeasure.setPath(path, false)
            val length = pathMeasure.length
            val start = ((trimPathStart + trimPathOffset) % 1f) * length
            val end = ((trimPathEnd + trimPathOffset) % 1f) * length
            if (start > end) {
                pathMeasure.getSegment(start, length, renderPath, true)
                pathMeasure.getSegment(0f, end, renderPath, true)
            } else {
                pathMeasure.getSegment(start, end, renderPath, true)
            }
        }
    }

    override fun DrawScope.draw() {
        if (isPathDirty) {
            updatePath()
        } else if (isTrimPathDirty) {
            updateRenderPath()
        }
        isPathDirty = false
        isTrimPathDirty = false

        fill?.let { drawPath(renderPath, brush = it, alpha = fillAlpha) }
        stroke?.let {
            var targetStroke = strokeStyle
            if (isStrokeDirty || targetStroke == null) {
                targetStroke =
                    Stroke(strokeLineWidth, strokeLineMiter, strokeLineCap, strokeLineJoin)
                strokeStyle = targetStroke
                isStrokeDirty = false
            }
            drawPath(renderPath, brush = it, alpha = strokeAlpha, style = targetStroke)
        }
    }

    override fun toString(): String {
        return path.toString()
    }
}

internal class GroupComponent : VNode() {

    private var groupMatrix: Matrix? = null

    private val children = mutableListOf<VNode>()

    var clipPathData: List<PathNode> = EmptyPath
        set(value) {
            field = value
            isClipPathDirty = true
            invalidate()
        }

    private val willClipPath: Boolean
        get() = clipPathData.isNotEmpty()

    private var isClipPathDirty = true

    private var clipPath: Path? = null
    private var parser: PathParser? = null

    override var invalidateListener: (() -> Unit)? = null
        set(value) {
            field = value
            children.fastForEach { child ->
                child.invalidateListener = value
            }
        }

    private fun updateClipPath() {
        if (willClipPath) {
            var targetParser = parser
            if (targetParser == null) {
                targetParser = PathParser()
                parser = targetParser
            } else {
                targetParser.clear()
            }

            var targetClip = clipPath
            if (targetClip == null) {
                targetClip = Path()
                clipPath = targetClip
            } else {
                targetClip.reset()
            }

            targetParser.addPathNodes(clipPathData).toPath(targetClip)
        }
    }

    // If the name changes we should re-draw as individual nodes could
    // be modified based off of this name parameter.
    var name: String = DefaultGroupName
        set(value) {
            field = value
            invalidate()
        }

    var rotation: Float = DefaultRotation
        set(value) {
            field = value
            isMatrixDirty = true
            invalidate()
        }

    var pivotX: Float = DefaultPivotX
        set(value) {
            field = value
            isMatrixDirty = true
            invalidate()
        }

    var pivotY: Float = DefaultPivotY
        set(value) {
            field = value
            isMatrixDirty = true
            invalidate()
        }

    var scaleX: Float = DefaultScaleX
        set(value) {
            field = value
            isMatrixDirty = true
            invalidate()
        }

    var scaleY: Float = DefaultScaleY
        set(value) {
            field = value
            isMatrixDirty = true
            invalidate()
        }

    var translationX: Float = DefaultTranslationX
        set(value) {
            field = value
            isMatrixDirty = true
            invalidate()
        }

    var translationY: Float = DefaultTranslationY
        set(value) {
            field = value
            isMatrixDirty = true
            invalidate()
        }

    private var isMatrixDirty = true

    private fun updateMatrix() {
        val matrix: Matrix
        val target = groupMatrix
        if (target == null) {
            matrix = Matrix()
            groupMatrix = matrix
        } else {
            matrix = target
            matrix.reset()
        }
        // M = T(translationX + pivotX, translationY + pivotY) *
        //     R(rotation) * S(scaleX, scaleY) *
        //     T(-pivotX, -pivotY)
        matrix.translate(translationX + pivotX, translationY + pivotY)
        matrix.rotateZ(degrees = rotation)
        matrix.scale(scaleX, scaleY, 1f)
        matrix.translate(-pivotX, -pivotY)
    }

    fun insertAt(index: Int, instance: VNode) {
        if (index < numChildren) {
            children[index] = instance
        } else {
            children.add(instance)
        }
        instance.invalidateListener = invalidateListener
        invalidate()
    }

    fun move(from: Int, to: Int, count: Int) {
        if (from > to) {
            var current = to
            repeat(count) {
                val node = children[from]
                children.removeAt(from)
                children.add(current, node)
                current++
            }
        } else {
            repeat(count) {
                val node = children[from]
                children.removeAt(from)
                children.add(to - 1, node)
            }
        }
        invalidate()
    }

    fun remove(index: Int, count: Int) {
        repeat(count) {
            if (index < children.size) {
                children[index].invalidateListener = null
                children.removeAt(index)
            }
        }
        invalidate()
    }

    override fun DrawScope.draw() {
        if (isMatrixDirty) {
            updateMatrix()
            isMatrixDirty = false
        }

        if (isClipPathDirty) {
            updateClipPath()
            isClipPathDirty = false
        }

        withTransform({
            groupMatrix?.let { transform(it) }
            val targetClip = clipPath
            if (willClipPath && targetClip != null) {
                clipPath(targetClip)
            }
        }) {
            children.fastForEach { node ->
                with(node) {
                    this@draw.draw()
                }
            }
        }
    }

    val numChildren: Int
        get() = children.size

    override fun toString(): String {
        val sb = StringBuilder().append("VGroup: ").append(name)
        children.fastForEach { node ->
            sb.append("\t").append(node.toString()).append("\n")
        }
        return sb.toString()
    }
}
