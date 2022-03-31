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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.Dp

/**
 * Vector graphics object that is generated as a result of [ImageVector.Builder]
 * It can be composed and rendered by passing it as an argument to [rememberVectorPainter]
 */
@Immutable
class ImageVector internal constructor(

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
    val root: VectorGroup,

    /**
     * Optional tint color to be applied to the vector graphic
     */
    val tintColor: Color,

    /**
     * Blend mode used to apply [tintColor]
     */
    val tintBlendMode: BlendMode,

    /**
     * Determines if the vector asset should automatically be mirrored for right to left locales
     */
    val autoMirror: Boolean
) {
    /**
     * Builder used to construct a Vector graphic tree.
     * This is useful for caching the result of expensive operations used to construct
     * a vector graphic for compose.
     * For example, the vector graphic could be serialized and downloaded from a server and represented
     * internally in a ImageVector before it is composed through [rememberVectorPainter]
     * The generated ImageVector is recommended to be memoized across composition calls to avoid
     * doing redundant work
     */
    @Suppress("MissingGetterMatchingBuilder")
    class Builder(

        /**
         * Name of the vector asset
         */
        private val name: String = DefaultGroupName,

        /**
         * Intrinsic width of the Vector in [Dp]
         */
        private val defaultWidth: Dp,

        /**
         * Intrinsic height of the Vector in [Dp]
         */
        private val defaultHeight: Dp,

        /**
         *  Used to define the width of the viewport space. Viewport is basically the virtual canvas
         *  where the paths are drawn on.
         */
        private val viewportWidth: Float,

        /**
         * Used to define the height of the viewport space. Viewport is basically the virtual canvas
         * where the paths are drawn on.
         */
        private val viewportHeight: Float,

        /**
         * Optional color used to tint the entire vector image
         */
        private val tintColor: Color = Color.Unspecified,

        /**
         * Blend mode used to apply the tint color
         */
        private val tintBlendMode: BlendMode = BlendMode.SrcIn,

        /**
         * Determines if the vector asset should automatically be mirrored for right to left locales
         */
        private val autoMirror: Boolean = false
    ) {

        // Secondary constructor to maintain API compatibility that defaults autoMirror to false
        @Deprecated(
            "Replace with ImageVector.Builder that consumes an optional auto " +
                "mirror parameter",
            replaceWith = ReplaceWith(
                "Builder(name, defaultWidth, defaultHeight, viewportWidth, " +
                    "viewportHeight, tintColor, tintBlendMode, false)",
                "androidx.compose.ui.graphics.vector"
            ),
            DeprecationLevel.HIDDEN
        )
        constructor(
            /**
             * Name of the vector asset
             */
            name: String = DefaultGroupName,

            /**
             * Intrinsic width of the Vector in [Dp]
             */
            defaultWidth: Dp,

            /**
             * Intrinsic height of the Vector in [Dp]
             */
            defaultHeight: Dp,

            /**
             *  Used to define the width of the viewport space. Viewport is basically the virtual
             *  canvas where the paths are drawn on.
             */
            viewportWidth: Float,

            /**
             * Used to define the height of the viewport space. Viewport is basically the virtual canvas
             * where the paths are drawn on.
             */
            viewportHeight: Float,

            /**
             * Optional color used to tint the entire vector image
             */
            tintColor: Color = Color.Unspecified,

            /**
             * Blend mode used to apply the tint color
             */
            tintBlendMode: BlendMode = BlendMode.SrcIn
        ) : this(
            name,
            defaultWidth,
            defaultHeight,
            viewportWidth,
            viewportHeight,
            tintColor,
            tintBlendMode,
            false
        )

        private val nodes = Stack<GroupParams>()

        private var root = GroupParams()
        private var isConsumed = false

        private val currentGroup: GroupParams
            get() = nodes.peek()

        init {
            nodes.push(root)
        }

        /**
         * Create a new group and push it to the front of the stack of ImageVector nodes
         *
         * @param name the name of the group
         * @param rotate the rotation of the group in degrees
         * @param pivotX the x coordinate of the pivot point to rotate or scale the group
         * @param pivotY the y coordinate of the pivot point to rotate or scale the group
         * @param scaleX the scale factor in the X-axis to apply to the group
         * @param scaleY the scale factor in the Y-axis to apply to the group
         * @param translationX the translation in virtual pixels to apply along the x-axis
         * @param translationY the translation in virtual pixels to apply along the y-axis
         * @param clipPathData the path information used to clip the content within the group
         *
         * @return This ImageVector.Builder instance as a convenience for chaining calls
         */
        @Suppress("MissingGetterMatchingBuilder")
        fun addGroup(
            name: String = DefaultGroupName,
            rotate: Float = DefaultRotation,
            pivotX: Float = DefaultPivotX,
            pivotY: Float = DefaultPivotY,
            scaleX: Float = DefaultScaleX,
            scaleY: Float = DefaultScaleY,
            translationX: Float = DefaultTranslationX,
            translationY: Float = DefaultTranslationY,
            clipPathData: List<PathNode> = EmptyPath
        ): Builder {
            ensureNotConsumed()
            val group = GroupParams(
                name,
                rotate,
                pivotX,
                pivotY,
                scaleX,
                scaleY,
                translationX,
                translationY,
                clipPathData
            )
            nodes.push(group)
            return this
        }

        /**
         * Pops the topmost VectorGroup from this ImageVector.Builder. This is used to indicate
         * that no additional ImageVector nodes will be added to the current VectorGroup
         * @return This ImageVector.Builder instance as a convenience for chaining calls
         */
        fun clearGroup(): Builder {
            ensureNotConsumed()
            val popped = nodes.pop()
            currentGroup.children.add(popped.asVectorGroup())
            return this
        }

        /**
         * Add a path to the ImageVector graphic. This represents a leaf node in the ImageVector graphics
         * tree structure
         *
         * @param pathData path information to render the shape of the path
         * @param pathFillType rule to determine how the interior of the path is to be calculated
         * @param name the name of the path
         * @param fill specifies the [Brush] used to fill the path
         * @param fillAlpha the alpha to fill the path
         * @param stroke specifies the [Brush] used to fill the stroke
         * @param strokeAlpha the alpha to stroke the path
         * @param strokeLineWidth the width of the line to stroke the path
         * @param strokeLineCap specifies the linecap for a stroked path
         * @param strokeLineJoin specifies the linejoin for a stroked path
         * @param strokeLineMiter specifies the miter limit for a stroked path
         * @param trimPathStart specifies the fraction of the path to trim from the start in the
         * range from 0 to 1. Values outside the range will wrap around the length of the path.
         * Default is 0.
         * @param trimPathStart specifies the fraction of the path to trim from the end in the
         * range from 0 to 1. Values outside the range will wrap around the length of the path.
         * Default is 1.
         * @param trimPathOffset specifies the fraction to shift the path trim region in the range
         * from 0 to 1. Values outside the range will wrap around the length of the path. Default is 0.
         *
         * @return This ImageVector.Builder instance as a convenience for chaining calls
         */
        @Suppress("MissingGetterMatchingBuilder")
        fun addPath(
            pathData: List<PathNode>,
            pathFillType: PathFillType = DefaultFillType,
            name: String = DefaultPathName,
            fill: Brush? = null,
            fillAlpha: Float = 1.0f,
            stroke: Brush? = null,
            strokeAlpha: Float = 1.0f,
            strokeLineWidth: Float = DefaultStrokeLineWidth,
            strokeLineCap: StrokeCap = DefaultStrokeLineCap,
            strokeLineJoin: StrokeJoin = DefaultStrokeLineJoin,
            strokeLineMiter: Float = DefaultStrokeLineMiter,
            trimPathStart: Float = DefaultTrimPathStart,
            trimPathEnd: Float = DefaultTrimPathEnd,
            trimPathOffset: Float = DefaultTrimPathOffset
        ): Builder {
            ensureNotConsumed()
            currentGroup.children.add(
                VectorPath(
                    name,
                    pathData,
                    pathFillType,
                    fill,
                    fillAlpha,
                    stroke,
                    strokeAlpha,
                    strokeLineWidth,
                    strokeLineCap,
                    strokeLineJoin,
                    strokeLineMiter,
                    trimPathStart,
                    trimPathEnd,
                    trimPathOffset
                )
            )
            return this
        }

        /**
         * Construct a ImageVector. This concludes the creation process of a ImageVector graphic
         * This builder cannot be re-used to create additional ImageVector instances
         * @return The newly created ImageVector instance
         */
        fun build(): ImageVector {
            ensureNotConsumed()
            // pop all groups except for the root
            while (nodes.size > 1) {
                clearGroup()
            }

            val vectorImage = ImageVector(
                name,
                defaultWidth,
                defaultHeight,
                viewportWidth,
                viewportHeight,
                root.asVectorGroup(),
                tintColor,
                tintBlendMode,
                autoMirror
            )

            isConsumed = true

            return vectorImage
        }

        /**
         * Throws IllegalStateException if the ImageVector.Builder has already been consumed
         */
        private fun ensureNotConsumed() {
            check(!isConsumed) {
                "ImageVector.Builder is single use, create a new instance " +
                    "to create a new ImageVector"
            }
        }

        /**
         * Helper method to create an immutable VectorGroup object
         * from an set of GroupParams which represent a group
         * that is in the middle of being constructed
         */
        private fun GroupParams.asVectorGroup(): VectorGroup =
            VectorGroup(
                name,
                rotate,
                pivotX,
                pivotY,
                scaleX,
                scaleY,
                translationX,
                translationY,
                clipPathData,
                children
            )

        /**
         * Internal helper class to help assist with in progress creation of
         * a vector group before creating the immutable result
         */
        private class GroupParams(
            var name: String = DefaultGroupName,
            var rotate: Float = DefaultRotation,
            var pivotX: Float = DefaultPivotX,
            var pivotY: Float = DefaultPivotY,
            var scaleX: Float = DefaultScaleX,
            var scaleY: Float = DefaultScaleY,
            var translationX: Float = DefaultTranslationX,
            var translationY: Float = DefaultTranslationY,
            var clipPathData: List<PathNode> = EmptyPath,
            var children: MutableList<VectorNode> = mutableListOf()
        )
    }

    /**
     * Provide an empty companion object to hang platform-specific companion extensions onto.
     */
    companion object { } // ktlint-disable no-empty-class-body

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImageVector) return false

        if (name != other.name) return false
        if (defaultWidth != other.defaultWidth) return false
        if (defaultHeight != other.defaultHeight) return false
        if (viewportWidth != other.viewportWidth) return false
        if (viewportHeight != other.viewportHeight) return false
        if (root != other.root) return false
        if (tintColor != other.tintColor) return false
        if (tintBlendMode != other.tintBlendMode) return false
        if (autoMirror != other.autoMirror) return false
        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + defaultWidth.hashCode()
        result = 31 * result + defaultHeight.hashCode()
        result = 31 * result + viewportWidth.hashCode()
        result = 31 * result + viewportHeight.hashCode()
        result = 31 * result + root.hashCode()
        result = 31 * result + tintColor.hashCode()
        result = 31 * result + tintBlendMode.hashCode()
        result = 31 * result + autoMirror.hashCode()
        return result
    }
}

sealed class VectorNode

/**
 * Defines a group of paths or subgroups, plus transformation information.
 * The transformations are defined in the same coordinates as the viewport.
 * The transformations are applied in the order of scale, rotate then translate.
 *
 * This is constructed as part of the result of [ImageVector.Builder] construction
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
 * to color and style the shape itself
 *
 * This is constructed as part of the result of [ImageVector.Builder] construction
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

/**
 * DSL extension for adding a [VectorPath] to [this].
 *
 * See [ImageVector.Builder.addPath] for the corresponding builder function.
 *
 * @param name the name for this path
 * @param fill specifies the [Brush] used to fill the path
 * @param fillAlpha the alpha to fill the path
 * @param stroke specifies the [Brush] used to fill the stroke
 * @param strokeAlpha the alpha to stroke the path
 * @param strokeLineWidth the width of the line to stroke the path
 * @param strokeLineCap specifies the linecap for a stroked path
 * @param strokeLineJoin specifies the linejoin for a stroked path
 * @param strokeLineMiter specifies the miter limit for a stroked path
 * @param pathBuilder [PathBuilder] lambda for adding [PathNode]s to this path.
 */
inline fun ImageVector.Builder.path(
    name: String = DefaultPathName,
    fill: Brush? = null,
    fillAlpha: Float = 1.0f,
    stroke: Brush? = null,
    strokeAlpha: Float = 1.0f,
    strokeLineWidth: Float = DefaultStrokeLineWidth,
    strokeLineCap: StrokeCap = DefaultStrokeLineCap,
    strokeLineJoin: StrokeJoin = DefaultStrokeLineJoin,
    strokeLineMiter: Float = DefaultStrokeLineMiter,
    pathFillType: PathFillType = DefaultFillType,
    pathBuilder: PathBuilder.() -> Unit
) = addPath(
    PathData(pathBuilder),
    pathFillType,
    name,
    fill,
    fillAlpha,
    stroke,
    strokeAlpha,
    strokeLineWidth,
    strokeLineCap,
    strokeLineJoin,
    strokeLineMiter
)

/**
 * DSL extension for adding a [VectorGroup] to [this].
 *
 * See [ImageVector.Builder.pushGroup] for the corresponding builder function.
 *
 * @param name the name of the group
 * @param rotate the rotation of the group in degrees
 * @param pivotX the x coordinate of the pivot point to rotate or scale the group
 * @param pivotY the y coordinate of the pivot point to rotate or scale the group
 * @param scaleX the scale factor in the X-axis to apply to the group
 * @param scaleY the scale factor in the Y-axis to apply to the group
 * @param translationX the translation in virtual pixels to apply along the x-axis
 * @param translationY the translation in virtual pixels to apply along the y-axis
 * @param clipPathData the path information used to clip the content within the group
 * @param block builder lambda to add children to this group
 */
inline fun ImageVector.Builder.group(
    name: String = DefaultGroupName,
    rotate: Float = DefaultRotation,
    pivotX: Float = DefaultPivotX,
    pivotY: Float = DefaultPivotY,
    scaleX: Float = DefaultScaleX,
    scaleY: Float = DefaultScaleY,
    translationX: Float = DefaultTranslationX,
    translationY: Float = DefaultTranslationY,
    clipPathData: List<PathNode> = EmptyPath,
    block: ImageVector.Builder.() -> Unit
) = apply {
    addGroup(
        name,
        rotate,
        pivotX,
        pivotY,
        scaleX,
        scaleY,
        translationX,
        translationY,
        clipPathData
    )
    block()
    clearGroup()
}

@kotlin.jvm.JvmInline
private value class Stack<T>(private val backing: ArrayList<T> = ArrayList<T>()) {
    val size: Int get() = backing.size

    fun push(value: T): Boolean = backing.add(value)
    fun pop(): T = backing.removeAt(size - 1)
    fun peek(): T = backing[size - 1]
}
