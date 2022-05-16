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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.tokens.BottomAppBarTokens
import androidx.compose.material3.tokens.TopAppBarLargeTokens
import androidx.compose.material3.tokens.TopAppBarMediumTokens
import androidx.compose.material3.tokens.TopAppBarSmallCenteredTokens
import androidx.compose.material3.tokens.TopAppBarSmallTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * <a href="https://m3.material.io/components/top-app-bar/overview" class="external" target="_blank">Material Design small top app bar</a>.
 *
 * Top app bars display information and actions at the top of a screen.
 *
 * This SmallTopAppBar has slots for a title, navigation icon, and actions.
 *
 * ![Small top app bar image](https://developer.android.com/images/reference/androidx/compose/material3/small-top-app-bar.png)
 *
 * A simple top app bar looks like:
 * @sample androidx.compose.material3.samples.SimpleSmallTopAppBar
 * A top app bar that uses a [scrollBehavior] to customize its nested scrolling behavior when
 * working in conjunction with a scrolling content looks like:
 * @sample androidx.compose.material3.samples.PinnedSmallTopAppBar
 * @sample androidx.compose.material3.samples.EnterAlwaysSmallTopAppBar
 *
 * @param title the title to be displayed in the top app bar
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param navigationIcon the navigation icon displayed at the start of the top app bar. This should
 * typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 * [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app
 * bar in different states. See [TopAppBarDefaults.smallTopAppBarColors].
 * @param scrollBehavior a [TopAppBarScrollBehavior] which holds various offset values that will be
 * applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 * work in conjunction with a scrolled content to change the top app bar appearance as the content
 * scrolls. See [TopAppBarScrollBehavior.nestedScrollConnection].
 */
@Composable
fun SmallTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.smallTopAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    SingleRowTopAppBar(
        modifier = modifier,
        title = title,
        titleTextStyle = MaterialTheme.typography.fromToken(TopAppBarSmallTokens.HeadlineFont),
        centeredTitle = false,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors,
        scrollBehavior = scrollBehavior
    )
}

/**
 * <a href="https://m3.material.io/components/top-app-bar/overview" class="external" target="_blank">Material Design center-aligned small top app bar</a>.
 *
 * Top app bars display information and actions at the top of a screen.
 *
 * This small top app bar has a header title that is horizontally aligned to the center.
 *
 * ![Center-aligned top app bar image](https://developer.android.com/images/reference/androidx/compose/material3/center-aligned-top-app-bar.png)
 *
 * This CenterAlignedTopAppBar has slots for a title, navigation icon, and actions.
 *
 * A center aligned top app bar that uses a [scrollBehavior] to customize its nested scrolling
 * behavior when working in conjunction with a scrolling content looks like:
 * @sample androidx.compose.material3.samples.SimpleCenterAlignedTopAppBar
 *
 * @param title the title to be displayed in the top app bar
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param navigationIcon the navigation icon displayed at the start of the top app bar. This should
 * typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 * [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app
 * bar in different states. See [TopAppBarDefaults.centerAlignedTopAppBarColors].
 * @param scrollBehavior a [TopAppBarScrollBehavior] which holds various offset values that will be
 * applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 * work in conjunction with a scrolled content to change the top app bar appearance as the content
 * scrolls. See [TopAppBarScrollBehavior.nestedScrollConnection].
 */
@Composable
fun CenterAlignedTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    SingleRowTopAppBar(
        modifier = modifier,
        title = title,
        titleTextStyle =
        MaterialTheme.typography.fromToken(TopAppBarSmallTokens.HeadlineFont),
        centeredTitle = true,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors,
        scrollBehavior = scrollBehavior
    )
}

/**
 * <a href="https://m3.material.io/components/top-app-bar/overview" class="external" target="_blank">Material Design medium top app bar</a>.
 *
 * Top app bars display information and actions at the top of a screen.
 *
 * ![Medium top app bar image](https://developer.android.com/images/reference/androidx/compose/material3/medium-top-app-bar.png)
 *
 * This MediumTopAppBar has slots for a title, navigation icon, and actions. In its default expanded
 * state, the title is displayed in a second row under the navigation and actions.
 *
 * A medium top app bar that uses a [scrollBehavior] to customize its nested scrolling behavior when
 * working in conjunction with scrolling content looks like:
 * @sample androidx.compose.material3.samples.ExitUntilCollapsedMediumTopAppBar
 *
 * @param title the title to be displayed in the top app bar. This title will be used in the app
 * bar's expanded and collapsed states, although in its collapsed state it will be composed with a
 * smaller sized [TextStyle]
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param navigationIcon the navigation icon displayed at the start of the top app bar. This should
 * typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 * [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app
 * bar in different states. See [TopAppBarDefaults.mediumTopAppBarColors].
 * @param scrollBehavior a [TopAppBarScrollBehavior] which holds various offset values that will be
 * applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 * work in conjunction with a scrolled content to change the top app bar appearance as the content
 * scrolls. See [TopAppBarScrollBehavior.nestedScrollConnection].
 */
@Composable
fun MediumTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.mediumTopAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TwoRowsTopAppBar(
        modifier = modifier,
        title = title,
        titleTextStyle = MaterialTheme.typography.fromToken(TopAppBarMediumTokens.HeadlineFont),
        smallTitleTextStyle = MaterialTheme.typography.fromToken(TopAppBarSmallTokens.HeadlineFont),
        titleBottomPadding = MediumTitleBottomPadding,
        smallTitle = title,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors,
        maxHeight = TopAppBarMediumTokens.ContainerHeight,
        pinnedHeight = TopAppBarSmallTokens.ContainerHeight,
        scrollBehavior = scrollBehavior
    )
}

/**
 * <a href="https://m3.material.io/components/top-app-bar/overview" class="external" target="_blank">Material Design large top app bar</a>.
 *
 * Top app bars display information and actions at the top of a screen.
 *
 * ![Large top app bar image](https://developer.android.com/images/reference/androidx/compose/material3/large-top-app-bar.png)
 *
 * This LargeTopAppBar has slots for a title, navigation icon, and actions. In its default expanded
 * state, the title is displayed in a second row under the navigation and actions.
 *
 * A large top app bar that uses a [scrollBehavior] to customize its nested scrolling behavior when
 * working in conjunction with scrolling content looks like:
 * @sample androidx.compose.material3.samples.ExitUntilCollapsedLargeTopAppBar
 *
 * @param title the title to be displayed in the top app bar. This title will be used in the app
 * bar's expanded and collapsed states, although in its collapsed state it will be composed with a
 * smaller sized [TextStyle]
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param navigationIcon the navigation icon displayed at the start of the top app bar. This should
 * typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 * [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app
 * bar in different states. See [TopAppBarDefaults.largeTopAppBarColors].
 * @param scrollBehavior a [TopAppBarScrollBehavior] which holds various offset values that will be
 * applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 * work in conjunction with a scrolled content to change the top app bar appearance as the content
 * scrolls. See [TopAppBarScrollBehavior.nestedScrollConnection].
 */
@Composable
fun LargeTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.largeTopAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TwoRowsTopAppBar(
        title = title,
        titleTextStyle = MaterialTheme.typography.fromToken(TopAppBarLargeTokens.HeadlineFont),
        smallTitleTextStyle = MaterialTheme.typography.fromToken(TopAppBarSmallTokens.HeadlineFont),
        titleBottomPadding = LargeTitleBottomPadding,
        smallTitle = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors,
        maxHeight = TopAppBarLargeTokens.ContainerHeight,
        pinnedHeight = TopAppBarSmallTokens.ContainerHeight,
        scrollBehavior = scrollBehavior
    )
}

/**
 * <a href="https://m3.material.io/components/bottom-app-bar/overview" class="external" target="_blank">Material Design bottom app bar</a>.
 *
 * A bottom app bar displays navigation and key actions at the bottom of mobile screens.
 *
 * ![Bottom app bar image](https://developer.android.com/images/reference/androidx/compose/material3/bottom-app-bar.png)
 *
 * @sample androidx.compose.material3.samples.SimpleBottomAppBar
 *
 * It can optionally display a [FloatingActionButton] embedded at the end of the BottomAppBar.
 *
 * @sample androidx.compose.material3.samples.BottomAppBarWithFAB
 *
 * Also see [NavigationBar].
 *
 * @param icons the icon content of this BottomAppBar. The default layout here is a [Row],
 * so content inside will be placed horizontally.
 * @param modifier the [Modifier] to be applied to this BottomAppBar
 * @param floatingActionButton optional floating action button at the end of this BottomAppBar
 * @param containerColor the color used for the background of this BottomAppBar. Use
 * [Color.Transparent] to have no color.
 * @param contentColor the preferred color for content inside this BottomAppBar. Defaults to either
 * the matching content color for [containerColor], or to the current [LocalContentColor] if
 * [containerColor] is not a color from the theme.
 * @param tonalElevation when [containerColor] is [ColorScheme.surface], a translucent primary color
 * overlay is applied on top of the container. A higher tonal elevation value will result in a
 * darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param contentPadding the padding applied to the content of this BottomAppBar
 */
@Composable
fun BottomAppBar(
    icons: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    floatingActionButton: @Composable (() -> Unit)? = null,
    containerColor: Color = BottomAppBarTokens.ContainerColor.toColor(),
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = BottomAppBarTokens.ContainerElevation,
    contentPadding: PaddingValues = BottomAppBarDefaults.ContentPadding,
) = BottomAppBar(
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        contentPadding = contentPadding
    ) {
        icons()
        if (floatingActionButton != null) {
            Spacer(Modifier.weight(1f, true))
            Box(
                Modifier.fillMaxHeight().padding(
                    top = FABVerticalPadding,
                    end = FABHorizontalPadding),
                contentAlignment = Alignment.TopStart
            ) {
                floatingActionButton()
            }
        }
    }

/**
 * <a href="https://m3.material.io/components/bottom-app-bar/overview" class="external" target="_blank">Material Design bottom app bar</a>.
 *
 * A bottom app bar displays navigation and key actions at the bottom of mobile screens.
 *
 * ![Bottom app bar image](https://developer.android.com/images/reference/androidx/compose/material3/bottom-app-bar.png)
 *
 * If you are interested in displaying a [FloatingActionButton], consider using another overload.
 *
 * Also see [NavigationBar].
 *
 * @param modifier the [Modifier] to be applied to this BottomAppBar
 * @param containerColor the color used for the background of this BottomAppBar. Use
 * [Color.Transparent] to have no color.
 * @param contentColor the preferred color for content inside this BottomAppBar. Defaults to either
 * the matching content color for [containerColor], or to the current [LocalContentColor] if
 * [containerColor] is not a color from the theme.
 * @param tonalElevation when [containerColor] is [ColorScheme.surface], a translucent primary color
 * overlay is applied on top of the container. A higher tonal elevation value will result in a
 * darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param contentPadding the padding applied to the content of this BottomAppBar
 * @param content the content of this BottomAppBar. The default layout here is a [Row],
 * so content inside will be placed horizontally.
 */
@Composable
fun BottomAppBar(
    modifier: Modifier = Modifier,
    containerColor: Color = BottomAppBarTokens.ContainerColor.toColor(),
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = BottomAppBarTokens.ContainerElevation,
    contentPadding: PaddingValues = BottomAppBarDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        // TODO(b/209583788): Consider adding a shape parameter if updated design guidance allows
        shape = BottomAppBarTokens.ContainerShape.toShape(),
        modifier = modifier
    ) {
        Row(
            Modifier.fillMaxWidth()
                .height(BottomAppBarTokens.ContainerHeight)
                .padding(contentPadding),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * A TopAppBarScrollBehavior defines how an app bar should behave when the content under it is
 * scrolled.
 *
 * @see [TopAppBarDefaults.pinnedScrollBehavior]
 * @see [TopAppBarDefaults.enterAlwaysScrollBehavior]
 * @see [TopAppBarDefaults.exitUntilCollapsedScrollBehavior]
 */
@Stable
interface TopAppBarScrollBehavior {

    /**
     * A [NestedScrollConnection] that should be attached to a [Modifier.nestedScroll] in order to
     * keep track of the scroll events.
     */
    val nestedScrollConnection: NestedScrollConnection

    /**
     * Returns the top app bar's current scroll fraction.
     *
     * A scrollFraction is a value between `0.0` to `1.0` that provides a percentage of the app
     * bar scroll position when the content is scrolled. `0.0` represents an expanded app bar,
     * while `1.0` represents a collapsed one (e.g. the app bar is scrolled to its target offset).
     * Note that this value will be updated on scroll even if the [offset] is pinned to a specific
     * value (see [TopAppBarDefaults.pinnedScrollBehavior]). In this case a value of 1.0 represents
     * that the scroll value has exceeded the height of the pinned app bar, as if the app bar was
     * collapsing.
     */
    val scrollFraction: Float

    /**
     * The top app bar's offset limit in pixels, which represents the offset that a top app bar is
     * allowed to scroll when the scrollable content is scrolled.
     *
     * This limit is represented by a negative [Float], and used to coerce the [offset] value when
     * the content is scrolled.
     */
    var offsetLimit: Float

    /**
     * The top app bar's current offset in pixels.
     *
     * The offset is usually between zero and the [offsetLimit].
     */
    var offset: Float

    /**
     * The current content offset that is updated when the nested scroll connection consumes scroll
     * events.
     *
     * A common behavior implementation would update this value to be the sum of all
     * [NestedScrollConnection.onPostScroll] `consumed.y` values.
     */
    var contentOffset: Float
}

/**
 * Represents the colors used by a top app bar in different states.
 *
 * Each app bar has their own default implementation available in [TopAppBarDefaults], such as
 * [TopAppBarDefaults.smallTopAppBarColors] for [SmallTopAppBar].
 */
@Stable
interface TopAppBarColors {
    /**
     * Represents the container color used for the top app bar, depending on whether the app bar is
     * scrolled, and the percentage of its area that is scrolled.
     *
     * @param scrollFraction the scroll percentage of the top app bar (0.0 when the app bar is
     * considered expanded to 1.0 when the app bar is scrolled to its target offset)
     */
    @Composable
    fun containerColor(scrollFraction: Float): State<Color>

    /**
     * Represents the content color used for the top app bar's navigation icon depending on whether
     * the app bar is scrolled, and the percentage of its area that is scrolled.
     *
     * @param scrollFraction the scroll percentage of the top app bar (0.0 when the app bar is
     * considered expanded to 1.0 when the app bar is scrolled to its target offset)
     */
    @Composable
    fun navigationIconContentColor(scrollFraction: Float): State<Color>

    /**
     * Represents the content color used for the top app bar's title depending on whether the app
     * bar is scrolled, and the percentage of its area that is scrolled.
     *
     * @param scrollFraction the scroll percentage of the top app bar (0.0 when the app bar is
     * considered expanded to 1.0 when the app bar is scrolled to its target offset)
     */
    @Composable
    fun titleContentColor(scrollFraction: Float): State<Color>

    /**
     * Represents the content color used for the top app bar's action icons depending on whether the
     * app bar is scrolled, and the percentage of its area that is scrolled.
     *
     * @param scrollFraction the scroll percentage of the top app bar (0.0 when the app bar is
     * considered expanded to 1.0 when the app bar is scrolled to its target offset)
     */
    @Composable
    fun actionIconContentColor(scrollFraction: Float): State<Color>
}

/** Contains default values used for the top app bar implementations. */
object TopAppBarDefaults {

    /**
     * Creates a [TopAppBarColors] for small top app bars. The default implementation animates
     * between the provided colors according to the Material Design specification.
     *
     * @param containerColor the container color
     * @param scrolledContainerColor the container color when content is scrolled behind it
     * @param navigationIconContentColor the content color used for the navigation icon
     * @param titleContentColor the content color used for the title
     * @param actionIconContentColor the content color used for actions
     * @return the resulting [TopAppBarColors] used for the top app bar
     */
    @Composable
    fun smallTopAppBarColors(
        containerColor: Color = TopAppBarSmallTokens.ContainerColor.toColor(),
        scrolledContainerColor: Color = MaterialTheme.colorScheme.applyTonalElevation(
            backgroundColor = containerColor,
            elevation = TopAppBarSmallTokens.OnScrollContainerElevation
        ),
        navigationIconContentColor: Color = TopAppBarSmallTokens.LeadingIconColor.toColor(),
        titleContentColor: Color = TopAppBarSmallTokens.HeadlineColor.toColor(),
        actionIconContentColor: Color = TopAppBarSmallTokens.TrailingIconColor.toColor(),
    ): TopAppBarColors {
        return remember(
            containerColor,
            scrolledContainerColor,
            navigationIconContentColor,
            titleContentColor,
            actionIconContentColor
        ) {
            AnimatingTopAppBarColors(
                containerColor,
                scrolledContainerColor,
                navigationIconContentColor,
                titleContentColor,
                actionIconContentColor
            )
        }
    }

    /**
     * Creates a [TopAppBarColors] for center aligned top app bars. The default implementation
     * animates between the provided colors according to the Material Design specification.
     *
     * @param containerColor the container color
     * @param scrolledContainerColor the container color when content is scrolled behind it
     * @param navigationIconContentColor the content color used for the navigation icon
     * @param titleContentColor the content color used for the title
     * @param actionIconContentColor the content color used for actions
     * @return the resulting [TopAppBarColors] used for the top app bar
     */
    @Composable
    fun centerAlignedTopAppBarColors(
        containerColor: Color = TopAppBarSmallCenteredTokens.ContainerColor.toColor(),
        scrolledContainerColor: Color = MaterialTheme.colorScheme.applyTonalElevation(
            backgroundColor = containerColor,
            elevation = TopAppBarSmallTokens.OnScrollContainerElevation
        ),
        navigationIconContentColor: Color = TopAppBarSmallCenteredTokens.LeadingIconColor.toColor(),
        titleContentColor: Color = TopAppBarSmallCenteredTokens.HeadlineColor.toColor(),
        actionIconContentColor: Color = TopAppBarSmallCenteredTokens.TrailingIconColor.toColor(),
    ): TopAppBarColors {
        return remember(
            containerColor,
            scrolledContainerColor,
            navigationIconContentColor,
            titleContentColor,
            actionIconContentColor
        ) {
            AnimatingTopAppBarColors(
                containerColor,
                scrolledContainerColor,
                navigationIconContentColor,
                titleContentColor,
                actionIconContentColor
            )
        }
    }

    /**
     * Creates a [TopAppBarColors] for medium top app bars. The default implementation interpolates
     * between the provided colors as the top app bar scrolls according to the Material Design
     * specification.
     *
     * @param containerColor the container color
     * @param scrolledContainerColor the container color when content is scrolled behind it
     * @param navigationIconContentColor the content color used for the navigation icon
     * @param titleContentColor the content color used for the title
     * @param actionIconContentColor the content color used for actions
     * @return the resulting [TopAppBarColors] used for the top app bar
     */
    @Composable
    fun mediumTopAppBarColors(
        containerColor: Color = TopAppBarMediumTokens.ContainerColor.toColor(),
        scrolledContainerColor: Color = MaterialTheme.colorScheme.applyTonalElevation(
            backgroundColor = containerColor,
            elevation = TopAppBarSmallTokens.OnScrollContainerElevation
        ),
        navigationIconContentColor: Color = TopAppBarMediumTokens.LeadingIconColor.toColor(),
        titleContentColor: Color = TopAppBarMediumTokens.HeadlineColor.toColor(),
        actionIconContentColor: Color = TopAppBarMediumTokens.TrailingIconColor.toColor(),
    ): TopAppBarColors {
        return remember(
            containerColor,
            scrolledContainerColor,
            navigationIconContentColor,
            titleContentColor,
            actionIconContentColor
        ) {
            InterpolatingTopAppBarColors(
                containerColor,
                scrolledContainerColor,
                navigationIconContentColor,
                titleContentColor,
                actionIconContentColor
            )
        }
    }

    /**
     * Creates a [TopAppBarColors] for large top app bars. The default implementation interpolates
     * between the provided colors as the top app bar scrolls according to the Material Design
     * specification.
     *
     * @param containerColor the container color
     * @param scrolledContainerColor the container color when content is scrolled behind it
     * @param navigationIconContentColor the content color used for the navigation icon
     * @param titleContentColor the content color used for the title
     * @param actionIconContentColor the content color used for actions
     * @return the resulting [TopAppBarColors] used for the top app bar
     */
    @Composable
    fun largeTopAppBarColors(
        containerColor: Color = TopAppBarLargeTokens.ContainerColor.toColor(),
        scrolledContainerColor: Color = MaterialTheme.colorScheme.applyTonalElevation(
            backgroundColor = containerColor,
            elevation = TopAppBarSmallTokens.OnScrollContainerElevation
        ),
        navigationIconContentColor: Color = TopAppBarLargeTokens.LeadingIconColor.toColor(),
        titleContentColor: Color = TopAppBarLargeTokens.HeadlineColor.toColor(),
        actionIconContentColor: Color = TopAppBarLargeTokens.TrailingIconColor.toColor(),
    ): TopAppBarColors {
        return remember(
            containerColor,
            scrolledContainerColor,
            navigationIconContentColor,
            titleContentColor,
            actionIconContentColor
        ) {
            InterpolatingTopAppBarColors(
                containerColor,
                scrolledContainerColor,
                navigationIconContentColor,
                titleContentColor,
                actionIconContentColor
            )
        }
    }

    /**
     * Returns a pinned [TopAppBarScrollBehavior] that tracks nested-scroll callbacks and
     * updates its [TopAppBarScrollBehavior.contentOffset] accordingly.
     *
     * @param canScroll a callback used to determine whether scroll events are to be handled by this
     * pinned [TopAppBarScrollBehavior]
     */
    @ExperimentalMaterial3Api
    fun pinnedScrollBehavior(canScroll: () -> Boolean = { true }): TopAppBarScrollBehavior =
        PinnedScrollBehavior(canScroll)

    /**
     * Returns a [TopAppBarScrollBehavior]. A top app bar that is set up with this
     * [TopAppBarScrollBehavior] will immediately collapse when the content is pulled up, and will
     * immediately appear when the content is pulled down.
     *
     * @param canScroll a callback used to determine whether scroll events are to be
     * handled by this [EnterAlwaysScrollBehavior]
     */
    @ExperimentalMaterial3Api
    fun enterAlwaysScrollBehavior(canScroll: () -> Boolean = { true }): TopAppBarScrollBehavior =
        EnterAlwaysScrollBehavior(canScroll)

    /**
     * Returns a [TopAppBarScrollBehavior] that adjusts its properties to affect the colors and
     * height of the top app bar.
     *
     * A top app bar that is set up with this [TopAppBarScrollBehavior] will immediately collapse
     * when the nested content is pulled up, and will expand back the collapsed area when the
     * content is  pulled all the way down.
     *
     * @param decayAnimationSpec a [DecayAnimationSpec] that will be used by the top app bar motion
     * when the user flings the content. Preferably, this should match the animation spec used by
     * the scrollable content. See also [androidx.compose.animation.rememberSplineBasedDecay] for a
     * default [DecayAnimationSpec] that can be used with this behavior.
     * @param canScroll a callback used to determine whether scroll events are to be
     * handled by this [ExitUntilCollapsedScrollBehavior]
     */
    @ExperimentalMaterial3Api
    fun exitUntilCollapsedScrollBehavior(
        decayAnimationSpec: DecayAnimationSpec<Float>,
        canScroll: () -> Boolean = { true }
    ): TopAppBarScrollBehavior =
        ExitUntilCollapsedScrollBehavior(decayAnimationSpec, canScroll)
}

/** Contains default values used for the bottom app bar implementations. */
object BottomAppBarDefaults {
    /**
     * Default padding used for [BottomAppBar] when content are default size (24dp) icons in
     * [IconButton] that meet the minimum touch target (48.dp).
     */
    val ContentPadding = PaddingValues(
        start = BottomAppBarHorizontalPadding,
        top = BottomAppBarVerticalPadding,
        end = BottomAppBarHorizontalPadding
    )

    /**
     * Creates a [FloatingActionButtonElevation] that represents the default elevation of a
     * [FloatingActionButton] used for [BottomAppBar] in different states.
     *
     * @param defaultElevation the elevation used when the [FloatingActionButton] has no other
     * [Interaction]s.
     * @param pressedElevation the elevation used when the [FloatingActionButton] is pressed.
     * @param focusedElevation the elevation used when the [FloatingActionButton] is focused.
     * @param hoveredElevation the elevation used when the [FloatingActionButton] is hovered.
     */
    @Composable
    fun floatingActionButtonElevation(
        defaultElevation: Dp = 0.dp,
        pressedElevation: Dp = 0.dp,
        focusedElevation: Dp = 0.dp,
        hoveredElevation: Dp = 0.dp,
    ): FloatingActionButtonElevation {
        return FloatingActionButtonDefaults.elevation(
            defaultElevation = defaultElevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation
        )
    }
}

// Padding minus IconButton's min touch target expansion
private val BottomAppBarHorizontalPadding = 16.dp - 12.dp
// Padding minus IconButton's min touch target expansion
private val BottomAppBarVerticalPadding = 16.dp - 12.dp

// Padding minus content padding
private val FABHorizontalPadding = 16.dp - BottomAppBarHorizontalPadding
private val FABVerticalPadding = 12.dp - BottomAppBarVerticalPadding

/**
 * A single-row top app bar that is designed to be called by the small and center aligned top app
 * bar composables.
 *
 * This SingleRowTopAppBar has slots for a title, navigation icon, and actions. When the
 * [centeredTitle] flag is true, the title will be horizontally aligned to the center of the top app
 * bar width.
 */
@Composable
private fun SingleRowTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    titleTextStyle: TextStyle,
    centeredTitle: Boolean,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    colors: TopAppBarColors,
    scrollBehavior: TopAppBarScrollBehavior?
) {
    // TODO(b/182393826): Check if there is a better place to set the offsetLimit.
    // Set a scroll offset limit to hide the entire app bar area when scrolling.
    val offsetLimit = with(LocalDensity.current) { -TopAppBarSmallTokens.ContainerHeight.toPx() }
    SideEffect {
        if (scrollBehavior?.offsetLimit != offsetLimit) {
            scrollBehavior?.offsetLimit = offsetLimit
        }
    }

    // Obtain the container color from the TopAppBarColors.
    // This may potentially animate or interpolate a transition between the container-color and the
    // container's scrolled-color according to the app bar's scroll state.
    val scrollFraction = scrollBehavior?.scrollFraction ?: 0f
    val appBarContainerColor by colors.containerColor(scrollFraction)

    // Wrap the given actions in a Row.
    val actionsRow = @Composable {
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
    // Compose a Surface with a TopAppBarLayout content. The surface's background color will be
    // animated as specified above, and the height of the app bar will be determined by the current
    // scroll-state offset.
    Surface(modifier = modifier, color = appBarContainerColor) {
        val height = LocalDensity.current.run {
            TopAppBarSmallTokens.ContainerHeight.toPx() + (scrollBehavior?.offset ?: 0f)
        }
        TopAppBarLayout(
            modifier = Modifier,
            heightPx = height,
            navigationIconContentColor = colors.navigationIconContentColor(scrollFraction).value,
            titleContentColor = colors.titleContentColor(scrollFraction).value,
            actionIconContentColor = colors.actionIconContentColor(scrollFraction).value,
            title = title,
            titleTextStyle = titleTextStyle,
            titleAlpha = 1f,
            titleVerticalArrangement = Arrangement.Center,
            titleHorizontalArrangement =
                if (centeredTitle) Arrangement.Center else Arrangement.Start,
            titleBottomPadding = 0,
            hideTitleSemantics = false,
            navigationIcon = navigationIcon,
            actions = actionsRow,
        )
    }
}

/**
 * A two-rows top app bar that is designed to be called by the Large and Medium top app bar
 * composables.
 *
 * @throws [IllegalArgumentException] if the given [maxHeight] is equal or smaller than the
 * [pinnedHeight]
 */
@Composable
private fun TwoRowsTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    titleTextStyle: TextStyle,
    titleBottomPadding: Dp,
    smallTitle: @Composable () -> Unit,
    smallTitleTextStyle: TextStyle,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    colors: TopAppBarColors,
    maxHeight: Dp,
    pinnedHeight: Dp,
    scrollBehavior: TopAppBarScrollBehavior?
) {
    if (maxHeight <= pinnedHeight) {
        throw IllegalArgumentException(
            "A TwoRowsTopAppBar max height should be greater than its pinned height"
        )
    }
    val pinnedHeightPx: Float
    val maxHeightPx: Float
    val titleBottomPaddingPx: Int
    LocalDensity.current.run {
        pinnedHeightPx = pinnedHeight.toPx()
        maxHeightPx = maxHeight.toPx()
        titleBottomPaddingPx = titleBottomPadding.roundToPx()
    }

    // Set a scroll offset limit that will hide just the title area and will keep the small title
    // area visible.
    SideEffect {
        if (scrollBehavior?.offsetLimit != pinnedHeightPx - maxHeightPx) {
            scrollBehavior?.offsetLimit = pinnedHeightPx - maxHeightPx
        }
    }

    val scrollPercentage =
        if (scrollBehavior == null || scrollBehavior.offsetLimit == 0f) {
            0f
        } else {
            scrollBehavior.offset / scrollBehavior.offsetLimit
        }

    // Obtain the container Color from the TopAppBarColors.
    // This will potentially animate or interpolate a transition between the container color and the
    // container's scrolled color according to the app bar's scroll state.
    val scrollFraction = scrollBehavior?.scrollFraction ?: 0f
    val appBarContainerColor by colors.containerColor(scrollFraction)

    // Wrap the given actions in a Row.
    val actionsRow = @Composable {
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
    val titleAlpha = 1f - scrollPercentage
    // Hide the top row title semantics when its alpha value goes below 0.5 threshold.
    // Hide the bottom row title semantics when the top title semantics are active.
    val hideTopRowSemantics = scrollPercentage < 0.5f
    val hideBottomRowSemantics = !hideTopRowSemantics
    Surface(modifier = modifier, color = appBarContainerColor) {
        Column {
            TopAppBarLayout(
                modifier = Modifier,
                heightPx = pinnedHeightPx,
                navigationIconContentColor =
                    colors.navigationIconContentColor(scrollFraction).value,
                titleContentColor = colors.titleContentColor(scrollFraction).value,
                actionIconContentColor = colors.actionIconContentColor(scrollFraction).value,
                title = smallTitle,
                titleTextStyle = smallTitleTextStyle,
                titleAlpha = 1f - titleAlpha,
                titleVerticalArrangement = Arrangement.Center,
                titleHorizontalArrangement = Arrangement.Start,
                titleBottomPadding = 0,
                hideTitleSemantics = hideTopRowSemantics,
                navigationIcon = navigationIcon,
                actions = actionsRow,
            )
            TopAppBarLayout(
                modifier = Modifier.clipToBounds(),
                heightPx = maxHeightPx - pinnedHeightPx + (scrollBehavior?.offset ?: 0f),
                navigationIconContentColor =
                    colors.navigationIconContentColor(scrollFraction).value,
                titleContentColor = colors.titleContentColor(scrollFraction).value,
                actionIconContentColor = colors.actionIconContentColor(scrollFraction).value,
                title = title,
                titleTextStyle = titleTextStyle,
                titleAlpha = titleAlpha,
                titleVerticalArrangement = Arrangement.Bottom,
                titleHorizontalArrangement = Arrangement.Start,
                titleBottomPadding = titleBottomPaddingPx,
                hideTitleSemantics = hideBottomRowSemantics,
                navigationIcon = {},
                actions = {}
            )
        }
    }
}

/**
 * The base [Layout] for all top app bars. This function lays out a top app bar navigation icon
 * (leading icon), a title (header), and action icons (trailing icons). Note that the navigation and
 * the actions are optional.
 *
 * @param heightPx the total height this layout is capped to
 * @param navigationIconContentColor the content color that will be applied via a
 * [LocalContentColor] when composing the navigation icon
 * @param titleContentColor the color that will be applied via a [LocalContentColor] when composing
 * the title
 * @param actionIconContentColor the content color that will be applied via a [LocalContentColor]
 * when composing the action icons
 * @param title the top app bar title (header)
 * @param titleTextStyle the title's text style
 * @param modifier a [Modifier]
 * @param titleAlpha the title's alpha
 * @param titleVerticalArrangement the title's vertical arrangement
 * @param titleHorizontalArrangement the title's horizontal arrangement
 * @param titleBottomPadding the title's bottom padding
 * @param hideTitleSemantics hides the title node from the semantic tree. Apply this
 * boolean when this layout is part of a [TwoRowsTopAppBar] to hide the title's semantics
 * from accessibility services. This is needed to avoid having multiple titles visible to
 * accessibility services at the same time, when animating between collapsed / expanded states.
 * @param navigationIcon a navigation icon [Composable]
 * @param actions actions [Composable]
 */
@Composable
private fun TopAppBarLayout(
    modifier: Modifier,
    heightPx: Float,
    navigationIconContentColor: Color,
    titleContentColor: Color,
    actionIconContentColor: Color,
    title: @Composable () -> Unit,
    titleTextStyle: TextStyle,
    titleAlpha: Float,
    titleVerticalArrangement: Arrangement.Vertical,
    titleHorizontalArrangement: Arrangement.Horizontal,
    titleBottomPadding: Int,
    hideTitleSemantics: Boolean,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable () -> Unit,
) {
    Layout(
        {
            Box(Modifier.layoutId("navigationIcon").padding(start = TopAppBarHorizontalPadding)) {
                CompositionLocalProvider(
                    LocalContentColor provides navigationIconContentColor,
                    content = navigationIcon
                )
            }
            Box(
                Modifier.layoutId("title").padding(horizontal = TopAppBarHorizontalPadding)
                    .then(if (hideTitleSemantics) Modifier.clearAndSetSemantics { } else Modifier)
            ) {
                ProvideTextStyle(value = titleTextStyle) {
                    CompositionLocalProvider(
                        LocalContentColor provides titleContentColor.copy(alpha = titleAlpha),
                        content = title
                    )
                }
            }
            Box(Modifier.layoutId("actionIcons").padding(end = TopAppBarHorizontalPadding)) {
                CompositionLocalProvider(
                    LocalContentColor provides actionIconContentColor,
                    content = actions
                )
            }
        },
        modifier = modifier
    ) { measurables, constraints ->
        val navigationIconPlaceable =
            measurables.first { it.layoutId == "navigationIcon" }
                .measure(constraints.copy(minWidth = 0))
        val actionIconsPlaceable =
            measurables.first { it.layoutId == "actionIcons" }
                .measure(constraints.copy(minWidth = 0))

        val maxTitleWidth = if (constraints.maxWidth == Constraints.Infinity) {
            constraints.maxWidth
        } else {
            (constraints.maxWidth - navigationIconPlaceable.width - actionIconsPlaceable.width)
                .coerceAtLeast(0)
        }
        val titlePlaceable =
            measurables.first { it.layoutId == "title" }
                .measure(constraints.copy(minWidth = 0, maxWidth = maxTitleWidth))

        // Locate the title's baseline.
        val titleBaseline =
            if (titlePlaceable[LastBaseline] != AlignmentLine.Unspecified) {
                titlePlaceable[LastBaseline]
            } else {
                0
            }

        val layoutHeight = heightPx.roundToInt()

        layout(constraints.maxWidth, layoutHeight) {
            // Navigation icon
            navigationIconPlaceable.placeRelative(
                x = 0,
                y = (layoutHeight - navigationIconPlaceable.height) / 2
            )

            // Title
            titlePlaceable.placeRelative(
                x = when (titleHorizontalArrangement) {
                    Arrangement.Center -> (constraints.maxWidth - titlePlaceable.width) / 2
                    Arrangement.End ->
                        constraints.maxWidth - titlePlaceable.width - actionIconsPlaceable.width
                    // Arrangement.Start.
                    // An TopAppBarTitleInset will make sure the title is offset in case the
                    // navigation icon is missing.
                    else -> max(TopAppBarTitleInset.roundToPx(), navigationIconPlaceable.width)
                },
                y = when (titleVerticalArrangement) {
                    Arrangement.Center -> (layoutHeight - titlePlaceable.height) / 2
                    // Apply bottom padding from the title's baseline only when the Arrangement is
                    // "Bottom".
                    Arrangement.Bottom ->
                        if (titleBottomPadding == 0) layoutHeight - titlePlaceable.height
                        else layoutHeight - titlePlaceable.height - max(
                            0,
                            titleBottomPadding - titlePlaceable.height + titleBaseline
                        )
                    // Arrangement.Top
                    else -> 0
                }
            )

            // Action icons
            actionIconsPlaceable.placeRelative(
                x = constraints.maxWidth - actionIconsPlaceable.width,
                y = (layoutHeight - actionIconsPlaceable.height) / 2
            )
        }
    }
}

/**
 * A [TopAppBarColors] implementation that animates the container color according to the top app
 * bar scroll state.
 *
 * This default implementation does not animate the leading, headline, or trailing colors.
 */
@Stable
private class AnimatingTopAppBarColors(
    private val containerColor: Color,
    private val scrolledContainerColor: Color,
    navigationIconContentColor: Color,
    titleContentColor: Color,
    actionIconContentColor: Color
) : TopAppBarColors {

    // In this TopAppBarColors implementation, the following colors never change their value as the
    // app bar scrolls.
    private val navigationIconColorState: State<Color> = mutableStateOf(navigationIconContentColor)
    private val titleColorState: State<Color> = mutableStateOf(titleContentColor)
    private val actionIconColorState: State<Color> = mutableStateOf(actionIconContentColor)

    @Composable
    override fun containerColor(scrollFraction: Float): State<Color> {
        return animateColorAsState(
            // Check if scrollFraction is slightly over zero to overcome float precision issues.
            targetValue = if (scrollFraction > 0.01f) {
                scrolledContainerColor
            } else {
                containerColor
            },
            animationSpec = tween(
                durationMillis = TopAppBarAnimationDurationMillis,
                easing = LinearOutSlowInEasing
            )
        )
    }

    @Composable
    override fun navigationIconContentColor(scrollFraction: Float): State<Color> =
        navigationIconColorState

    @Composable
    override fun titleContentColor(scrollFraction: Float): State<Color> = titleColorState

    @Composable
    override fun actionIconContentColor(scrollFraction: Float): State<Color> = actionIconColorState
}

/**
 * A [TopAppBarColors] implementation that interpolates the container color according to the top
 * app bar scroll state percentage.
 *
 * This default implementation does not interpolate the leading, headline, or trailing colors.
 */
@Stable
private class InterpolatingTopAppBarColors(
    private val containerColor: Color,
    private val scrolledContainerColor: Color,
    navigationIconContentColor: Color,
    titleContentColor: Color,
    actionIconContentColor: Color
) : TopAppBarColors {

    // In this TopAppBarColors implementation, the following colors never change their value as the
    // app bar scrolls.
    private val navigationIconColorState: State<Color> = mutableStateOf(navigationIconContentColor)
    private val titleColorState: State<Color> = mutableStateOf(titleContentColor)
    private val actionIconColorState: State<Color> = mutableStateOf(actionIconContentColor)

    @Composable
    override fun containerColor(scrollFraction: Float): State<Color> {
        return rememberUpdatedState(
            lerp(
                containerColor,
                scrolledContainerColor,
                FastOutLinearInEasing.transform(scrollFraction)
            )
        )
    }

    @Composable
    override fun navigationIconContentColor(scrollFraction: Float): State<Color> =
        navigationIconColorState

    @Composable
    override fun titleContentColor(scrollFraction: Float): State<Color> = titleColorState

    @Composable
    override fun actionIconContentColor(scrollFraction: Float): State<Color> = actionIconColorState
}

/**
 * Returns a [TopAppBarScrollBehavior] that only adjusts its content offset, without adjusting any
 * properties that affect the height of a top app bar.
 *
 * @param canScroll a callback used to determine whether scroll events are to be
 * handled by this [PinnedScrollBehavior]
 */
private class PinnedScrollBehavior(val canScroll: () -> Boolean = { true }) :
    TopAppBarScrollBehavior {
    override var offsetLimit = -Float.MAX_VALUE
    override val scrollFraction: Float
        get() = if (offsetLimit != 0f) {
            1 - ((offsetLimit - contentOffset).coerceIn(
                minimumValue = offsetLimit,
                maximumValue = 0f
            ) / offsetLimit)
        } else {
            0f
        }
    override var offset = 0f
    override var contentOffset by mutableStateOf(0f)
    override var nestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!canScroll()) return Offset.Zero
                if (consumed.y == 0f && available.y > 0f) {
                    // Reset the total offset to zero when scrolling all the way down. This will
                    // eliminate some float precision inaccuracies.
                    contentOffset = 0f
                } else {
                    contentOffset += consumed.y
                }
                return Offset.Zero
            }
        }
}

/**
 * A [TopAppBarScrollBehavior] that adjusts its properties to affect the colors and height of a top
 * app bar.
 *
 * A top app bar that is set up with this [TopAppBarScrollBehavior] will immediately collapse when
 * the nested content is pulled up, and will immediately appear when the content is pulled down.
 *
 * @param canScroll a callback used to determine whether scroll events are to be
 * handled by this [EnterAlwaysScrollBehavior]
 */
private class EnterAlwaysScrollBehavior(val canScroll: () -> Boolean = { true }) :
    TopAppBarScrollBehavior {
    override val scrollFraction: Float
        get() = if (offsetLimit != 0f) {
            1 - ((offsetLimit - contentOffset).coerceIn(
                minimumValue = offsetLimit,
                maximumValue = 0f
            ) / offsetLimit)
        } else {
            0f
        }
    override var offsetLimit by mutableStateOf(-Float.MAX_VALUE)
    override var offset by mutableStateOf(0f)
    override var contentOffset by mutableStateOf(0f)
    override var nestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!canScroll()) return Offset.Zero
                val newOffset = (offset + available.y)
                val coerced = newOffset.coerceIn(minimumValue = offsetLimit, maximumValue = 0f)
                return if (newOffset == coerced) {
                    // Nothing coerced, meaning we're in the middle of top app bar collapse or
                    // expand.
                    offset = coerced
                    // Consume only the scroll on the Y axis.
                    available.copy(x = 0f)
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!canScroll()) return Offset.Zero
                contentOffset += consumed.y
                if (offset == 0f || offset == offsetLimit) {
                    if (consumed.y == 0f && available.y > 0f) {
                        // Reset the total offset to zero when scrolling all the way down.
                        // This will eliminate some float precision inaccuracies.
                        contentOffset = 0f
                    }
                }
                offset = (offset + consumed.y).coerceIn(
                    minimumValue = offsetLimit,
                    maximumValue = 0f
                )
                return Offset.Zero
            }
        }
}

/**
 * A [TopAppBarScrollBehavior] that adjusts its properties to affect the colors and height of a top
 * app bar.
 *
 * A top app bar that is set up with this [TopAppBarScrollBehavior] will immediately collapse when
 * the nested content is pulled up, and will expand back the collapsed area when the content is
 * pulled all the way down.
 *
 * @param decayAnimationSpec a [DecayAnimationSpec] that will be used by the top app bar motion
 * when the user flings the content. Preferably, this should match the animation spec used by the
 * scrollable content. See also [androidx.compose.animation.rememberSplineBasedDecay] for a
 * default [DecayAnimationSpec] that can be used with this behavior.
 * @param canScroll a callback used to determine whether scroll events are to be
 * handled by this [ExitUntilCollapsedScrollBehavior]
 */
private class ExitUntilCollapsedScrollBehavior(
    val decayAnimationSpec: DecayAnimationSpec<Float>,
    val canScroll: () -> Boolean = { true }
) : TopAppBarScrollBehavior {
    override val scrollFraction: Float
        get() = if (offsetLimit != 0f) offset / offsetLimit else 0f
    override var offsetLimit by mutableStateOf(-Float.MAX_VALUE)
    override var offset by mutableStateOf(0f)
    override var contentOffset by mutableStateOf(0f)
    override var nestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Don't intercept if scrolling down.
                if (!canScroll() || available.y > 0f) return Offset.Zero

                val newOffset = (offset + available.y)
                val coerced = newOffset.coerceIn(minimumValue = offsetLimit, maximumValue = 0f)
                return if (newOffset == coerced) {
                    // Nothing coerced, meaning we're in the middle of top app bar collapse or
                    // expand.
                    offset = coerced
                    // Consume only the scroll on the Y axis.
                    available.copy(x = 0f)
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!canScroll()) return Offset.Zero
                contentOffset += consumed.y

                if (available.y < 0f || consumed.y < 0f) {
                    // When scrolling up, just update the state's offset.
                    val oldOffset = offset
                    offset = (offset + consumed.y).coerceIn(
                        minimumValue = offsetLimit,
                        maximumValue = 0f
                    )
                    return Offset(0f, offset - oldOffset)
                }

                if (consumed.y == 0f && available.y > 0) {
                    // Reset the total offset to zero when scrolling all the way down. This will
                    // eliminate some float precision inaccuracies.
                    contentOffset = 0f
                }

                if (available.y > 0f) {
                    // Adjust the offset in case the consumed delta Y is less than what was recorded
                    // as available delta Y in the pre-scroll.
                    val oldOffset = offset
                    offset = (offset + available.y).coerceIn(
                        minimumValue = offsetLimit,
                        maximumValue = 0f
                    )
                    return Offset(0f, offset - oldOffset)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val result = super.onPostFling(consumed, available)
                // TODO(b/179417109): We get positive Velocity when flinging up while the top app
                //  bar is changing its height. Track b/179417109 for a fix.
                if ((available.y < 0f && contentOffset == 0f) ||
                    (available.y > 0f && offset < 0f)
                ) {
                    return result +
                        onTopBarFling(
                            scrollBehavior = this@ExitUntilCollapsedScrollBehavior,
                            initialVelocity = available.y,
                            decayAnimationSpec = decayAnimationSpec,
                            snap = true
                        )
                }
                return result
            }
        }
}

private suspend fun onTopBarFling(
    scrollBehavior: TopAppBarScrollBehavior,
    initialVelocity: Float,
    decayAnimationSpec: DecayAnimationSpec<Float>,
    snap: Boolean
): Velocity {
    if (abs(initialVelocity) > 1f) {
        var remainingVelocity = initialVelocity
        var lastValue = 0f
        AnimationState(
            initialValue = 0f,
            initialVelocity = initialVelocity,
        )
            .animateDecay(decayAnimationSpec) {
                val delta = value - lastValue
                val initialOffset = scrollBehavior.offset
                scrollBehavior.offset =
                    (initialOffset + delta).coerceIn(
                        minimumValue = scrollBehavior.offsetLimit,
                        maximumValue = 0f
                    )
                val consumed = abs(initialOffset - scrollBehavior.offset)
                lastValue = value
                remainingVelocity = this.velocity
                // avoid rounding errors and stop if anything is unconsumed
                if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
            }

        if (snap &&
            scrollBehavior.offset < 0 &&
            scrollBehavior.offset > scrollBehavior.offsetLimit
        ) {
            AnimationState(initialValue = scrollBehavior.offset).animateTo(
                // Snap the top app bar offset to completely collapse or completely expand according
                // to the initial velocity direction.
                if (initialVelocity > 0) 0f else scrollBehavior.offsetLimit,
                animationSpec = tween(
                    durationMillis = TopAppBarAnimationDurationMillis,
                    easing = LinearOutSlowInEasing
                )
            ) { scrollBehavior.offset = value }
        }
        return Velocity(0f, remainingVelocity)
    }
    return Velocity.Zero
}

private val MediumTitleBottomPadding = 24.dp
private val LargeTitleBottomPadding = 28.dp
private val TopAppBarHorizontalPadding = 4.dp

// TODO: this should probably be part of the touch target of the start and end icons, clarify this
private val AppBarHorizontalPadding = 4.dp

// A title inset when the App-Bar is a Medium or Large one. Also used to size a spacer when the
// navigation icon is missing.
private val TopAppBarTitleInset = 16.dp - TopAppBarHorizontalPadding

private const val TopAppBarAnimationDurationMillis = 500
