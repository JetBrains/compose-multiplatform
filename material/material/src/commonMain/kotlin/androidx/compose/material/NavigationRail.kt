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

package androidx.compose.material

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.VectorizedAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * <a href="https://material.io/components/navigation-rail" class="external" target="_blank">Material Design navigation rail</a>.
 *
 * The Navigation Rail is a side navigation component that allows movement between primary
 * destinations in an app. The navigation rail should be used to display three to seven app
 * destinations and, optionally, a  Floating Action Button or a logo header. Each destination is
 * typically represented by an icon and an optional text label.
 *
 * ![Navigation rail image](https://developer.android.com/images/reference/androidx/compose/material/navigation-rail.png)
 *
 * NavigationRail should contain multiple [NavigationRailItem]s, each representing a singular
 * destination.
 *
 * A simple example looks like:
 *
 * @sample androidx.compose.material.samples.NavigationRailSample
 *
 * See [NavigationRailItem] for configuration specific to each item, and not the overall
 * NavigationRail component.
 *
 * For more information, see [Navigation Rail](https://material.io/components/navigation-rail/)
 *
 * @param modifier optional [Modifier] for this NavigationRail
 * @param backgroundColor The background color for this NavigationRail
 * @param contentColor The preferred content color provided by this NavigationRail to its
 * children. Defaults to either the matching content color for [backgroundColor], or if
 * [backgroundColor] is not a color from the theme, this will keep the same value set above this
 * NavigationRail.
 * @param elevation elevation for this NavigationRail
 * @param header an optional header that may hold a Floating Action Button or a logo
 * @param content destinations inside this NavigationRail, this should contain multiple
 * [NavigationRailItem]s
 */
@ExperimentalMaterialApi
@Composable
fun NavigationRail(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    elevation: Dp = NavigationRailDefaults.Elevation,
    header: @Composable (ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = backgroundColor,
        contentColor = contentColor,
        elevation = elevation
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .padding(vertical = NavigationRailPadding)
                .selectableGroup(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (header != null) {
                header()
                Spacer(Modifier.height(HeaderPadding))
            }
            content()
        }
    }
}

/**
 * <a href="https://material.io/components/navigation-rail" class="external" target="_blank">Material Design navigation rail</a> item.
 *
 * A NavigationRailItem always shows text labels (if it exists) when selected. Showing text
 * labels if not selected is controlled by [alwaysShowLabel].
 *
 * @param selected whether this item is selected (active)
 * @param onClick the callback to be invoked when this item is selected
 * @param icon icon for this item, typically this will be an [Icon]
 * @param modifier optional [Modifier] for this item
 * @param enabled controls the enabled state of this item. When `false`, this item will not
 * be clickable and will appear disabled to accessibility services.
 * @param label optional text label for this item
 * @param alwaysShowLabel whether to always show the label for this item. If false, the label will
 * only be shown when this item is selected.
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this NavigationRailItem. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this NavigationRailItem in different [Interaction]s.
 * @param selectedContentColor the color of the text label and icon when this item is selected,
 * and the color of the ripple.
 * @param unselectedContentColor the color of the text label and icon when this item is not selected
 */
@ExperimentalMaterialApi
@Composable
fun NavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    selectedContentColor: Color = MaterialTheme.colors.primary,
    unselectedContentColor: Color = LocalContentColor.current.copy(alpha = ContentAlpha.medium)
) {
    val styledLabel: @Composable (() -> Unit)? = label?.let {
        @Composable {
            val style = MaterialTheme.typography.caption.copy(textAlign = TextAlign.Center)
            ProvideTextStyle(style, content = label)
        }
    }
    // Default to compact size when the item has no label, or a regular size when it does.
    // Any size value that was set on the given Modifier will take precedence and allow custom
    // sizing.
    val itemSize = if (label == null) NavigationRailItemCompactSize else NavigationRailItemSize
    // The color of the Ripple should always the selected color, as we want to show the color
    // before the item is considered selected, and hence before the new contentColor is
    // provided by NavigationRailTransition.
    val ripple = rememberRipple(
        bounded = false,
        color = selectedContentColor
    )
    Box(
        modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = ripple
            ).size(itemSize),
        contentAlignment = Alignment.Center
    ) {
        NavigationRailTransition(
            selectedContentColor,
            unselectedContentColor,
            selected
        ) { progress ->
            val animationProgress = if (alwaysShowLabel) 1f else progress

            NavigationRailItemBaselineLayout(
                icon = icon,
                label = styledLabel,
                iconPositionAnimationProgress = animationProgress
            )
        }
    }
}

/**
 * Contains default values used for [NavigationRail].
 */
@ExperimentalMaterialApi
object NavigationRailDefaults {
    /**
     * Default elevation used for [NavigationRail].
     */
    val Elevation = 8.dp
}

/**
 * Transition that animates [LocalContentColor] between [inactiveColor] and [activeColor], depending
 * on [selected]. This component also provides the animation fraction as a parameter to [content],
 * to allow animating the position of the icon and the scale of the label alongside this color
 * animation.
 *
 * @param activeColor [LocalContentColor] when this item is [selected]
 * @param inactiveColor [LocalContentColor] when this item is not [selected]
 * @param selected whether this item is selected
 * @param content the content of the [NavigationRailItem] to animate [LocalContentColor] for,
 * where the animationProgress is the current progress of the animation from 0f to 1f.
 */
@Composable
private fun NavigationRailTransition(
    activeColor: Color,
    inactiveColor: Color,
    selected: Boolean,
    content: @Composable (animationProgress: Float) -> Unit
) {
    val animationProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = NavigationRailAnimationSpec
    )

    val color = lerp(inactiveColor, activeColor, animationProgress)

    CompositionLocalProvider(
        LocalContentColor provides color.copy(alpha = 1f),
        LocalContentAlpha provides color.alpha,
    ) {
        content(animationProgress)
    }
}

/**
 * Base layout for a [NavigationRailItem]
 *
 * @param icon icon for this item
 * @param label text label for this item
 * @param iconPositionAnimationProgress progress of the animation that controls icon position,
 * where 0 represents its unselected position and 1 represents its selected position. If both the
 * [icon] and [label] should be shown at all times, this will always be 1, as the icon position
 * should remain constant.
 */
@Composable
private fun NavigationRailItemBaselineLayout(
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,
    /*@FloatRange(from = 0.0, to = 1.0)*/
    iconPositionAnimationProgress: Float
) {
    Layout(
        {
            Box(Modifier.layoutId("icon")) { icon() }
            if (label != null) {
                Box(
                    Modifier
                        .layoutId("label")
                        .alpha(iconPositionAnimationProgress)
                ) { label() }
            }
        }
    ) { measurables, constraints ->
        val iconPlaceable = measurables.first { it.layoutId == "icon" }.measure(constraints)

        val labelPlaceable = label?.let {
            measurables.first { it.layoutId == "label" }.measure(
                // Measure with loose constraints for height as we don't want the label to take up more
                // space than it needs
                constraints.copy(minHeight = 0)
            )
        }

        // If there is no label, just place the icon.
        if (label == null) {
            placeIcon(iconPlaceable, constraints)
        } else {
            placeLabelAndIcon(
                labelPlaceable!!,
                iconPlaceable,
                constraints,
                iconPositionAnimationProgress
            )
        }
    }
}

/**
 * Places the provided [iconPlaceable] in the vertical center of the provided [constraints]
 */
private fun MeasureScope.placeIcon(
    iconPlaceable: Placeable,
    constraints: Constraints
): MeasureResult {
    val iconX = max(0, (constraints.maxWidth - iconPlaceable.width) / 2)
    val iconY = max(0, (constraints.maxHeight - iconPlaceable.height) / 2)
    return layout(constraints.maxWidth, constraints.maxHeight) {
        iconPlaceable.placeRelative(iconX, iconY)
    }
}

/**
 * Places the provided [labelPlaceable] and [iconPlaceable] in the correct position, depending on
 * [iconPositionAnimationProgress].
 *
 * When [iconPositionAnimationProgress] is 0, [iconPlaceable] will be placed in the center, as with
 * [placeIcon], and [labelPlaceable] will not be shown.
 *
 * When [iconPositionAnimationProgress] is 1, [iconPlaceable] will be placed near the top of item,
 * and [labelPlaceable] will be placed at the bottom of the item, according to the spec.
 *
 * When [iconPositionAnimationProgress] is animating between these values, [iconPlaceable] will be
 * placed at an interpolated position between its centered position and final resting position.
 *
 * @param labelPlaceable text label placeable inside this item
 * @param iconPlaceable icon placeable inside this item
 * @param iconPositionAnimationProgress the progress of the icon position animation, where 0
 * represents centered icon and no label, and 1 represents top aligned icon with label.
 * Values between 0 and 1 interpolate the icon position so we can smoothly move the icon.
 */
private fun MeasureScope.placeLabelAndIcon(
    labelPlaceable: Placeable,
    iconPlaceable: Placeable,
    constraints: Constraints,
    /*@FloatRange(from = 0.0, to = 1.0)*/
    iconPositionAnimationProgress: Float
): MeasureResult {
    val baseline = labelPlaceable[LastBaseline]
    val labelBaselineOffset = ItemLabelBaselineBottomOffset.roundToPx()
    // Label should be [ItemLabelBaselineBottomOffset] from the bottom
    val labelY = constraints.maxHeight - baseline - labelBaselineOffset
    val labelX = (constraints.maxWidth - labelPlaceable.width) / 2

    // Icon should be [ItemIconTopOffset] from the top when selected
    val selectedIconY = ItemIconTopOffset.roundToPx()
    val unselectedIconY = (constraints.maxHeight - iconPlaceable.height) / 2
    val iconX = (constraints.maxWidth - iconPlaceable.width) / 2
    // How far the icon needs to move between unselected and selected states
    val iconDistance = unselectedIconY - selectedIconY

    // When selected the icon is above the unselected position, so we will animate moving
    // downwards from the selected state, so when progress is 1, the total distance is 0, and we
    // are at the selected state.
    val offset = (iconDistance * (1 - iconPositionAnimationProgress)).roundToInt()

    return layout(constraints.maxWidth, constraints.maxHeight) {
        if (iconPositionAnimationProgress != 0f) {
            labelPlaceable.placeRelative(labelX, labelY + offset)
        }
        iconPlaceable.placeRelative(iconX, selectedIconY + offset)
    }
}

/**
 * [VectorizedAnimationSpec] controlling the transition between unselected and selected
 * [NavigationRailItem]s.
 */
private val NavigationRailAnimationSpec = TweenSpec<Float>(
    durationMillis = 300,
    easing = FastOutSlowInEasing
)

/**
 * Size of a regular [NavigationRailItem].
 */
private val NavigationRailItemSize = 72.dp

/**
 * Size of a compact [NavigationRailItem].
 */
private val NavigationRailItemCompactSize = 56.dp

/**
 * Padding at the top and the bottom of the [NavigationRail]
 */
private val NavigationRailPadding = 8.dp

/**
 * Padding at the bottom of the [NavigationRail]'s header [Composable]. This padding will only be
 * added when the header is not null.
 */
private val HeaderPadding = 8.dp

/**
 * The space between the text label's baseline and the bottom of the container.
 */
private val ItemLabelBaselineBottomOffset = 16.dp

/**
 * The space between the icon and the top of the container when an item contains a label and icon.
 */
private val ItemIconTopOffset = 14.dp