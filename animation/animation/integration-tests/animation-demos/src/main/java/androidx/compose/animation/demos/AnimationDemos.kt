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

import androidx.compose.animation.demos.fancy.AnimatedClockDemo
import androidx.compose.animation.demos.fancy.AnimatedDotsDemo
import androidx.compose.animation.demos.fancy.ChatScreen
import androidx.compose.animation.demos.fancy.FlingGameDemo
import androidx.compose.animation.demos.fancy.SpringChainDemo
import androidx.compose.animation.demos.gesture.FancyScrollingDemo
import androidx.compose.animation.demos.gesture.SpringBackScrollingDemo
import androidx.compose.animation.demos.gesture.SwipeToDismissDemo
import androidx.compose.animation.demos.layoutanimation.AnimateContentSizeDemo
import androidx.compose.animation.demos.layoutanimation.AnimateEnterExitDemo
import androidx.compose.animation.demos.layoutanimation.AnimateIncrementDecrementDemo
import androidx.compose.animation.demos.layoutanimation.AnimatedContentWithContentKeyDemo
import androidx.compose.animation.demos.layoutanimation.AnimatedPlacementDemo
import androidx.compose.animation.demos.layoutanimation.AnimatedVisibilityDemo
import androidx.compose.animation.demos.layoutanimation.AnimatedVisibilityLazyColumnDemo
import androidx.compose.animation.demos.layoutanimation.NestedMenuDemo
import androidx.compose.animation.demos.layoutanimation.ScaleEnterExitDemo
import androidx.compose.animation.demos.layoutanimation.ScreenTransitionDemo
import androidx.compose.animation.demos.layoutanimation.ShrineCartDemo
import androidx.compose.animation.demos.lookahead.CraneDemo
import androidx.compose.animation.demos.lookahead.LookaheadLayoutWithAlignmentLinesDemo
import androidx.compose.animation.demos.lookahead.LookaheadMeasurePlaceDemo
import androidx.compose.animation.demos.lookahead.LookaheadWithMovableContentDemo
import androidx.compose.animation.demos.lookahead.ScreenSizeChangeDemo
import androidx.compose.animation.demos.singlevalue.SingleValueAnimationDemo
import androidx.compose.animation.demos.statetransition.CrossfadeDemo
import androidx.compose.animation.demos.statetransition.DoubleTapToLikeDemo
import androidx.compose.animation.demos.statetransition.GestureBasedAnimationDemo
import androidx.compose.animation.demos.statetransition.InfiniteTransitionDemo
import androidx.compose.animation.demos.statetransition.LoadingAnimationDemo
import androidx.compose.animation.demos.statetransition.MultiDimensionalAnimationDemo
import androidx.compose.animation.demos.statetransition.RepeatedRotationDemo
import androidx.compose.animation.demos.suspendfun.InfiniteAnimationDemo
import androidx.compose.animation.demos.suspendfun.SuspendAnimationDemo
import androidx.compose.animation.demos.suspendfun.SuspendDoubleTapToLikeDemo
import androidx.compose.animation.demos.vectorgraphics.AnimatedVectorGraphicsDemo
import androidx.compose.animation.demos.visualaid.ColumnConfigurationDemo
import androidx.compose.animation.demos.visualaid.EasingInfoDemo
import androidx.compose.animation.demos.visualaid.RowConfigurationDemo
import androidx.compose.animation.demos.visualinspection.AnimatedVisibilityContentSizeChange
import androidx.compose.animation.demos.visualinspection.EnterExitCombination
import androidx.compose.animation.demos.visualinspection.SlideInContentVariedSizes
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
            "\uD83E\uDD7C\uD83E\uDDD1\u200D\uD83D\uDD2C Lookahead Animation Demos",
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
                    SlideInContentVariedSizes()
                },
                ComposableDemo("Enter/ExitTransition Combo") { EnterExitCombination() },
                ComposableDemo("AnimatedVisibility with Content Size Change") {
                    AnimatedVisibilityContentSizeChange()
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
                ComposableDemo("Game of fling") { FlingGameDemo() },
                ComposableDemo("Spring chain") { SpringChainDemo() },
            )
        )
    )
)
