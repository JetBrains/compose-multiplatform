/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.gestures

import android.content.Context
import android.widget.EdgeEffect
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.EdgeEffectCompat.distanceCompat
import androidx.compose.foundation.gestures.EdgeEffectCompat.onAbsorbCompat
import androidx.compose.foundation.gestures.EdgeEffectCompat.onPullDistanceCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.NativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastForEach
import kotlin.math.roundToInt

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal actual fun rememberOverScrollController(): OverScrollController {
    val context = LocalContext.current
    val config = LocalOverScrollConfiguration.current
    return remember(context, config) {
        if (config != null) {
            AndroidEdgeEffectOverScrollController(context, config)
        } else {
            NoOpOverscrollController
        }
    }
}

internal actual fun Modifier.overScroll(
    overScrollController: OverScrollController
): Modifier = drawWithContent {
    drawContent()
    with(overScrollController) {
        drawOverScroll()
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class AndroidEdgeEffectOverScrollController(
    context: Context,
    val overScrollConfig: OverScrollConfiguration
) : OverScrollController {
    private val topEffect = EdgeEffectCompat.create(context, null)
    private val bottomEffect = EdgeEffectCompat.create(context, null)
    private val leftEffect = EdgeEffectCompat.create(context, null)
    private val rightEffect = EdgeEffectCompat.create(context, null)
    private val allEffects = listOf(leftEffect, topEffect, rightEffect, bottomEffect)

    // hack explanation: those edge effects are used to negate the previous effect
    // of the corresponding edge
    // used to mimic the render node reset that is not available in the platform
    private val topEffectNegation = EdgeEffectCompat.create(context, null)
    private val bottomEffectNegation = EdgeEffectCompat.create(context, null)
    private val leftEffectNegation = EdgeEffectCompat.create(context, null)
    private val rightEffectNegation = EdgeEffectCompat.create(context, null)

    init {
        allEffects.fastForEach { it.color = overScrollConfig.glowColor.toArgb() }
    }

    private val redrawSignal = mutableStateOf(0)

    private fun invalidateOverScroll() {
        redrawSignal.value += 1
    }

    override fun release() {
        if (ignoreOverscroll()) return
        var needsInvalidation = false
        allEffects.fastForEach {
            it.onRelease()
            needsInvalidation = it.isFinished || needsInvalidation
        }
        if (needsInvalidation) invalidateOverScroll()
    }

    override fun consumePreScroll(
        scrollDelta: Offset,
        pointerPosition: Offset?,
        source: NestedScrollSource
    ): Offset {
        if (ignoreOverscroll() || source != NestedScrollSource.Drag) return Offset.Zero
        val pointer = pointerPosition ?: containerSize.center
        val consumedPixelsY = when {
            scrollDelta.y == 0f -> 0f
            topEffect.distanceCompat != 0f -> {
                pullTop(scrollDelta, pointer).also {
                    if (topEffect.distanceCompat == 0f) topEffect.onRelease()
                }
            }
            bottomEffect.distanceCompat != 0f -> {
                pullBottom(scrollDelta, pointer).also {
                    if (bottomEffect.distanceCompat == 0f) bottomEffect.onRelease()
                }
            }
            else -> 0f
        }
        val consumedPixelsX = when {
            scrollDelta.x == 0f -> 0f
            leftEffect.distanceCompat != 0f -> {
                pullLeft(scrollDelta, pointer).also {
                    if (leftEffect.distanceCompat == 0f) leftEffect.onRelease()
                }
            }
            rightEffect.distanceCompat != 0f -> {
                pullRight(scrollDelta, pointer).also {
                    if (rightEffect.distanceCompat == 0f) rightEffect.onRelease()
                }
            }
            else -> 0f
        }
        val consumedOffset = Offset(consumedPixelsX, consumedPixelsY)
        if (consumedOffset != Offset.Zero) invalidateOverScroll()
        return consumedOffset
    }

    override fun consumePostScroll(
        initialDragDelta: Offset,
        overScrollDelta: Offset,
        pointerPosition: Offset?,
        source: NestedScrollSource
    ) {
        if (ignoreOverscroll()) return
        var needsInvalidation = false
        if (source == NestedScrollSource.Drag) {
            val pointer = pointerPosition ?: containerSize.center
            if (overScrollDelta.x > 0) {
                pullLeft(overScrollDelta, pointer)
            } else if (overScrollDelta.x < 0) {
                pullRight(overScrollDelta, pointer)
            }
            if (overScrollDelta.y > 0) {
                pullTop(overScrollDelta, pointer)
            } else if (overScrollDelta.y < 0) {
                pullBottom(overScrollDelta, pointer)
            }
            needsInvalidation = overScrollDelta != Offset.Zero || needsInvalidation
        }
        needsInvalidation = releaseOppositeOverscroll(initialDragDelta) || needsInvalidation
        if (needsInvalidation) invalidateOverScroll()
    }

    override fun consumePreFling(velocity: Velocity): Velocity {
        if (ignoreOverscroll()) return Velocity.Zero
        val consumedX = if (velocity.x > 0f && leftEffect.distanceCompat != 0f) {
            leftEffect.onAbsorbCompat(velocity.x.roundToInt())
            velocity.x
        } else if (velocity.x < 0 && rightEffect.distanceCompat != 0f) {
            rightEffect.onAbsorbCompat(-velocity.x.roundToInt())
            velocity.x
        } else {
            0f
        }
        val consumedY = if (velocity.y > 0f && topEffect.distanceCompat != 0f) {
            topEffect.onAbsorbCompat(velocity.y.roundToInt())
            velocity.y
        } else if (velocity.y < 0f && bottomEffect.distanceCompat != 0f) {
            bottomEffect.onAbsorbCompat(-velocity.y.roundToInt())
            velocity.y
        } else {
            0f
        }
        val consumed = Velocity(consumedX, consumedY)
        if (consumed != Velocity.Zero) invalidateOverScroll()
        return consumed
    }

    override fun consumePostFling(velocity: Velocity) {
        if (ignoreOverscroll()) return
        if (velocity.x > 0) {
            leftEffect.onAbsorbCompat(velocity.x.roundToInt())
        } else if (velocity.x < 0) {
            rightEffect.onAbsorbCompat(-velocity.x.roundToInt())
        }
        if (velocity.y > 0) {
            topEffect.onAbsorbCompat(velocity.y.roundToInt())
        } else if (velocity.y < 0) {
            bottomEffect.onAbsorbCompat(-velocity.y.roundToInt())
        }
        if (velocity != Velocity.Zero) invalidateOverScroll()
    }

    private var containerSize by mutableStateOf(Size.Zero)
    private var isContentScrolls by mutableStateOf(false)

    override fun refreshContainerInfo(size: Size, isContentScrolls: Boolean) {
        val differentSize = size != containerSize
        val differentScroll = this.isContentScrolls != isContentScrolls
        containerSize = size
        this.isContentScrolls = isContentScrolls
        if (differentSize) {
            topEffect.setSize(size.width.roundToInt(), size.height.roundToInt())
            bottomEffect.setSize(size.width.roundToInt(), size.height.roundToInt())
            leftEffect.setSize(size.height.roundToInt(), size.width.roundToInt())
            rightEffect.setSize(size.height.roundToInt(), size.width.roundToInt())

            topEffectNegation.setSize(size.width.roundToInt(), size.height.roundToInt())
            bottomEffectNegation.setSize(size.width.roundToInt(), size.height.roundToInt())
            leftEffectNegation.setSize(size.height.roundToInt(), size.width.roundToInt())
            rightEffectNegation.setSize(size.height.roundToInt(), size.width.roundToInt())
        }

        if (differentScroll || differentSize) release()
    }

    override fun DrawScope.drawOverScroll() {
        this.drawIntoCanvas { it ->
            redrawSignal.value // <-- value read to redraw if needed
            if (ignoreOverscroll()) return
            val canvas = it.nativeCanvas
            var needsInvalidate = false
            // each side workflow:
            // 1. reset what was draw in the past cycle, effectively clearing the effect
            // 2. Draw the effect on the edge
            // 3. Remember how much was drawn to clear in 1. in the next cycle
            if (leftEffectNegation.distanceCompat != 0f) {
                drawRight(leftEffectNegation, canvas)
                leftEffectNegation.finish()
            }
            if (!leftEffect.isFinished) {
                needsInvalidate = drawLeft(leftEffect, canvas) || needsInvalidate
                leftEffectNegation.onPullDistanceCompat(leftEffect.distanceCompat, 0f)
            }
            if (topEffectNegation.distanceCompat != 0f) {
                drawBottom(topEffectNegation, canvas)
                topEffectNegation.finish()
            }
            if (!topEffect.isFinished) {
                needsInvalidate = drawTop(topEffect, canvas) || needsInvalidate
                topEffectNegation.onPullDistanceCompat(topEffect.distanceCompat, 0f)
            }
            if (rightEffectNegation.distanceCompat != 0f) {
                drawLeft(rightEffectNegation, canvas)
                rightEffectNegation.finish()
            }
            if (!rightEffect.isFinished) {
                needsInvalidate = drawRight(rightEffect, canvas) || needsInvalidate
                rightEffectNegation.onPullDistanceCompat(rightEffect.distanceCompat, 0f)
            }
            if (bottomEffectNegation.distanceCompat != 0f) {
                drawTop(bottomEffectNegation, canvas)
                bottomEffectNegation.finish()
            }
            if (!bottomEffect.isFinished) {
                needsInvalidate = drawBottom(bottomEffect, canvas) || needsInvalidate
                bottomEffectNegation.onPullDistanceCompat(bottomEffect.distanceCompat, 0f)
            }
            if (needsInvalidate) invalidateOverScroll()
        }
    }

    private fun DrawScope.drawLeft(left: EdgeEffect, canvas: NativeCanvas): Boolean {
        val restore = canvas.save()
        canvas.rotate(270f)
        canvas.translate(
            -containerSize.height,
            overScrollConfig.drawPadding.calculateLeftPadding(layoutDirection).toPx()
        )
        val needsInvalidate = left.draw(canvas)
        canvas.restoreToCount(restore)
        return needsInvalidate
    }

    private fun DrawScope.drawTop(top: EdgeEffect, canvas: NativeCanvas): Boolean {
        val restore = canvas.save()
        canvas.translate(0f, overScrollConfig.drawPadding.calculateTopPadding().toPx())
        val needsInvalidate = top.draw(canvas)
        canvas.restoreToCount(restore)
        return needsInvalidate
    }

    private fun DrawScope.drawRight(right: EdgeEffect, canvas: NativeCanvas): Boolean {
        val restore = canvas.save()
        val width = containerSize.width.roundToInt()
        val rightPadding = overScrollConfig.drawPadding.calculateRightPadding(layoutDirection)
        canvas.rotate(90f)
        canvas.translate(0f, -width.toFloat() + rightPadding.toPx())
        val needsInvalidate = right.draw(canvas)
        canvas.restoreToCount(restore)
        return needsInvalidate
    }

    private fun DrawScope.drawBottom(bottom: EdgeEffect, canvas: NativeCanvas): Boolean {
        val restore = canvas.save()
        canvas.rotate(180f)
        val bottomPadding = overScrollConfig.drawPadding.calculateBottomPadding().toPx()
        canvas.translate(-containerSize.width, -containerSize.height + bottomPadding)
        val needsInvalidate = bottom.draw(canvas)
        canvas.restoreToCount(restore)
        return needsInvalidate
    }

    override fun stopOverscrollAnimation(): Boolean {
        var stopped = false
        val fakeDisplacement = containerSize.center // displacement doesn't matter here
        if (leftEffect.distanceCompat != 0f) {
            pullLeft(Offset.Zero, fakeDisplacement)
            stopped = true
        }
        if (rightEffect.distanceCompat != 0f) {
            pullRight(Offset.Zero, fakeDisplacement)
            stopped = true
        }
        if (topEffect.distanceCompat != 0f) {
            pullTop(Offset.Zero, fakeDisplacement)
            stopped = true
        }
        if (bottomEffect.distanceCompat != 0f) {
            pullBottom(Offset.Zero, fakeDisplacement)
            stopped = true
        }
        return stopped
    }

    private fun releaseOppositeOverscroll(delta: Offset): Boolean {
        var needsInvalidation = false
        if (!leftEffect.isFinished && delta.x < 0) {
            leftEffect.onRelease()
            needsInvalidation = leftEffect.isFinished
        }
        if (!rightEffect.isFinished && delta.x > 0) {
            rightEffect.onRelease()
            needsInvalidation = needsInvalidation || rightEffect.isFinished
        }
        if (!topEffect.isFinished && delta.y < 0) {
            topEffect.onRelease()
            needsInvalidation = needsInvalidation || topEffect.isFinished
        }
        if (!bottomEffect.isFinished && delta.y > 0) {
            bottomEffect.onRelease()
            needsInvalidation = needsInvalidation || bottomEffect.isFinished
        }
        return needsInvalidation
    }

    private fun pullTop(scroll: Offset, displacement: Offset): Float {
        val displacementX: Float = displacement.x / containerSize.width
        val pullY = scroll.y / containerSize.height
        return topEffect.onPullDistanceCompat(pullY, displacementX) * containerSize.height
    }

    private fun pullBottom(scroll: Offset, displacement: Offset): Float {
        val displacementX: Float = displacement.x / containerSize.width
        val pullY = scroll.y / containerSize.height
        return -bottomEffect.onPullDistanceCompat(-pullY, 1 - displacementX) * containerSize.height
    }

    private fun pullLeft(scroll: Offset, displacement: Offset): Float {
        val displacementY: Float = displacement.y / containerSize.height
        val pullX = scroll.x / containerSize.width
        return leftEffect.onPullDistanceCompat(pullX, 1 - displacementY) * containerSize.width
    }

    private fun pullRight(scroll: Offset, displacement: Offset): Float {
        val displacementY: Float = displacement.y / containerSize.height
        val pullX = scroll.x / containerSize.width
        return -rightEffect.onPullDistanceCompat(-pullX, displacementY) * containerSize.width
    }

    private fun ignoreOverscroll(): Boolean {
        return !overScrollConfig.forceShowAlways && !isContentScrolls
    }
}

private val NoOpOverscrollController = object : OverScrollController {
    override fun release() {}

    override fun consumePreScroll(
        scrollDelta: Offset,
        pointerPosition: Offset?,
        source: NestedScrollSource
    ): Offset = Offset.Zero

    override fun consumePostScroll(
        initialDragDelta: Offset,
        overScrollDelta: Offset,
        pointerPosition: Offset?,
        source: NestedScrollSource
    ) {
    }

    override fun consumePreFling(velocity: Velocity): Velocity = Velocity.Zero

    override fun consumePostFling(velocity: Velocity) {}

    override fun refreshContainerInfo(size: Size, isContentScrolls: Boolean) {}

    override fun stopOverscrollAnimation(): Boolean = false

    override fun DrawScope.drawOverScroll() {}
}
