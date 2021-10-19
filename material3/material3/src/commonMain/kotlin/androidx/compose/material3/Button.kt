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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.tokens.ElevatedButton
import androidx.compose.material3.tokens.FilledButton
import androidx.compose.material3.tokens.FilledButtonTonal
import androidx.compose.material3.tokens.OutlinedButton
import androidx.compose.material3.tokens.TextButton
import androidx.compose.material3.tokens.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import kotlinx.coroutines.flow.collect

/**
 * ![Filled button image](https://developer.android.com/images/reference/androidx/compose/material3/filled-button.png)
 *
 * A default Material button, which is also known as a Filled button. Buttons contain actions for
 * your app.
 *
 * Filled buttons are high-emphasis buttons. Filled buttons have the most visual impact after the
 * [FloatingActionButton], and should be used for important, final actions that complete a flow,
 * like "Save", "Join now", or "Confirm".
 *
 * @sample androidx.compose.material3.samples.ButtonSample
 * @sample androidx.compose.material3.samples.ButtonWithIconSample
 *
 * Choose the best button for an action based on the amount of emphasis it needs. The more important
 * an action is, the higher emphasis its button should be.
 *
 * - See [OutlinedButton] for a medium-emphasis button with a border.
 * - See [ElevatedButton] for an [OutlinedButton] with a shadow.
 * - See [TextButton] for a low-emphasis button with no border.
 * - See [FilledTonalButton] for a middle ground between [OutlinedButton] and [Button].
 *
 * The default text style for internal [Text] components will be set to [Typography.LabelLarge].
 *
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 * clickable.
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this Button. You can create and pass in your own remembered [MutableInteractionSource] if you
 * want to observe [Interaction]s and customize the appearance / behavior of this Button in
 * different [Interaction]s.
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 * states. This controls the size of the shadow below the button. When the container color is
 * [ColorScheme.surface], a higher elevation (surface blended with more primary) will result in a
 * darker surface color in light theme and lighter color in dark theme.
 * @param shape Defines the button's shape as well as its shadow.
 * @param border Border to draw around the button. Pass `null` here for no border.
 * @param colors [ButtonColors] that will be used to resolve the container and content color for
 * this button in different states. See [ButtonDefaults.buttonColors].
 * @param contentPadding The spacing values to apply internally between the container and the
 * content.
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    shape: Shape = FilledButton.ContainerShape,
    border: BorderStroke? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val containerColor = colors.containerColor(enabled).value
    val contentColor = colors.contentColor(enabled).value
    val shadowElevation = elevation?.shadowElevation(enabled, interactionSource)?.value ?: 0.dp
    val tonalElevation = elevation?.tonalElevation(enabled, interactionSource)?.value ?: 0.dp

    // TODO(b/202880001): Apply shadow color from token (will not be possibly any time soon, if ever).
    Surface(
        modifier = modifier.minimumTouchTargetSize(),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = shadowElevation,
        tonalElevation = tonalElevation,
        border = border,
    ) {
        val clickAndSemanticsModifier =
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(),
                enabled = true,
                onClickLabel = null,
                role = Role.Button,
                onClick = onClick
            )
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(value = Typography.LabelLarge) {
                Row(
                    Modifier.defaultMinSize(
                        minWidth = ButtonDefaults.MinWidth,
                        minHeight = ButtonDefaults.MinHeight
                    )
                        .then(clickAndSemanticsModifier)
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
 * ![Elevated button image](https://developer.android.com/images/reference/androidx/compose/material3/elevated-button.png)
 *
 * A Material Elevated button. Buttons contain actions for your app.
 *
 * Elevated buttons are high-emphasis buttons that are essentially [FilledTonalButton]s with a
 * shadow. To prevent shadow creep, only use them when absolutely necessary, such as when the button
 * requires visual separation from patterned container.
 *
 * @sample androidx.compose.material3.samples.ElevatedButtonSample
 *
 * Choose the best button for an action based on the amount of emphasis it needs. The more important
 * an action is, the higher emphasis its button should be.
 *
 * - See [Button] for a high-emphasis button without a shadow, also known as a filled button.
 * - See [FilledTonalButton] for a middle ground between [OutlinedButton] and [Button].
 * - See [OutlinedButton] for a medium-emphasis button with a border.
 * - See [TextButton] for a low-emphasis button with no border.
 *
 * The default text style for internal [Text] components will be set to [Typography.LabelLarge].
 *
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 * clickable.
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this Button. You can create and pass in your own remembered [MutableInteractionSource] if you
 * want to observe [Interaction]s and customize the appearance / behavior of this Button in
 * different [Interaction]s.
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 * states. This controls the size of the shadow below the button. When the container color is
 * [ColorScheme.surface], a higher elevation (surface blended with more primary) will result in a
 * darker surface color in light theme and lighter color in dark theme.
 * [ButtonDefaults.elevatedButtonElevation].
 * @param shape Defines the button's shape as well as its shadow.
 * @param border Border to draw around the button. Pass `null` here for no border.
 * @param colors [ButtonColors] that will be used to resolve the container and content color for
 * this button in different states. See [ButtonDefaults.elevatedButtonColors].
 * @param contentPadding The spacing values to apply internally between the container and the
 * content.
 */
@Composable
fun ElevatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ButtonElevation? = ButtonDefaults.elevatedButtonElevation(),
    shape: Shape = ElevatedButton.ContainerShape,
    border: BorderStroke? = null,
    colors: ButtonColors = ButtonDefaults.elevatedButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) =
    Button(
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
 * ![Filled tonal button image](https://developer.android.com/images/reference/androidx/compose/material3/filled-tonal-button.png)
 *
 * A Material Filled tonal button. Buttons contain actions for your app.
 *
 * Filled tonal buttons are medium-emphasis buttons that is an alternative middle ground between
 * default [Button]s (filled) and [OutlinedButton]s. They can be used in contexts where
 * lower-priority button requires slightly more emphasis than an outline would give, such as "Next"
 * in an onboarding flow. Tonal buttons use the secondary color mapping.
 *
 * @sample androidx.compose.material3.samples.FilledTonalButtonSample
 *
 * Choose the best button for an action based on the amount of emphasis it needs. The more important
 * an action is, the higher emphasis its button should be.
 *
 * - See [Button] for a high-emphasis button without a shadow, also known as a filled button.
 * - See [ElevatedButton] for a [FilledTonalButton] with a shadow.
 * - See [OutlinedButton] for a medium-emphasis button with a border.
 * - See [TextButton] for a low-emphasis button with no border.
 *
 * The default text style for internal [Text] components will be set to [Typography.LabelLarge].
 *
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 * clickable.
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this Button. You can create and pass in your own remembered [MutableInteractionSource] if you
 * want to observe [Interaction]s and customize the appearance / behavior of this Button in
 * different [Interaction]s.
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 * states. This controls the size of the shadow below the button. When the container color is
 * [ColorScheme.surface], a higher elevation (surface blended with more primary) will result in a
 * darker surface color in light theme and lighter color in dark theme.
 * @param shape Defines the button's shape as well as its shadow.
 * @param border Border to draw around the button. Pass `null` here for no border.
 * @param colors [ButtonColors] that will be used to resolve the container and content color for
 * this button in different states. See [ButtonDefaults.elevatedButtonColors].
 * @param contentPadding The spacing values to apply internally between the container and the
 * content.
 */
@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    shape: Shape = FilledButtonTonal.TonalContainerShape,
    border: BorderStroke? = null,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) =
    Button(
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
 * ![Outlined button image](https://developer.android.com/images/reference/androidx/compose/material3/outlined-button.png)
 *
 * A Material Outlined button. Buttons contain actions for your app.
 *
 * Outlined buttons are medium-emphasis buttons. They contain actions that are important, but are
 * not the primary action in an app. Outlined buttons pair well with [Button]s to indicate an
 * alternative, secondary action.
 *
 * @sample androidx.compose.material3.samples.OutlinedButtonSample
 *
 * Choose the best button for an action based on the amount of emphasis it needs. The more important
 * an action is, the higher emphasis its button should be.
 *
 * - See [Button] for a high-emphasis button without a shadow, also known as a filled button.
 * - See [FilledTonalButton] for a middle ground between [OutlinedButton] and [Button].
 * - See [OutlinedButton] for a medium-emphasis button with a border.
 * - See [TextButton] for a low-emphasis button with no border.
 *
 * The default text style for internal [Text] components will be set to [Typography.LabelLarge].
 *
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 * clickable.
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this Button. You can create and pass in your own remembered [MutableInteractionSource] if you
 * want to observe [Interaction]s and customize the appearance / behavior of this Button in
 * different [Interaction]s.
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 * states. This controls the size of the shadow below the button. When the container color is
 * [ColorScheme.surface], a higher elevation (surface blended with more primary) will result in a
 * darker surface color in light theme and lighter color in dark theme.
 * @param shape Defines the button's shape as well as its shadow.
 * @param border Border to draw around the button. Pass `null` here for no border.
 * @param colors [ButtonColors] that will be used to resolve the container and content color for
 * this button in different states. See [ButtonDefaults.elevatedButtonColors].
 * @param contentPadding The spacing values to apply internally between the container and the
 * content.
 */
@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ButtonElevation? = null,
    shape: Shape = OutlinedButton.ContainerShape,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) =
    Button(
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
 * ![Text button image](https://developer.android.com/images/reference/androidx/compose/material3/text-button.png)
 *
 * A Material Text button. Buttons contain actions for your app.
 *
 * Text buttons are typically used for less-pronounced actions, including those located in dialogs
 * and cards. In cards, text buttons help maintain an emphasis on card content. Text buttons are
 * used for the lowest priority actions, especially when presenting multiple options.
 *
 * @sample androidx.compose.material3.samples.TextButtonSample
 *
 * Choose the best button for an action based on the amount of emphasis it needs. The more important
 * an action is, the higher emphasis its button should be.
 *
 * - See [Button] for a high-emphasis button without a shadow, also known as a filled button.
 * - See [ElevatedButton] for a [FilledTonalButton] with a shadow.
 * - See [FilledTonalButton] for a middle ground between [OutlinedButton] and [Button].
 * - See [OutlinedButton] for a medium-emphasis button with a border.
 *
 * The default text style for internal [Text] components will be set to [Typography.LabelLarge].
 *
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 * clickable.
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this Button. You can create and pass in your own remembered [MutableInteractionSource] if you
 * want to observe [Interaction]s and customize the appearance / behavior of this Button in
 * different [Interaction]s.
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 * states. This controls the size of the shadow below the button. When the container color is
 * [ColorScheme.surface], a higher elevation (surface blended with more primary) will result in a
 * darker surface color in light theme and lighter color in dark theme. A TextButton typically has
 * no elevation, and the default value is `null`. See [ElevatedButton] for a button with elevation.
 * @param shape Defines the button's shape. A TextButton typically has no elevation or shadow, but
 * if a non-zero or non-null elevation is passed in, then the shape also defines the bounds of the
 * shadow.
 * @param border Border to draw around the button.
 * @param colors [ButtonColors] that will be used to resolve the container and content color for
 * this button in different states. See [ButtonDefaults.textButtonColors].
 * @param contentPadding The spacing values to apply internally between the container and the
 * content.
 */
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ButtonElevation? = null,
    shape: Shape = TextButton.ContainerShape,
    border: BorderStroke? = null,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    content: @Composable RowScope.() -> Unit
) =
    Button(
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

// TODO(b/201341237): Use token values for 0 elevation?
// TODO(b/201341237): Use token values for null border?
// TODO(b/201341237): Use token values for no color (transparent)?
/**
 * Contains the default values used by all 5 button types.
 *
 * Default values that apply to all buttons types are [MinWidth], [MinHeight], [IconSize], and
 * [IconSpacing].
 *
 * A default value that applies only to [Button], [ElevatedButton], [FilledTonalButton], and
 * [OutlinedButton] is [ContentPadding].
 *
 * Default values that apply only to [Button] are [buttonColors] and [buttonElevation].
 * Default values that apply only to [ElevatedButton] are [elevatedButtonColors] and [elevatedButtonElevation].
 * Default values that apply only to [FilledTonalButton] are [filledTonalButtonColors] and [filledTonalButtonElevation].
 * A default value that applies only to [OutlinedButton] is [outlinedButtonColors].
 * Default values that apply only to [TextButton] are [TextButtonContentPadding] and [textButtonColors].
 */
object ButtonDefaults {

    // TODO(b/201344013): Make sure these values stay up to date until replaced with tokens.
    private val ButtonHorizontalPadding = 24.dp

    // TODO(b/202453316): There is no current vertical padding in the spec.
    // Instead, the height is const 40dp, and the content is vertically center-aligned.
    private val ButtonVerticalPadding = 8.dp

    /**
     * The default content padding used by [Button], [ElevatedButton], [FilledTonalButton], and
     * [OutlinedButton] buttons.
     *
     * - See [TextButtonContentPadding] for content padding used by [TextButton].
     */
    // TODO(b/201343537): Use tokens.
    val ContentPadding =
        PaddingValues(
            start = ButtonHorizontalPadding,
            top = ButtonVerticalPadding,
            end = ButtonHorizontalPadding,
            bottom = ButtonVerticalPadding
        )

    // TODO(b/201344013): Make sure these values stay up to date until replaced with tokens.
    private val TextButtonHorizontalPadding = 12.dp

    /** The default content padding used by [TextButton] */
    val TextButtonContentPadding =
        PaddingValues(
            start = TextButtonHorizontalPadding,
            top = ContentPadding.calculateTopPadding(),
            end = TextButtonHorizontalPadding,
            bottom = ContentPadding.calculateBottomPadding()
        )

    /**
     * The default min width applied for all buttons. Note that you can override it by applying
     * Modifier.widthIn directly on the button composable.
     */
    // TODO(b/202453316): Make sure this value stays up to date until replaced with a token.
    val MinWidth = 58.dp

    /**
     * The default min height applied for all buttons. Note that you can override it by applying
     * Modifier.heightIn directly on the button composable.
     */
    // TODO(b/202453316): Make sure this value stays up to date until replaced with a token.
    val MinHeight = 40.dp

    /** The default size of the icon when used inside any button. */
    // TODO(b/201344013): Make sure this value stays up to date until replaced with a token.
    val IconSize = 18.dp

    /**
     * The default size of the spacing between an icon and a text when they used inside any button.
     */
    // TODO(b/201344013): Make sure this value stays up to date until replaced with a token.
    val IconSpacing = 8.dp

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in a
     * [Button].
     *
     * @param containerColor the container color of this [Button] when enabled.
     * @param contentColor the content color of this [Button] when enabled.
     * @param disabledContainerColor the container color of this [Button] when not enabled.
     * @param disabledContentColor the content color of this [Button] when not enabled.
     */
    @Composable
    fun buttonColors(
        containerColor: Color = MaterialTheme.colorScheme.fromToken(FilledButton.ContainerColor),
        contentColor: Color = MaterialTheme.colorScheme.fromToken(FilledButton.LabelTextColor),
        disabledContainerColor: Color =
            MaterialTheme.colorScheme
                .fromToken(FilledButton.DisabledContainerColor)
                .copy(alpha = FilledButton.DisabledContainerOpacity),
        disabledContentColor: Color =
            MaterialTheme.colorScheme
                .fromToken(FilledButton.DisabledLabelTextColor)
                .copy(alpha = FilledButton.DisabledLabelTextOpacity),
    ): ButtonColors =
        DefaultButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        )

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in an
     * [ElevatedButton].
     *
     * @param containerColor the container color of this [ElevatedButton] when enabled
     * @param contentColor the content color of this [ElevatedButton] when enabled
     * @param disabledContainerColor the container color of this [ElevatedButton] when not enabled
     * @param disabledContentColor the content color of this [ElevatedButton] when not enabled
     */
    @Composable
    fun elevatedButtonColors(
        containerColor: Color = MaterialTheme.colorScheme.fromToken(ElevatedButton.ContainerColor),
        contentColor: Color = MaterialTheme.colorScheme.fromToken(ElevatedButton.LabelTextColor),
        disabledContainerColor: Color =
            MaterialTheme.colorScheme
                .fromToken(ElevatedButton.DisabledContainerColor)
                .copy(alpha = ElevatedButton.DisabledContainerOpacity),
        disabledContentColor: Color =
            MaterialTheme.colorScheme
                .fromToken(ElevatedButton.DisabledLabelTextColor)
                .copy(alpha = ElevatedButton.DisabledLabelTextOpacity),
    ): ButtonColors =
        DefaultButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        )

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in an
     * [FilledTonalButton].
     *
     * @param containerColor the container color of this [FilledTonalButton] when enabled
     * @param contentColor the content color of this [FilledTonalButton] when enabled
     * @param disabledContainerColor the container color of this [FilledTonalButton] when not enabled
     * @param disabledContentColor the content color of this [FilledTonalButton] when not enabled
     */
    @Composable
    fun filledTonalButtonColors(
        containerColor: Color =
            MaterialTheme.colorScheme.fromToken(FilledButtonTonal.TonalContainerColor),
        contentColor: Color =
            MaterialTheme.colorScheme.fromToken(FilledButtonTonal.TonalLabelTextColor),
        disabledContainerColor: Color =
            MaterialTheme.colorScheme
                .fromToken(FilledButtonTonal.TonalDisabledContainerColor)
                .copy(alpha = FilledButtonTonal.TonalDisabledContainerOpacity),
        disabledContentColor: Color =
            MaterialTheme.colorScheme
                .fromToken(FilledButtonTonal.TonalDisabledLabelTextColor)
                .copy(alpha = FilledButtonTonal.TonalDisabledLabelTextOpacity),
    ): ButtonColors =
        DefaultButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        )

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in an
     * [OutlinedButton].
     *
     * @param containerColor the container color of this [OutlinedButton] when enabled
     * @param contentColor the content color of this [OutlinedButton] when enabled
     * @param disabledContainerColor the container color of this [OutlinedButton] when not enabled
     * @param disabledContentColor the content color of this [OutlinedButton] when not enabled
     */
    @Composable
    fun outlinedButtonColors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = MaterialTheme.colorScheme.fromToken(OutlinedButton.LabelTextColor),
        disabledContainerColor: Color = Color.Transparent,
        disabledContentColor: Color =
            MaterialTheme.colorScheme
                .fromToken(OutlinedButton.DisabledLabelTextColor)
                .copy(alpha = OutlinedButton.DisabledLabelTextOpacity),
    ): ButtonColors =
        DefaultButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        )

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in a
     * [TextButton].
     *
     * @param containerColor the container color of this [TextButton] when enabled
     * @param contentColor the content color of this [TextButton] when enabled
     * @param disabledContainerColor the container color of this [TextButton] when not enabled
     * @param disabledContentColor the content color of this [TextButton] when not enabled
     */
    @Composable
    fun textButtonColors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = MaterialTheme.colorScheme.fromToken(TextButton.LabelTextColor),
        disabledContainerColor: Color =
            MaterialTheme.colorScheme
                .fromToken(TextButton.DisabledContainerColor)
                .copy(alpha = TextButton.DisabledContainerOpacity),
        disabledContentColor: Color =
            MaterialTheme.colorScheme
                .fromToken(TextButton.DisabledLabelTextColor)
                .copy(alpha = TextButton.DisabledLabelTextOpacity),
    ): ButtonColors =
        DefaultButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        )

    /**
     * Creates a [ButtonElevation] that will animate between the provided values according to the
     * Material specification for a [Button].
     *
     * @param defaultElevation the elevation used when the [Button] is enabled, and has no other
     * [Interaction]s.
     * @param pressedElevation the elevation used when this [Button] is enabled and pressed.
     * @param focusedElevation the elevation used when the [Button] is enabled and focused.
     * @param hoveredElevation the elevation used when the [Button] is enabled and hovered.
     * @param disabledElevation the elevation used when the [Button] is not enabled.
     */
    @Composable
    fun buttonElevation(
        defaultElevation: Dp = FilledButton.ContainerElevation,
        pressedElevation: Dp = FilledButton.PressedContainerElevation,
        focusedElevation: Dp = FilledButton.FocusContainerElevation,
        hoveredElevation: Dp = FilledButton.HoverContainerElevation,
        disabledElevation: Dp = FilledButton.DisabledContainerElevation,
    ): ButtonElevation {
        return remember(
            defaultElevation,
            pressedElevation,
            focusedElevation,
            hoveredElevation,
            disabledElevation
        ) {
            DefaultButtonElevation(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                focusedElevation = focusedElevation,
                hoveredElevation = hoveredElevation,
                disabledElevation = disabledElevation,
            )
        }
    }

    /**
     * Creates a [ButtonElevation] that will animate between the provided values according to the
     * Material specification for a [ElevatedButton].
     *
     * @param defaultElevation the elevation used when the [ElevatedButton] is enabled, and has no
     * other [Interaction]s.
     * @param pressedElevation the elevation used when this [ElevatedButton] is enabled and pressed.
     * @param focusedElevation the elevation used when the [ElevatedButton] is enabled and focused.
     * @param hoveredElevation the elevation used when the [ElevatedButton] is enabled and hovered.
     * @param disabledElevation the elevation used when the [ElevatedButton] is not enabled.
     */
    @Composable
    fun elevatedButtonElevation(
        defaultElevation: Dp = ElevatedButton.ContainerElevation,
        pressedElevation: Dp = ElevatedButton.PressedContainerElevation,
        focusedElevation: Dp = ElevatedButton.FocusContainerElevation,
        hoveredElevation: Dp = ElevatedButton.HoverContainerElevation,
        disabledElevation: Dp = ElevatedButton.DisabledContainerElevation
    ): ButtonElevation {
        return remember(
            defaultElevation,
            pressedElevation,
            focusedElevation,
            hoveredElevation,
            disabledElevation
        ) {
            DefaultButtonElevation(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                focusedElevation = focusedElevation,
                hoveredElevation = hoveredElevation,
                disabledElevation = disabledElevation
            )
        }
    }

    /**
     * Creates a [ButtonElevation] that will animate between the provided values according to the
     * Material specification for a [FilledTonalButton].
     *
     * @param defaultElevation the elevation used when the [FilledTonalButton] is enabled, and has no
     * other [Interaction]s.
     * @param pressedElevation the elevation used when this [FilledTonalButton] is enabled and
     * pressed.
     * @param focusedElevation the elevation used when the [FilledTonalButton] is enabled and focused.
     * @param hoveredElevation the elevation used when the [FilledTonalButton] is enabled and hovered.
     * @param disabledElevation the elevation used when the [FilledTonalButton] is not enabled.
     */
    @Composable
    fun filledTonalButtonElevation(
        defaultElevation: Dp = FilledButtonTonal.TonalContainerElevation,
        pressedElevation: Dp = FilledButtonTonal.TonalPressedContainerElevation,
        focusedElevation: Dp = FilledButtonTonal.TonalFocusContainerElevation,
        hoveredElevation: Dp = FilledButtonTonal.TonalHoverContainerElevation,
        disabledElevation: Dp = 0.dp
    ): ButtonElevation {
        return remember(
            defaultElevation,
            pressedElevation,
            focusedElevation,
            hoveredElevation,
            disabledElevation
        ) {
            DefaultButtonElevation(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                focusedElevation = focusedElevation,
                hoveredElevation = hoveredElevation,
                disabledElevation = disabledElevation
            )
        }
    }

    /** The default [BorderStroke] used by [OutlinedButton]. */
    val outlinedButtonBorder: BorderStroke
        @Composable
        get() = BorderStroke(
            width = OutlinedButton.OutlineWidth,
            color = MaterialTheme.colorScheme.fromToken(OutlinedButton.OutlineColor)
        )
}

/**
 * Represents the elevation for a button in different states.
 *
 * - See [ButtonDefaults.buttonElevation] for the default elevation used in a [Button].
 * - See [ButtonDefaults.elevatedButtonElevation] for the default elevation used in a
 * [ElevatedButton].
 */
@Stable
interface ButtonElevation {
    /**
     * Represents the tonal elevation used in a button, depending on [enabled] and
     * [interactionSource]. This should typically be the same value as the [shadowElevation].
     *
     * Tonal elevation is used to apply a color shift to the surface to give the it higher emphasis.
     * When surface's color is [ColorScheme.surface], a higher the elevation will result
     * in a darker color in light theme and lighter color in dark theme.
     *
     * See [shadowElevation] which controls the elevation of the shadow drawn around the FAB.
     *
     * @param enabled whether the button is enabled
     * @param interactionSource the [InteractionSource] for this button
     */
    // TODO(b/202954622): Align docs with [FloatingActionButtonElevation].
    @Composable
    fun tonalElevation(enabled: Boolean, interactionSource: InteractionSource): State<Dp>

    /**
     * Represents the shadow elevation used in a button, depending on [enabled] and
     * [interactionSource]. This should typically be the same value as the [tonalElevation].
     *
     * Shadow elevation is used to apply a shadow around the surface to give it higher emphasis.
     *
     * See [tonalElevation] which controls the elevation with a color shift to the surface.
     *
     * @param enabled whether the button is enabled
     * @param interactionSource the [InteractionSource] for this button
     */
    // TODO(b/202954622): Align docs with [FloatingActionButtonElevation].
    @Composable
    fun shadowElevation(enabled: Boolean, interactionSource: InteractionSource): State<Dp>
}

/**
 * Represents the container and content colors used in a button in different states.
 *
 * - See [ButtonDefaults.buttonColors] for the default colors used in a [Button].
 * - See [ButtonDefaults.elevatedButtonColors] for the default colors used in a [ElevatedButton].
 * - See [ButtonDefaults.textButtonColors] for the default colors used in a [TextButton].
 */
@Stable
interface ButtonColors {
    /**
     * Represents the container color for this button, depending on [enabled].
     *
     * @param enabled whether the button is enabled
     */
    @Composable
    fun containerColor(enabled: Boolean): State<Color>

    /**
     * Represents the content color for this button, depending on [enabled].
     *
     * @param enabled whether the button is enabled
     */
    @Composable
    fun contentColor(enabled: Boolean): State<Color>
}

/** Default [ButtonElevation] implementation. */
@Stable
private class DefaultButtonElevation(
    private val defaultElevation: Dp,
    private val pressedElevation: Dp,
    private val focusedElevation: Dp,
    private val hoveredElevation: Dp,
    private val disabledElevation: Dp,
) : ButtonElevation {
    @Composable
    override fun tonalElevation(enabled: Boolean, interactionSource: InteractionSource): State<Dp> {
        return animateElevation(enabled = enabled, interactionSource = interactionSource)
    }

    @Composable
    override fun shadowElevation(
        enabled: Boolean,
        interactionSource: InteractionSource
    ): State<Dp> {
        return animateElevation(enabled = enabled, interactionSource = interactionSource)
    }

    @Composable
    private fun animateElevation(
        enabled: Boolean,
        interactionSource: InteractionSource
    ): State<Dp> {
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

        val target =
            if (!enabled) {
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
            LaunchedEffect(target) { animatable.snapTo(target) }
        } else {
            LaunchedEffect(target) {
                // TODO(b/203232064): Use lastInteraction and animateElevation from Elevation.kt.
                animatable.snapTo(target)
            }
        }

        return animatable.asState()
    }
}

/** Default [ButtonColors] implementation. */
@Immutable
private class DefaultButtonColors(
    private val containerColor: Color,
    private val contentColor: Color,
    private val disabledContainerColor: Color,
    private val disabledContentColor: Color,
) : ButtonColors {
    @Composable
    override fun containerColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) containerColor else disabledContainerColor)
    }

    @Composable
    override fun contentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) contentColor else disabledContentColor)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultButtonColors

        if (containerColor != other.containerColor) return false
        if (contentColor != other.contentColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (disabledContentColor != other.disabledContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        return result
    }
}