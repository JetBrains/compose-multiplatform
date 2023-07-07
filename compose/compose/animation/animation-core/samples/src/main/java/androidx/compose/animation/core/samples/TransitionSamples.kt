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

package androidx.compose.animation.core.samples

import androidx.annotation.Sampled
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.createChildTransition
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun GestureAnimationSample() {
    // enum class ComponentState { Pressed, Released }
    var useRed by remember { mutableStateOf(false) }
    var toState by remember { mutableStateOf(ComponentState.Released) }
    val modifier = Modifier.pointerInput(Unit) {
        detectTapGestures(
            onPress = {
                toState = ComponentState.Pressed
                tryAwaitRelease()
                toState = ComponentState.Released
            }
        )
    }

    // Defines a transition of `ComponentState`, and updates the transition when the provided
    // [targetState] changes. The tran
    // sition will run all of the child animations towards the new
    // [targetState] in response to the [targetState] change.
    val transition: Transition<ComponentState> = updateTransition(targetState = toState)
    // Defines a float animation as a child animation the transition. The current animation value
    // can be read from the returned State<Float>.
    val scale: Float by transition.animateFloat(
        // Defines a transition spec that uses the same low-stiffness spring for *all*
        // transitions of this float, no matter what the target is.
        transitionSpec = { spring(stiffness = 50f) }
    ) { state ->
        // This code block declares a mapping from state to value.
        if (state == ComponentState.Pressed) 3f else 1f
    }

    // Defines a color animation as a child animation of the transition.
    val color: Color by transition.animateColor(
        transitionSpec = {
            when {
                ComponentState.Pressed isTransitioningTo ComponentState.Released ->
                    // Uses spring for the transition going from pressed to released
                    spring(stiffness = 50f)
                else ->
                    // Uses tween for all the other transitions. (In this case there is
                    // only one other transition. i.e. released -> pressed.)
                    tween(durationMillis = 500)
            }
        }
    ) { state ->
        when (state) {
            // Similar to the float animation, we need to declare the target values
            // for each state. In this code block we can access theme colors.
            ComponentState.Pressed -> MaterialTheme.colors.primary
            // We can also have the target value depend on other mutableStates,
            // such as `useRed` here. Whenever the target value changes, transition
            // will automatically animate to the new value even if it has already
            // arrived at its target state.
            ComponentState.Released -> if (useRed) Color.Red else MaterialTheme.colors.secondary
        }
    }
    Column {
        Button(
            modifier = Modifier.padding(10.dp).align(Alignment.CenterHorizontally),
            onClick = { useRed = !useRed }
        ) {
            Text("Change Color")
        }
        Box(
            modifier.fillMaxSize().wrapContentSize(Alignment.Center)
                .size((100 * scale).dp).background(color)
        )
    }
}

private enum class ComponentState { Pressed, Released }
private enum class ButtonStatus { Initial, Pressed, Released }

@Sampled
@Composable
fun AnimateFloatSample() {
    // enum class ButtonStatus {Initial, Pressed, Released}
    @Composable
    fun AnimateAlphaAndScale(
        modifier: Modifier,
        transition: Transition<ButtonStatus>
    ) {
        // Defines a float animation as a child animation of transition. This allows the
        // transition to manage the states of this animation. The returned State<Float> from the
        // [animateFloat] function is used here as a property delegate.
        // This float animation will use the default [spring] for all transition destinations, as
        // specified by the default `transitionSpec`.
        val scale: Float by transition.animateFloat { state ->
            if (state == ButtonStatus.Pressed) 1.2f else 1f
        }

        // Alternatively, we can specify different animation specs based on the initial state and
        // target state of the a transition run using `transitionSpec`.
        val alpha: Float by transition.animateFloat(
            transitionSpec = {
                when {
                    ButtonStatus.Initial isTransitioningTo ButtonStatus.Pressed -> {
                        keyframes {
                            durationMillis = 225
                            0f at 0 // optional
                            0.3f at 75
                            0.2f at 225 // optional
                        }
                    }
                    ButtonStatus.Pressed isTransitioningTo ButtonStatus.Released -> {
                        tween(durationMillis = 220)
                    }
                    else -> {
                        snap()
                    }
                }
            }
        ) { state ->
            // Same target value for Initial and Released states
            if (state == ButtonStatus.Pressed) 0.2f else 0f
        }

        Box(modifier.graphicsLayer(alpha = alpha, scaleX = scale)) {
            // content goes here
        }
    }
}

@Sampled
fun InitialStateSample() {
    // This composable enters the composition with a custom enter transition. This is achieved by
    // defining a different initialState than the first target state using `MutableTransitionState`
    @Composable
    fun PoppingInCard() {
        // Creates a transition state with an initial state where visible = false
        val visibleState = remember { MutableTransitionState(false) }
        // Sets the target state of the transition state to true. As it's different than the initial
        // state, a transition from not visible to visible will be triggered.
        visibleState.targetState = true

        // Creates a transition with the transition state created above.
        val transition = updateTransition(visibleState)
        // Adds a scale animation to the transition to scale the card up when transitioning in.
        val scale by transition.animateFloat(
            // Uses a custom spring for the transition.
            transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy) }
        ) { visible ->
            if (visible) 1f else 0.8f
        }
        // Adds an elevation animation that animates the dp value of the animation.
        val elevation by transition.animateDp(
            // Uses a tween animation
            transitionSpec = {
                // Uses different animations for when animating from visible to not visible, and
                // the other way around
                if (false isTransitioningTo true) {
                    tween(1000)
                } else {
                    spring()
                }
            }
        ) { visible ->
            if (visible) 10.dp else 0.dp
        }

        Card(
            Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                .size(200.dp, 100.dp).fillMaxWidth(),
            elevation = elevation
        ) {}
    }
}

enum class LikedStates {
    Initial,
    Liked,
    Disappeared
}

@Sampled
@Composable
fun DoubleTapToLikeSample() {
    // enum class LikedStates { Initial, Liked, Disappeared }
    @Composable
    fun doubleTapToLike() {
        // Creates a transition state that starts in [Disappeared] State
        var transitionState by remember {
            mutableStateOf(MutableTransitionState(LikedStates.Disappeared))
        }

        Box(
            Modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        // This creates a new `MutableTransitionState` object. When a new
                        // `MutableTransitionState` object gets passed to `updateTransition`, a
                        // new transition will be created. All existing values, velocities will
                        // be lost as a result. Hence, in most cases, this is not recommended.
                        // The exception is when it's more important to respond immediately to
                        // user interaction than preserving continuity.
                        transitionState = MutableTransitionState(LikedStates.Initial)
                    }
                )
            }
        ) {
            // This ensures sequential states: Initial -> Liked -> Disappeared
            if (transitionState.currentState == LikedStates.Initial) {
                transitionState.targetState = LikedStates.Liked
            } else if (transitionState.currentState == LikedStates.Liked) {
                // currentState will be updated to targetState when the transition is finished, so
                // it can be used as a signal to start the next transition.
                transitionState.targetState = LikedStates.Disappeared
            }

            // Creates a transition using the TransitionState object that gets recreated at each
            // double tap.
            val transition = updateTransition(transitionState)
            // Creates an alpha animation, as a part of the transition.
            val alpha by transition.animateFloat(
                transitionSpec = {
                    when {
                        // Uses different animation specs for transitioning from/to different states
                        LikedStates.Initial isTransitioningTo LikedStates.Liked ->
                            keyframes {
                                durationMillis = 500
                                0f at 0 // optional
                                0.5f at 100
                                1f at 225 // optional
                            }
                        LikedStates.Liked isTransitioningTo LikedStates.Disappeared ->
                            tween(durationMillis = 200)
                        else -> snap()
                    }
                }
            ) {
                if (it == LikedStates.Liked) 1f else 0f
            }

            // Creates a scale animation, as a part of the transition
            val scale by transition.animateFloat(
                transitionSpec = {
                    when {
                        // Uses different animation specs for transitioning from/to different states
                        LikedStates.Initial isTransitioningTo LikedStates.Liked ->
                            spring(dampingRatio = Spring.DampingRatioHighBouncy)
                        LikedStates.Liked isTransitioningTo LikedStates.Disappeared ->
                            tween(200)
                        else -> snap()
                    }
                }
            ) {
                when (it) {
                    LikedStates.Initial -> 0f
                    LikedStates.Liked -> 4f
                    LikedStates.Disappeared -> 2f
                }
            }

            Icon(
                Icons.Filled.Favorite,
                "Like",
                Modifier.align(Alignment.Center)
                    .graphicsLayer(
                        alpha = alpha,
                        scaleX = scale,
                        scaleY = scale
                    ),
                tint = Color.Red
            )
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
@Sampled
fun CreateChildTransitionSample() {
    // enum class DialerState { DialerMinimized, NumberPad }
    @OptIn(ExperimentalTransitionApi::class)
    @Composable
    fun DialerButton(visibilityTransition: Transition<Boolean>, modifier: Modifier) {
        val scale by visibilityTransition.animateFloat { visible ->
            if (visible) 1f else 2f
        }
        Box(modifier.scale(scale).background(Color.Black)) {
            // Content goes here
        }
    }

    @Composable
    fun NumberPad(visibilityTransition: Transition<Boolean>) {
        // Create animations using the provided Transition for visibility change here...
    }

    @OptIn(ExperimentalTransitionApi::class)
    @Composable
    fun childTransitionSample() {
        var dialerState by remember { mutableStateOf(DialerState.NumberPad) }
        Box(Modifier.fillMaxSize()) {
            val parentTransition = updateTransition(dialerState)

            // Animate to different corner radius based on target state
            val cornerRadius by parentTransition.animateDp {
                if (it == DialerState.NumberPad) 0.dp else 20.dp
            }

            Box(
                Modifier.align(Alignment.BottomCenter).widthIn(50.dp).heightIn(50.dp)
                    .clip(RoundedCornerShape(cornerRadius))
            ) {
                NumberPad(
                    // Creates a child transition that derives its target state from the parent
                    // transition, and the mapping from parent state to child state.
                    // This will allow:
                    // 1) Parent transition to account for additional animations in the child
                    // Transitions before it considers itself finished. This is useful when you
                    // have a subsequent action after all animations triggered by a state change
                    // have finished.
                    // 2) Separation of concerns. This allows the child composable (i.e.
                    // NumberPad) to only care about its own visibility, rather than knowing about
                    // DialerState.
                    visibilityTransition = parentTransition.createChildTransition {
                        // This is the lambda that defines how the parent target state maps to
                        // child target state.
                        it == DialerState.NumberPad
                    }
                    // Note: If it's not important for the animations within the child composable to
                    // be observable, it's perfectly valid to not hoist the animations through
                    // a Transition object and instead use animate*AsState.
                )
                DialerButton(
                    visibilityTransition = parentTransition.createChildTransition {
                        it == DialerState.DialerMinimized
                    },
                    modifier = Modifier.matchParentSize()
                )
            }
        }
    }
}

enum class DialerState {
    DialerMinimized,
    NumberPad
}

@Sampled
@OptIn(ExperimentalTransitionApi::class)
@Composable
fun TransitionStateIsIdleSample() {
    @Composable
    fun SelectableItem(selectedState: MutableTransitionState<Boolean>) {
        val transition = updateTransition(selectedState)
        val cornerRadius by transition.animateDp { selected -> if (selected) 10.dp else 0.dp }
        val backgroundColor by transition.animateColor { selected ->
            if (selected) Color.Red else Color.White
        }
        Box(Modifier.background(backgroundColor, RoundedCornerShape(cornerRadius))) {
            // Item content goes here
        }
    }

    @OptIn(ExperimentalTransitionApi::class)
    @Composable
    fun ItemsSample(selectedId: Int) {
        Column {
            repeat(3) { id ->
                Box {
                    // Initialize the selected state as false to produce a transition going from
                    // false to true if `selected` parameter is true when entering composition.
                    val selectedState = remember { MutableTransitionState(false) }
                    // Mutate target state as needed.
                    selectedState.targetState = id == selectedId
                    // Now we pass the `MutableTransitionState` to the `Selectable` item and
                    // observe state change.
                    SelectableItem(selectedState)
                    if (selectedState.isIdle && selectedState.targetState) {
                        // If isIdle == true, it means the transition has arrived at its target state
                        // and there is no pending animation.
                        // Now we can do something after the selection transition is
                        // finished:
                        Text("Nice choice")
                    }
                }
            }
        }
    }
}