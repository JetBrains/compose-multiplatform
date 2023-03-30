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

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Defines a group of [Path]s and other [Group]s inside a [VectorPainter]. This is not a regular UI
 * composable, it can only be called inside composables called from the content parameter to
 * [rememberVectorPainter].
 *
 * @param name Optional name of the group used when describing the vector as a string.
 * @param rotation The rotation of the group around the Z axis, in degrees.
 * @param pivotX The horizontal pivot point used for rotation, in pixels.
 * @param pivotY The vertical pivot point used for rotation, in pixels.
 * @param scaleX Factor to scale the group by horizontally.
 * @param scaleY Factor to scale the group by vertically.
 * @param translationX Horizontal offset of the group, in pixels.
 * @param translationY Vertical offset of the group, in pixels.
 * @param clipPathData A list of [PathNode]s that define how to clip the group. Empty by default.
 * @param content A composable that defines the contents of the group.
 */
@Composable
@VectorComposable
fun Group(
    name: String = DefaultGroupName,
    rotation: Float = DefaultRotation,
    pivotX: Float = DefaultPivotX,
    pivotY: Float = DefaultPivotY,
    scaleX: Float = DefaultScaleX,
    scaleY: Float = DefaultScaleY,
    translationX: Float = DefaultTranslationX,
    translationY: Float = DefaultTranslationY,
    clipPathData: List<PathNode> = EmptyPath,
    content: @Composable @VectorComposable () -> Unit
) {
    ComposeNode<GroupComponent, VectorApplier>(
        factory = { GroupComponent() },
        update = {
            set(name) { this.name = it }
            set(rotation) { this.rotation = it }
            set(pivotX) { this.pivotX = it }
            set(pivotY) { this.pivotY = it }
            set(scaleX) { this.scaleX = it }
            set(scaleY) { this.scaleY = it }
            set(translationX) { this.translationX = it }
            set(translationY) { this.translationY = it }
            set(clipPathData) { this.clipPathData = it }
        }
    ) {
        content()
    }
}

/**
 * Defines a path inside a [VectorPainter]. This is not a regular UI composable, it can only be
 * called inside composables called from the content parameter to [rememberVectorPainter].
 *
 * @param pathData List of [PathNode]s that define the path.
 * @param pathFillType The [PathFillType] that specifies how to fill the path.
 * @param name Optional name of the path used when describing the vector as a string.
 * @param fill The [Brush] used to fill the path.
 * @param fillAlpha The alpha value to use for [fill].
 * @param stroke The [Brush] used to stroke the path.
 * @param strokeAlpha The alpha value to use for [stroke].
 * @param strokeLineWidth The width of the [stroke]. See [Stroke.width] for details.
 * @param strokeLineCap The [StrokeCap] of [stroke]. See [Stroke.cap] for details.
 * @param strokeLineJoin The [StrokeJoin] of [stroke]. See [Stroke.join] for details.
 * @param strokeLineMiter The stroke miter value. See [Stroke.miter] for details.
 * @param trimPathStart The fraction of the path that specifies the start of the clipped region of
 * the path. See [PathMeasure.getSegment].
 * @param trimPathEnd The fraction of the path that specifies the end of the clipped region of the
 * path. See [PathMeasure.getSegment].
 * @param trimPathOffset The amount to offset both [trimPathStart] and [trimPathEnd].
 */
@Composable
@VectorComposable
fun Path(
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
) {
    ComposeNode<PathComponent, VectorApplier>(
        factory = { PathComponent() },
        update = {
            set(name) { this.name = it }
            set(pathData) { this.pathData = it }
            set(pathFillType) { this.pathFillType = it }
            set(fill) { this.fill = it }
            set(fillAlpha) { this.fillAlpha = it }
            set(stroke) { this.stroke = it }
            set(strokeAlpha) { this.strokeAlpha = it }
            set(strokeLineWidth) { this.strokeLineWidth = it }
            set(strokeLineJoin) { this.strokeLineJoin = it }
            set(strokeLineCap) { this.strokeLineCap = it }
            set(strokeLineMiter) { this.strokeLineMiter = it }
            set(trimPathStart) { this.trimPathStart = it }
            set(trimPathEnd) { this.trimPathEnd = it }
            set(trimPathOffset) { this.trimPathOffset = it }
        }
    )
}

class VectorApplier(root: VNode) : AbstractApplier<VNode>(root) {
    override fun insertTopDown(index: Int, instance: VNode) {
        current.asGroup().insertAt(index, instance)
    }

    override fun insertBottomUp(index: Int, instance: VNode) {
        // Ignored as the tree is built top-down.
    }

    override fun remove(index: Int, count: Int) {
        current.asGroup().remove(index, count)
    }

    override fun onClear() {
        root.asGroup().let { it.remove(0, it.numChildren) }
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.asGroup().move(from, to, count)
    }

    private fun VNode.asGroup(): GroupComponent {
        return when (this) {
            is GroupComponent -> this
            else -> error("Cannot only insert VNode into Group")
        }
    }
}
