/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.overscroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun OverscrollSample() {
    @OptIn(ExperimentalFoundationApi::class)
    // our custom offset overscroll that offset the element it is applied to when we hit the bound
    // on the scrollable container.
    class OffsetOverscrollEffect(val scope: CoroutineScope) : OverscrollEffect {
        private val overscrollOffset = Animatable(0f)
        override fun consumePreScroll(
            scrollDelta: Offset,
            pointerPosition: Offset?,
            source: NestedScrollSource
        ): Offset {
            // in pre scroll we relax the overscroll if needed
            // relaxation: when we are in progress of the overscroll and user scrolls in the
            // different direction = substract the overscroll first
            val sameDirection = sign(scrollDelta.y) == sign(overscrollOffset.value)
            return if (abs(overscrollOffset.value) > 0.5 && !sameDirection && isEnabled) {
                val prevOverscrollValue = overscrollOffset.value
                val newOverscrollValue = overscrollOffset.value + scrollDelta.y
                if (sign(prevOverscrollValue) != sign(newOverscrollValue)) {
                    // sign changed, coerce to start scrolling and exit
                    scope.launch { overscrollOffset.snapTo(0f) }
                    Offset(x = 0f, y = scrollDelta.y + prevOverscrollValue)
                } else {
                    scope.launch {
                        overscrollOffset.snapTo(overscrollOffset.value + scrollDelta.y)
                    }
                    scrollDelta.copy(x = 0f)
                }
            } else {
                Offset.Zero
            }
        }

        override fun consumePostScroll(
            initialDragDelta: Offset,
            overscrollDelta: Offset,
            pointerPosition: Offset?,
            source: NestedScrollSource
        ) {
            // if it is a drag, not a fling, add the delta left to our over scroll value
            if (abs(overscrollDelta.y) > 0.5 && isEnabled && source == NestedScrollSource.Drag) {
                scope.launch {
                    // multiply by 0.1 for the sake of parallax effect
                    overscrollOffset.snapTo(overscrollOffset.value + overscrollDelta.y * 0.1f)
                }
            }
        }

        override suspend fun consumePreFling(velocity: Velocity): Velocity = Velocity.Zero

        override suspend fun consumePostFling(velocity: Velocity) {
            // when the fling happens - we just gradually animate our overscroll to 0
            if (isEnabled) {
                overscrollOffset.animateTo(
                    targetValue = 0f,
                    initialVelocity = velocity.y,
                    animationSpec = spring()
                )
            }
        }

        override var isEnabled: Boolean by mutableStateOf(true)

        override val isInProgress: Boolean
            get() = overscrollOffset.isRunning

        // as we're building an offset modifiers, let's offset of our value we calculated
        override val effectModifier: Modifier = Modifier.offset {
            IntOffset(x = 0, y = overscrollOffset.value.roundToInt())
        }
    }

    val offset = remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()
    // Create the overscroll controller
    val overscroll = remember(scope) { OffsetOverscrollEffect(scope) }
    // let's build a scrollable that scroll until -512 to 512
    val scrollStateRange = (-512f).rangeTo(512f)
    Box(
        Modifier
            .size(150.dp)
            .scrollable(
                orientation = Orientation.Vertical,
                state = rememberScrollableState { delta ->
                    // use the scroll data and indicate how much this element consumed.
                    val oldValue = offset.value
                    // coerce to our range
                    offset.value = (offset.value + delta).coerceIn(scrollStateRange)

                    offset.value - oldValue // indicate that we consumed what's needed
                },
                // pass the overscroll to the scrollable so the data is updated
                overscrollEffect = overscroll
            )
            .background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        Text(
            offset.value.roundToInt().toString(),
            style = TextStyle(fontSize = 32.sp),
            modifier = Modifier
                // show the overscroll only on the text, not the containers (just for fun)
                .overscroll(overscroll)
        )
    }
}