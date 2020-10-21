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

package androidx.compose.ui.graphics.vector

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.Dp

private inline class Stack<T>(private val backing: ArrayList<T> = ArrayList<T>()) {
    val size: Int get() = backing.size

    fun push(value: T) = backing.add(value)
    fun pop(): T = backing.removeAt(size - 1)
    fun peek(): T = backing[size - 1]
    fun isEmpty() = backing.isEmpty()
    fun isNotEmpty() = !isEmpty()
    fun clear() = backing.clear()
}

/**
 * Builder used to construct a Vector graphic tree.
 * This is useful for caching the result of expensive operations used to construct
 * a vector graphic for compose.
 * For example, the vector graphic could be serialized and downloaded from a server and represented
 * internally in a VectorAsset before it is composed through [rememberVectorPainter]
 * The generated VectorAsset is recommended to be memoized across composition calls to avoid
 * doing redundant work
 */
class VectorAssetBuilder(

    /**
     * Name of the vector asset
     */
    val name: String = DefaultGroupName,

    /**
     * Intrinsic width of the Vector in [Dp]
     */
    val defaultWidth: Dp,

    /**
     * Intrinsic height of the Vector in [Dp]
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
    val viewportHeight: Float
) {
    private val nodes = Stack<GroupParams>()

    private var root = GroupParams()
    private var isConsumed = false

    private val currentGroup: GroupParams
        get() = nodes.peek()

    init {
        nodes.push(root)
    }

    /**
     * Create a new group and push it to the front of the stack of VectorAsset nodes
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
     * @return This VectorAssetBuilder instance as a convenience for chaining calls
     */
    fun pushGroup(
        name: String = DefaultGroupName,
        rotate: Float = DefaultRotation,
        pivotX: Float = DefaultPivotX,
        pivotY: Float = DefaultPivotY,
        scaleX: Float = DefaultScaleX,
        scaleY: Float = DefaultScaleY,
        translationX: Float = DefaultTranslationX,
        translationY: Float = DefaultTranslationY,
        clipPathData: List<PathNode> = EmptyPath
    ): VectorAssetBuilder {
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
     * Pops the topmost VectorGroup from this VectorAssetBuilder. This is used to indicate
     * that no additional VectorAsset nodes will be added to the current VectorGroup
     * @return This VectorAssetBuilder instance as a convenience for chaining calls
     */
    fun popGroup(): VectorAssetBuilder {
        ensureNotConsumed()
        val popped = nodes.pop()
        currentGroup.children.add(popped.asVectorGroup())
        return this
    }

    /**
     * Add a path to the VectorAsset graphic. This represents a leaf node in the VectorAsset graphics
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
     * @return This VectorAssetBuilder instance as a convenience for chaining calls
     */
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
    ): VectorAssetBuilder {
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
     * Construct a VectorAsset. This concludes the creation process of a VectorAsset graphic
     * This builder cannot be re-used to create additional VectorAsset instances
     * @return The newly created VectorAsset instance
     */
    fun build(): VectorAsset {
        ensureNotConsumed()
        // pop all groups except for the root
        while (nodes.size > 1) {
            popGroup()
        }

        val vectorImage = VectorAsset(
            name,
            defaultWidth,
            defaultHeight,
            viewportWidth,
            viewportHeight,
            root.asVectorGroup()
        )

        isConsumed = true

        return vectorImage
    }

    /**
     * Throws IllegalStateException if the VectorAssetBuilder has already been consumed
     */
    private fun ensureNotConsumed() {
        check(!isConsumed) {
            "VectorAssetBuilder is single use, create a new instance to create a new VectorAsset"
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
    private class GroupParams (
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
 * DSL extension for adding a [VectorPath] to [this].
 *
 * See [VectorAssetBuilder.addPath] for the corresponding builder function.
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
inline fun VectorAssetBuilder.path(
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
 * See [VectorAssetBuilder.pushGroup] for the corresponding builder function.
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
inline fun VectorAssetBuilder.group(
    name: String = DefaultGroupName,
    rotate: Float = DefaultRotation,
    pivotX: Float = DefaultPivotX,
    pivotY: Float = DefaultPivotY,
    scaleX: Float = DefaultScaleX,
    scaleY: Float = DefaultScaleY,
    translationX: Float = DefaultTranslationX,
    translationY: Float = DefaultTranslationY,
    clipPathData: List<PathNode> = EmptyPath,
    block: VectorAssetBuilder.() -> Unit
) = apply {
    pushGroup(
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
    popGroup()
}
