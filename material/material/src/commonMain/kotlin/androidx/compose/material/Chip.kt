/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Material Design implementation of an
 * [action Chip](https://material.io/components/chips#action-chips).
 *
 * Action chips offer actions related to primary content. They should appear dynamically and
 * contextually in a UI.
 *
 * @sample androidx.compose.material.samples.ChipSample
 *
 * You can create an [outlined action chip](https://material.io/components/chips#action-chips)
 * using [ChipDefaults.outlinedChipColors]
 * @sample androidx.compose.material.samples.OutlinedChipWithIconSample
 *
 * Action chips should appear in a set and can be horizontally scrollable
 * @sample androidx.compose.material.samples.ChipGroupSingleLineSample
 *
 * Alternatively, use Accompanist's [Flow Layouts](https://google.github.io/accompanist/flowlayout/)
 * to wrap chips to a new line.
 *
 * @param onClick called when the chip is clicked. If null, then this chip will be considered
 * read-only unless something else handles its input events and updates its state.
 * @param modifier Modifier to be applied to the chip
 * @param enabled When disabled, chip will not respond to user input. It will also appear visually
 * disabled and disabled to accessibility services.
 * @param interactionSource The [MutableInteractionSource] represents the stream of [Interaction]s
 * for this chip. You can create and pass in your own remembered [MutableInteractionSource] if you
 * want to observe [Interaction]s and customize the appearance / behavior of this [*component*] in
 * different [Interaction]s.
 * @param border Border to draw around the chip. Pass `null` here for no border.
 * @param colors [ChipColors] that will be used to resolve the background and content color for
 * this chip in different states. See [ChipDefaults.chipColors].
 * @param leadingIcon Optional icon at the start of the chip, preceding the content text.
 * @param content the content of this chip
 */
@ExperimentalMaterialApi
@Composable
fun Chip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50)),
    border: BorderStroke? = null,
    colors: ChipColors = ChipDefaults.chipColors(),
    leadingIcon: @Composable (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val contentColor by colors.contentColor(enabled)
    Surface(
        modifier = modifier,
        shape = shape,
        color = colors.backgroundColor(enabled).value,
        contentColor = contentColor.copy(1.0f),
        border = border,
        enabled = enabled,
        onClick = onClick,
        role = Role.Button,
        interactionSource = interactionSource,
        indication = rememberRipple(),
    ) {
        CompositionLocalProvider(LocalContentAlpha provides contentColor.alpha) {
            ProvideTextStyle(
                value = MaterialTheme.typography.body2
            ) {
                Row(
                    Modifier
                        .defaultMinSize(
                            minHeight = ChipDefaults.MinHeight
                        )
                        .padding(
                            start = if (leadingIcon == null) {
                                HorizontalPadding
                            } else 0.dp,
                            end = HorizontalPadding,
                        ),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (leadingIcon != null) {
                        Spacer(Modifier.width(LeadingIconStartSpacing))
                        val leadingIconContentColor by colors.leadingIconContentColor(enabled)
                        CompositionLocalProvider(
                            LocalContentColor provides leadingIconContentColor,
                            LocalContentAlpha provides leadingIconContentColor.alpha,
                            content = leadingIcon
                        )
                        Spacer(Modifier.width(LeadingIconEndSpacing))
                    }
                    content()
                }
            }
        }
    }
}

/**
 * Represents the background and content colors used in a clickable chip in different states.
 *
 * See [ChipDefaults.chipColors] for the default colors used in a filled [Chip].
 * See [ChipDefaults.outlinedChipColors] for the default colors used in a outlined [Chip],
 */
@Stable
@ExperimentalMaterialApi
interface ChipColors {
    /**
     * Represents the background color for this chip, depending on [enabled].
     *
     * @param enabled whether the chip is enabled
     */
    @Composable
    fun backgroundColor(enabled: Boolean): State<Color>

    /**
     * Represents the content color for this chip, depending on [enabled], see [leadingIconContentColor].
     *
     * @param enabled whether the chip is enabled
     */
    @Composable
    fun contentColor(enabled: Boolean): State<Color>

    /**
     * Represents the leading icon's content color for this chip, depending on [enabled].
     *
     * @param enabled whether the chip is enabled
     */
    @Composable
    fun leadingIconContentColor(enabled: Boolean): State<Color>
}

/**
 * Contains the baseline values used by chips.
 */
@ExperimentalMaterialApi
object ChipDefaults {
    /**
     * The min height applied for a chip.
     * Note that you can override it by applying Modifier.height directly on a chip.
     */
    val MinHeight = 32.dp

    /**
     * Creates a [ChipColors] that represents the default background and content colors used in
     * a filled [Chip].
     *
     * @param backgroundColor the background color of this chip when enabled
     * @param contentColor the content color of this chip when enabled, there is a separate param
     * for icon colors
     * @param leadingIconContentColor the color of this chip's start icon when enabled
     * @param disabledBackgroundColor the background color of this chip when not enabled
     * @param disabledContentColor the content color of this chip when not enabled
     * @param disabledLeadingIconContentColor the color of this chip's start icon when not enabled
     */
    @Composable
    fun chipColors(
        backgroundColor: Color = MaterialTheme.colors.onSurface.copy(alpha = SurfaceOverlayOpacity)
            .compositeOver(MaterialTheme.colors.surface),
        contentColor: Color = MaterialTheme.colors.onSurface.copy(alpha = ContentOpacity),
        leadingIconContentColor: Color = contentColor.copy(alpha = LeadingIconOpacity),
        disabledBackgroundColor: Color =
            MaterialTheme.colors.onSurface.copy(
                alpha = ContentAlpha.disabled * SurfaceOverlayOpacity
            ).compositeOver(MaterialTheme.colors.surface),
        disabledContentColor: Color = contentColor.copy(
            alpha = ContentAlpha.disabled * ContentOpacity
        ),
        disabledLeadingIconContentColor: Color =
            leadingIconContentColor.copy(alpha = ContentAlpha.disabled * LeadingIconOpacity),
    ): ChipColors = DefaultChipColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        leadingIconContentColor = leadingIconContentColor,
        disabledBackgroundColor = disabledBackgroundColor,
        disabledContentColor = disabledContentColor,
        disabledLeadingIconContentColor = disabledLeadingIconContentColor,
    )

    /**
     * Creates a [ChipColors] that represents the default background and content colors used in
     * an outlined [Chip].
     *
     * @param backgroundColor the background color of this chip when enabled
     * @param contentColor the content color of this chip when enabled, there is a separate param
     * for icon colors
     * @para leadingIconContentColor the color of this chip's start icon when enabled
     * @param disabledBackgroundColor the background color of this chip when not enabled
     * @param disabledContentColor the content color of this chip when not enabled
     * @param disabledLeadingIconContentColor the color of this chip's start icon when not enabled
     */
    @Composable
    fun outlinedChipColors(
        backgroundColor: Color = MaterialTheme.colors.surface,
        contentColor: Color = MaterialTheme.colors.onSurface.copy(alpha = ContentOpacity),
        leadingIconContentColor: Color = contentColor.copy(alpha = LeadingIconOpacity),
        disabledBackgroundColor: Color = backgroundColor,
        disabledContentColor: Color = contentColor.copy(
            alpha = ContentAlpha.disabled * ContentOpacity
        ),
        disabledLeadingIconContentColor: Color =
            leadingIconContentColor.copy(alpha = ContentAlpha.disabled * LeadingIconOpacity),
    ): ChipColors = chipColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        leadingIconContentColor = leadingIconContentColor,
        disabledBackgroundColor = disabledBackgroundColor,
        disabledContentColor = disabledContentColor,
        disabledLeadingIconContentColor = disabledLeadingIconContentColor
    )

    /**
     * The border used by all types of outlined chips
     */
    val outlinedBorder: BorderStroke
        @Composable
        get() = BorderStroke(
            OutlinedBorderSize, MaterialTheme.colors.onSurface.copy(alpha = OutlinedBorderOpacity)
        )

    /**
     * The color opacity used for chip's leading icon color
     */
    const val LeadingIconOpacity = 0.54f

    /**
     * The color opacity used for chip's content color
     */
    const val ContentOpacity = 0.87f

    /**
     * The color opacity used for the outlined chip's border color
     */
    const val OutlinedBorderOpacity = 0.12f

    /**
     * The outlined chip's border size
     */
    val OutlinedBorderSize = 1.dp
}

/**
 * Default [ChipColors] implementation.
 */
@ExperimentalMaterialApi
@Immutable
private class DefaultChipColors(
    private val backgroundColor: Color,
    private val contentColor: Color,
    private val leadingIconContentColor: Color,
    private val disabledBackgroundColor: Color,
    private val disabledContentColor: Color,
    private val disabledLeadingIconContentColor: Color
    // TODO(b/113855296): Support other states: hover, focus, drag
) : ChipColors {
    @Composable
    override fun backgroundColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) backgroundColor else disabledBackgroundColor)
    }

    @Composable
    override fun contentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) contentColor else disabledContentColor)
    }

    @Composable
    override fun leadingIconContentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) leadingIconContentColor else disabledLeadingIconContentColor
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultChipColors

        if (backgroundColor != other.backgroundColor) return false
        if (contentColor != other.contentColor) return false
        if (leadingIconContentColor != other.leadingIconContentColor) return false
        if (disabledBackgroundColor != other.disabledBackgroundColor) return false
        if (disabledContentColor != other.disabledContentColor) return false
        if (disabledLeadingIconContentColor != other.disabledLeadingIconContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backgroundColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + leadingIconContentColor.hashCode()
        result = 31 * result + disabledBackgroundColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        result = 31 * result + disabledLeadingIconContentColor.hashCode()

        return result
    }
}

/**
 * The content padding used by a chip.
 * Used as start padding when there's leading icon, used as eng padding when there's no
 * trailing icon.
 */
private val HorizontalPadding = 12.dp

/**
 * The size of the spacing before the leading icon when they used inside a chip.
 */
private val LeadingIconStartSpacing = 4.dp

/**
 * The size of the spacing between the leading icon and a text inside a chip.
 */
private val LeadingIconEndSpacing = 8.dp

/**
 * The color opacity used for chip's surface overlay
 */
private const val SurfaceOverlayOpacity = 0.12f
