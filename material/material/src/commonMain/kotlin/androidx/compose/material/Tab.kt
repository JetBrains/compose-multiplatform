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

import androidx.compose.animation.ColorPropKey
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateAsState
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.core.tween
import androidx.compose.animation.transition
import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

/**
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
 * @param onClick the callback to be invoked when this tab is selected
 * @param modifier optional [Modifier] for this tab
 * @param text the text label displayed in this tab
 * @param icon the icon displayed in this tab
 * @param interactionState the [InteractionState] representing the different [Interaction]s
 * present on this Tab. You can create and pass in your own remembered [InteractionState] if
 * you want to read the [InteractionState] and customize the appearance / behavior of this Tab
 * in different [Interaction]s.
 * @param selectedContentColor the color for the content of this tab when selected, and the color
 * of the ripple.
 * @param unselectedContentColor the color for the content of this tab when not selected
 */
@Composable
fun Tab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit = emptyContent(),
    icon: @Composable () -> Unit = emptyContent(),
    interactionState: InteractionState = remember { InteractionState() },
    selectedContentColor: Color = AmbientContentColor.current,
    unselectedContentColor: Color = selectedContentColor.copy(alpha = ContentAlpha.medium)
) {
    val styledText = @Composable {
        val style = MaterialTheme.typography.button.copy(textAlign = TextAlign.Center)
        ProvideTextStyle(style, content = text)
    }
    Tab(
        selected,
        onClick,
        modifier,
        interactionState,
        selectedContentColor,
        unselectedContentColor
    ) {
        TabBaselineLayout(icon = icon, text = styledText)
    }
}

/**
 * Generic [Tab] overload that is not opinionated about content / color. See the other overload
 * for a Tab that has specific slots for text and / or an icon, as well as providing the correct
 * colors for selected / unselected states.
 *
 * A custom tab using this API may look like:
 *
 * @sample androidx.compose.material.samples.FancyTab
 *
 * @param selected whether this tab is selected or not
 * @param onClick the callback to be invoked when this tab is selected
 * @param modifier optional [Modifier] for this tab
 * @param interactionState the [InteractionState] representing the different [Interaction]s
 * present on this Tab. You can create and pass in your own remembered [InteractionState] if
 * you want to read the [InteractionState] and customize the appearance / behavior of this Tab
 * in different [Interaction]s.
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
    interactionState: InteractionState = remember { InteractionState() },
    selectedContentColor: Color = AmbientContentColor.current,
    unselectedContentColor: Color = selectedContentColor.copy(alpha = ContentAlpha.medium),
    content: @Composable ColumnScope.() -> Unit
) {
    // The color of the Ripple should always the selected color, as we want to show the color
    // before the item is considered selected, and hence before the new contentColor is
    // provided by TabTransition.
    val ripple = rememberRipple(bounded = false, color = selectedContentColor)

    TabTransition(selectedContentColor, unselectedContentColor, selected) {
        Column(
            modifier = modifier
                .selectable(
                    selected = selected,
                    onClick = onClick,
                    interactionState = interactionState,
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
 * Contains default values used by tabs from the Material specification.
 */
@Deprecated(
    "TabConstants has been replaced with TabDefaults",
    ReplaceWith(
        "TabDefaults",
        "androidx.compose.material.TabDefaults"
    )
)
object TabConstants {
    /**
     * Default [Divider], which will be positioned at the bottom of the [TabRow], underneath the
     * indicator.
     *
     * @param modifier modifier for the divider's layout
     * @param thickness thickness of the divider
     * @param color color of the divider
     */
    @Composable
    fun DefaultDivider(
        modifier: Modifier = Modifier,
        thickness: Dp = DefaultDividerThickness,
        color: Color = AmbientContentColor.current.copy(alpha = DefaultDividerOpacity)
    ) {
        Divider(modifier = modifier, thickness = thickness, color = color)
    }

    /**
     * Default indicator, which will be positioned at the bottom of the [TabRow], on top of the
     * divider.
     *
     * @param modifier modifier for the indicator's layout
     * @param height height of the indicator
     * @param color color of the indicator
     */
    @Composable
    fun DefaultIndicator(
        modifier: Modifier = Modifier,
        height: Dp = DefaultIndicatorHeight,
        color: Color = AmbientContentColor.current
    ) {
        Box(
            modifier
                .fillMaxWidth()
                .preferredHeight(height)
                .background(color = color)
        )
    }

    /**
     * [Modifier] that takes up all the available width inside the [TabRow], and then animates
     * the offset of the indicator it is applied to, depending on the [currentTabPosition].
     *
     * @param currentTabPosition [TabPosition] of the currently selected tab. This is used to
     * calculate the offset of the indicator this modifier is applied to, as well as its width.
     */
    fun Modifier.defaultTabIndicatorOffset(
        currentTabPosition: TabPosition
    ): Modifier = composed(
        inspectorInfo = debugInspectorInfo {
            name = "defaultTabIndicatorOffset"
            value = currentTabPosition
        }
    ) {
        // TODO: should we animate the width of the indicator as it moves between tabs of different
        // sizes inside a scrollable tab row?
        val currentTabWidth = currentTabPosition.width
        val indicatorOffset by animateAsState(
            targetValue = currentTabPosition.left,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
        )
        fillMaxWidth()
            .wrapContentSize(Alignment.BottomStart)
            .offset(x = indicatorOffset)
            .preferredWidth(currentTabWidth)
    }

    /**
     * Default opacity for the color of [DefaultDivider]
     */
    const val DefaultDividerOpacity = 0.12f

    /**
     * Default thickness for [DefaultDivider]
     */
    val DefaultDividerThickness = 1.dp

    /**
     * Default height for [DefaultIndicator]
     */
    val DefaultIndicatorHeight = 2.dp

    /**
     * The default padding from the starting edge before a tab in a [ScrollableTabRow].
     */
    val DefaultScrollableTabRowPadding = 52.dp
}

/**
 * Contains default values used by tabs from the Material specification.
 */
object TabDefaults {
    /**
     * Default [Divider], which will be positioned at the bottom of the [TabRow], underneath the
     * indicator.
     *
     * @param modifier modifier for the divider's layout
     * @param thickness thickness of the divider
     * @param color color of the divider
     */
    @Composable
    fun Divider(
        modifier: Modifier = Modifier,
        thickness: Dp = DividerThickness,
        color: Color = AmbientContentColor.current.copy(alpha = DividerOpacity)
    ) {
        androidx.compose.material.Divider(modifier = modifier, thickness = thickness, color = color)
    }

    /**
     * Default indicator, which will be positioned at the bottom of the [TabRow], on top of the
     * divider.
     *
     * @param modifier modifier for the indicator's layout
     * @param height height of the indicator
     * @param color color of the indicator
     */
    @Composable
    fun Indicator(
        modifier: Modifier = Modifier,
        height: Dp = IndicatorHeight,
        color: Color = AmbientContentColor.current
    ) {
        Box(
            modifier
                .fillMaxWidth()
                .preferredHeight(height)
                .background(color = color)
        )
    }

    /**
     * [Modifier] that takes up all the available width inside the [TabRow], and then animates
     * the offset of the indicator it is applied to, depending on the [currentTabPosition].
     *
     * @param currentTabPosition [TabPosition] of the currently selected tab. This is used to
     * calculate the offset of the indicator this modifier is applied to, as well as its width.
     */
    fun Modifier.tabIndicatorOffset(
        currentTabPosition: TabPosition
    ): Modifier = composed(
        inspectorInfo = debugInspectorInfo {
            name = "tabIndicatorOffset"
            value = currentTabPosition
        }
    ) {
        // TODO: should we animate the width of the indicator as it moves between tabs of different
        // sizes inside a scrollable tab row?
        val currentTabWidth = currentTabPosition.width
        val indicatorOffset by animateAsState(
            targetValue = currentTabPosition.left,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
        )
        fillMaxWidth()
            .wrapContentSize(Alignment.BottomStart)
            .offset(x = indicatorOffset)
            .preferredWidth(currentTabWidth)
    }

    /**
     * Default opacity for the color of [Divider]
     */
    const val DividerOpacity = 0.12f

    /**
     * Default thickness for [Divider]
     */
    val DividerThickness = 1.dp

    /**
     * Default height for [Indicator]
     */
    val IndicatorHeight = 2.dp

    /**
     * The default padding from the starting edge before a tab in a [ScrollableTabRow].
     */
    val ScrollableTabRowPadding = 52.dp
}

private val TabTintColor = ColorPropKey()

/**
 * [transition] defining how the tint color for a tab animates, when a new tab is selected. This
 * component uses [AmbientContentColor] to provide an interpolated value between [activeColor]
 * and [inactiveColor] depending on the animation status.
 */
@Composable
private fun TabTransition(
    activeColor: Color,
    inactiveColor: Color,
    selected: Boolean,
    content: @Composable () -> Unit
) {
    val transitionDefinition = remember(activeColor, inactiveColor) {
        transitionDefinition<Boolean> {
            state(true) {
                this[TabTintColor] = activeColor
            }

            state(false) {
                this[TabTintColor] = inactiveColor
            }

            transition(toState = false, fromState = true) {
                TabTintColor using tween(
                    durationMillis = TabFadeInAnimationDuration,
                    delayMillis = TabFadeInAnimationDelay,
                    easing = LinearEasing
                )
            }

            transition(fromState = true, toState = false) {
                TabTintColor using tween(
                    durationMillis = TabFadeOutAnimationDuration,
                    easing = LinearEasing
                )
            }
        }
    }
    val state = transition(transitionDefinition, selected)
    val color = state[TabTintColor]
    Providers(
        AmbientContentColor provides color.copy(alpha = 1f),
        AmbientContentAlpha provides color.alpha,
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
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit
) {
    Layout(
        {
            Box(Modifier.layoutId("text").padding(horizontal = HorizontalTextPadding)) { text() }
            Box(Modifier.layoutId("icon")) { icon() }
        }
    ) { measurables, constraints ->
        val textPlaceable = measurables.first { it.layoutId == "text" }.measure(
            // Measure with loose constraints for height as we don't want the text to take up more
            // space than it needs
            constraints.copy(minHeight = 0)
        )

        val iconPlaceable = measurables.first { it.layoutId == "icon" }.measure(constraints)

        val hasTextPlaceable =
            textPlaceable.width != 0 && textPlaceable.height != 0

        val hasIconPlaceable =
            iconPlaceable.width != 0 && iconPlaceable.height != 0

        val tabWidth = max(textPlaceable.width, iconPlaceable.width)

        val tabHeight =
            (if (hasTextPlaceable && hasIconPlaceable) LargeTabHeight else SmallTabHeight).toIntPx()

        val firstBaseline = textPlaceable[FirstBaseline]
        val lastBaseline = textPlaceable[LastBaseline]

        layout(tabWidth, tabHeight) {
            when {
                hasTextPlaceable && hasIconPlaceable -> placeTextAndIcon(
                    density = this@Layout,
                    textPlaceable = textPlaceable,
                    iconPlaceable = iconPlaceable,
                    tabWidth = tabWidth,
                    tabHeight = tabHeight,
                    firstBaseline = firstBaseline,
                    lastBaseline = lastBaseline
                )
                hasTextPlaceable -> placeText(
                    density = this@Layout,
                    textPlaceable = textPlaceable,
                    tabHeight = tabHeight,
                    firstBaseline = firstBaseline,
                    lastBaseline = lastBaseline
                )
                hasIconPlaceable -> placeIcon(iconPlaceable, tabHeight)
                else -> {}
            }
        }
    }
}

/**
 * Places the provided [iconPlaceable] in the vertical center of the provided [tabHeight].
 */
private fun Placeable.PlacementScope.placeIcon(
    iconPlaceable: Placeable,
    tabHeight: Int
) {
    val iconY = (tabHeight - iconPlaceable.height) / 2
    iconPlaceable.placeRelative(0, iconY)
}

/**
 * Places the provided [textPlaceable] offset from the bottom of the tab using the correct
 * baseline offset.
 */
private fun Placeable.PlacementScope.placeText(
    density: Density,
    textPlaceable: Placeable,
    tabHeight: Int,
    firstBaseline: Int,
    lastBaseline: Int
) {
    val baselineOffset = if (firstBaseline == lastBaseline) {
        SingleLineTextBaseline
    } else {
        DoubleLineTextBaseline
    }

    // Total offset between the last text baseline and the bottom of the Tab layout
    val totalOffset = with(density) {
        baselineOffset.toIntPx() + TabDefaults.IndicatorHeight.toIntPx()
    }

    val textPlaceableY = tabHeight - lastBaseline - totalOffset
    textPlaceable.placeRelative(0, textPlaceableY)
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
        baselineOffset.toIntPx() + TabDefaults.IndicatorHeight.toIntPx()
    }

    // How much space there is between the top of the icon (essentially the top of this layout)
    // and the top of the text layout's bounding box (not baseline)
    val iconOffset = with(density) {
        iconPlaceable.height + IconDistanceFromBaseline.toIntPx() - firstBaseline
    }

    val textPlaceableX = (tabWidth - textPlaceable.width) / 2
    val textPlaceableY = tabHeight - lastBaseline - textOffset
    textPlaceable.placeRelative(textPlaceableX, textPlaceableY)

    val iconPlaceableX = (tabWidth - iconPlaceable.width) / 2
    val iconPlaceableY = textPlaceableY - iconOffset
    iconPlaceable.placeRelative(iconPlaceableX, iconPlaceableY)
}

// Tab specifications
private val SmallTabHeight = 48.dp
private val LargeTabHeight = 72.dp

// Tab transition specifications
private const val TabFadeInAnimationDuration = 150
private const val TabFadeInAnimationDelay = 100
private const val TabFadeOutAnimationDuration = 100

// The horizontal padding on the left and right of text
private val HorizontalTextPadding = 16.dp

// Distance from the top of the indicator to the text baseline when there is one line of text
private val SingleLineTextBaseline = 18.dp
// Distance from the top of the indicator to the text baseline when there is one line of text and an
// icon
private val SingleLineTextBaselineWithIcon = 14.dp
// Distance from the top of the indicator to the last text baseline when there are two lines of text
private val DoubleLineTextBaseline = 10.dp
// Distance from the top of the indicator to the last text baseline when there are two lines of text
// and an icon
private val DoubleLineTextBaselineWithIcon = 6.dp
// Distance from the first text baseline to the bottom of the icon in a combined tab
private val IconDistanceFromBaseline = 20.sp
