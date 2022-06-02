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

package androidx.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.tokens.ListTokens
import androidx.compose.material3.tokens.TypographyKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// TODO: Provide M3 ListItem asset and doc link when available
/**
 * Material Design list item.
 *
 * Lists are continuous, vertical indexes of text or images.
 *
 * This component can be used to achieve the list item templates existing in the spec. One-line list
 * items have a singular line of headline text. Two-line list items additionally have either
 * supporting or overline text. Three-line list items have either both supporting and overline text,
 * or extended (two-line) supporting text. For example:
 * - one-line item
 * @sample androidx.compose.material3.samples.OneLineListItem
 * - two-line item
 * @sample androidx.compose.material3.samples.TwoLineListItem
 * - three-line item
 * @sample androidx.compose.material3.samples.ThreeLineListItem
 *
 * @param headlineText the headline text of the list item
 * @param modifier [Modifier] to be applied to the list item
 * @param overlineText the text displayed above the headline text
 * @param supportingText the supporting text of the list item
 * @param leadingContent the leading supporting visual of the list item
 * @param trailingContent the trailing meta text, icon, switch or checkbox
 * @param colors [ListItemColors] that will be used to resolve the background and content color for
 * this list item in different states. See [ListItemDefaults.colors]
 * @param tonalElevation the tonal elevation of this list item
 * @param shadowElevation the shadow elevation of this list item
 */
@Composable
@ExperimentalMaterial3Api
fun ListItem(
    headlineText: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overlineText: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    colors: ListItemColors = ListItemDefaults.colors(),
    tonalElevation: Dp = ListItemDefaults.Elevation,
    shadowElevation: Dp = ListItemDefaults.Elevation,
) {
    if (overlineText == null && supportingText == null) {
        // One-Line List Item
        ListItem(
            modifier = modifier
                .heightIn(min = ListTokens.ListItemContainerHeight)
                .padding(
                    vertical = ListItemVerticalPadding,
                    horizontal = ListItemHorizontalPadding
                ),
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
        ) {
            if (leadingContent != null) {
                leadingContent(
                    leadingContent = leadingContent,
                    contentColor = colors.leadingIconColor(enabled = true).value,
                    topAlign = false
                )()
            }
            Box(
                Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                ProvideTextStyleFromToken(
                    colors.headlineColor(enabled = true).value,
                    ListTokens.ListItemLabelTextFont,
                    headlineText
                )
            }
            if (trailingContent != null) {
                trailingContent(
                    trailingContent = trailingContent,
                    contentColor = colors.trailingIconColor(enabled = true).value,
                    topAlign = false
                )()
            }
        }
    } else if (overlineText == null) {
        // Two-Line List Item
        ListItem(
            modifier = modifier
                .heightIn(min = TwoLineListItemContainerHeight)
                .padding(
                    vertical = ListItemVerticalPadding,
                    horizontal = ListItemHorizontalPadding
                ),
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
        ) {
            if (leadingContent != null) {
                leadingContent(
                    leadingContent = leadingContent,
                    contentColor = colors.leadingIconColor(enabled = true).value,
                    topAlign = false
                )()
            }
            Box(
                Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                Column {
                    ProvideTextStyleFromToken(
                        colors.headlineColor(enabled = true).value,
                        ListTokens.ListItemLabelTextFont,
                        headlineText
                    )
                    ProvideTextStyleFromToken(
                        colors.supportingColor(enabled = true).value,
                        ListTokens.ListItemSupportingTextFont,
                        supportingText!!
                    )
                }
            }
            if (trailingContent != null) {
                trailingContent(
                    trailingContent = trailingContent,
                    contentColor = colors.trailingIconColor(enabled = true).value,
                    topAlign = false
                )()
            }
        }
    } else if (supportingText == null) {
        // Two-Line List Item
        ListItem(
            modifier = modifier
                .heightIn(min = TwoLineListItemContainerHeight)
                .padding(
                    vertical = ListItemVerticalPadding,
                    horizontal = ListItemHorizontalPadding
                ),
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
        ) {
            if (leadingContent != null) {
                leadingContent(
                    leadingContent = leadingContent,
                    contentColor = colors.leadingIconColor(enabled = true).value,
                    topAlign = false
                )()
            }
            Box(
                Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                Column {
                    ProvideTextStyleFromToken(
                        colors.overlineColor(enabled = true).value,
                        ListTokens.ListItemOverlineFont,
                        overlineText
                    )
                    ProvideTextStyleFromToken(
                        colors.headlineColor(enabled = true).value,
                        ListTokens.ListItemLabelTextFont,
                        headlineText
                    )
                }
            }
            if (trailingContent != null) {
                trailingContent(
                    trailingContent = trailingContent,
                    contentColor = colors.trailingIconColor(enabled = true).value,
                    topAlign = false
                )()
            }
        }
    } else {
        // Three-Line List Item
        ListItem(
            modifier = modifier
                .heightIn(min = ThreeLineListItemContainerHeight)
                .padding(
                    vertical = ListItemThreeLineVerticalPadding,
                    horizontal = ListItemHorizontalPadding
                ),
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
        ) {
            if (leadingContent != null) {
                leadingContent(
                    leadingContent = leadingContent,
                    contentColor = colors.leadingIconColor(enabled = true).value,
                    topAlign = true
                )()
            }
            Box(
                Modifier
                    .weight(1f)
                    .padding(end = ContentEndPadding),
            ) {
                Column {
                    ProvideTextStyleFromToken(
                        colors.overlineColor(enabled = true).value,
                        ListTokens.ListItemOverlineFont,
                        overlineText
                    )
                    ProvideTextStyleFromToken(
                        colors.headlineColor(enabled = true).value,
                        ListTokens.ListItemLabelTextFont,
                        headlineText
                    )
                    ProvideTextStyleFromToken(
                        colors.supportingColor(enabled = true).value,
                        ListTokens.ListItemSupportingTextFont,
                        supportingText
                    )
                }
            }
            if (trailingContent != null) {
                trailingContent(
                    trailingContent = trailingContent,
                    contentColor = colors.trailingIconColor(enabled = true).value,
                    topAlign = true
                )()
            }
        }
    }
}

// TODO(b/233782301): Complete 3-line list item
// TODO: Provide M3 ListItem asset and doc link when available
/**
 * Material Design list item.
 *
 * Lists are continuous, vertical indexes of text or images. For more opinionated List Items,
 * consider using another overload
 *
 * @param modifier [Modifier] to be applied to the list item
 * @param shape defines the list item's shape
 * @param containerColor the container color of this list item
 * @param contentColor the content color of this list item
 * @param tonalElevation the tonal elevation of this list item
 * @param shadowElevation the shadow elevation of this list item
 * @param content the content to be displayed in the middle section of this list item
 */
@Composable
@ExperimentalMaterial3Api
private fun ListItem(
    modifier: Modifier = Modifier,
    shape: Shape = ListItemDefaults.Shape,
    containerColor: Color = ListItemDefaults.ContainerColor,
    contentColor: Color = ListItemDefaults.ContentColor,
    tonalElevation: Dp = ListItemDefaults.Elevation,
    shadowElevation: Dp = ListItemDefaults.Elevation,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
    ) {
        Row(content = content)
    }
}

@Composable
private fun leadingContent(
    leadingContent: @Composable (() -> Unit),
    contentColor: Color,
    topAlign: Boolean,
): @Composable RowScope.() -> Unit {
    return {
        CompositionLocalProvider(
            LocalContentColor provides contentColor) {
            if (topAlign) {
                Box(Modifier
                    .padding(end = LeadingContentEndPadding),
                    contentAlignment = Alignment.TopStart
                ) { leadingContent() }
            } else {
                Box(
                    Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = LeadingContentEndPadding)
                ) { leadingContent() }
            }
        }
    }
}

@Composable
private fun trailingContent(
    trailingContent: @Composable (() -> Unit),
    contentColor: Color,
    topAlign: Boolean,
): @Composable RowScope.() -> Unit {
    return {
        if (topAlign) {
            Box(Modifier
                .padding(horizontal = TrailingHorizontalPadding),
                contentAlignment = Alignment.TopStart
            ) {
                ProvideTextStyleFromToken(
                    contentColor,
                    ListTokens.ListItemTrailingSupportingTextFont,
                    trailingContent
                ) }
        } else {
            Box(
                Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = TrailingHorizontalPadding)
            ) {
                ProvideTextStyleFromToken(
                    contentColor,
                    ListTokens.ListItemTrailingSupportingTextFont,
                    trailingContent
                ) }
        }
    }
}

/**
 * Contains the default values used by list items.
 */
object ListItemDefaults {
    /** The default shape of a list item */
    val Shape: Shape @Composable get() = ListTokens.ListItemContainerShape.toShape()

    /** The default elevation of a list item */
    val Elevation: Dp = ListTokens.ListItemContainerElevation

    /** The container color of a list item */
    val ContainerColor: Color @Composable get() = ListTokens.ListItemContainerColor.toColor()

    /** The content color of a list item */
    val ContentColor: Color @Composable get() = ListTokens.ListItemLabelTextColor.toColor()

    /**
     * Creates a [ListItemColors] that represents the default container and content colors used in a
     * [ListItem].
     *
     * @param containerColor the container color of this list item when enabled.
     * @param headlineColor the headline text content color of this list item when
     * enabled.
     * @param leadingIconColor the color of this list item's leading content when enabled.
     * @param overlineColor the overline text color of this list item
     * @param supportingColor the supporting text color of this list item
     * @param trailingIconColor the color of this list item's trailing content when enabled.
     * @param disabledHeadlineColor the content color of this list item when not enabled.
     * @param disabledLeadingIconColor the color of this list item's leading content when not
     * enabled.
     * @param disabledTrailingIconColor the color of this list item's trailing content when not
     * enabled.
     */
    @Composable
    fun colors(
        containerColor: Color = ListTokens.ListItemContainerColor.toColor(),
        headlineColor: Color = ListTokens.ListItemLabelTextColor.toColor(),
        leadingIconColor: Color = ListTokens.ListItemLeadingIconColor.toColor(),
        overlineColor: Color = ListTokens.ListItemOverlineColor.toColor(),
        supportingColor: Color = ListTokens.ListItemSupportingTextColor.toColor(),
        trailingIconColor: Color = ListTokens.ListItemTrailingIconColor.toColor(),
        disabledHeadlineColor: Color = ListTokens.ListItemDisabledLabelTextColor.toColor()
            .copy(alpha = ListTokens.ListItemDisabledLabelTextOpacity),
        disabledLeadingIconColor: Color = ListTokens.ListItemDisabledLeadingIconColor.toColor()
            .copy(alpha = ListTokens.ListItemDisabledLeadingIconOpacity),
        disabledTrailingIconColor: Color = ListTokens.ListItemDisabledTrailingIconColor.toColor()
            .copy(alpha = ListTokens.ListItemDisabledTrailingIconOpacity)
    ): ListItemColors =
        DefaultListItemColors(
            containerColor = containerColor,
            headlineColor = headlineColor,
            leadingIconColor = leadingIconColor,
            overlineColor = overlineColor,
            supportingTextColor = supportingColor,
            trailingIconColor = trailingIconColor,
            disabledHeadlineColor = disabledHeadlineColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
        )
}

/**
 * Represents the container and content colors used in a list item in different states.
 *
 * - See [ListItemDefaults.colors] for the default colors used in a [ListItem].
 */
@Stable
interface ListItemColors {

    /** The container color of this [ListItem] based on enabled state */
    @Composable
    fun containerColor(enabled: Boolean): State<Color>

    /** The color of this [ListItem]'s headline text based on enabled state */
    @Composable
    fun headlineColor(enabled: Boolean): State<Color>

    /** The color of this [ListItem]'s leading content based on enabled state */
    @Composable
    fun leadingIconColor(enabled: Boolean): State<Color>

    /** The color of this [ListItem]'s overline text based on enabled state */
    @Composable
    fun overlineColor(enabled: Boolean): State<Color>

    /** The color of this [ListItem]'s supporting text based on enabled state */
    @Composable
    fun supportingColor(enabled: Boolean): State<Color>

    /** The color of this [ListItem]'s trailing content based on enabled state */
    @Composable
    fun trailingIconColor(enabled: Boolean): State<Color>
}

/** Default [ListItemColors] implementation. */
@Immutable
private class DefaultListItemColors(
    private val containerColor: Color,
    private val headlineColor: Color,
    private val leadingIconColor: Color,
    private val overlineColor: Color,
    private val supportingTextColor: Color,
    private val trailingIconColor: Color,
    private val disabledHeadlineColor: Color,
    private val disabledLeadingIconColor: Color,
    private val disabledTrailingIconColor: Color,
) : ListItemColors {
    @Composable
    override fun containerColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(containerColor)
    }

    @Composable
    override fun headlineColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) headlineColor else disabledHeadlineColor
        )
    }

    @Composable
    override fun leadingIconColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) leadingIconColor else disabledLeadingIconColor
        )
    }

    @Composable
    override fun overlineColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(overlineColor)
    }

    @Composable
    override fun supportingColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(supportingTextColor)
    }

    @Composable
    override fun trailingIconColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) trailingIconColor else disabledTrailingIconColor
        )
    }
}

@Composable
private fun ProvideTextStyleFromToken(
    color: Color,
    textToken: TypographyKeyTokens,
    content: @Composable () -> Unit,
) {
    val textStyle = MaterialTheme.typography.fromToken(textToken)
    CompositionLocalProvider(LocalContentColor provides color) {
        ProvideTextStyle(textStyle, content)
    }
}

// Container related defaults
// TODO: Make sure these values stay up to date until replaced with tokens.
private val TwoLineListItemContainerHeight = 72.dp
private val ThreeLineListItemContainerHeight = 88.dp
private val ListItemVerticalPadding = 8.dp
private val ListItemThreeLineVerticalPadding = 16.dp
private val ListItemHorizontalPadding = 16.dp

// Icon related defaults.
// TODO: Make sure these values stay up to date until replaced with tokens.
private val LeadingContentEndPadding = 16.dp

// Content related defaults.
// TODO: Make sure these values stay up to date until replaced with tokens.
private val ContentEndPadding = 8.dp

// Trailing related defaults.
// TODO: Make sure these values stay up to date until replaced with tokens.
private val TrailingHorizontalPadding = 8.dp
