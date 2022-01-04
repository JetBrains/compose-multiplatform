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
                ComposableDemo("Double tap to like") { DoubleTapToLikeDemo() },
                ComposableDemo("Gesture based animation") { GestureBasedAnimationDemo() },
                ComposableDemo("Infinite transition") { InfiniteTransitionDemo() },
                ComposableDemo("Multi-dimensional prop") { MultiDimensionalAnimationDemo() },
                ComposableDemo("Repeating rotation") { RepeatedRotationDemo() },
            )
        ),
        DemoCategory(
            "Layout Animation Demos",
            listOf(
                ComposableDemo("Animate Content Size") { AnimateContentSizeDemo() },
                ComposableDemo("Animate Increment/Decrement") { AnimateIncrementDecrementDemo() },
                ComposableDemo("Animate Placement") { AnimatedPlacementDemo() },
                ComposableDemo("Animate Visibility Demo") { AnimatedVisibilityDemo() },
                ComposableDemo("Animate Visibility Lazy Column Demo") {
                    AnimatedVisibilityLazyColumnDemo()
                },
                ComposableDemo("Cross Fade") { CrossfadeDemo() },
                ComposableDemo("Modifier.animateEnterExit Demo") { AnimateEnterExitDemo() },
                ComposableDemo("Nested Menu") { NestedMenuDemo() },
                ComposableDemo("Save/Restore in AnimatedContent") {
                    AnimatedContentWithContentKeyDemo()
                },
                ComposableDemo("Scaled Enter/Exit") { ScaleEnterExitDemo() },
                ComposableDemo("Shrine Cart") { ShrineCartDemo() },
                ComposableDemo("Screen Transition") { ScreenTransitionDemo() },
            )
        ),
        DemoCategory(
            "\uD83E\uDD7C\uD83E\uDDD1\u200D\uD83D\uDD2C Layout Animation Demos",
            listOf(
                ComposableDemo("Crane Nested Shared Element") { CraneDemo() },
                ComposableDemo("Screen Size Change Demo") { ScreenSizeChangeDemo() },
                ComposableDemo("Lookahead With Movable Content") {
                    LookaheadWithMovableContentDemo()
                },
                ComposableDemo("Lookahead With Alignment Lines") {
                    LookaheadLayoutWithAlignmentLinesDemo()
                },
                ComposableDemo("Flow Row Lookahead") { LookaheadMeasurePlaceDemo() },
            )
        ),
        DemoCategory(
            "Suspend Animation Demos",
            listOf(
                ComposableDemo("Animated scrolling") { FancyScrollingDemo() },
                ComposableDemo("animateColorAsState") { SingleValueAnimationDemo() },
                ComposableDemo("Double Tap To Like") { SuspendDoubleTapToLikeDemo() },
                ComposableDemo("Follow the tap") { SuspendAnimationDemo() },
                ComposableDemo("Infinitely Animating") { InfiniteAnimationDemo() },
                ComposableDemo("Loading Animation Demo") { LoadingAnimationDemo() },
                ComposableDemo("Spring back scrolling") { SpringBackScrollingDemo() },
                ComposableDemo("Swipe to dismiss") { SwipeToDismissDemo() },
            )
        ),
        DemoCategory(
            "Graphics Animation Demos",
            listOf(
                ComposableDemo("Animated Vector Graphics") { AnimatedVectorGraphicsDemo() },
            )
        ),

        DemoCategory(
            "⛔ DO NOT ENTER ⛔",
            listOf(
                ComposableDemo("AnimatedContent alignment/slideInto") {
                    InspectionSlideInContentVariedSizes()
                },
                ComposableDemo("Enter/ExitTransition Combo") { InspectionEnterExitCombination() },
                ComposableDemo("AnimatedVisibility with Content Size Change") {
                    InspectionAnimatedVisibilityContentSizeChange()
                },
            )
        ),

        DemoCategory(
            "Visual Aid \uD83D\uDC40 \uD83D\uDC40",
            listOf(
                ComposableDemo("Column Arrangements Demo") { ColumnConfigurationDemo() },
                ComposableDemo("Row Arrangements Demo (Landscape)") { RowConfigurationDemo() },
                ComposableDemo("Easing Functions Demo") { EasingInfoDemo() },
            )
        ),

        DemoCategory(
            "\uD83C\uDF89 Fun Demos",
            listOf(
                ComposableDemo("Animated clock") { AnimatedClockDemo() },
                ComposableDemo("Animated dots") { AnimatedDotsDemo() },
                ComposableDemo("Chat screen") { ChatScreen() },
                ComposableDemo("Game of fling") { FlingGame() },
                ComposableDemo("Spring chain") { SpringChainDemo() },
            )
        )
    )
)
