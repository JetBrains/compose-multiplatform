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

package androidx.compose.animation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.AmbientAnimationClock
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMaxBy

/**
 * [AnimatedVisibility] composable animates the appearance and disappearance of its content, as
 * [visible] value changes. Different [EnterTransition]s and [ExitTransition]s can be defined in
 * [enter] and [exit] for the appearance and disappearance animation. There are 3 types of
 * [EnterTransition] and [ExitTransition]: Fade, Expand/Shrink and Slide. The enter transitions
 * and exit transitions can be combined using `+`. The order of the combination does not matter,
 * as the transition animations will start simultaneously. See [EnterTransition] and
 * [ExitTransition] for details on the three types of transition. Here's an example of combining
 * all three types of transitions together:
 *
 * @sample androidx.compose.animation.samples.FullyLoadedTransition
 *
 * This composable function creates a custom [Layout] for its content. The size of the custom
 * layout is determined by the largest width and largest height of the children. All children
 * will be arranged in a box (aligned to the top start of the [Layout]).
 *
 * __Note__: Once the exit transition is finished, the [content] composable will be skipped (i.e.
 * the content will be removed from the tree, and disposed).
 *
 * By default, the enter transition will be a combination of fading in and expanding the content in
 * from the bottom end. And the exit transition will be shrinking the content towards the bottom
 * end while fading out. The expanding and shrinking will likely also animate the parent and
 * siblings if they rely on the size of appearing/disappearing content. When the
 * [AnimatedVisibility] composable is put in a [Row] or a [Column], the default enter and exit
 * transitions are tailored to that particular container. See [RowScope.AnimatedVisibility] and
 * [ColumnScope.AnimatedVisibility] for details.
 *
 * [initiallyVisible] defaults to the same value as [visible]. This means when the
 * [AnimatedVisibility] is first added to the tree, there is no appearing animation. If it is
 * desired to show an appearing animation for the first appearance of the content,
 * [initiallyVisible] can be set to false and [visible] to true.
 *
 * @param visible defines whether the content should be visible
 * @param modifier modifier for the [Layout] created to contain the [content]
 * @param enter [EnterTransition]s used for the appearing animation, fading in while expanding by
 *              default
 * @param exit [ExitTransition](s) used for the disappearing animation, fading out while
 *             shrinking by default
 * @param initiallyVisible controls whether the first appearance should be animated, defaulting
 *                         to match [visible] (i.e. not animating the first appearance)
 *
 * @see EnterTransition
 * @see ExitTransition
 * @see fadeIn
 * @see expandIn
 * @see fadeOut
 * @see shrinkOut
 * @see RowScope.AnimatedVisibility
 * @see ColumnScope.AnimatedVisibility
 */
@ExperimentalAnimationApi
@Composable
fun AnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandIn(),
    exit: ExitTransition = shrinkOut() + fadeOut(),
    initiallyVisible: Boolean = visible,
    content: @Composable () -> Unit
) {
    AnimatedVisibilityImpl(visible, modifier, enter, exit, initiallyVisible, content)
}

/**
 * [AnimatedVisibility] composable animates the appearance and disappearance of its content, as
 * [visible] value changes. Different [EnterTransition]s and [ExitTransition]s can be defined in
 * [enter] and [exit] for the appearance and disappearance animation. There are 3 types of
 * [EnterTransition] and [ExitTransition]: Fade, Expand/Shrink and Slide. The enter transitions
 * and exit transitions can be combined using `+`. The order of the combination does not matter,
 * as the transition animations will start simultaneously. See [EnterTransition] and
 * [ExitTransition] for details on the three types of transition. Here's an example of using
 * [RowScope.AnimatedVisibility] in a [Row]:
 *
 * @sample androidx.compose.animation.samples.AnimatedFloatingActionButton
 *
 * This composable function creates a custom [Layout] for its content. The size of the custom
 * layout is determined by the largest width and largest height of the children. All children
 * will be arranged in a box (aligned to the top start of the [Layout]).
 *
 * __Note__: Once the exit transition is finished, the [content] composable will be skipped (i.e.
 * the content will be removed from the tree, and disposed).
 *
 * By default, the enter transition will be a combination of fading in and expanding the content
 * horizontally. The end of the content will be the leading edge as the content expands to its
 * full width. And the exit transition will be shrinking the content with the end of the
 * content being the leading edge while fading out. The expanding and shrinking will likely also
 * animate the parent and siblings in the row since they rely on the size of appearing/disappearing
 * content.
 *
 * [initiallyVisible] defaults to the same value as [visible]. This means when the
 * [AnimatedVisibility] is first added to the tree, there is no appearing animation. If it is
 * desired to show an appearing animation for the first appearance of the content,
 * [initiallyVisible] can be set to false and [visible] to true.
 *
 * @param visible defines whether the content should be visible
 * @param modifier modifier for the [Layout] created to contain the [content]
 * @param enter [EnterTransition]s used for the appearing animation, fading in while expanding
 *              horizontally by default
 * @param exit [ExitTransition](s) used for the disappearing animation, fading out while
 *             shrinking horizontally by default
 * @param initiallyVisible controls whether the first appearance should be animated, defaulting
 *                         to match [visible] (i.e. not animating the first appearance)
 *
 * @see EnterTransition
 * @see ExitTransition
 * @see fadeIn
 * @see expandIn
 * @see fadeOut
 * @see shrinkOut
 * @see AnimatedVisibility
 * @see ColumnScope.AnimatedVisibility
 */
@ExperimentalAnimationApi
@Composable
fun RowScope.AnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandHorizontally(),
    exit: ExitTransition = fadeOut() + shrinkHorizontally(),
    initiallyVisible: Boolean = visible,
    content: @Composable () -> Unit
) {
    AnimatedVisibilityImpl(visible, modifier, enter, exit, initiallyVisible, content)
}

/**
 * [AnimatedVisibility] composable animates the appearance and disappearance of its content, as
 * [visible] value changes. Different [EnterTransition]s and [ExitTransition]s can be defined in
 * [enter] and [exit] for the appearance and disappearance animation. There are 3 types of
 * [EnterTransition] and [ExitTransition]: Fade, Expand/Shrink and Slide. The enter transitions
 * and exit transitions can be combined using `+`. The order of the combination does not matter,
 * as the transition animations will start simultaneously. See [EnterTransition] and
 * [ExitTransition] for details on the three types of transition. Here's an example of using
 * [ColumnScope.AnimatedVisibility] in a [Column]:
 *
 * @sample androidx.compose.animation.samples.ColumnAnimatedVisibilitySample
 *
 * This composable function creates a custom [Layout] for its content. The size of the custom
 * layout is determined by the largest width and largest height of the children. All children
 * will be arranged in a box (aligned to the top start of the [Layout]).
 *
 * __Note__: Once the exit transition is finished, the [content] composable will be skipped (i.e.
 * the content will be removed from the tree, and disposed).
 *
 * By default, the enter transition will be a combination of fading in and expanding the content
 * vertically in the [Column]. The bottom of the content will be the leading edge as the content
 * expands to its full height. And the exit transition will be shrinking the content with the
 * bottom of the content being the leading edge while fading out. The expanding and shrinking will
 * likely also animate the parent and siblings in the column since they rely on the size of
 * appearing/disappearing content.
 *
 * [initiallyVisible] defaults to the same value as [visible]. This means when the
 * [AnimatedVisibility] is first added to the tree, there is no appearing animation. If it is
 * desired to show an appearing animation for the first appearance of the content,
 * [initiallyVisible] can be set to false and [visible] to true.
 *
 * @param visible defines whether the content should be visible
 * @param modifier modifier for the [Layout] created to contain the [content]
 * @param enter [EnterTransition]s used for the appearing animation, fading in while expanding
 *              vertically by default
 * @param exit [ExitTransition](s) used for the disappearing animation, fading out while
 *             shrinking vertically by default
 * @param initiallyVisible controls whether the first appearance should be animated, defaulting
 *                         to match [visible] (i.e. not animating the first appearance)
 *
 * @see EnterTransition
 * @see ExitTransition
 * @see fadeIn
 * @see expandIn
 * @see fadeOut
 * @see shrinkOut
 * @see AnimatedVisibility
 * @see ColumnScope.AnimatedVisibility
 */
@ExperimentalAnimationApi
@Composable
fun ColumnScope.AnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandVertically(),
    exit: ExitTransition = fadeOut() + shrinkVertically(),
    initiallyVisible: Boolean = visible,
    content: @Composable () -> Unit
) {
    AnimatedVisibilityImpl(visible, modifier, enter, exit, initiallyVisible, content)
}

@ExperimentalAnimationApi
@Composable
private fun AnimatedVisibilityImpl(
    visible: Boolean,
    modifier: Modifier,
    enter: EnterTransition,
    exit: ExitTransition,
    initiallyVisible: Boolean,
    content: @Composable () -> Unit
) {

    // Set up initial transition states, based on the initial visibility.
    var transitionState by remember {
        mutableStateOf(if (initiallyVisible) AnimStates.Visible else AnimStates.Gone)
    }

    var isAnimating by remember { mutableStateOf(false) }

    // Update transition states, based on the current visibility.
    if (visible) {
        if (transitionState == AnimStates.Gone ||
            transitionState == AnimStates.Exiting
        ) {
            transitionState = AnimStates.Entering
            isAnimating = true
        }
    } else {
        if (transitionState == AnimStates.Visible ||
            transitionState == AnimStates.Entering
        ) {
            transitionState = AnimStates.Exiting
            isAnimating = true
        }
    }

    val clock = AmbientAnimationClock.current.asDisposableClock()
    val animations = remember(clock, enter, exit) {
        // TODO: Should we delay changing enter/exit after on-going animations are finished?
        TransitionAnimations(enter, exit, clock) {
            isAnimating = false
        }
    }
    animations.updateState(transitionState)

    // If the exit animation has finished, skip the child composable altogether
    if (transitionState == AnimStates.Gone) {
        return
    }

    Layout(
        content = content,
        modifier = modifier.then(animations.modifier)
    ) { measureables, constraints ->

        val placeables = measureables.map { it.measure(constraints) }
        val maxWidth: Int = placeables.fastMaxBy { it.width }?.width ?: 0
        val maxHeight = placeables.fastMaxBy { it.height }?.height ?: 0

        val offset: IntOffset
        val animatedSize: IntSize
        val animSize = animations.getAnimatedSize(
            IntSize(maxWidth, maxHeight)
        )
        if (animSize != null) {
            offset = animSize.first
            animatedSize = animSize.second
        } else {
            offset = IntOffset.Zero
            animatedSize = IntSize(maxWidth, maxHeight)
        }

        // If animation has finished update state
        if (!isAnimating) {
            if (transitionState == AnimStates.Exiting) {
                transitionState = AnimStates.Gone
            } else if (transitionState == AnimStates.Entering) {
                transitionState = AnimStates.Visible
            }
        }

        // Position the children.
        layout(animatedSize.width, animatedSize.height) {
            placeables.fastForEach {
                it.place(offset.x, offset.y)
            }
        }
    }
}
