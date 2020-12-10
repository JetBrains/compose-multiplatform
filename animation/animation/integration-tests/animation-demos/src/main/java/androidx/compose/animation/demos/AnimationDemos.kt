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

package androidx.compose.animation.demos

import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.integration.demos.common.DemoCategory

val AnimationDemos = DemoCategory(
    "Animation",
    listOf(
        DemoCategory(
            "State Transition Demos",
            listOf(
                ComposableDemo("Multi-dimensional prop") { MultiDimensionalAnimationDemo() },
                ComposableDemo("State based ripple") { StateBasedRippleDemo() },
                ComposableDemo("Repeating rotation") { RepeatedRotationDemo() },
                ComposableDemo("Manual animation clock") { AnimatableSeekBarDemo() },
                ComposableDemo("Gesture based animation") { GestureBasedAnimationDemo() },
            )
        ),
        DemoCategory(
            "Single Value Animation Demos",
            listOf(
                ComposableDemo("Animated scrolling") { FancyScrollingDemo() },
                ComposableDemo("animate()") { SingleValueAnimationDemo() },
            )
        ),
        DemoCategory(
            "Layout Animation Demos",
            listOf(
                ComposableDemo("Animate Content Size") { AnimateContentSizeDemo() },
                ComposableDemo("Animate Visibility Demo") { AnimatedVisibilityDemo() },
                ComposableDemo("Animate Visibility Lazy Column Demo") {
                    AnimatedVisibilityLazyColumnDemo()
                },
                ComposableDemo("Cross Fade") { CrossfadeDemo() },
                ComposableDemo("Enter/Exit Transition Demo") { EnterExitTransitionDemo() },
            )
        ),
        DemoCategory(
            "Suspend Animation Demos",
            listOf(
                ComposableDemo("Follow the tap") { SuspendAnimationDemo() },
                ComposableDemo("Infinitely Animating") { InfiniteAnimationDemo() },
                ComposableDemo("Spring back scrolling") { SpringBackScrollingDemo() },
                ComposableDemo("Swipe to dismiss") { SwipeToDismissDemo() },
            )
        ),
    )
)
