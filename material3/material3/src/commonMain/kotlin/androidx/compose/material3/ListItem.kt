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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.tokens.ListTokens
import androidx.compose.material3.tokens.TypographyKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * <a href="https://m3.material.io/components/lists/overview" class="external" target="_blank">Material Design list item.</a>
 *
 * Lists are continuous, vertical indexes of text or images.
 *
 * ![Lists image](https://developer.android.com/images/reference/androidx/compose/material3/lists.png)
 *
 * This component can be used to achieve the list item templates existing in the spec. One-line list
 * items have a singular line of headline content. Two-line list items additionally have either
 * supporting or overline content. Three-line list items have either both supporting and overline
 * content, or extended (two-line) supporting text. For example:
 * - one-line item
 * @sample androidx.compose.material3.samples.OneLineListItem
 * - two-line item
 * @sample androidx.compose.material3.samples.TwoLineListItem
 * - three-line item
 * @sample androidx.compose.material3.samples.ThreeLineListItem
 *
 * @param headlineContent the headline content of the list item
 * @param modifier [Modifier] to be applied to the list item
 * @param overlineContent the content displayed above the headline content
 * @param supportingContent the supporting content of the list item
 * @param leadingContent the leading content of the list item
 * @param trailingContent the trailing meta text, icon, switch or checkbox
 * @param colors [ListItemColors] that will be used to resolve the background and content color for
 * this list item in different states. See [ListItemDefaults.colors]
 * @param tonalElevation the tonal elevation of this list item
 * @param shadowElevation the shadow elevation of this list item
 */
@Composable
fun ListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    colors: ListItemColors = ListItemDefaults.colors(),
    tonalElevation: Dp = ListItemDefaults.Elevation,
    shadowElevation: Dp = ListItemDefaults.Elevation,
) {
    val decoratedHeadlineContent: @Composable () -> Unit = {
        ProvideTextStyleFromToken(
            colors.headlineColor(enabled = true).value,
            ListTokens.ListItemLabelTextFont,
            headlineContent
        )
    }
    val decoratedSupportingContent: @Composable (() -> Unit)? = supportingContent?.let {
        @Composable {
            ProvideTextStyleFromToken(
                colors.supportingColor().value,
                ListTokens.ListItemSupportingTextFont,
                it
            )
        }
    }
    val decoratedOverlineContent: @Composable (() -> Unit)? = overlineContent?.let {
        @Composable {
            ProvideTextStyleFromToken(
                colors.overlineColor().value,
                ListTokens.ListItemOverlineFont,
                it
            )
        }
    }

    val listItemType = ListItemType.getListItemType(
        hasOverline = decoratedOverlineContent != null,
        hasSupporting = decoratedSupportingContent != null
    )

    val decoratedLeadingContent: @Composable (RowScope.() -> Unit)? = leadingContent?.let {
        {
            LeadingContent(
                contentColor = colors.leadingIconColor(enabled = true).value,
                topAlign = listItemType == ListItemType.ThreeLine,
                content = it
            )
        }
    }

    val decoratedTrailingContent: @Composable (RowScope.() -> Unit)? = trailingContent?.let {
        {
            TrailingContent(
                contentColor = colors.trailingIconColor(enabled = true).value,
                topAlign = listItemType == ListItemType.ThreeLine,
                content = it
            )
        }
    }
    val minHeight: Dp = when (listItemType) {
        ListItemType.OneLine -> ListTokens.ListItemOneLineContainerHeight
        ListItemType.TwoLine -> ListTokens.ListItemTwoLineContainerHeight
        else -> ListTokens.ListItemThreeLineContainerHeight // 3
    }
    val outerPaddingValues =
        PaddingValues(
            horizontal = ListItemHorizontalPadding,
            vertical = if (listItemType == ListItemType.ThreeLine)
                ListItemThreeLineVerticalPadding else ListItemVerticalPadding
        )
    val contentPaddingValues = PaddingValues(
        end = if (listItemType == ListItemType.ThreeLine) ContentEndPadding else 0.dp
    )
    val columnArrangement = if (listItemType == ListItemType.ThreeLine)
        Arrangement.Top else Arrangement.Center
    val boxAlignment = if (listItemType == ListItemType.ThreeLine)
        Alignment.Top else CenterVertically

    ListItem(
        modifier = modifier,
        containerColor = colors.containerColor().value,
        contentColor = colors.headlineColor(enabled = true).value,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        minHeight = minHeight,
        paddingValues = outerPaddingValues
    ) {
        if (decoratedLeadingContent != null) {
            decoratedLeadingContent()
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(contentPaddingValues)
                .align(boxAlignment),
            verticalArrangement = columnArrangement
        ) {
            if (decoratedOverlineContent != null) {
                decoratedOverlineContent()
            }
            decoratedHeadlineContent()
            if (decoratedSupportingContent != null) {
                decoratedSupportingContent()
            }
        }
        if (decoratedTrailingContent != null) {
            decoratedTrailingContent()
        }
    }
}

// TODO(b/233782301): Complete 3-line list item
/**
 * <a href="https://m3.material.io/components/lists/overview" class="external" target="_blank">Material Design list item.</a>
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
private fun ListItem(
    modifier: Modifier = Modifier,
    shape: Shape = ListItemDefaults.shape,
    containerColor: Color = ListItemDefaults.containerColor,
    contentColor: Color = ListItemDefaults.contentColor,
    tonalElevation: Dp = ListItemDefaults.Elevation,
    shadowElevation: Dp = ListItemDefaults.Elevation,
    minHeight: Dp,
    paddingValues: PaddingValues,
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
        Row(
            modifier = Modifier
                .heightIn(min = minHeight)
                .padding(paddingValues)
                .semantics(mergeDescendants = true) {},
            content = content
        )
    }
}

@Composable
private fun RowScope.LeadingContent(
    contentColor: Color,
    topAlign: Boolean,
    content: @Composable () -> Unit,
) = CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            Modifier
                .padding(end = LeadingContentEndPadding)
                .then(if (!topAlign) Modifier.align(CenterVertically) else Modifier),
        ) { content() }
    }

@Composable
private fun RowScope.TrailingContent(
    contentColor: Color,
    topAlign: Boolean,
    content: @Composable () -> Unit,
) = Box(
    Modifier
        .padding(horizontal = TrailingHorizontalPadding)
        .then(if (!topAlign) Modifier.align(CenterVertically) else Modifier),
    ) {
        ProvideTextStyleFromToken(
            contentColor,
            ListTokens.ListItemTrailingSupportingTextFont,
            content
        )
    }

/**
 * Contains the default values used by list items.
 */
object ListItemDefaults {
    /** The default elevation of a list item */
    val Elevation: Dp = ListTokens.ListItemContainerElevation

    /** The default shape of a list item */
    val shape: Shape
        @Composable
        @ReadOnlyComposable get() = ListTokens.ListItemContainerShape.toShape()

    /** The container color of a list item */
    val containerColor: Color
        @Composable
        @ReadOnlyComposable get() = ListTokens.ListItemContainerColor.toColor()

    /** The content color of a list item */
    val contentColor: Color
        @Composable
        @ReadOnlyComposable get() = ListTokens.ListItemLabelTextColor.toColor()

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
        ListItemColors(
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
@Immutable
class ListItemColors internal constructor(
    private val containerColor: Color,
    private val headlineColor: Color,
    private val leadingIconColor: Color,
    private val overlineColor: Color,
    private val supportingTextColor: Color,
    private val trailingIconColor: Color,
    private val disabledHeadlineColor: Color,
    private val disabledLeadingIconColor: Color,
    private val disabledTrailingIconColor: Color,
) {
    /** The container color of this [ListItem] based on enabled state */
    @Composable
    internal fun containerColor(): State<Color> {
        return rememberUpdatedState(containerColor)
    }

    /** The color of this [ListItem]'s headline text based on enabled state */
    @Composable
    internal fun headlineColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) headlineColor else disabledHeadlineColor
        )
    }

    /** The color of this [ListItem]'s leading content based on enabled state */
    @Composable
    internal fun leadingIconColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) leadingIconColor else disabledLeadingIconColor
        )
    }

    /** The color of this [ListItem]'s overline text based on enabled state */
    @Composable
    internal fun overlineColor(): State<Color> {
        return rememberUpdatedState(overlineColor)
    }

    /** The color of this [ListItem]'s supporting text based on enabled state */
    @Composable
    internal fun supportingColor(): State<Color> {
        return rememberUpdatedState(supportingTextColor)
    }

    /** The color of this [ListItem]'s trailing content based on enabled state */
    @Composable
    internal fun trailingIconColor(enabled: Boolean): State<Color> {
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

/**
 * Helper class to define list item type. Used for padding and sizing definition.
 */
@JvmInline
private value class ListItemType private constructor(private val lines: Int) :
    Comparable<ListItemType> {

    override operator fun compareTo(other: ListItemType) = lines.compareTo(other.lines)

    companion object {
        /** One line list item */
        val OneLine = ListItemType(1)

        /** Two line list item */
        val TwoLine = ListItemType(2)

        /** Three line list item */
        val ThreeLine = ListItemType(3)

        internal fun getListItemType(hasOverline: Boolean, hasSupporting: Boolean): ListItemType {
            return when {
                hasOverline && hasSupporting -> ThreeLine
                hasOverline || hasSupporting -> TwoLine
                else -> OneLine
            }
        }
    }
}

// Container related defaults
// TODO: Make sure these values stay up to date until replaced with tokens.
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
