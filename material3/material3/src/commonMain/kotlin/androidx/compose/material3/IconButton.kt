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
package androidx.compose.material3

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.tokens.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * IconButton is a clickable icon, used to represent actions. An IconButton has an overall minimum
 * touch target size of 48 x 48dp, to meet accessibility guidelines. [content] is centered inside
 * the IconButton.
 *
 * This component is typically used inside an App Bar for the navigation icon / actions. See App Bar
 * documentation for samples of this.
 *
 * [content] should typically be an [Icon], using an icon from
 * [androidx.compose.material.icons.Icons]. If using a custom icon, note that the typical size for
 * the internal icon is 24 x 24 dp.
 *
 * @sample androidx.compose.material3.samples.IconButtonSample
 *
 * @param onClick the lambda to be invoked when this icon is pressed
 * @param modifier optional [Modifier] for this IconButton
 * @param enabled whether or not this IconButton will handle input events and appear enabled for
 * semantics purposes
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this IconButton. You can create and pass in your own remembered [MutableInteractionSource] if
 * you want to observe [Interaction]s and customize the appearance / behavior of this IconButton in
 * different [Interaction]s.
 * @param content the content (icon) to be drawn inside the IconButton. This is typically an [Icon].
 */
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    Box(
        modifier =
            modifier
                .clickable(
                    onClick = onClick,
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication = rememberRipple(bounded = false, radius = RippleRadius)
                )
                .then(IconButtonSizeModifier),
        contentAlignment = Alignment.Center
    ) {
        val contentColor =
            if (enabled) LocalContentColor.current
            else
                MaterialTheme.colorScheme
                    .fromToken(IconButton.DisabledIconColor)
                    .copy(alpha = IconButton.DisabledIconOpacity)
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}

/**
 * An [IconButton] with two states, for icons that can be toggled 'on' and 'off', such as a bookmark
 * icon, or a navigation icon that opens a drawer.
 *
 * @sample androidx.compose.material3.samples.IconToggleButtonSample
 *
 * @param checked whether this IconToggleButton is currently checked
 * @param onCheckedChange callback to be invoked when this icon is selected
 * @param modifier optional [Modifier] for this IconToggleButton
 * @param enabled enabled whether or not this [IconToggleButton] will handle input events and appear
 * enabled for semantics purposes
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this IconToggleButton. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the appearance /
 * behavior of this IconToggleButton in different [Interaction]s.
 * @param content the content (icon) to be drawn inside the IconToggleButton. This is typically an
 * [Icon].
 */
@Composable
fun IconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    Box(
        modifier =
            modifier
                .toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    enabled = enabled,
                    role = Role.Checkbox,
                    interactionSource = interactionSource,
                    indication = rememberRipple(bounded = false, radius = RippleRadius)
                )
                .then(IconButtonSizeModifier),
        contentAlignment = Alignment.Center
    ) {
        val contentColor =
            if (enabled) LocalContentColor.current
            else
                MaterialTheme.colorScheme
                    .fromToken(IconButton.DisabledIconColor)
                    .copy(alpha = IconButton.DisabledIconOpacity)
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}

// Default radius of an unbounded ripple in an IconButton
private val RippleRadius = IconButton.StateLayerSize

// TODO: b/149691127 investigate our strategy around accessibility touch targets, and remove
// per-component definitions of this size.
// Diameter of the IconButton, to allow for correct minimum touch target size for accessibility
private val IconButtonSizeModifier = Modifier.size(48.dp)