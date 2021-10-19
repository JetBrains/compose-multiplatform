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

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.tokens.NavigationDrawer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * <a href="https://material.io/design/layout/understanding-layout.html" class="external" target="_blank">Material Design layout</a>.
 *
 * Scaffold implements the basic material design visual layout structure.
 *
 * This component provides API to put together several material components to construct your
 * screen, by ensuring proper layout strategy for them and collecting necessary data so these
 * components will work together correctly.
 *
 * @param modifier optional Modifier for the root of the [Scaffold]
 * @param scaffoldState state of this scaffold widget. It contains the state of the screen, e.g.
 * variables to provide manual control over the drawer behavior, sizes of components, etc
 * @param topBar top app bar of the screen. Consider using [SmallTopAppBar].
 * @param bottomBar bottom bar of the screen. Consider using [NavigationBar].
 * @param floatingActionButton Main action button of your screen. Consider using
 * [FloatingActionButton] for this slot.
 * @param floatingActionButtonPosition position of the FAB on the screen. See [FabPosition] for
 * possible options available.
 * @param drawerContent content of the Drawer sheet that can be pulled from the left side (right
 * for RTL).
 * @param drawerGesturesEnabled whether or not drawer (if set) can be interacted with via gestures
 * @param drawerShape shape of the drawer sheet (if set)
 * @param drawerTonalElevation Affects the alpha of the color overlay applied on the container color
 * of the drawer sheet.
 * @param drawerContainerColor container color to be used for the drawer sheet
 * @param drawerContentColor color of the content to use inside the drawer sheet. Defaults to
 * either the matching content color for [drawerContainerColor], or, if it is not a color from
 * the theme, this will keep the same value set above this Surface.
 * @param drawerScrimColor color of the scrim that obscures content when the drawer is open
 * @param containerColor background of the scaffold body
 * @param contentColor color of the content in scaffold body. Defaults to either the matching
 * content color for [containerColor], or, if it is not a color from the theme, this will keep
 * the same value set above this Surface.
 * @param content content of your screen. The lambda receives an [PaddingValues] that should be
 * applied to the content root via Modifier.padding to properly offset top and bottom bars. If
 * you're using Modifier.VerticalScroll, apply this modifier to the child of the scroll, and not on
 * the scroll itself.
 */
// TODO(b/198144133): Add Simple example of a Scaffold with [SmallTopAppBar], [FloatingActionButton]
//  and [Navigation drawer].
@ExperimentalMaterial3Api
@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    drawerContent: @Composable (ColumnScope.() -> Unit)? = null,
    drawerGesturesEnabled: Boolean = true,
    drawerShape: Shape = RoundedCornerShape(16.dp),
    drawerTonalElevation: Dp = DrawerDefaults.Elevation,
    drawerContainerColor: Color =
        MaterialTheme.colorScheme.fromToken(NavigationDrawer.ContainerColor),
    drawerContentColor: Color = contentColorFor(drawerContainerColor),
    drawerScrimColor: Color = DrawerDefaults.scrimColor,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable (PaddingValues) -> Unit
) {
    val child = @Composable { childModifier: Modifier ->
        Surface(modifier = childModifier, color = containerColor, contentColor = contentColor) {
            ScaffoldLayout(
                fabPosition = floatingActionButtonPosition,
                topBar = topBar,
                bottomBar = bottomBar,
                content = content,
                fab = floatingActionButton
            )
        }
    }

    if (drawerContent != null) {
        NavigationDrawer(
            modifier = modifier,
            drawerState = scaffoldState.drawerState,
            gesturesEnabled = drawerGesturesEnabled,
            drawerContent = drawerContent,
            drawerShape = drawerShape,
            drawerTonalElevation = drawerTonalElevation,
            drawerContainerColor = drawerContainerColor,
            drawerContentColor = drawerContentColor,
            scrimColor = drawerScrimColor,
            content = { child(Modifier) }
        )
    } else {
        child(modifier)
    }
}

/**
 * Layout for a [Scaffold]'s content.
 *
 * @param fabPosition [FabPosition] for the FAB (if present)
 * @param topBar the content to place at the top of the [Scaffold], typically a [SmallTopAppBar]
 * @param content the main 'body' of the [Scaffold]
 * @param fab the [FloatingActionButton] displayed on top of the [content]
 * @param bottomBar the content to place at the bottom of the [Scaffold], on top of the
 * [content], typically a [NavigationBar].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScaffoldLayout(
    fabPosition: FabPosition,
    topBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
    fab: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit

) {
    SubcomposeLayout { constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        layout(layoutWidth, layoutHeight) {
            val topBarPlaceables = subcompose(ScaffoldLayoutContent.TopBar, topBar).map {
                it.measure(looseConstraints)
            }

            val topBarHeight = topBarPlaceables.maxByOrNull { it.height }?.height ?: 0

            val fabPlaceables =
                subcompose(ScaffoldLayoutContent.Fab, fab).mapNotNull { measurable ->
                    measurable.measure(looseConstraints).takeIf { it.height != 0 && it.width != 0 }
                }

            val fabPlacement = if (fabPlaceables.isNotEmpty()) {
                val fabWidth = fabPlaceables.maxByOrNull { it.width }!!.width
                val fabHeight = fabPlaceables.maxByOrNull { it.height }!!.height
                // FAB distance from the left of the layout, taking into account LTR / RTL
                val fabLeftOffset = if (fabPosition == FabPosition.End) {
                    if (layoutDirection == LayoutDirection.Ltr) {
                        layoutWidth - FabSpacing.roundToPx() - fabWidth
                    } else {
                        FabSpacing.roundToPx()
                    }
                } else {
                    (layoutWidth - fabWidth) / 2
                }

                FabPlacement(
                    left = fabLeftOffset,
                    width = fabWidth,
                    height = fabHeight
                )
            } else {
                null
            }

            val bottomBarPlaceables = subcompose(ScaffoldLayoutContent.BottomBar) {
                CompositionLocalProvider(
                    LocalFabPlacement provides fabPlacement,
                    content = bottomBar
                )
            }.map { it.measure(looseConstraints) }

            val bottomBarHeight = bottomBarPlaceables.maxByOrNull { it.height }?.height ?: 0
            val fabOffsetFromBottom = fabPlacement?.let {
                if (bottomBarHeight == 0) {
                    it.height + FabSpacing.roundToPx()
                } else {
                    // Total height is the bottom bar height + the FAB height + the padding
                    // between the FAB and bottom bar
                    bottomBarHeight + it.height + FabSpacing.roundToPx()
                }
            }

            val bodyContentHeight = layoutHeight - topBarHeight

            val bodyContentPlaceables = subcompose(ScaffoldLayoutContent.MainContent) {
                val innerPadding = PaddingValues(bottom = bottomBarHeight.toDp())
                content(innerPadding)
            }.map { it.measure(looseConstraints.copy(maxHeight = bodyContentHeight)) }

            // Placing to control drawing order to match default elevation of each placeable

            bodyContentPlaceables.forEach {
                it.place(0, topBarHeight)
            }
            topBarPlaceables.forEach {
                it.place(0, 0)
            }
            // The bottom bar is always at the bottom of the layout
            bottomBarPlaceables.forEach {
                it.place(0, layoutHeight - bottomBarHeight)
            }
            // Explicitly not using placeRelative here as `leftOffset` already accounts for RTL
            fabPlacement?.let { placement ->
                fabPlaceables.forEach {
                    it.place(placement.left, layoutHeight - fabOffsetFromBottom!!)
                }
            }
        }
    }
}

/**
 * State for [Scaffold] composable component.
 *
 * Contains basic screen state, e.g. Drawer configuration, as well as sizes of components after
 * layout has happened
 *
 * @param drawerState the drawer state
 */
@Stable
@ExperimentalMaterial3Api
class ScaffoldState(
    val drawerState: DrawerState,
)

/**
 * Creates a [ScaffoldState] with the default animation clock and memoizes it.
 *
 * @param drawerState the drawer state
 */
@ExperimentalMaterial3Api
@Composable
fun rememberScaffoldState(
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
): ScaffoldState = remember {
    ScaffoldState(drawerState)
}

/**
 * The possible positions for a [FloatingActionButton] attached to a [Scaffold].
 */
// TODO(b/200553810): Mark as experimental
@ExperimentalMaterial3Api
@Suppress("INLINE_CLASS_DEPRECATED", "EXPERIMENTAL_FEATURE_WARNING")
inline class FabPosition internal constructor(@Suppress("unused") private val value: Int) {
    companion object {
        /**
         * Position FAB at the bottom of the screen in the center, above the [NavigationBar] (if it
         * exists)
         */
        val Center = FabPosition(0)

        /**
         * Position FAB at the bottom of the screen at the end, above the [NavigationBar] (if it
         * exists)
         */
        val End = FabPosition(1)
    }

    override fun toString(): String {
        return when (this) {
            Center -> "FabPosition.Center"
            else -> "FabPosition.End"
        }
    }
}

/**
 * Placement information for a [FloatingActionButton] inside a [Scaffold].
 *
 * @property left the FAB's offset from the left edge of the bottom bar, already adjusted for RTL
 * support
 * @property width the width of the FAB
 * @property height the height of the FAB
 */
@Immutable
internal class FabPlacement(
    val left: Int,
    val width: Int,
    val height: Int
)

/**
 * CompositionLocal containing a [FabPlacement] that is used to calculate the FAB bottom offset.
 */
internal val LocalFabPlacement = staticCompositionLocalOf<FabPlacement?> { null }

// FAB spacing above the bottom bar / bottom of the Scaffold
private val FabSpacing = 16.dp

private enum class ScaffoldLayoutContent { TopBar, MainContent, Fab, BottomBar }
