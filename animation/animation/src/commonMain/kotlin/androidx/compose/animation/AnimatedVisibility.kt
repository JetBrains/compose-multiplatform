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

import androidx.compose.animation.EnterExitState.PostExit
import androidx.compose.animation.EnterExitState.PreEnter
import androidx.compose.animation.EnterExitState.Visible
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.InternalAnimationApi
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.createChildTransition
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMaxBy
import kotlinx.coroutines.flow.collect
import kotlin.jvm.JvmDefaultWithCompatibility

/**
 * [AnimatedVisibility] composable animates the appearance and disappearance of its content, as
 * [visible] value changes. Different [EnterTransition]s and [ExitTransition]s can be defined in
 * [enter] and [exit] for the appearance and disappearance animation. There are 4 types of
 * [EnterTransition] and [ExitTransition]: Fade, Expand/Shrink, Scale and Slide. The enter
 * transitions can be combined using `+`. Same for exit transitions. The order of the combination
 * does not matter, as the transition animations will start simultaneously. See [EnterTransition]
 * and [ExitTransition] for details on the three types of transition.
 *
 * Aside from these three types of [EnterTransition] and [ExitTransition], [AnimatedVisibility]
 * also supports custom enter/exit animations. Some use cases may benefit from custom enter/exit
 * animations on shape, scale, color, etc. Custom enter/exit animations can be created using the
 * `Transition<EnterExitState>` object from the [AnimatedVisibilityScope] (i.e.
 * [AnimatedVisibilityScope.transition]). See the second sample code snippet below for example.
 * These custom animations will be running alongside of the built-in animations specified in
 * [enter] and [exit]. In cases where the enter/exit animation needs to be completely customized,
 * [enter] and/or [exit] can be specified as [EnterTransition.None] and/or [ExitTransition.None]
 * as needed. [AnimatedVisibility] will wait until *all* of enter/exit animations to finish before
 * it considers itself idle. [content] will only be removed after all the (built-in and custom)
 * exit animations have finished.
 *
 * [AnimatedVisibility] creates a custom [Layout] for its content. The size of the custom
 * layout is determined by the largest width and largest height of the children. All children
 * will be aligned to the top start of the [Layout].
 *
 * __Note__: Once the exit transition is finished, the [content] composable will be removed
 * from the tree, and disposed. If there's a need to observe the state change of the enter/exit
 * transition and follow up additional action (e.g. remove data, sequential animation, etc),
 * consider the AnimatedVisibility API variant that takes a [MutableTransitionState] parameter.
 *
 * By default, the enter transition will be a combination of [fadeIn] and [expandIn] of the
 * content from the bottom end. And the exit transition will be shrinking the content towards the
 * bottom end while fading out (i.e. [fadeOut] + [shrinkOut]). The expanding and shrinking will
 * likely also animate the parent and siblings if they rely on the size of appearing/disappearing
 * content. When the [AnimatedVisibility] composable is put in a [Row] or a [Column], the default
 * enter and exit transitions are tailored to that particular container. See
 * [RowScope.AnimatedVisibility] and [ColumnScope.AnimatedVisibility] for details.
 *
 * Here are two examples of [AnimatedVisibility]: one using the built-in enter/exit transition, the
 * other using a custom enter/exit animation.
 *
 * @sample androidx.compose.animation.samples.FullyLoadedTransition
 *
 * The example blow shows how a custom enter/exit animation can be created using the Transition
 * object (i.e. Transition<EnterExitState>) from [AnimatedVisibilityScope].
 *
 * @sample androidx.compose.animation.samples.AnimatedVisibilityWithBooleanVisibleParamNoReceiver
 *
 * @param visible defines whether the content should be visible
 * @param modifier modifier for the [Layout] created to contain the [content]
 * @param enter EnterTransition(s) used for the appearing animation, fading in while expanding by
 *              default
 * @param exit ExitTransition(s) used for the disappearing animation, fading out while
 *             shrinking by default
 * @param content Content to appear or disappear based on the value of [visible]
 *
 * @see EnterTransition
 * @see ExitTransition
 * @see fadeIn
 * @see expandIn
 * @see fadeOut
 * @see shrinkOut
 * @see AnimatedVisibilityScope
 */
@Composable
fun AnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandIn(),
    exit: ExitTransition = shrinkOut() + fadeOut(),
    label: String = "AnimatedVisibility",
    content: @Composable() AnimatedVisibilityScope.() -> Unit
) {
    val transition = updateTransition(visible, label)
    AnimatedEnterExitImpl(transition, { it }, modifier, enter, exit, content)
}

/**
 * [RowScope.AnimatedVisibility] composable animates the appearance and disappearance of its
 * content when the [AnimatedVisibility] is in a [Row]. The default animations are tailored
 * specific to the [Row] layout. See more details below.
 *
 * Different [EnterTransition]s and [ExitTransition]s can be defined in
 * [enter] and [exit] for the appearance and disappearance animation. There are 4 types of
 * [EnterTransition] and [ExitTransition]: Fade, Expand/Shrink, Scale, and Slide. The enter
 * transitions can be combined using `+`. Same for exit transitions. The order of the combination
 * does not matter, as the transition animations will start simultaneously. See [EnterTransition]
 * and [ExitTransition] for details on the three types of transition.
 *
 * The default [enter] and [exit] transition is configured based on the horizontal layout of a
 * [Row]. [enter] defaults to a combination of fading in and expanding the content horizontally.
 * (The end of the content will be the leading edge as the content expands to its
 * full width.) And [exit] defaults to shrinking the content horizontally with the end of the
 * content being the leading edge while fading out. The expanding and shrinking will likely also
 * animate the parent and siblings in the row since they rely on the size of appearing/disappearing
 * content.
 *
 * Aside from these three types of [EnterTransition] and [ExitTransition], [AnimatedVisibility]
 * also supports custom enter/exit animations. Some use cases may benefit from custom enter/exit
 * animations on shape, scale, color, etc. Custom enter/exit animations can be created using the
 * `Transition<EnterExitState>` object from the [AnimatedVisibilityScope] (i.e.
 * [AnimatedVisibilityScope.transition]). See [EnterExitState] for an example of custom animations.
 * These custom animations will be running along side of the built-in animations specified in
 * [enter] and [exit]. In cases where the enter/exit animation needs to be completely customized,
 * [enter] and/or [exit] can be specified as [EnterTransition.None] and/or [ExitTransition.None]
 * as needed. [AnimatedVisibility] will wait until *all* of enter/exit animations to finish
 * before it considers itself idle. [content] will only be removed after all the (built-in and
 * custom) exit animations have finished.
 *
 * [AnimatedVisibility] creates a custom [Layout] for its content. The size of the custom
 * layout is determined by the largest width and largest height of the children. All children
 * will be aligned to the top start of the [Layout].
 *
 * __Note__: Once the exit transition is finished, the [content] composable will be removed
 * from the tree, and disposed. If there's a need to observe the state change of the enter/exit
 * transition and follow up additional action (e.g. remove data, sequential animation, etc),
 * consider the AnimatedVisibility API variant that takes a [MutableTransitionState] parameter.
 *
 * Here's an example of using [RowScope.AnimatedVisibility] in a [Row]:
 * @sample androidx.compose.animation.samples.AnimatedFloatingActionButton
 *
 * @param visible defines whether the content should be visible
 * @param modifier modifier for the [Layout] created to contain the [content]
 * @param enter EnterTransition(s) used for the appearing animation, fading in while expanding
 *              horizontally by default
 * @param exit ExitTransition(s) used for the disappearing animation, fading out while
 *             shrinking horizontally by default
 * @param content Content to appear or disappear based on the value of [visible]
 *
 * @see EnterTransition
 * @see ExitTransition
 * @see fadeIn
 * @see expandIn
 * @see fadeOut
 * @see shrinkOut
 * @see AnimatedVisibility
 * @see ColumnScope.AnimatedVisibility
 * @see AnimatedVisibilityScope
 */
@Composable
fun RowScope.AnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandHorizontally(),
    exit: ExitTransition = fadeOut() + shrinkHorizontally(),
    label: String = "AnimatedVisibility",
    content: @Composable() AnimatedVisibilityScope.() -> Unit
) {
    val transition = updateTransition(visible, label)
    AnimatedEnterExitImpl(transition, { it }, modifier, enter, exit, content)
}

/**
 * [ColumnScope.AnimatedVisibility] composable animates the appearance and disappearance of its
 * content when the [AnimatedVisibility] is in a [Column]. The default animations are tailored
 * specific to the [Column] layout. See more details below.
 *
 * Different [EnterTransition]s and [ExitTransition]s can be defined in
 * [enter] and [exit] for the appearance and disappearance animation. There are 4 types of
 * [EnterTransition] and [ExitTransition]: Fade, Expand/Shrink, Scale and Slide. The enter
 * transitions can be combined using `+`. Same for exit transitions. The order of the combination
 * does not matter, as the transition animations will start simultaneously. See [EnterTransition]
 * and [ExitTransition] for details on the three types of transition.
 *
 * The default [enter] and [exit] transition is configured based on the vertical layout of a
 * [Column]. [enter] defaults to a combination of fading in and expanding the content vertically.
 * (The bottom of the content will be the leading edge as the content expands to its full height.)
 * And the [exit] defaults to shrinking the content vertically with the bottom of the content being
 * the leading edge while fading out. The expanding and shrinking will likely also animate the
 * parent and siblings in the column since they rely on the size of appearing/disappearing content.
 *
 * Aside from these three types of [EnterTransition] and [ExitTransition], [AnimatedVisibility]
 * also supports custom enter/exit animations. Some use cases may benefit from custom enter/exit
 * animations on shape, scale, color, etc. Custom enter/exit animations can be created using the
 * `Transition<EnterExitState>` object from the [AnimatedVisibilityScope] (i.e.
 * [AnimatedVisibilityScope.transition]). See [EnterExitState] for an example of custom animations.
 * These custom animations will be running along side of the built-in animations specified in
 * [enter] and [exit]. In cases where the enter/exit animation needs to be completely customized,
 * [enter] and/or [exit] can be specified as [EnterTransition.None] and/or [ExitTransition.None]
 * as needed. [AnimatedVisibility] will wait until *all* of enter/exit animations to finish
 * before it considers itself idle. [content] will only be removed after all the (built-in and
 * custom) exit animations have finished.
 *
 * [AnimatedVisibility] creates a custom [Layout] for its content. The size of the custom
 * layout is determined by the largest width and largest height of the children. All children
 * will be aligned to the top start of the [Layout].
 *
 * __Note__: Once the exit transition is finished, the [content] composable will be removed
 * from the tree, and disposed. If there's a need to observe the state change of the enter/exit
 * transition and follow up additional action (e.g. remove data, sequential animation, etc),
 * consider the AnimatedVisibility API variant that takes a [MutableTransitionState] parameter.
 *
 * Here's an example of using [ColumnScope.AnimatedVisibility] in a [Column]:
 * @sample androidx.compose.animation.samples.ColumnAnimatedVisibilitySample
 *
 * @param visible defines whether the content should be visible
 * @param modifier modifier for the [Layout] created to contain the [content]
 * @param enter EnterTransition(s) used for the appearing animation, fading in while expanding
 *              vertically by default
 * @param exit ExitTransition(s) used for the disappearing animation, fading out while
 *             shrinking vertically by default
 * @param content Content to appear or disappear based on the value of [visible]
 *
 * @see EnterTransition
 * @see ExitTransition
 * @see fadeIn
 * @see expandIn
 * @see fadeOut
 * @see shrinkOut
 * @see AnimatedVisibility
 * @see AnimatedVisibilityScope
 */
@Composable
fun ColumnScope.AnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandVertically(),
    exit: ExitTransition = fadeOut() + shrinkVertically(),
    label: String = "AnimatedVisibility",
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val transition = updateTransition(visible, label)
    AnimatedEnterExitImpl(transition, { it }, modifier, enter, exit, content)
}

/**
 * [EnterExitState] contains the three states that are involved in the enter and exit transition
 * of [AnimatedVisibility]. More specifically, [PreEnter] and [Visible] defines the initial and
 * target state of an *enter* transition, whereas [Visible] and [PostExit] are the initial and
 * target state of an *exit* transition.
 *
 * See blow for an example of custom enter/exit animation in [AnimatedVisibility] using
 * `Transition<EnterExitState>` (i.e. [AnimatedVisibilityScope.transition]):
 *
 * @sample androidx.compose.animation.samples.AnimatedVisibilityWithBooleanVisibleParamNoReceiver
 * @see AnimatedVisibility
 */
@ExperimentalAnimationApi
enum class EnterExitState {
    /**
     * The initial state of a custom enter animation in [AnimatedVisibility]..
     */
    PreEnter,

    /**
     * The `Visible` state is the target state of a custom *enter* animation, also the initial
     * state of a custom *exit* animation in [AnimatedVisibility].
     */
    Visible,

    /**
     * Target state of a custom *exit* animation in [AnimatedVisibility].
     */
    PostExit
}

/**
 * [AnimatedVisibility] composable animates the appearance and disappearance of its content, as
 * [visibleState]'s [targetState][MutableTransitionState.targetState] changes. The [visibleState]
 * can also be used to observe the state of [AnimatedVisibility]. For example:
 * `visibleState.isIdle` indicates whether all the animations have finished in [AnimatedVisibility],
 * and `visibleState.currentState` returns the initial state of the current animations.
 *
 * Different [EnterTransition]s and [ExitTransition]s can be defined in
 * [enter] and [exit] for the appearance and disappearance animation. There are 4 types of
 * [EnterTransition] and [ExitTransition]: Fade, Expand/Shrink, Scale and Slide. The enter
 * transitions can be combined using `+`. Same for exit transitions. The order of the combination
 * does not matter, as the transition animations will start simultaneously. See [EnterTransition]
 * and [ExitTransition] for details on the three types of transition.
 *
 * Aside from these three types of [EnterTransition] and [ExitTransition], [AnimatedVisibility]
 * also supports custom enter/exit animations. Some use cases may benefit from custom enter/exit
 * animations on shape, scale, color, etc. Custom enter/exit animations can be created using the
 * `Transition<EnterExitState>` object from the [AnimatedVisibilityScope] (i.e.
 * [AnimatedVisibilityScope.transition]). See [EnterExitState] for an example of custom animations.
 * These custom animations will be running along side of the built-in animations specified in
 * [enter] and [exit]. In cases where the enter/exit animation needs to be completely customized,
 * [enter] and/or [exit] can be specified as [EnterTransition.None] and/or [ExitTransition.None]
 * as needed. [AnimatedVisibility] will wait until *all* of enter/exit animations to finish
 * before it considers itself idle. [content] will only be removed after all the (built-in and
 * custom) exit animations have finished.
 *
 * [AnimatedVisibility] creates a custom [Layout] for its content. The size of the custom
 * layout is determined by the largest width and largest height of the children. All children
 * will be aligned to the top start of the [Layout].
 *
 * __Note__: Once the exit transition is finished, the [content] composable will be removed
 * from the tree, and disposed. Both `currentState` and `targetState` will be `false` for
 * [visibleState].
 *
 * By default, the enter transition will be a combination of [fadeIn] and [expandIn] of the
 * content from the bottom end. And the exit transition will be shrinking the content towards the
 * bottom end while fading out (i.e. [fadeOut] + [shrinkOut]). The expanding and shrinking will
 * likely also animate the parent and siblings if they rely on the size of appearing/disappearing
 * content. When the [AnimatedVisibility] composable is put in a [Row] or a [Column], the default
 * enter and exit transitions are tailored to that particular container. See
 * [RowScope.AnimatedVisibility] and [ColumnScope.AnimatedVisibility] for details.
 *
 * @sample androidx.compose.animation.samples.AnimatedVisibilityLazyColumnSample
 *
 * @param visibleState defines whether the content should be visible
 * @param modifier modifier for the [Layout] created to contain the [content]
 * @param enter EnterTransition(s) used for the appearing animation, fading in while expanding by
 *              default
 * @param exit ExitTransition(s) used for the disappearing animation, fading out while
 *             shrinking by default
 * @param content Content to appear or disappear based on the value of [visibleState]
 *
 * @see EnterTransition
 * @see ExitTransition
 * @see fadeIn
 * @see expandIn
 * @see fadeOut
 * @see shrinkOut
 * @see AnimatedVisibility
 * @see Transition.AnimatedVisibility
 * @see AnimatedVisibilityScope
 */
@Composable
fun AnimatedVisibility(
    visibleState: MutableTransitionState<Boolean>,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandIn(),
    exit: ExitTransition = fadeOut() + shrinkOut(),
    label: String = "AnimatedVisibility",
    content: @Composable() AnimatedVisibilityScope.() -> Unit
) {
    val transition = updateTransition(visibleState, label)
    AnimatedEnterExitImpl(transition, { it }, modifier, enter, exit, content)
}

/**
 * [RowScope.AnimatedVisibility] composable animates the appearance and disappearance of its
 * content as [visibleState]'s [targetState][MutableTransitionState.targetState] changes. The
 * default [enter] and [exit] transitions are tailored specific to the [Row] layout. See more
 * details below. The [visibleState] can also be used to observe the state of [AnimatedVisibility].
 * For example: `visibleState.isIdle` indicates whether all the animations have finished in
 * [AnimatedVisibility], and `visibleState.currentState` returns the initial state of the current
 * animations.
 *
 * Different [EnterTransition]s and [ExitTransition]s can be defined in
 * [enter] and [exit] for the appearance and disappearance animation. There are 4 types of
 * [EnterTransition] and [ExitTransition]: Fade, Expand/Shrink, Scale and Slide. The enter
 * transitions can be combined using `+`. Same for exit transitions. The order of the combination
 * does not matter, as the transition animations will start simultaneously. See [EnterTransition]
 * and [ExitTransition] for details on the three types of transition.
 *
 * The default [enter] and [exit] transition is configured based on the horizontal layout of a
 * [Row]. [enter] defaults to a combination of fading in and expanding the content horizontally.
 * (The end of the content will be the leading edge as the content expands to its
 * full width.) And [exit] defaults to shrinking the content horizontally with the end of the
 * content being the leading edge while fading out. The expanding and shrinking will likely also
 * animate the parent and siblings in the row since they rely on the size of appearing/disappearing
 * content.
 *
 * Aside from these three types of [EnterTransition] and [ExitTransition], [AnimatedVisibility]
 * also supports custom enter/exit animations. Some use cases may benefit from custom enter/exit
 * animations on shape, scale, color, etc. Custom enter/exit animations can be created using the
 * `Transition<EnterExitState>` object from the [AnimatedVisibilityScope] (i.e.
 * [AnimatedVisibilityScope.transition]). See [EnterExitState] for an example of custom animations.
 * These custom animations will be running along side of the built-in animations specified in
 * [enter] and [exit]. In cases where the enter/exit animation needs to be completely customized,
 * [enter] and/or [exit] can be specified as [EnterTransition.None] and/or [ExitTransition.None]
 * as needed. [AnimatedVisibility] will wait until *all* of enter/exit animations to finish
 * before it considers itself idle. [content] will only be removed after all the (built-in and
 * custom) exit animations have finished.
 *
 * [AnimatedVisibility] creates a custom [Layout] for its content. The size of the custom
 * layout is determined by the largest width and largest height of the children. All children
 * will be aligned to the top start of the [Layout].
 *
 * __Note__: Once the exit transition is finished, the [content] composable will be removed
 * from the tree, and disposed. Both `currentState` and `targetState` will be `false` for
 * [visibleState].
 *
 * @param visibleState defines whether the content should be visible
 * @param modifier modifier for the [Layout] created to contain the [content]
 * @param enter EnterTransition(s) used for the appearing animation, fading in while expanding
 *              vertically by default
 * @param exit ExitTransition(s) used for the disappearing animation, fading out while
 *             shrinking vertically by default
 * @param content Content to appear or disappear based on the value of [visibleState]
 *
 * @see EnterTransition
 * @see ExitTransition
 * @see fadeIn
 * @see expandIn
 * @see fadeOut
 * @see shrinkOut
 * @see AnimatedVisibility
 * @see Transition.AnimatedVisibility
 * @see AnimatedVisibilityScope
 */
@Composable
fun RowScope.AnimatedVisibility(
    visibleState: MutableTransitionState<Boolean>,
    modifier: Modifier = Modifier,
    enter: EnterTransition = expandHorizontally() + fadeIn(),
    exit: ExitTransition = shrinkHorizontally() + fadeOut(),
    label: String = "AnimatedVisibility",
    content: @Composable() AnimatedVisibilityScope.() -> Unit
) {
    val transition = updateTransition(visibleState, label)
    AnimatedEnterExitImpl(transition, { it }, modifier, enter, exit, content)
}

/**
 * [ColumnScope.AnimatedVisibility] composable animates the appearance and disappearance of its
 * content as [visibleState]'s [targetState][MutableTransitionState.targetState] changes. The
 * default [enter] and [exit] transitions are tailored specific to the [Column] layout. See more
 * details below. The [visibleState] can also be used to observe the state of [AnimatedVisibility].
 * For example: `visibleState.isIdle` indicates whether all the animations have finished in
 * [AnimatedVisibility], and `visibleState.currentState` returns the initial state of the current
 * animations.
 *
 * Different [EnterTransition]s and [ExitTransition]s can be defined in
 * [enter] and [exit] for the appearance and disappearance animation. There are 4 types of
 * [EnterTransition] and [ExitTransition]: Fade, Expand/Shrink, Scale and Slide. The enter
 * transitions can be combined using `+`. Same for exit transitions. The order of the combination
 * does not matter, as the transition animations will start simultaneously. See [EnterTransition]
 * and [ExitTransition] for details on the three types of transition.
 *
 * The default [enter] and [exit] transition is configured based on the vertical layout of a
 * [Column]. [enter] defaults to a combination of fading in and expanding the content vertically.
 * (The bottom of the content will be the leading edge as the content expands to its full height.)
 * And the [exit] defaults to shrinking the content vertically with the bottom of the content being
 * the leading edge while fading out. The expanding and shrinking will likely also animate the
 * parent and siblings in the column since they rely on the size of appearing/disappearing content.
 *
 * Aside from these three types of [EnterTransition] and [ExitTransition], [AnimatedVisibility]
 * also supports custom enter/exit animations. Some use cases may benefit from custom enter/exit
 * animations on shape, scale, color, etc. Custom enter/exit animations can be created using the
 * `Transition<EnterExitState>` object from the [AnimatedVisibilityScope] (i.e.
 * [AnimatedVisibilityScope.transition]). See [EnterExitState] for an example of custom animations.
 * These custom animations will be running along side of the built-in animations specified in
 * [enter] and [exit]. In cases where the enter/exit animation needs to be completely customized,
 * [enter] and/or [exit] can be specified as [EnterTransition.None] and/or [ExitTransition.None]
 * as needed. [AnimatedVisibility] will wait until *all* of enter/exit animations to finish
 * before it considers itself idle. [content] will only be removed after all the (built-in and
 * custom) exit animations have finished.
 *
 * [AnimatedVisibility] creates a custom [Layout] for its content. The size of the custom
 * layout is determined by the largest width and largest height of the children. All children
 * will be aligned to the top start of the [Layout].
 *
 * __Note__: Once the exit transition is finished, the [content] composable will be removed
 * from the tree, and disposed. Both `currentState` and `targetState` will be `false` for
 * [visibleState].
 *
 * @sample androidx.compose.animation.samples.AVColumnScopeWithMutableTransitionState
 *
 * @param visibleState defines whether the content should be visible
 * @param modifier modifier for the [Layout] created to contain the [content]
 * @param enter EnterTransition(s) used for the appearing animation, fading in while expanding
 *              vertically by default
 * @param exit ExitTransition(s) used for the disappearing animation, fading out while
 *             shrinking vertically by default
 * @param content Content to appear or disappear based on of [visibleState]
 *
 * @see EnterTransition
 * @see ExitTransition
 * @see fadeIn
 * @see expandIn
 * @see fadeOut
 * @see shrinkOut
 * @see AnimatedVisibility
 * @see Transition.AnimatedVisibility
 * @see AnimatedVisibilityScope
 */
@Composable
fun ColumnScope.AnimatedVisibility(
    visibleState: MutableTransitionState<Boolean>,
    modifier: Modifier = Modifier,
    enter: EnterTransition = expandVertically() + fadeIn(),
    exit: ExitTransition = shrinkVertically() + fadeOut(),
    label: String = "AnimatedVisibility",
    content: @Composable() AnimatedVisibilityScope.() -> Unit
) {
    val transition = updateTransition(visibleState, label)
    AnimatedEnterExitImpl(transition, { it }, modifier, enter, exit, content)
}

/**
 * This extension function creates an [AnimatedVisibility] composable as a child Transition of
 * the given Transition. This means: 1) the enter/exit transition is now triggered by the provided
 * [Transition]'s [targetState][Transition.targetState] change. When the targetState changes, the
 * visibility will be derived using the [visible] lambda and [Transition.targetState]. 2)
 * The enter/exit transitions, as well as any custom enter/exit animations defined in
 * [AnimatedVisibility] are now hoisted to the parent Transition. The parent Transition will wait
 * for all of them to finish before it considers itself finished (i.e. [Transition.currentState]
 * = [Transition.targetState]), and subsequently removes the content in the exit case.
 *
 * Different [EnterTransition]s and [ExitTransition]s can be defined in
 * [enter] and [exit] for the appearance and disappearance animation. There are 4 types of
 * [EnterTransition] and [ExitTransition]: Fade, Expand/Shrink, Scale and Slide. The enter
 * transitions can be combined using `+`. Same for exit transitions. The order of the combination
 * does not matter, as the transition animations will start simultaneously. See [EnterTransition]
 * and [ExitTransition] for details on the three types of transition.
 *
 * Aside from these three types of [EnterTransition] and [ExitTransition], [AnimatedVisibility]
 * also supports custom enter/exit animations. Some use cases may benefit from custom enter/exit
 * animations on shape, scale, color, etc. Custom enter/exit animations can be created using the
 * `Transition<EnterExitState>` object from the [AnimatedVisibilityScope] (i.e.
 * [AnimatedVisibilityScope.transition]). See [EnterExitState] for an example of custom animations.
 * These custom animations will be running along side of the built-in animations specified in
 * [enter] and [exit]. In cases where the enter/exit animation needs to be completely customized,
 * [enter] and/or [exit] can be specified as [EnterTransition.None] and/or [ExitTransition.None]
 * as needed. [AnimatedVisibility] will wait until *all* of enter/exit animations to finish
 * before it considers itself idle. [content] will only be removed after all the (built-in and
 * custom) exit animations have finished.
 *
 * [AnimatedVisibility] creates a custom [Layout] for its content. The size of the custom
 * layout is determined by the largest width and largest height of the children. All children
 * will be aligned to the top start of the [Layout].
 *
 * __Note__: Once the exit transition is finished, the [content] composable will be removed
 * from the tree, and disposed.
 *
 * By default, the enter transition will be a combination of [fadeIn] and [expandIn] of the
 * content from the bottom end. And the exit transition will be shrinking the content towards the
 * bottom end while fading out (i.e. [fadeOut] + [shrinkOut]). The expanding and shrinking will
 * likely also animate the parent and siblings if they rely on the size of appearing/disappearing
 * content.
 *
 * @sample androidx.compose.animation.samples.AddAnimatedVisibilityToGenericTransitionSample
 *
 * @param visible defines whether the content should be visible based on transition state T
 * @param modifier modifier for the [Layout] created to contain the [content]
 * @param enter EnterTransition(s) used for the appearing animation, fading in while expanding
 *              vertically by default
 * @param exit ExitTransition(s) used for the disappearing animation, fading out while
 *             shrinking vertically by default
 * @param content Content to appear or disappear based on the visibility derived from the
 *                [Transition.targetState] and the provided [visible] lambda
 *
 * @see EnterTransition
 * @see ExitTransition
 * @see fadeIn
 * @see expandIn
 * @see fadeOut
 * @see shrinkOut
 * @see AnimatedVisibilityScope
 * @see Transition.AnimatedVisibility
 */
@ExperimentalAnimationApi
@Composable
fun <T> Transition<T>.AnimatedVisibility(
    visible: (T) -> Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandIn(),
    exit: ExitTransition = shrinkOut() + fadeOut(),
    content: @Composable() AnimatedVisibilityScope.() -> Unit
) = AnimatedEnterExitImpl(this, visible, modifier, enter, exit, content)

/**
 * This is the scope for the content of [AnimatedVisibility]. In this scope, direct and
 * indirect children of [AnimatedVisibility] will be able to define their own enter/exit
 * transitions using the built-in options via [Modifier.animateEnterExit]. They will also be able
 * define custom enter/exit animations using the [transition] object. [AnimatedVisibility] will
 * ensure both custom and built-in enter/exit animations finish before it considers itself idle,
 * and subsequently removes its content in the case of exit.
 *
 * __Note:__ Custom enter/exit animations that are created *independent* of the
 * [AnimatedVisibilityScope.transition] will have no guarantee to finish when
 * exiting, as [AnimatedVisibility] would have no visibility of such animations.
 *
 * @sample androidx.compose.animation.samples.AVScopeAnimateEnterExit
 */
@JvmDefaultWithCompatibility
interface AnimatedVisibilityScope {
    /**
     * [transition] allows custom enter/exit animations to be specified. It will run simultaneously
     * with the built-in enter/exit transitions specified in [AnimatedVisibility].
     */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @get:ExperimentalAnimationApi
    @ExperimentalAnimationApi
    val transition: Transition<EnterExitState>

    /**
     * [animateEnterExit] modifier can be used for any direct or indirect children of
     * [AnimatedVisibility] to create a different enter/exit animation than what's specified in
     * [AnimatedVisibility]. The visual effect of these children will be a combination of the
     * [AnimatedVisibility]'s animation and their own enter/exit animations.
     *
     * [enter] and [exit] defines different [EnterTransition]s and [ExitTransition]s that will be
     * used for the appearance and disappearance animation. There are 4 types of
     * [EnterTransition] and [ExitTransition]: Fade, Expand/Shrink, Scale and Slide. The enter
     * transitions can be combined using `+`. Same for exit transitions. The order of the combination
     * does not matter, as the transition animations will start simultaneously. See [EnterTransition]
     * and [ExitTransition] for details on the three types of transition.
     *
     * By default, the enter transition will be a combination of [fadeIn] and [expandIn] of the
     * content from the bottom end. And the exit transition will be shrinking the content towards
     * the bottom end while fading out (i.e. [fadeOut] + [shrinkOut]). The expanding and shrinking
     * will likely also animate the parent and siblings if they rely on the size of
     * appearing/disappearing content.
     *
     * In some cases it may be desirable to have [AnimatedVisibility] apply no animation at all for
     * enter and/or exit, such that children of [AnimatedVisibility] can each have their distinct
     * animations. To achieve this, [EnterTransition.None] and/or [ExitTransition.None] can be
     * used for [AnimatedVisibility].
     *
     * @sample androidx.compose.animation.samples.AnimateEnterExitPartialContent
     */
    @ExperimentalAnimationApi
    fun Modifier.animateEnterExit(
        enter: EnterTransition = fadeIn() + expandIn(),
        exit: ExitTransition = fadeOut() + shrinkOut(),
        label: String = "animateEnterExit"
    ): Modifier = composed(
        inspectorInfo = debugInspectorInfo {
            name = "animateEnterExit"
            properties["enter"] = enter
            properties["exit"] = exit
            properties["label"] = label
        }
    ) {
        this.then(transition.createModifier(enter, exit, label))
    }
}

@ExperimentalAnimationApi
internal class AnimatedVisibilityScopeImpl internal constructor(
    transition: Transition<EnterExitState>
) : AnimatedVisibilityScope {
    override var transition = transition
    internal val targetSize = mutableStateOf(IntSize.Zero)
}

@ExperimentalAnimationApi
@Composable
@Deprecated(
    "AnimatedVisibility no longer accepts initiallyVisible as a parameter, please use " +
        "AnimatedVisibility(MutableTransitionState, Modifier, ...) API instead",
    replaceWith = ReplaceWith(
        "AnimatedVisibility(" +
            "transitionState = remember { MutableTransitionState(initiallyVisible) }\n" +
            ".apply { targetState = visible },\n" +
            "modifier = modifier,\n" +
            "enter = enter,\n" +
            "exit = exit) {\n" +
            "content() \n" +
            "}",
        "androidx.compose.animation.core.MutableTransitionState"
    )
)
fun AnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition,
    exit: ExitTransition,
    initiallyVisible: Boolean,
    content: @Composable () -> Unit
) = AnimatedVisibility(
    visibleState = remember { MutableTransitionState(initiallyVisible) }
        .apply { targetState = visible },
    modifier = modifier,
    enter = enter,
    exit = exit
) {
    content()
}

// RowScope and ColumnScope AnimatedEnterExit extensions and AnimatedEnterExit without a receiver
// converge here.
@OptIn(
    ExperimentalTransitionApi::class,
    InternalAnimationApi::class,
    ExperimentalAnimationApi::class
)
@Composable
private fun <T> AnimatedEnterExitImpl(
    transition: Transition<T>,
    visible: (T) -> Boolean,
    modifier: Modifier,
    enter: EnterTransition,
    exit: ExitTransition,
    content: @Composable() AnimatedVisibilityScope.() -> Unit
) {
    val isAnimationVisible = remember(transition) {
        mutableStateOf(visible(transition.currentState))
    }

    if (visible(transition.targetState) || isAnimationVisible.value || transition.isSeeking) {
        val childTransition = transition.createChildTransition(label = "EnterExitTransition") {
            transition.targetEnterExit(visible, it)
        }

        LaunchedEffect(childTransition) {
            snapshotFlow {
                childTransition.currentState == EnterExitState.Visible ||
                    childTransition.targetState == EnterExitState.Visible
            }.collect {
                isAnimationVisible.value = it
            }
        }

        AnimatedEnterExitImpl(
            childTransition,
            modifier,
            enter = enter,
            exit = exit,
            content = content
        )
    }
}

@OptIn(ExperimentalTransitionApi::class, ExperimentalAnimationApi::class)
@Composable
private inline fun AnimatedEnterExitImpl(
    transition: Transition<EnterExitState>,
    modifier: Modifier,
    enter: EnterTransition,
    exit: ExitTransition,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    // TODO: Get some feedback on whether there's a need to observe this state change in user
    //  code. If there is, this if check will need to be moved to measure stage, along with some
    //  structural changes.
    if (transition.currentState == EnterExitState.Visible ||
        transition.targetState == EnterExitState.Visible
    ) {
        val scope = remember(transition) { AnimatedVisibilityScopeImpl(transition) }
        Layout(
            content = { scope.content() },
            modifier = modifier.then(transition.createModifier(enter, exit, "Built-in")),
            measurePolicy = remember { AnimatedEnterExitMeasurePolicy(scope) }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
private class AnimatedEnterExitMeasurePolicy(
    val scope: AnimatedVisibilityScopeImpl
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val placeables = measurables.map { it.measure(constraints) }
        val maxWidth: Int = placeables.fastMaxBy { it.width }?.width ?: 0
        val maxHeight = placeables.fastMaxBy { it.height }?.height ?: 0
        // Position the children.
        scope.targetSize.value = IntSize(maxWidth, maxHeight)
        return layout(maxWidth, maxHeight) {
            placeables.fastForEach {
                it.place(0, 0)
            }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ) = measurables.asSequence().map { it.minIntrinsicWidth(height) }.maxOrNull() ?: 0

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ) = measurables.asSequence().map { it.minIntrinsicHeight(width) }.maxOrNull() ?: 0

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ) = measurables.asSequence().map { it.maxIntrinsicWidth(height) }.maxOrNull() ?: 0

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ) = measurables.asSequence().map { it.maxIntrinsicHeight(width) }.maxOrNull() ?: 0
}

// This converts Boolean visible to EnterExitState
@OptIn(InternalAnimationApi::class, ExperimentalAnimationApi::class)
@Composable
private fun <T> Transition<T>.targetEnterExit(
    visible: (T) -> Boolean,
    targetState: T
): EnterExitState = key(this) {

    if (this.isSeeking) {
        if (visible(targetState)) {
            Visible
        } else {
            if (visible(this.currentState)) {
                PostExit
            } else {
                PreEnter
            }
        }
    } else {
        val hasBeenVisible = remember { mutableStateOf(false) }
        if (visible(currentState)) {
            hasBeenVisible.value = true
        }
        if (visible(targetState)) {
            EnterExitState.Visible
        } else {
            // If never been visible, visible = false means PreEnter, otherwise PostExit
            if (hasBeenVisible.value) {
                EnterExitState.PostExit
            } else {
                EnterExitState.PreEnter
            }
        }
    }
}
