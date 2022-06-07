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

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.tokens.PrimaryNavigationTabTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import kotlin.math.max

// TODO: Provide M3 tab asset and docs when available.
/**
 * Material Design tab.
 *
 * A default Tab, also known as a Primary Navigation Tab. Tabs organize content across different
 * screens, data sets, and other interactions.
 *
 * A Tab represents a single page of content using a text label and/or icon. It represents its
 * selected state by tinting the text label and/or image with [selectedContentColor].
 *
 * This should typically be used inside of a [TabRow], see the corresponding documentation for
 * example usage.
 *
 * This Tab has slots for [text] and/or [icon] - see the other Tab overload for a generic Tab
 * that is not opinionated about its content.
 *
 * @param selected whether this tab is selected or not
 * @param onClick called when this tab is clicked
 * @param modifier the [Modifier] to be applied to this tab
 * @param enabled controls the enabled state of this tab. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param text the text label displayed in this tab
 * @param icon the icon displayed in this tab
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this tab. You can create and pass in your own `remember`ed instance to observe [Interaction]s
 * and customize the appearance / behavior of this tab in different states.
 * @param selectedContentColor the color for the content of this tab when selected, and the color
 * of the ripple.
 * @param unselectedContentColor the color for the content of this tab when not selected
 *
 * @see LeadingIconTab
 */
@Composable
fun Tab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor
) {
    val styledText: @Composable (() -> Unit)? = text?.let {
        @Composable {
            val style =
                MaterialTheme.typography.fromToken(PrimaryNavigationTabTokens.LabelTextFont)
                .copy(textAlign = TextAlign.Center)
            ProvideTextStyle(style, content = text)
        }
    }
    Tab(
        selected,
        onClick,
        modifier,
        enabled,
        interactionSource,
        selectedContentColor,
        unselectedContentColor
    ) {
        TabBaselineLayout(icon = icon, text = styledText)
    }
}

// TODO: Provide M3 tab asset and docs when available.
/**
 * Material Design tab.
 *
 * Tabs organize content across different screens, data sets, and other interactions.
 *
 * A LeadingIconTab represents a single page of content using a text label and an icon in
 * front of the label.
 * It represents its selected state by tinting the text label and icon with [selectedContentColor].
 *
 * This should typically be used inside of a [TabRow], see the corresponding documentation for
 * example usage.
 *
 * @param selected whether this tab is selected or not
 * @param onClick called when this tab is clicked
 * @param text the text label displayed in this tab
 * @param icon the icon displayed in this tab
 * @param modifier the [Modifier] to be applied to this tab
 * @param enabled controls the enabled state of this tab. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this tab. You can create and pass in your own `remember`ed instance to observe [Interaction]s
 * and customize the appearance / behavior of this tab in different states.
 * @param selectedContentColor the color for the content of this tab when selected, and the color
 * of the ripple.
 * @param unselectedContentColor the color for the content of this tab when not selected
 *
 * @see Tab
 */
@Composable
fun LeadingIconTab(
    selected: Boolean,
    onClick: () -> Unit,
    text: @Composable (() -> Unit),
    icon: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor
) {
    // The color of the Ripple should always the be selected color, as we want to show the color
    // before the item is considered selected, and hence before the new contentColor is
    // provided by TabTransition.
    val ripple = rememberRipple(bounded = true, color = selectedContentColor)

    TabTransition(selectedContentColor, unselectedContentColor, selected) {
        Row(
            modifier = modifier
                .height(SmallTabHeight)
                .selectable(
                    selected = selected,
                    onClick = onClick,
                    enabled = enabled,
                    role = Role.Tab,
                    interactionSource = interactionSource,
                    indication = ripple
                )
                .padding(horizontal = HorizontalTextPadding)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(Modifier.requiredWidth(TextDistanceFromLeadingIcon))
            val style = MaterialTheme.typography.fromToken(PrimaryNavigationTabTokens.LabelTextFont)
                .copy(textAlign = TextAlign.Center)
            ProvideTextStyle(style, content = text)
        }
    }
}

// TODO: Provide M3 tab asset and docs when available.
/**
 * Material Design tab.
 *
 * Tabs organize content across different screens, data sets, and other interactions.
 *
 * Generic [Tab] overload that is not opinionated about content / color. See the other overload
 * for a Tab that has specific slots for text and / or an icon, as well as providing the correct
 * colors for selected / unselected states.
 *
 * A custom tab using this API may look like:
 *
 * @sample androidx.compose.material3.samples.FancyTab
 *
 * @param selected whether this tab is selected or not
 * @param onClick called when this tab is clicked
 * @param modifier the [Modifier] to be applied to this tab
 * @param enabled controls the enabled state of this tab. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this tab. You can create and pass in your own `remember`ed instance to observe [Interaction]s
 * and customize the appearance / behavior of this tab in different states.
 * @param selectedContentColor the color for the content of this tab when selected, and the color
 * of the ripple.
 * @param unselectedContentColor the color for the content of this tab when not selected
 * @param content the content of this tab
 */
@Composable
fun Tab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor,
    content: @Composable ColumnScope.() -> Unit
) {
    // The color of the Ripple should always the selected color, as we want to show the color
    // before the item is considered selected, and hence before the new contentColor is
    // provided by TabTransition.
    val ripple = rememberRipple(bounded = true, color = selectedContentColor)

    TabTransition(selectedContentColor, unselectedContentColor, selected) {
        Column(
            modifier = modifier
                .selectable(
                    selected = selected,
                    onClick = onClick,
                    enabled = enabled,
                    role = Role.Tab,
                    interactionSource = interactionSource,
                    indication = ripple
                )
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content
        )
    }
}

/**
 * Transition defining how the tint color for a tab animates, when a new tab is selected. This
 * component uses [LocalContentColor] to provide an interpolated value between [activeColor]
 * and [inactiveColor] depending on the animation status.
 */
@Composable
private fun TabTransition(
    activeColor: Color,
    inactiveColor: Color,
    selected: Boolean,
    content: @Composable () -> Unit
) {
    val transition = updateTransition(selected)
    val color by transition.animateColor(
        transitionSpec = {
            if (false isTransitioningTo true) {
                tween(
                    durationMillis = TabFadeInAnimationDuration,
                    delayMillis = TabFadeInAnimationDelay,
                    easing = LinearEasing
                )
            } else {
                tween(
                    durationMillis = TabFadeOutAnimationDuration,
                    easing = LinearEasing
                )
            }
        }
    ) {
        if (it) activeColor else inactiveColor
    }
    CompositionLocalProvider(
        LocalContentColor provides color,
        content = content
    )
}

/**
 * A [Layout] that positions [text] and an optional [icon] with the correct baseline distances. This
 * Layout will either be [SmallTabHeight] or [LargeTabHeight] depending on its content, and then
 * place the text and/or icon inside with the correct baseline alignment.
 */
@Composable
private fun TabBaselineLayout(
    text: @Composable (() -> Unit)?,
    icon: @Composable (() -> Unit)?
) {
    Layout(
        {
            if (text != null) {
                Box(
                    Modifier.layoutId("text").padding(horizontal = HorizontalTextPadding)
                ) { text() }
            }
            if (icon != null) {
                Box(Modifier.layoutId("icon")) { icon() }
            }
        }
    ) { measurables, constraints ->
        val textPlaceable = text?.let {
            measurables.first { it.layoutId == "text" }.measure(
                // Measure with loose constraints for height as we don't want the text to take up more
                // space than it needs
                constraints.copy(minHeight = 0)
            )
        }

        val iconPlaceable = icon?.let {
            measurables.first { it.layoutId == "icon" }.measure(constraints)
        }

        val tabWidth = max(textPlaceable?.width ?: 0, iconPlaceable?.width ?: 0)

        val tabHeight = if (textPlaceable != null && iconPlaceable != null) {
            LargeTabHeight
        } else {
            SmallTabHeight
        }.roundToPx()

        val firstBaseline = textPlaceable?.get(FirstBaseline)
        val lastBaseline = textPlaceable?.get(LastBaseline)

        layout(tabWidth, tabHeight) {
            when {
                textPlaceable != null && iconPlaceable != null -> placeTextAndIcon(
                    density = this@Layout,
                    textPlaceable = textPlaceable,
                    iconPlaceable = iconPlaceable,
                    tabWidth = tabWidth,
                    tabHeight = tabHeight,
                    firstBaseline = firstBaseline!!,
                    lastBaseline = lastBaseline!!
                )
                textPlaceable != null -> placeTextOrIcon(textPlaceable, tabHeight)
                iconPlaceable != null -> placeTextOrIcon(iconPlaceable, tabHeight)
                else -> {
                }
            }
        }
    }
}

/**
 * Places the provided [textOrIconPlaceable] in the vertical center of the provided
 * [tabHeight].
 */
private fun Placeable.PlacementScope.placeTextOrIcon(
    textOrIconPlaceable: Placeable,
    tabHeight: Int
) {
    val contentY = (tabHeight - textOrIconPlaceable.height) / 2
    textOrIconPlaceable.placeRelative(0, contentY)
}

/**
 * Places the provided [textPlaceable] offset from the bottom of the tab using the correct
 * baseline offset, with the provided [iconPlaceable] placed above the text using the correct
 * baseline offset.
 */
private fun Placeable.PlacementScope.placeTextAndIcon(
    density: Density,
    textPlaceable: Placeable,
    iconPlaceable: Placeable,
    tabWidth: Int,
    tabHeight: Int,
    firstBaseline: Int,
    lastBaseline: Int
) {
    val baselineOffset = if (firstBaseline == lastBaseline) {
        SingleLineTextBaselineWithIcon
    } else {
        DoubleLineTextBaselineWithIcon
    }

    // Total offset between the last text baseline and the bottom of the Tab layout
    val textOffset = with(density) {
        baselineOffset.roundToPx() + PrimaryNavigationTabTokens.ActiveIndicatorHeight.roundToPx()
    }

    // How much space there is between the top of the icon (essentially the top of this layout)
    // and the top of the text layout's bounding box (not baseline)
    val iconOffset = with(density) {
        iconPlaceable.height + IconDistanceFromBaseline.roundToPx() - firstBaseline
    }

    val textPlaceableX = (tabWidth - textPlaceable.width) / 2
    val textPlaceableY = tabHeight - lastBaseline - textOffset
    textPlaceable.placeRelative(textPlaceableX, textPlaceableY)

    val iconPlaceableX = (tabWidth - iconPlaceable.width) / 2
    val iconPlaceableY = textPlaceableY - iconOffset
    iconPlaceable.placeRelative(iconPlaceableX, iconPlaceableY)
}

// Tab specifications
private val SmallTabHeight = PrimaryNavigationTabTokens.ContainerHeight
private val LargeTabHeight = 72.dp

// Tab transition specifications
private const val TabFadeInAnimationDuration = 150
private const val TabFadeInAnimationDelay = 100
private const val TabFadeOutAnimationDuration = 100

// The horizontal padding on the left and right of text
private val HorizontalTextPadding = 16.dp

// Distance from the top of the indicator to the text baseline when there is one line of text and an
// icon
private val SingleLineTextBaselineWithIcon = 14.dp
// Distance from the top of the indicator to the last text baseline when there are two lines of text
// and an icon
private val DoubleLineTextBaselineWithIcon = 6.dp
// Distance from the first text baseline to the bottom of the icon in a combined tab
private val IconDistanceFromBaseline = 20.sp
// Distance from the end of the leading icon to the start of the text
private val TextDistanceFromLeadingIcon = 8.dp
