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

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy

/**
 * State for [Scaffold] composable component.
 *
 * Contains basic screen state, e.g. Drawer configuration, as well as sizes of components after
 * layout has happened
 *
 * @param drawerState the drawer state
 * @param snackbarHostState instance of [SnackbarHostState] to be used to show [Snackbar]s
 * inside of the [Scaffold]
 */
@Stable
class ScaffoldState(
    val drawerState: DrawerState,
    val snackbarHostState: SnackbarHostState
)

/**
 * Creates a [ScaffoldState] with the default animation clock and memoizes it.
 *
 * @param drawerState the drawer state
 * @param snackbarHostState instance of [SnackbarHostState] to be used to show [Snackbar]s
 * inside of the [Scaffold]
 */
@Composable
fun rememberScaffoldState(
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
): ScaffoldState = remember {
    ScaffoldState(drawerState, snackbarHostState)
}

/**
 * The possible positions for a [FloatingActionButton] attached to a [Scaffold].
 */
@kotlin.jvm.JvmInline
value class FabPosition internal constructor(@Suppress("unused") private val value: Int) {
    companion object {
        /**
         * Position FAB at the bottom of the screen in the center, above the [BottomAppBar] (if it
         * exists)
         */
        val Center = FabPosition(0)

        /**
         * Position FAB at the bottom of the screen at the end, above the [BottomAppBar] (if it
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
 * <a href="https://material.io/design/layout/understanding-layout.html" class="external" target="_blank">Material Design layout</a>.
 *
 * Scaffold implements the basic material design visual layout structure.
 *
 * This component provides API to put together several material components to construct your
 * screen, by ensuring proper layout strategy for them and collecting necessary data so these
 * components will work together correctly.
 *
 * For similar components that implement different layout structures, see [BackdropScaffold],
 * which uses a backdrop as the centerpiece of the screen, and [BottomSheetScaffold], which uses
 * a persistent bottom sheet as the centerpiece of the screen.
 *
 * Simple example of a Scaffold with [TopAppBar], [FloatingActionButton] and drawer:
 *
 * @sample androidx.compose.material.samples.SimpleScaffoldWithTopBar
 *
 * More fancy usage with [BottomAppBar] with cutout and docked [FloatingActionButton], which
 * animates it's shape when clicked:
 *
 * @sample androidx.compose.material.samples.ScaffoldWithBottomBarAndCutout
 *
 * To show a [Snackbar], use [SnackbarHostState.showSnackbar]. Scaffold state already
 * have [ScaffoldState.snackbarHostState] when created
 *
 * @sample androidx.compose.material.samples.ScaffoldWithSimpleSnackbar
 *
 * @param modifier optional Modifier for the root of the [Scaffold]
 * @param scaffoldState state of this scaffold widget. It contains the state of the screen, e.g.
 * variables to provide manual control over the drawer behavior, sizes of components, etc
 * @param topBar top app bar of the screen. Consider using [TopAppBar].
 * @param bottomBar bottom bar of the screen. Consider using [BottomAppBar].
 * @param snackbarHost component to host [Snackbar]s that are pushed to be shown via
 * [SnackbarHostState.showSnackbar]. Usually it's a [SnackbarHost]
 * @param floatingActionButton Main action button of your screen. Consider using
 * [FloatingActionButton] for this slot.
 * @param floatingActionButtonPosition position of the FAB on the screen. See [FabPosition] for
 * possible options available.
 * @param isFloatingActionButtonDocked whether [floatingActionButton] should overlap with
 * [bottomBar] for half a height, if [bottomBar] exists. Ignored if there's no [bottomBar] or no
 * [floatingActionButton].
 * @param drawerContent content of the Drawer sheet that can be pulled from the left side (right
 * for RTL).
 * @param drawerGesturesEnabled whether or not drawer (if set) can be interacted with via gestures
 * @param drawerShape shape of the drawer sheet (if set)
 * @param drawerElevation drawer sheet elevation. This controls the size of the shadow
 * below the drawer sheet (if set)
 * @param drawerBackgroundColor background color to be used for the drawer sheet
 * @param drawerContentColor color of the content to use inside the drawer sheet. Defaults to
 * either the matching content color for [drawerBackgroundColor], or, if it is not a color from
 * the theme, this will keep the same value set above this Surface.
 * @param drawerScrimColor color of the scrim that obscures content when the drawer is open
 * @param backgroundColor background of the scaffold body
 * @param contentColor color of the content in scaffold body. Defaults to either the matching
 * content color for [backgroundColor], or, if it is not a color from the theme, this will keep
 * the same value set above this Surface.
 * @param content content of your screen. The lambda receives an [PaddingValues] that should be
 * applied to the content root via Modifier.padding to properly offset top and bottom bars. If
 * you're using VerticalScroller, apply this modifier to the child of the scroller, and not on
 * the scroller itself.
 */
@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SnackbarHost(it) },
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    isFloatingActionButtonDocked: Boolean = false,
    drawerContent: @Composable (ColumnScope.() -> Unit)? = null,
    drawerGesturesEnabled: Boolean = true,
    drawerShape: Shape = MaterialTheme.shapes.large,
    drawerElevation: Dp = DrawerDefaults.Elevation,
    drawerBackgroundColor: Color = MaterialTheme.colors.surface,
    drawerContentColor: Color = contentColorFor(drawerBackgroundColor),
    drawerScrimColor: Color = DrawerDefaults.scrimColor,
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    content: @Composable (PaddingValues) -> Unit
) {
    val child = @Composable { childModifier: Modifier ->
        Surface(modifier = childModifier, color = backgroundColor, contentColor = contentColor) {
            ScaffoldLayout(
                isFabDocked = isFloatingActionButtonDocked,
                fabPosition = floatingActionButtonPosition,
                topBar = topBar,
                content = content,
                snackbar = {
                    snackbarHost(scaffoldState.snackbarHostState)
                },
                fab = floatingActionButton,
                bottomBar = bottomBar
            )
        }
    }

    if (drawerContent != null) {
        ModalDrawer(
            modifier = modifier,
            drawerState = scaffoldState.drawerState,
            gesturesEnabled = drawerGesturesEnabled,
            drawerContent = drawerContent,
            drawerShape = drawerShape,
            drawerElevation = drawerElevation,
            drawerBackgroundColor = drawerBackgroundColor,
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
 * @param isFabDocked whether the FAB (if present) is docked to the bottom bar or not
 * @param fabPosition [FabPosition] for the FAB (if present)
 * @param topBar the content to place at the top of the [Scaffold], typically a [TopAppBar]
 * @param content the main 'body' of the [Scaffold]
 * @param snackbar the [Snackbar] displayed on top of the [content]
 * @param fab the [FloatingActionButton] displayed on top of the [content], below the [snackbar]
 * and above the [bottomBar]
 * @param bottomBar the content to place at the bottom of the [Scaffold], on top of the
 * [content], typically a [BottomAppBar].
 */
@Composable
@UiComposable
private fun ScaffoldLayout(
    isFabDocked: Boolean,
    fabPosition: FabPosition,
    topBar: @Composable @UiComposable () -> Unit,
    content: @Composable @UiComposable (PaddingValues) -> Unit,
    snackbar: @Composable @UiComposable () -> Unit,
    fab: @Composable @UiComposable () -> Unit,
    bottomBar: @Composable @UiComposable () -> Unit
) {
    SubcomposeLayout { constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        layout(layoutWidth, layoutHeight) {
            val topBarPlaceables = subcompose(ScaffoldLayoutContent.TopBar, topBar).fastMap {
                it.measure(looseConstraints)
            }

            val topBarHeight = topBarPlaceables.fastMaxBy { it.height }?.height ?: 0

            val snackbarPlaceables = subcompose(ScaffoldLayoutContent.Snackbar, snackbar).fastMap {
                it.measure(looseConstraints)
            }

            val snackbarHeight = snackbarPlaceables.fastMaxBy { it.height }?.height ?: 0

            val fabPlaceables =
                subcompose(ScaffoldLayoutContent.Fab, fab).fastMap { measurable ->
                    measurable.measure(looseConstraints)
                }

            val fabPlacement = if (fabPlaceables.isNotEmpty()) {
                val fabWidth = fabPlaceables.fastMaxBy { it.width }?.width ?: 0
                val fabHeight = fabPlaceables.fastMaxBy { it.height }?.height ?: 0
                // FAB distance from the left of the layout, taking into account LTR / RTL
                if (fabWidth != 0 && fabHeight != 0) {
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
                        isDocked = isFabDocked,
                        left = fabLeftOffset,
                        width = fabWidth,
                        height = fabHeight
                    )
                } else {
                    null
                }
            } else {
                null
            }

            val bottomBarPlaceables = subcompose(ScaffoldLayoutContent.BottomBar) {
                CompositionLocalProvider(
                    LocalFabPlacement provides fabPlacement,
                    content = bottomBar
                )
            }.fastMap { it.measure(looseConstraints) }

            val bottomBarHeight = bottomBarPlaceables.fastMaxBy { it.height }?.height ?: 0
            val fabOffsetFromBottom = fabPlacement?.let {
                if (bottomBarHeight == 0) {
                    it.height + FabSpacing.roundToPx()
                } else {
                    if (isFabDocked) {
                        // Total height is the bottom bar height + half the FAB height
                        bottomBarHeight + (it.height / 2)
                    } else {
                        // Total height is the bottom bar height + the FAB height + the padding
                        // between the FAB and bottom bar
                        bottomBarHeight + it.height + FabSpacing.roundToPx()
                    }
                }
            }

            val snackbarOffsetFromBottom = if (snackbarHeight != 0) {
                snackbarHeight + (fabOffsetFromBottom ?: bottomBarHeight)
            } else {
                0
            }

            val bodyContentHeight = layoutHeight - topBarHeight

            val bodyContentPlaceables = subcompose(ScaffoldLayoutContent.MainContent) {
                val innerPadding = PaddingValues(bottom = bottomBarHeight.toDp())
                content(innerPadding)
            }.fastMap { it.measure(looseConstraints.copy(maxHeight = bodyContentHeight)) }

            // Placing to control drawing order to match default elevation of each placeable

            bodyContentPlaceables.fastForEach {
                it.place(0, topBarHeight)
            }
            topBarPlaceables.fastForEach {
                it.place(0, 0)
            }
            snackbarPlaceables.fastForEach {
                it.place(0, layoutHeight - snackbarOffsetFromBottom)
            }
            // The bottom bar is always at the bottom of the layout
            bottomBarPlaceables.fastForEach {
                it.place(0, layoutHeight - bottomBarHeight)
            }
            // Explicitly not using placeRelative here as `leftOffset` already accounts for RTL
            fabPlaceables.fastForEach {
                it.place(fabPlacement?.left ?: 0, layoutHeight - (fabOffsetFromBottom ?: 0))
            }
        }
    }
}

/**
 * Placement information for a [FloatingActionButton] inside a [Scaffold].
 *
 * @property isDocked whether the FAB should be docked with the bottom bar
 * @property left the FAB's offset from the left edge of the bottom bar, already adjusted for RTL
 * support
 * @property width the width of the FAB
 * @property height the height of the FAB
 */
@Immutable
internal class FabPlacement(
    val isDocked: Boolean,
    val left: Int,
    val width: Int,
    val height: Int
)

/**
 * CompositionLocal containing a [FabPlacement] that is read by [BottomAppBar] to calculate notch
 * location.
 */
internal val LocalFabPlacement = staticCompositionLocalOf<FabPlacement?> { null }

// FAB spacing above the bottom bar / bottom of the Scaffold
private val FabSpacing = 16.dp

private enum class ScaffoldLayoutContent { TopBar, MainContent, Snackbar, Fab, BottomBar }
