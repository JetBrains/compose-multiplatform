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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.IndicationAmbient
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.ProvideTextStyle
import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.InnerPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSizeConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material Design implementation of a
 * [Material Contained Button](https://material.io/design/components/buttons.html#contained-button).
 *
 * Contained buttons are high-emphasis, distinguished by their use of elevation and fill. They
 * contain actions that are primary to your app.
 *
 * To make a button clickable, you must provide an onClick. If no onClick is provided, this button
 * will display itself as disabled.
 *
 * The default text style for internal [Text] components will be set to [Typography.button]. Text
 * color will try to match the correlated color for the background color. For example if the
 * background color is set to [Colors.primary] then the text will by default use
 * [Colors.onPrimary].
 *
 * @sample androidx.compose.material.samples.ButtonSample
 *
 * If you need to add an icon just put it inside the [content] slot together with a spacing
 * and a text:
 *
 * @sample androidx.compose.material.samples.ButtonWithIconSample
 *
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable
 * @param elevation The z-coordinate at which to place this button. This controls the size
 * of the shadow below the button
 * @param disabledElevation The elevation used when [enabled] is false
 * @param shape Defines the button's shape as well as its shadow
 * @param border Border to draw around the button
 * @param backgroundColor The background color. Use [Color.Transparent] to have no color
 * @param disabledBackgroundColor The background color used when [enabled] is false
 * @param contentColor The preferred content color. Will be used by text and iconography
 * @param disabledContentColor The preferred content color used when [enabled] is false
 * @param contentPadding The spacing values to apply internally between the container and the content
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    elevation: Dp = 2.dp,
    disabledElevation: Dp = 0.dp,
    shape: Shape = MaterialTheme.shapes.small,
    border: BorderStroke? = null,
    backgroundColor: Color = MaterialTheme.colors.primary,
    disabledBackgroundColor: Color = ButtonConstants.defaultDisabledBackgroundColor,
    contentColor: Color = contentColorFor(backgroundColor),
    disabledContentColor: Color = ButtonConstants.defaultDisabledContentColor,
    contentPadding: InnerPadding = ButtonConstants.DefaultContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    // TODO(aelias): Avoid manually putting the clickable above the clip and
    // the ripple below the clip once http://b/157687898 is fixed and we have
    // more flexibility to move the clickable modifier (see candidate approach
    // aosp/1361921)
    val interactionState = remember { InteractionState() }
    Surface(
        shape = shape,
        color = if (enabled) backgroundColor else disabledBackgroundColor,
        contentColor = if (enabled) contentColor else disabledContentColor,
        border = border,
        elevation = if (enabled) elevation else disabledElevation,
        modifier = modifier.clickable(
            onClick = onClick,
            enabled = enabled,
            interactionState = interactionState,
            indication = null)
    ) {
        ProvideTextStyle(
            value = MaterialTheme.typography.button
        ) {
            Row(
                Modifier
                    .defaultMinSizeConstraints(
                        minWidth = ButtonConstants.DefaultMinWidth,
                        minHeight = ButtonConstants.DefaultMinHeight
                    )
                    .indication(interactionState, IndicationAmbient.current())
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalGravity = Alignment.CenterVertically,
                children = content
            )
        }
    }
}

/**
 * Material Design implementation of a
 * [Material Outlined Button](https://material.io/design/components/buttons.html#outlined-button).
 *
 * Outlined buttons are medium-emphasis buttons. They contain actions that are important, but are
 * not the primary action in an app.
 *
 * Outlined buttons are also a lower emphasis alternative to contained buttons, or a higher emphasis
 * alternative to text buttons.
 *
 * To make a button clickable, you must provide an onClick. If no onClick is provided, this button
 * will display itself as disabled.
 *
 * The default text style for internal [Text] components will be set to [Typography.button]. Text
 * color will try to match the correlated color for the background color. For example if the
 * background color is set to [Colors.primary] then the text will by default use
 * [Colors.onPrimary].
 *
 * @sample androidx.compose.material.samples.OutlinedButtonSample
 *
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable
 * @param elevation The z-coordinate at which to place this button. This controls the size
 * of the shadow below the button
 * @param shape Defines the button's shape as well as its shadow
 * @param border Border to draw around the button
 * @param backgroundColor The background color. Use [Color.Transparent] to have no color
 * @param contentColor The preferred content color. Will be used by text and iconography
 * @param disabledContentColor The preferred content color used when [enabled] is false
 * @param contentPadding The spacing values to apply internally between the container and the content
 */
@Composable
inline fun OutlinedButton(
    noinline onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    elevation: Dp = 0.dp,
    shape: Shape = MaterialTheme.shapes.small,
    border: BorderStroke? = ButtonConstants.defaultOutlinedBorder,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = MaterialTheme.colors.primary,
    disabledContentColor: Color = ButtonConstants.defaultDisabledContentColor,
    contentPadding: InnerPadding = ButtonConstants.DefaultContentPadding,
    noinline content: @Composable RowScope.() -> Unit
) = Button(
    modifier = modifier,
    onClick = onClick,
    enabled = enabled,
    elevation = elevation,
    disabledElevation = 0.dp,
    shape = shape,
    border = border,
    backgroundColor = backgroundColor,
    disabledBackgroundColor = backgroundColor,
    contentColor = contentColor,
    disabledContentColor = disabledContentColor,
    contentPadding = contentPadding,
    content = content
)

/**
 * Material Design implementation of a
 * [Material Text Button](https://material.io/design/components/buttons.html#text-button).
 *
 * Text buttons are typically used for less-pronounced actions, including those located in cards and
 * dialogs.
 *
 * To make a button clickable, you must provide an onClick. If no onClick is provided, this button
 * will display itself as disabled.
 *
 * The default text style for internal [Text] components will be set to [Typography.button]. Text
 * color will try to match the correlated color for the background color. For example if the
 * background color is set to [Colors.primary] then the text will by default use
 * [Colors.onPrimary].
 *
 * @sample androidx.compose.material.samples.TextButtonSample
 *
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable
 * @param elevation The z-coordinate at which to place this button. This controls the size
 * of the shadow below the button
 * @param shape Defines the button's shape as well as its shadow
 * @param border Border to draw around the button
 * @param backgroundColor The background color. Use [Color.Transparent] to have no color
 * @param contentColor The preferred content color. Will be used by text and iconography
 * @param disabledContentColor The preferred content color used when [enabled] is false
 * @param contentPadding The spacing values to apply internally between the container and the content
 */
@Composable
inline fun TextButton(
    noinline onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    elevation: Dp = 0.dp,
    shape: Shape = MaterialTheme.shapes.small,
    border: BorderStroke? = null,
    backgroundColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colors.primary,
    disabledContentColor: Color = ButtonConstants.defaultDisabledContentColor,
    contentPadding: InnerPadding = ButtonConstants.DefaultTextContentPadding,
    noinline content: @Composable RowScope.() -> Unit
) = Button(
    modifier = modifier,
    onClick = onClick,
    enabled = enabled,
    elevation = elevation,
    disabledElevation = 0.dp,
    shape = shape,
    border = border,
    backgroundColor = backgroundColor,
    disabledBackgroundColor = backgroundColor,
    contentColor = contentColor,
    disabledContentColor = disabledContentColor,
    contentPadding = contentPadding,
    content = content
)

/**
 * Contains the default values used by [Button]
 */
object ButtonConstants {
    private val ButtonHorizontalPadding = 16.dp
    private val ButtonVerticalPadding = 8.dp

    /**
     * The default content padding used by [Button]
     */
    val DefaultContentPadding = InnerPadding(
        start = ButtonHorizontalPadding,
        top = ButtonVerticalPadding,
        end = ButtonHorizontalPadding,
        bottom = ButtonVerticalPadding
    )

    /**
     * The default min width applied for the [Button].
     * Note that you can override it by applying [Modifier.widthIn] directly on [Button].
     */
    val DefaultMinWidth = 64.dp

    /**
     * The default min width applied for the [Button].
     * Note that you can override it by applying [Modifier.heightIn] directly on [Button].
     */
    val DefaultMinHeight = 36.dp

    /**
     * The default size of the icon when used inside a [Button].
     *
     * @sample androidx.compose.material.samples.ButtonWithIconSample
     */
    val DefaultIconSize = 18.dp

    /**
     * The default size of the spacing between an icon and a text when they used inside a [Button].
     *
     * @sample androidx.compose.material.samples.ButtonWithIconSample
     */
    val DefaultIconSpacing = 8.dp

    /**
     * The default disabled background color used by Contained [Button]s
     */
    @Composable
    val defaultDisabledBackgroundColor
        get(): Color = with(MaterialTheme.colors) {
            // we have to composite it over surface here as if we provide a transparent background for
            // Surface and non-zero elevation the artifacts from casting the shadow will be visible
            // below the background.
            onSurface.copy(alpha = 0.12f).compositeOver(surface)
        }

    /**
     * The default disabled content color used by all types of [Button]s
     */
    @Composable
    val defaultDisabledContentColor
        get(): Color = with(MaterialTheme.colors) {
            EmphasisAmbient.current.disabled.applyEmphasis(onSurface)
        }

    /**
     * The default color opacity used for an [OutlinedButton]'s border color
     */
    const val OutlinedBorderOpacity = 0.12f

    /**
     * The default [OutlinedButton]'s border size
     */
    val OutlinedBorderSize = 1.dp

    /**
     * The default disabled content color used by all types of [Button]s
     */
    @Composable
    val defaultOutlinedBorder: BorderStroke
        get() = BorderStroke(
            OutlinedBorderSize, MaterialTheme.colors.onSurface.copy(alpha = OutlinedBorderOpacity)
        )

    private val TextButtonHorizontalPadding = 8.dp

    /**
     * The default content padding used by [TextButton]
     */
    val DefaultTextContentPadding = DefaultContentPadding.copy(
        start = TextButtonHorizontalPadding,
        end = TextButtonHorizontalPadding
    )
}
