/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.compose.animation.ColorPropKey
import androidx.compose.animation.DpPropKey
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.transition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private enum class State {
    First,
    Second
}

@Sampled
fun TransitionSample() {
    val colorKey = ColorPropKey()
    val widthKey = DpPropKey()
    val heightKey = DpPropKey()

    val definition = transitionDefinition<State> {
        state(State.First) {
            this[colorKey] = Color.Red
            this[widthKey] = 200.dp
            this[heightKey] = 400.dp
        }
        state(State.Second) {
            this[colorKey] = Color.Green
            this[widthKey] = 300.dp
            this[heightKey] = 300.dp
        }
    }

    @Composable
    fun TransitionBasedColoredRect() {
        // This puts the transition in State.First. Any subsequent state change will trigger a
        // transition animation, as defined in the transition definition.
        val state = transition(definition = definition, toState = State.First)
        Box(
            Modifier
                .preferredSize(state[widthKey], state[heightKey])
                .background(color = state[colorKey])
        )
    }

    @Composable
    fun ColorRectWithInitState() {
        // This starts the transition going from State.First to State.Second when this composable
        // gets composed for the first time.
        val state = transition(
            definition = definition, initState = State.First, toState = State.Second
        )
        Box(
            Modifier
                .preferredSize(state[widthKey], state[heightKey])
                .background(state[colorKey])
        )
    }
}
