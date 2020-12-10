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

package androidx.compose.material.studies.rally

import androidx.compose.animation.ColorPropKey
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.core.tween
import androidx.compose.animation.transition
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun RallyTopAppBar(
    allScreens: List<RallyScreenState>,
    onTabSelected: (RallyScreenState) -> Unit,
    currentScreen: RallyScreenState
) {
    Surface(Modifier.preferredHeight(TabHeight).fillMaxWidth()) {
        Row {
            allScreens.forEachIndexed { index, screen ->
                RallyTab(
                    text = screen.name.toUpperCase(Locale.getDefault()),
                    icon = screen.icon,
                    onSelected = { onTabSelected(screen) },
                    selected = currentScreen.ordinal == index
                )
            }
        }
    }
}

@Composable
private fun RallyTab(
    text: String,
    icon: ImageVector,
    onSelected: () -> Unit,
    selected: Boolean
) {
    TabTransition(selected = selected) { tabTintColor ->
        Row(
            modifier = Modifier
                .padding(16.dp)
                .preferredHeight(TabHeight)
                .selectable(
                    selected = selected,
                    onClick = onSelected,
                    indication = rememberRipple(bounded = false)
                )
        ) {
            Icon(icon, tint = tabTintColor)
            if (selected) {
                Spacer(Modifier.preferredWidth(12.dp))
                Text(text, color = tabTintColor)
            }
        }
    }
}

@Composable
private fun TabTransition(
    selected: Boolean,
    content: @Composable (color: Color) -> Unit
) {
    val color = MaterialTheme.colors.onSurface
    val transitionDefinition = remember {
        transitionDefinition<Boolean> {
            state(true) {
                this[TabTintColorKey] = color
            }

            state(false) {
                this[TabTintColorKey] = color.copy(alpha = InactiveTabOpacity)
            }

            transition(fromState = false, toState = true) {
                TabTintColorKey using tween(
                    durationMillis = TabFadeInAnimationDuration,
                    delayMillis = TabFadeInAnimationDelay,
                    easing = LinearEasing
                )
            }

            transition(fromState = true, toState = false) {
                TabTintColorKey using tween(
                    durationMillis = TabFadeOutAnimationDuration,
                    delayMillis = TabFadeInAnimationDelay,
                    easing = LinearEasing
                )
            }
        }
    }
    val state = transition(transitionDefinition, selected)
    content(state[TabTintColorKey])
}

private val TabTintColorKey = ColorPropKey()
private val TabHeight = 56.dp
private const val InactiveTabOpacity = 0.60f

private const val TabFadeInAnimationDuration = 150
private const val TabFadeInAnimationDelay = 100
private const val TabFadeOutAnimationDuration = 100