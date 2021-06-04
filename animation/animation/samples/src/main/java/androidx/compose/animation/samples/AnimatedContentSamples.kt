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

package androidx.compose.animation.samples

import androidx.annotation.Sampled
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentScope.SlideDirection
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalAnimationApi::class)
@Composable
@Sampled
fun AnimateIncrementDecrementSample() {
    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        var count by remember { mutableStateOf(0) }
        // The `AnimatedContent` below uses an integer count as its target state. So when the
        // count changes, it will animate out the content associated with the previous count, and
        // animate in the content associated with the target state.
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                // We can define how the new target content comes in and how initial content
                // leaves in the ContentTransform. Here we want to create the impression that the
                // different numbers have a spatial relationship - larger numbers are
                // positioned (vertically) below smaller numbers.
                if (targetState > initialState) {
                    // If the incoming number is larger, new number slides up and fades in while
                    // the previous (smaller) number slides up to make room and fades out.
                    slideInVertically({ it }) + fadeIn() with
                        slideOutVertically({ -it }) + fadeOut()
                } else {
                    // If the incoming number is smaller, new number slides down and fades in while
                    // the previous number slides down and fades out.
                    slideInVertically({ -it }) + fadeIn() with
                        slideOutVertically({ it }) + fadeOut()
                    // Disable clipping since the faded slide-out is desired out of bounds, but
                    // the size transform is still needed from number getting longer
                }.using(SizeTransform(clip = false)) // Using default spring for the size change.
            }
        ) { targetCount ->
            // This establishes a mapping between the target state and the content in the form of a
            // Composable function. IMPORTANT: The parameter of this content lambda should
            // *always* be used. During the content transform, the old content will be looked up
            // using this lambda with the old state, until it's fully animated out.

            // Since AnimatedContent differentiates the contents using their target states as the
            // key, the same composable function returned by the content lambda like below will be
            // invoked under different keys and therefore treated as different entities.
            Text("$targetCount", fontSize = 20.sp)
        }
        Spacer(Modifier.size(20.dp))
        Row(horizontalArrangement = Arrangement.SpaceAround) {
            Button(onClick = { count-- }) { Text("Minus") }
            Spacer(Modifier.size(60.dp))
            Button(onClick = { count++ }) { Text("Plus ") }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
@Sampled
fun SimpleAnimatedContentSample() {
    // enum class ContentState { Foo, Bar, Baz }
    @Composable
    fun Foo() { Box(Modifier.size(200.dp).background(Color(0xffffdb00))) }

    @Composable
    fun Bar() { Box(Modifier.size(40.dp).background(Color(0xffff8100))) }

    @Composable
    fun Baz() { Box(Modifier.size(80.dp, 20.dp).background(Color(0xffff4400))) }

    var contentState: ContentState by remember { mutableStateOf(ContentState.Foo) }
    AnimatedContent(contentState) {
        when (it) {
            // Specifies the mapping between a given ContentState and a composable function.
            ContentState.Foo -> Foo()
            ContentState.Bar -> Bar()
            ContentState.Baz -> Baz()
        }
    }
}

private enum class ContentState { Foo, Bar, Baz }

@OptIn(ExperimentalAnimationApi::class)
@Suppress("UNUSED_VARIABLE")
@Sampled
fun AnimatedContentTransitionSpecSample() {
    // enum class CartState { Expanded, Collapsed }
    val transitionSpec: AnimatedContentScope<CartState>.() -> ContentTransform =
        {
            // Fade in with a delay so that it starts after fade out
            fadeIn(animationSpec = tween(150, delayMillis = 150))
                .with(fadeOut(animationSpec = tween(150)))
                .using(
                    SizeTransform { initialSize, targetSize ->
                        // Using different SizeTransform for different state change
                        if (CartState.Collapsed isTransitioningTo CartState.Expanded) {
                            keyframes {
                                durationMillis = 500
                                // Animate to full target width and by 200px in height at 150ms
                                IntSize(targetSize.width, initialSize.height + 200) at 150
                            }
                        } else {
                            keyframes {
                                durationMillis = 500
                                // Animate 1/2 the height without changing the width at 150ms.
                                // The width and rest of the height will be animated in the
                                // timeframe between 150ms and duration (i.e. 500ms)
                                IntSize(
                                    initialSize.width,
                                    (initialSize.height + targetSize.height) / 2
                                ) at 150
                            }
                        }
                    }
                )
        }
}

@Sampled
@Composable
@OptIn(ExperimentalAnimationApi::class)
fun TransitionExtensionAnimatedContentSample() {
    @Composable
    fun CollapsedCart() { /* Some content here */ }
    @Composable
    fun ExpandedCart() { /* Some content here */ }

    // enum class CartState { Expanded, Collapsed }
    var cartState by remember { mutableStateOf(CartState.Collapsed) }
    // Creates a transition here to animate the corner shape and content.
    val cartOpenTransition = updateTransition(cartState, "CartOpenTransition")
    val cornerSize by cartOpenTransition.animateDp(
        label = "cartCornerSize",
        transitionSpec = {
            when {
                CartState.Expanded isTransitioningTo CartState.Collapsed ->
                    tween(durationMillis = 433, delayMillis = 67)
                else ->
                    tween(durationMillis = 150)
            }
        }
    ) { if (it == CartState.Expanded) 0.dp else 24.dp }

    Surface(
        Modifier.shadow(8.dp, CutCornerShape(topStart = cornerSize))
            .clip(CutCornerShape(topStart = cornerSize)),
        color = Color(0xfffff0ea),
    ) {
        // Creates an AnimatedContent using the transition. This AnimatedContent will
        // derive its target state from cartOpenTransition.targetState. All the animations
        // created inside of AnimatedContent for size change, enter/exit will be added to the
        // Transition.
        cartOpenTransition.AnimatedContent(
            transitionSpec = {
                fadeIn(animationSpec = tween(150, delayMillis = 150))
                    .with(fadeOut(animationSpec = tween(150)))
                    .using(
                        SizeTransform { initialSize, targetSize ->
                            // Using different SizeTransform for different state change
                            if (CartState.Collapsed isTransitioningTo CartState.Expanded) {
                                keyframes {
                                    durationMillis = 500
                                    // Animate to full target width and by 200px in height at 150ms
                                    IntSize(targetSize.width, initialSize.height + 200) at 150
                                }
                            } else {
                                keyframes {
                                    durationMillis = 500
                                    // Animate 1/2 the height without changing the width at 150ms.
                                    // The width and rest of the height will be animated in the
                                    // timeframe between 150ms and duration (i.e. 500ms)
                                    IntSize(
                                        initialSize.width,
                                        (initialSize.height + targetSize.height) / 2
                                    ) at 150
                                }
                            }
                        }
                    ).apply {
                        targetContentZIndex = when (targetState) {
                            // This defines a relationship along z-axis during the momentary
                            // overlap as both incoming and outgoing content is on screen. This
                            // fixed zOrder will ensure that collapsed content will always be on
                            // top of the expanded content - it will come in on top, and
                            // disappear over the expanded content as well.
                            CartState.Expanded -> 1f
                            CartState.Collapsed -> 2f
                        }
                    }
            }
        ) {
            // This defines the mapping from state to composable. It's critical to use the state
            // parameter (i.e. `it`) that is passed into this block of code to ensure correct
            // content lookup.
            when (it) {
                CartState.Expanded -> ExpandedCart()
                CartState.Collapsed -> CollapsedCart()
            }
        }
    }
}

@Suppress("UNUSED_VARIABLE")
@Sampled
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SlideIntoContainerSample() {
    // enum class NestedMenuState { Level1, Level2, Level3 }
    // This is an example of creating a transitionSpec for navigating in a nested menu. The goal
    // is to 1) establish a z-order for different levels of the menu, and 2) imply a spatial
    // order between the menus via the different slide direction when navigating to child menu vs
    // parent menu. See the demos directory of the source code for a full demo.
    val transitionSpec: AnimatedContentScope<NestedMenuState>.() -> ContentTransform = {
        if (initialState < targetState) {
            // Going from parent menu to child menu, slide towards left
            slideIntoContainer(towards = SlideDirection.Left) with
                // Slide the parent out by 1/2 the amount required to be completely
                // out of the bounds. This creates a sense of child menu catching up. Since
                // the child menu has a higher z-order, it will cover the parent meu as it
                // comes in.
                slideOutOfContainer(
                    towards = SlideDirection.Left,
                    targetOffset = { offsetForFullSlide -> offsetForFullSlide / 2 }
                )
        } else {
            // Going from child menu to parent menu, slide towards right.
            // Slide parent by half amount compared to child menu to create an interesting
            // parallax visual effect.
            slideIntoContainer(
                towards = SlideDirection.Right,
                initialOffset = { offsetForFullSlide -> offsetForFullSlide / 2 }
            ) with
                slideOutOfContainer(towards = SlideDirection.Right)
        }.apply {
            // Here we can specify the zIndex for the target (i.e. incoming) content.
            targetContentZIndex = when (targetState) {
                NestedMenuState.Level1 -> 1f
                NestedMenuState.Level2 -> 2f
                NestedMenuState.Level3 -> 3f
            }
        }
    }
}

private enum class CartState {
    Expanded,
    Collapsed
}

private enum class NestedMenuState { Level1, Level2, Level3 }
