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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.Dp

/**
 * Vector graphics object that is generated as a result of [VectorAssetBuilder]
 * It can be composed and rendered by passing it as an argument to [rememberVectorPainter]
 */
@Immutable
data class VectorAsset internal constructor(

    /**
     * Name of the Vector asset
     */
    val name: String,

    /**
     * Intrinsic width of the vector asset in [Dp]
     */
    val defaultWidth: Dp,

    /**
     * Intrinsic height of the vector asset in [Dp]
     */
    val defaultHeight: Dp,

    /**
     *  Used to define the width of the viewport space. Viewport is basically the virtual canvas
     *  where the paths are drawn on.
     */
    val viewportWidth: Float,

    /**
     * Used to define the height of the viewport space. Viewport is basically the virtual canvas
     * where the paths are drawn on.
     */
    val viewportHeight: Float,

    /**
     * Root group of the vector asset that contains all the child groups and paths
     */
    val root: VectorGroup
)

sealed class VectorNode

/**
 * Defines a group of paths or subgroups, plus transformation information.
 * The transformations are defined in the same coordinates as the viewport.
 * The transformations are applied in the order of scale, rotate then translate.
 *
 * This is constructed as part of the result of [VectorAssetBuilder] construction
 */
@Immutable
class VectorGroup internal constructor(
    /**
     * Name of the corresponding group
     */
    val name: String = DefaultGroupName,

    /**
     * Rotation of the group in degrees
     */
    val rotation: Float = DefaultRotation,

    /**
     * X coordinate of the pivot point to rotate or scale the group
     */
    val pivotX: Float = DefaultPivotX,

    /**
     * Y coordinate of the pivot point to rotate or scale the group
     */
    val pivotY: Float = DefaultPivotY,

    /**
     * Scale factor in the X-axis to apply to the group
     */
    val scaleX: Float = DefaultScaleX,

    /**
     * Scale factor in the Y-axis to apply to the group
     */
    val scaleY: Float = DefaultScaleY,

    /**
     * Translation in virtual pixels to apply along the x-axis
     */
    val translationX: Float = DefaultTranslationX,

    /**
     * Translation in virtual pixels to apply along the y-axis
     */
    val translationY: Float = DefaultTranslationY,

    /**
     * Path information used to clip the content within the group
     */
    val clipPathData: List<PathNode> = EmptyPath,

    /**
     * Child Vector nodes that are part of this group, this can contain
     * paths or other groups
     */
    private val children: List<VectorNode> = emptyList()
) : VectorNode(), Iterable<VectorNode> {

    val size: Int
        get() = children.size

    operator fun get(index: Int): VectorNode {
        return children[index]
    }

    override fun iterator(): Iterator<VectorNode> {
        return object : Iterator<VectorNode> {

            val it = children.iterator()

            override fun hasNext(): Boolean = it.hasNext()

            override fun next(): VectorNode = it.next()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is VectorGroup) return false

        if (name != other.name) return false
        if (rotation != other.rotation) return false
        if (pivotX != other.pivotX) return false
        if (pivotY != other.pivotY) return false
        if (scaleX != other.scaleX) return false
        if (scaleY != other.scaleY) return false
        if (translationX != other.translationX) return false
        if (translationY != other.translationY) return false
        if (clipPathData != other.clipPathData) return false
        if (children != other.children) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + rotation.hashCode()
        result = 31 * result + pivotX.hashCode()
        result = 31 * result + pivotY.hashCode()
        result = 31 * result + scaleX.hashCode()
        result = 31 * result + scaleY.hashCode()
        result = 31 * result + translationX.hashCode()
        result = 31 * result + translationY.hashCode()
        result = 31 * result + clipPathData.hashCode()
        result = 31 * result + children.hashCode()
        return result
    }
}

/**
 * Leaf node of a Vector graphics tree. This specifies a path shape and parameters
 * to color and style the the shape itself
 *
 * This is constructed as part of the result of [VectorAssetBuilder] construction
 */
@Immutable
class VectorPath internal constructor(
    /**
     * Name of the corresponding path
     */
    val name: String = DefaultPathName,

    /**
     * Path information to render the shape of the path
     */
    val pathData: List<PathNode>,

    /**
     * Rule to determine how the interior of the path is to be calculated
     */
    val pathFillType: PathFillType,

    /**
     *  Specifies the color or gradient used to fill the path
     */
    val fill: Brush? = null,

    /**
     * Opacity to fill the path
     */
    val fillAlpha: Float = 1.0f,

    /**
     * Specifies the color or gradient used to fill the stroke
     */
    val stroke: Brush? = null,

    /**
     * Opacity to stroke the path
     */
    val strokeAlpha: Float = 1.0f,

    /**
     * Width of the line to stroke the path
     */
    val strokeLineWidth: Float = DefaultStrokeLineWidth,

    /**
     * Specifies the linecap for a stroked path, either butt, round, or square. The default is butt.
     */
    val strokeLineCap: StrokeCap = DefaultStrokeLineCap,

    /**
     * Specifies the linejoin for a stroked path, either miter, round or bevel. The default is miter
     */
    val strokeLineJoin: StrokeJoin = DefaultStrokeLineJoin,

    /**
     * Specifies the miter limit for a stroked path, the default is 4
     */
    val strokeLineMiter: Float = DefaultStrokeLineMiter,

    /**
     * Specifies the fraction of the path to trim from the start, in the range from 0 to 1.
     * The default is 0.
     */
    val trimPathStart: Float = DefaultTrimPathStart,

    /**
     * Specifies the fraction of the path to trim from the end, in the range from 0 to 1.
     * The default is 1.
     */
    val trimPathEnd: Float = DefaultTrimPathEnd,

    /**
     * Specifies the offset of the trim region (allows showed region to include the start and end),
     * in the range from 0 to 1. The default is 0.
     */
    val trimPathOffset: Float = DefaultTrimPathOffset
) : VectorNode() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as VectorPath

        if (name != other.name) return false
        if (fill != other.fill) return false
        if (fillAlpha != other.fillAlpha) return false
        if (stroke != other.stroke) return false
        if (strokeAlpha != other.strokeAlpha) return false
        if (strokeLineWidth != other.strokeLineWidth) return false
        if (strokeLineCap != other.strokeLineCap) return false
        if (strokeLineJoin != other.strokeLineJoin) return false
        if (strokeLineMiter != other.strokeLineMiter) return false
        if (trimPathStart != other.trimPathStart) return false
        if (trimPathEnd != other.trimPathEnd) return false
        if (trimPathOffset != other.trimPathOffset) return false
        if (pathFillType != other.pathFillType) return false
        if (pathData != other.pathData) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + pathData.hashCode()
        result = 31 * result + (fill?.hashCode() ?: 0)
        result = 31 * result + fillAlpha.hashCode()
        result = 31 * result + (stroke?.hashCode() ?: 0)
        result = 31 * result + strokeAlpha.hashCode()
        result = 31 * result + strokeLineWidth.hashCode()
        result = 31 * result + strokeLineCap.hashCode()
        result = 31 * result + strokeLineJoin.hashCode()
        result = 31 * result + strokeLineMiter.hashCode()
        result = 31 * result + trimPathStart.hashCode()
        result = 31 * result + trimPathEnd.hashCode()
        result = 31 * result + trimPathOffset.hashCode()
        result = 31 * result + pathFillType.hashCode()
        return result
    }
}
