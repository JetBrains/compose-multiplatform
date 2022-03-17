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

package androidx.compose.material

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect

/**
 * <a href="https://material.io/components/buttons#contained-button" class="external" target="_blank">Material Design contained button</a>.
 *
 * Contained buttons are high-emphasis, distinguished by their use of elevation and fill. They
 * contain actions that are primary to your app.
 *
 * ![Contained button image](https://developer.android.com/images/reference/androidx/compose/material/contained-button.png)
 *
 * The default text style for internal [Text] components will be set to [Typography.button].
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
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Button in different [Interaction]s.
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 * states. This controls the size of the shadow below the button. Pass `null` here to disable
 * elevation for this button. See [ButtonDefaults.elevation].
 * @param shape Defines the button's shape as well as its shadow
 * @param border Border to draw around the button
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 * this button in different states. See [ButtonDefaults.buttonColors].
 * @param contentPadding The spacing values to apply internally between the container and the content
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ButtonElevation? = ButtonDefaults.elevation(),
    shape: Shape = MaterialTheme.shapes.small,
    border: BorderStroke? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val contentColor by colors.contentColor(enabled)
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = colors.backgroundColor(enabled).value,
        contentColor = contentColor.copy(alpha = 1f),
        border = border,
        elevation = elevation?.elevation(enabled, interactionSource)?.value ?: 0.dp,
        interactionSource = interactionSource,
    ) {
        CompositionLocalProvider(LocalContentAlpha provides contentColor.alpha) {
            ProvideTextStyle(
                value = MaterialTheme.typography.button
            ) {
                Row(
                    Modifier
                        .defaultMinSize(
                            minWidth = ButtonDefaults.MinWidth,
                            minHeight = ButtonDefaults.MinHeight
                        )
                        .padding(contentPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }
}

/**
 * <a href="https://material.io/components/buttons#outlined-button" class="external" target="_blank">Material Design outlined button</a>.
 *
 * Outlined buttons are medium-emphasis buttons. They contain actions that are important, but aren't
 * the primary action in an app.
 *
 * ![Outlined button image](https://developer.android.com/images/reference/androidx/compose/material/outlined-button.png)
 *
 * The default text style for internal [Text] components will be set to [Typography.button].
 *
 * @sample androidx.compose.material.samples.OutlinedButtonSample
 *
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Button in different [Interaction]s.
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 * states. An OutlinedButton typically has no elevation, see [Button] for a button with elevation.
 * @param shape Defines the button's shape as well as its shadow
 * @param border Border to draw around the button
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 * this button in different states. See [ButtonDefaults.outlinedButtonColors].
 * @param contentPadding The spacing values to apply internally between the container and the content
 */
@Composable
@NonRestartableComposable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ButtonElevation? = null,
    shape: Shape = MaterialTheme.shapes.small,
    border: BorderStroke? = ButtonDefaults.outlinedBorder,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) = Button(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    interactionSource = interactionSource,
    elevation = elevation,
    shape = shape,
    border = border,
    colors = colors,
    contentPadding = contentPadding,
    content = content
)

/**
 * <a href="https://material.io/components/buttons#text-button" class="external" target="_blank">Material Design text button</a>.
 *
 * Text buttons are typically used for less-pronounced actions, including those located in dialogs
 * and cards. In cards, text buttons help maintain an emphasis on card content.
 *
 * ![Text button image](https://developer.android.com/images/reference/androidx/compose/material/text-button.png)
 *
 * The default text style for internal [Text] components will be set to [Typography.button].
 *
 * @sample androidx.compose.material.samples.TextButtonSample
 *
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Button in different [Interaction]s.
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 * states. A TextButton typically has no elevation, see [Button] for a button with elevation.
 * @param shape Defines the button's shape as well as its shadow
 * @param border Border to draw around the button
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 * this button in different states. See [ButtonDefaults.textButtonColors].
 * @param contentPadding The spacing values to apply internally between the container and the content
 */
@Composable
@NonRestartableComposable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ButtonElevation? = null,
    shape: Shape = MaterialTheme.shapes.small,
    border: BorderStroke? = null,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    content: @Composable RowScope.() -> Unit
) = Button(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    interactionSource = interactionSource,
    elevation = elevation,
    shape = shape,
    border = border,
    colors = colors,
    contentPadding = contentPadding,
    content = content
)

/**
 * Represents the elevation for a button in different states.
 *
 * See [ButtonDefaults.elevation] for the default elevation used in a [Button].
 */
@Stable
interface ButtonElevation {
    /**
     * Represents the elevation used in a button, depending on [enabled] and
     * [interactionSource].
     *
     * @param enabled whether the button is enabled
     * @param interactionSource the [InteractionSource] for this button
     */
    @Composable
    fun elevation(enabled: Boolean, interactionSource: InteractionSource): State<Dp>
}

/**
 * Represents the background and content colors used in a button in different states.
 *
 * See [ButtonDefaults.buttonColors] for the default colors used in a [Button].
 * See [ButtonDefaults.outlinedButtonColors] for the default colors used in a
 * [OutlinedButton].
 * See [ButtonDefaults.textButtonColors] for the default colors used in a [TextButton].
 */
@Stable
interface ButtonColors {
    /**
     * Represents the background color for this button, depending on [enabled].
     *
     * @param enabled whether the button is enabled
     */
    @Composable
    fun backgroundColor(enabled: Boolean): State<Color>

    /**
     * Represents the content color for this button, depending on [enabled].
     *
     * @param enabled whether the button is enabled
     */
    @Composable
    fun contentColor(enabled: Boolean): State<Color>
}

/**
 * Contains the default values used by [Button]
 */
object ButtonDefaults {
    private val ButtonHorizontalPadding = 16.dp
    private val ButtonVerticalPadding = 8.dp

    /**
     * The default content padding used by [Button]
     */
    val ContentPadding = PaddingValues(
        start = ButtonHorizontalPadding,
        top = ButtonVerticalPadding,
        end = ButtonHorizontalPadding,
        bottom = ButtonVerticalPadding
    )

    /**
     * The default min width applied for the [Button].
     * Note that you can override it by applying Modifier.widthIn directly on [Button].
     */
    val MinWidth = 64.dp

    /**
     * The default min height applied for the [Button].
     * Note that you can override it by applying Modifier.heightIn directly on [Button].
     */
    val MinHeight = 36.dp

    /**
     * The default size of the icon when used inside a [Button].
     *
     * @sample androidx.compose.material.samples.ButtonWithIconSample
     */
    val IconSize = 18.dp

    /**
     * The default size of the spacing between an icon and a text when they used inside a [Button].
     *
     * @sample androidx.compose.material.samples.ButtonWithIconSample
     */
    val IconSpacing = 8.dp

    /**
     * Creates a [ButtonElevation] that will animate between the provided values according to the
     * Material specification for a [Button].
     *
     * @param defaultElevation the elevation to use when the [Button] is enabled, and has no
     * other [Interaction]s.
     * @param pressedElevation the elevation to use when the [Button] is enabled and
     * is pressed.
     * @param disabledElevation the elevation to use when the [Button] is not enabled.
     */
    @Deprecated("Use another overload of elevation", level = DeprecationLevel.HIDDEN)
    @Composable
    fun elevation(
        defaultElevation: Dp = 2.dp,
        pressedElevation: Dp = 8.dp,
        disabledElevation: Dp = 0.dp
    ): ButtonElevation = elevation(
        defaultElevation,
        pressedElevation,
        disabledElevation,
        hoveredElevation = 4.dp,
        focusedElevation = 4.dp,
    )

    /**
     * Creates a [ButtonElevation] that will animate between the provided values according to the
     * Material specification for a [Button].
     *
     * @param defaultElevation the elevation to use when the [Button] is enabled, and has no
     * other [Interaction]s.
     * @param pressedElevation the elevation to use when the [Button] is enabled and
     * is pressed.
     * @param disabledElevation the elevation to use when the [Button] is not enabled.
     * @param hoveredElevation the elevation to use when the [Button] is enabled and is hovered.
     * @param focusedElevation the elevation to use when the [Button] is enabled and is focused.
     */
    @Suppress("UNUSED_PARAMETER")
    @Composable
    fun elevation(
        defaultElevation: Dp = 2.dp,
        pressedElevation: Dp = 8.dp,
        disabledElevation: Dp = 0.dp,
        hoveredElevation: Dp = 4.dp,
        focusedElevation: Dp = 4.dp,
    ): ButtonElevation {
        return remember(
            defaultElevation,
            pressedElevation,
            disabledElevation,
            hoveredElevation,
            focusedElevation
        ) {
            DefaultButtonElevation(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                disabledElevation = disabledElevation,
                hoveredElevation = hoveredElevation,
                focusedElevation = focusedElevation
            )
        }
    }

    /**
     * Creates a [ButtonColors] that represents the default background and content colors used in
     * a [Button].
     *
     * @param backgroundColor the background color of this [Button] when enabled
     * @param contentColor the content color of this [Button] when enabled
     * @param disabledBackgroundColor the background color of this [Button] when not enabled
     * @param disabledContentColor the content color of this [Button] when not enabled
     */
    @Composable
    fun buttonColors(
        backgroundColor: Color = MaterialTheme.colors.primary,
        contentColor: Color = contentColorFor(backgroundColor),
        disabledBackgroundColor: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
            .compositeOver(MaterialTheme.colors.surface),
        disabledContentColor: Color = MaterialTheme.colors.onSurface
            .copy(alpha = ContentAlpha.disabled)
    ): ButtonColors = DefaultButtonColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        disabledBackgroundColor = disabledBackgroundColor,
        disabledContentColor = disabledContentColor
    )

    /**
     * Creates a [ButtonColors] that represents the default background and content colors used in
     * an [OutlinedButton].
     *
     * @param backgroundColor the background color of this [OutlinedButton]
     * @param contentColor the content color of this [OutlinedButton] when enabled
     * @param disabledContentColor the content color of this [OutlinedButton] when not enabled
     */
    @Composable
    fun outlinedButtonColors(
        backgroundColor: Color = MaterialTheme.colors.surface,
        contentColor: Color = MaterialTheme.colors.primary,
        disabledContentColor: Color = MaterialTheme.colors.onSurface
            .copy(alpha = ContentAlpha.disabled)
    ): ButtonColors = DefaultButtonColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        disabledBackgroundColor = backgroundColor,
        disabledContentColor = disabledContentColor
    )

    /**
     * Creates a [ButtonColors] that represents the default background and content colors used in
     * a [TextButton].
     *
     * @param backgroundColor the background color of this [TextButton]
     * @param contentColor the content color of this [TextButton] when enabled
     * @param disabledContentColor the content color of this [TextButton] when not enabled
     */
    @Composable
    fun textButtonColors(
        backgroundColor: Color = Color.Transparent,
        contentColor: Color = MaterialTheme.colors.primary,
        disabledContentColor: Color = MaterialTheme.colors.onSurface
            .copy(alpha = ContentAlpha.disabled)
    ): ButtonColors = DefaultButtonColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        disabledBackgroundColor = backgroundColor,
        disabledContentColor = disabledContentColor
    )

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
    val outlinedBorder: BorderStroke
        @Composable
        get() = BorderStroke(
            OutlinedBorderSize, MaterialTheme.colors.onSurface.copy(alpha = OutlinedBorderOpacity)
        )

    private val TextButtonHorizontalPadding = 8.dp

    /**
     * The default content padding used by [TextButton]
     */
    val TextButtonContentPadding = PaddingValues(
        start = TextButtonHorizontalPadding,
        top = ContentPadding.calculateTopPadding(),
        end = TextButtonHorizontalPadding,
        bottom = ContentPadding.calculateBottomPadding()
    )
}

/**
 * Default [ButtonElevation] implementation.
 */
@Stable
private class DefaultButtonElevation(
    private val defaultElevation: Dp,
    private val pressedElevation: Dp,
    private val disabledElevation: Dp,
    private val hoveredElevation: Dp,
    private val focusedElevation: Dp,
) : ButtonElevation {
    @Composable
    override fun elevation(enabled: Boolean, interactionSource: InteractionSource): State<Dp> {
        val interactions = remember { mutableStateListOf<Interaction>() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is HoverInteraction.Enter -> {
                        interactions.add(interaction)
                    }
                    is HoverInteraction.Exit -> {
                        interactions.remove(interaction.enter)
                    }
                    is FocusInteraction.Focus -> {
                        interactions.add(interaction)
                    }
                    is FocusInteraction.Unfocus -> {
                        interactions.remove(interaction.focus)
                    }
                    is PressInteraction.Press -> {
                        interactions.add(interaction)
                    }
                    is PressInteraction.Release -> {
                        interactions.remove(interaction.press)
                    }
                    is PressInteraction.Cancel -> {
                        interactions.remove(interaction.press)
                    }
                }
            }
        }

        val interaction = interactions.lastOrNull()

        val target = if (!enabled) {
            disabledElevation
        } else {
            when (interaction) {
                is PressInteraction.Press -> pressedElevation
                is HoverInteraction.Enter -> hoveredElevation
                is FocusInteraction.Focus -> focusedElevation
                else -> defaultElevation
            }
        }

        val animatable = remember { Animatable(target, Dp.VectorConverter) }

        if (!enabled) {
            // No transition when moving to a disabled state
            LaunchedEffect(target) {
                animatable.snapTo(target)
            }
        } else {
            LaunchedEffect(target) {
                val lastInteraction = when (animatable.targetValue) {
                    pressedElevation -> PressInteraction.Press(Offset.Zero)
                    hoveredElevation -> HoverInteraction.Enter()
                    focusedElevation -> FocusInteraction.Focus()
                    else -> null
                }
                animatable.animateElevation(
                    from = lastInteraction,
                    to = interaction,
                    target = target
                )
            }
        }

        return animatable.asState()
    }
}

/**
 * Default [ButtonColors] implementation.
 */
@Immutable
private class DefaultButtonColors(
    private val backgroundColor: Color,
    private val contentColor: Color,
    private val disabledBackgroundColor: Color,
    private val disabledContentColor: Color
) : ButtonColors {
    @Composable
    override fun backgroundColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) backgroundColor else disabledBackgroundColor)
    }

    @Composable
    override fun contentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) contentColor else disabledContentColor)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultButtonColors

        if (backgroundColor != other.backgroundColor) return false
        if (contentColor != other.contentColor) return false
        if (disabledBackgroundColor != other.disabledBackgroundColor) return false
        if (disabledContentColor != other.disabledContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backgroundColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + disabledBackgroundColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        return result
    }
}
