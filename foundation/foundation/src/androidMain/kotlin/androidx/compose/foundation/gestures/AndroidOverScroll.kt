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
import android.os.Build
import android.widget.EdgeEffect
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
    private val topEffect = EdgeEffect(context)
    private val bottomEffect = EdgeEffect(context)
    private val leftEffect = EdgeEffect(context)
    private val rightEffect = EdgeEffect(context)
    private val allEffects = listOf(leftEffect, topEffect, rightEffect, bottomEffect)

    init {
        allEffects.fastForEach { it.color = overScrollConfig.glowColor.toArgb() }
    }

    private val redrawSignal = mutableStateOf(0)

    private fun invalidateOverScroll() {
        redrawSignal.value += 1
    }

    override fun release(): Boolean {
        var needsInvalidation = false
        allEffects.fastForEach {
            it.onRelease()
            needsInvalidation = needsInvalidation || it.isFinished
        }
        if (needsInvalidation) invalidateOverScroll()
        return needsInvalidation
    }

    override fun processDragDelta(
        initialDragDelta: Offset,
        overScrollDelta: Offset,
        pointerPosition: Offset?
    ): Boolean {
        if (ignoreOverscroll()) return false
        var needsInvalidation = false
        if (pointerPosition != null) {
            if (overScrollDelta.x < 0) {
                leftEffect.onPull(
                    -overScrollDelta.x / containerSize.width,
                    1f - pointerPosition.y / containerSize.height
                )
            } else if (overScrollDelta.x > 0) {
                rightEffect.onPull(
                    overScrollDelta.x / containerSize.width,
                    pointerPosition.y / containerSize.height
                )
            }
            if (overScrollDelta.y < 0) {
                topEffect.onPull(
                    -overScrollDelta.y / containerSize.height,
                    pointerPosition.x / containerSize.width
                )
            } else if (overScrollDelta.y > 0) {
                bottomEffect.onPull(
                    overScrollDelta.y / containerSize.height,
                    1f - pointerPosition.x / containerSize.width
                )
            }
            needsInvalidation = needsInvalidation || overScrollDelta != Offset.Zero
        }
        needsInvalidation = needsInvalidation || releaseOverScrollPostScroll(initialDragDelta)
        if (needsInvalidation) invalidateOverScroll()
        return needsInvalidation
    }

    override fun processVelocity(velocity: Velocity): Boolean {
        if (ignoreOverscroll()) return false
        if (velocity.x < 0 && leftEffect.isFinished) {
            leftEffect.onAbsorb(-velocity.x.roundToInt())
        } else if (velocity.x > 0 && rightEffect.isFinished) {
            rightEffect.onAbsorb(velocity.x.roundToInt())
        }
        if (velocity.y < 0 && topEffect.isFinished) {
            topEffect.onAbsorb(-velocity.y.roundToInt())
        } else if (velocity.y > 0 && bottomEffect.isFinished) {
            bottomEffect.onAbsorb(velocity.y.roundToInt())
        }
        if (velocity != Velocity.Zero) invalidateOverScroll()
        return velocity != Velocity.Zero
    }

    private var containerSize by mutableStateOf(Size.Zero)
    private var isContentScrolls by mutableStateOf(false)

    override fun refreshContainerInfo(size: Size, isContentScrolls: Boolean) {
        val needRelease = size != containerSize || this.isContentScrolls != isContentScrolls
        containerSize = size
        this.isContentScrolls = isContentScrolls
        topEffect.setSize(size.width.roundToInt(), size.height.roundToInt())
        bottomEffect.setSize(size.width.roundToInt(), size.height.roundToInt())
        leftEffect.setSize(size.height.roundToInt(), size.width.roundToInt())
        rightEffect.setSize(size.height.roundToInt(), size.width.roundToInt())

        if (needRelease) release()
    }

    override fun DrawScope.drawOverScroll() {
        this.drawIntoCanvas { it ->
            val canvas = it.nativeCanvas
            redrawSignal.value // <-- value read to redraw if needed
            if (ignoreOverscroll()) return
            var needsInvalidate = false
            val padding = overScrollConfig.drawPadding
            if (!leftEffect.isFinished) {
                val restore = canvas.save()
                canvas.rotate(270f)
                canvas.translate(
                    -containerSize.height,
                    padding.calculateLeftPadding(layoutDirection).toPx()
                )
                needsInvalidate = leftEffect.draw(canvas) || needsInvalidate
                canvas.restoreToCount(restore)
            }
            if (!topEffect.isFinished) {
                val restore = canvas.save()
                canvas.translate(0f, padding.calculateTopPadding().toPx())
                needsInvalidate = topEffect.draw(canvas) || needsInvalidate
                canvas.restoreToCount(restore)
            }
            if (!rightEffect.isFinished) {
                val restore = canvas.save()
                val width = containerSize.width.roundToInt()
                canvas.rotate(90f)
                canvas.translate(
                    0f,
                    -width.toFloat() + padding.calculateRightPadding(layoutDirection).toPx()
                )
                needsInvalidate = rightEffect.draw(canvas) || needsInvalidate
                canvas.restoreToCount(restore)
            }
            if (!bottomEffect.isFinished) {
                val restore = canvas.save()
                canvas.rotate(180f)
                val bottomPadding = padding.calculateBottomPadding().toPx()
                canvas.translate(-containerSize.width, -containerSize.height + bottomPadding)
                needsInvalidate = bottomEffect.draw(canvas) || needsInvalidate
                canvas.restoreToCount(restore)
            }
            if (needsInvalidate) invalidateOverScroll()
        }
    }

    private fun releaseOverScrollPostScroll(delta: Offset): Boolean {
        if (ignoreOverscroll()) return false
        var needsInvalidation = false
        if (!leftEffect.isFinished() && delta.x > 0) {
            leftEffect.onRelease()
            needsInvalidation = leftEffect.isFinished()
        }
        if (!rightEffect.isFinished() && delta.x < 0) {
            rightEffect.onRelease()
            needsInvalidation = needsInvalidation || rightEffect.isFinished()
        }
        if (!topEffect.isFinished() && delta.y > 0) {
            topEffect.onRelease()
            needsInvalidation = needsInvalidation || topEffect.isFinished()
        }
        if (!bottomEffect.isFinished() && delta.y < 0) {
            bottomEffect.onRelease()
            needsInvalidation = needsInvalidation || bottomEffect.isFinished()
        }
        return needsInvalidation
    }

    private fun ignoreOverscroll(): Boolean {
        return (!overScrollConfig.forceShowAlways && !isContentScrolls) ||
            Build.VERSION.SDK_INT > Build.VERSION_CODES.R // no stretch support just yet
    }
}

private val NoOpOverscrollController = object : OverScrollController {
    override fun release(): Boolean = false

    override fun processDragDelta(
        initialDragDelta: Offset,
        overScrollDelta: Offset,
        pointerPosition: Offset?
    ): Boolean = false

    override fun processVelocity(velocity: Velocity): Boolean = false

    override fun refreshContainerInfo(size: Size, isContentScrolls: Boolean) {}

    override fun DrawScope.drawOverScroll() {}
}