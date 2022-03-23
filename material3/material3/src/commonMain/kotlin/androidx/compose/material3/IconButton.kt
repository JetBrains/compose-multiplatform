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
import androidx.compose.material3.tokens.IconButtonTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external" target="_blank">Material Design icon button</a>.
 *
 * A "standard" icon button is a clickable icon, used to represent an action.
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Filled button image](https://developer.android.com/images/reference/androidx/compose/material3/standard-icon-button.png)
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp.
 * This icon button has an overall minimum touch target size of 48 x 48dp, to meet accessibility
 * guidelines.
 *
 * @sample androidx.compose.material3.samples.IconButtonSample
 *
 * @param onClick callback to be called when the icon button is clicked
 * @param modifier Modifier to be applied to the layout of the icon button
 * @param enabled whether or not this icon button will handle input events and appear enabled for
 * semantics purposes
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this icon button. You can create and pass in your own remembered
 * [MutableInteractionSource] to observe [Interaction]s that will customize the appearance
 * / behavior of this icon button in different states
 * @param content the content (icon) to be drawn inside the icon button. This is typically an
 * [Icon].
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
                .minimumTouchTargetSize()
                .size(IconButtonTokens.StateLayerSize)
                .clickable(
                    onClick = onClick,
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication = rememberRipple(
                        bounded = false,
                        radius = IconButtonTokens.StateLayerSize / 2
                    )
                ),
        contentAlignment = Alignment.Center
    ) {
        val contentColor =
            if (enabled) {
                IconButtonTokens.UnselectedIconColor.toColor()
            } else {
                IconButtonTokens.DisabledIconColor.toColor()
                    .copy(alpha = IconButtonTokens.DisabledIconOpacity)
            }
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external" target="_blank">Material Design toggleable icon button</a>.
 *
 * A toggleable icon button, used to represent an action. This version of a "standard" icon
 * button is responsible for a toggling its checked state as well as everything else that a
 * clickable icon button does.
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Filled button image](https://developer.android.com/images/reference/androidx/compose/material3/standard-icon-toggle-button.png)
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp.
 * This icon button has an overall minimum touch target size of 48 x 48dp, to meet accessibility
 * guidelines.
 *
 * @sample androidx.compose.material3.samples.IconToggleButtonSample
 *
 * @param checked whether or not this icon button is toggled on or off
 * @param onCheckedChange callback to be invoked when the toggleable icon button is clicked
 * @param modifier Modifier to be applied to the layout of the icon button
 * @param enabled whether or not this icon button will handle input events and appear enabled for
 * semantics purposes
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this icon button. You can create and pass in your own remembered
 * [MutableInteractionSource] to observe [Interaction]s that will customize the appearance
 * / behavior of this icon button in different states
 * @param content the content (icon) to be drawn inside the icon button. This is typically an
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
                .minimumTouchTargetSize()
                .size(IconButtonTokens.StateLayerSize)
                .toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    enabled = enabled,
                    role = Role.Checkbox,
                    interactionSource = interactionSource,
                    indication = rememberRipple(
                        bounded = false,
                        radius = IconButtonTokens.StateLayerSize / 2
                    )
                ),
        contentAlignment = Alignment.Center
    ) {
        val contentColor = when {
            !enabled -> IconButtonTokens.DisabledIconColor.toColor()
                .copy(alpha = IconButtonTokens.DisabledIconOpacity)
            !checked -> IconButtonTokens.UnselectedIconColor.toColor()
            else -> IconButtonTokens.SelectedIconColor.toColor()
        }
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}
