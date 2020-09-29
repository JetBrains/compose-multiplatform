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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticAmbientOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.onGloballyPositioned
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * State for [Scaffold] composable component.
 *
 * Contains basic screen state, e.g. Drawer configuration, as well as sizes of components after
 * layout has happened
 *
 * @param drawerState the drawer state
 * @param snackbarHostState instance of [SnackbarHostState] to be used to show [Snackbar]s
 * inside of the [Scaffold]
 * @param isDrawerGesturesEnabled whether or not drawer can be interacted with via gestures
 */
@Stable
@OptIn(ExperimentalMaterialApi::class)
class ScaffoldState(
    val drawerState: DrawerState,
    val snackbarHostState: SnackbarHostState,
    isDrawerGesturesEnabled: Boolean = true
) {

    /**
     * Whether or not drawer sheet in scaffold (if set) can be interacted by gestures.
     */
    var isDrawerGesturesEnabled by mutableStateOf(isDrawerGesturesEnabled)

    internal val scaffoldGeometry = ScaffoldGeometry()
}

/**
 * Creates a [ScaffoldState] with the default animation clock and memoizes it.
 *
 * @param drawerState the drawer state
 * @param snackbarHostState instance of [SnackbarHostState] to be used to show [Snackbar]s
 * inside of the [Scaffold]
 * @param isDrawerGesturesEnabled whether or not drawer can be interacted with via gestures
 */
@Composable
@OptIn(ExperimentalMaterialApi::class)
fun rememberScaffoldState(
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    snackbarHostState: SnackbarHostState = SnackbarHostState(),
    isDrawerGesturesEnabled: Boolean = true
): ScaffoldState = remember {
    ScaffoldState(drawerState, snackbarHostState, isDrawerGesturesEnabled)
}

@Stable
internal class ScaffoldGeometry {
    var topBarBounds by mutableStateOf<Rect?>(null, structuralEqualityPolicy())
    var bottomBarBounds by mutableStateOf<Rect?>(null, structuralEqualityPolicy())
    var fabBounds by mutableStateOf<Rect?>(null, structuralEqualityPolicy())

    var isFabDocked by mutableStateOf(false)
}

internal val AmbientScaffoldGeometry = staticAmbientOf { ScaffoldGeometry() }

/**
 * The possible positions for a [FloatingActionButton] attached to a [Scaffold].
 */
enum class FabPosition {
    /**
     * Position FAB at the bottom of the screen in the center, above the [BottomAppBar] (if it
     * exists)
     */

    Center,

    /**
     * Position FAB at the bottom of the screen at the end, above the [BottomAppBar] (if it
     * exists)
     */
    End
}

/**
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
 * @param drawerShape shape of the drawer sheet (if set)
 * @param drawerElevation drawer sheet elevation. This controls the size of the shadow
 * below the drawer sheet (if set)
 * @param drawerBackgroundColor background color to be used for the drawer sheet
 * @param drawerContentColor color of the content to use inside the drawer sheet. Defaults to
 * either the matching `onFoo` color for [drawerBackgroundColor], or, if it is not a color from
 * the theme, this will keep the same value set above this Surface.
 * @param drawerScrimColor color of the scrim that obscures content when the drawer is open
 * @param backgroundColor background of the scaffold body
 * @param contentColor color of the content in scaffold body. Defaults to either the matching
 * `onFoo` color for [backgroundColor], or, if it is not a color from the theme, this will keep
 * the same value set above this Surface.
 * @param bodyContent content of your screen. The lambda receives an [PaddingValues] that should be
 * applied to the content root via [Modifier.padding] to properly offset top and bottom bars. If
 * you're using VerticalScroller, apply this modifier to the child of the scroller, and not on
 * the scroller itself.
 */
@Composable
@OptIn(ExperimentalMaterialApi::class)
fun Scaffold(
    modifier: Modifier = Modifier,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    topBar: @Composable (() -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SnackbarHost(it) },
    floatingActionButton: @Composable (() -> Unit)? = null,
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    isFloatingActionButtonDocked: Boolean = false,
    drawerContent: @Composable (ColumnScope.() -> Unit)? = null,
    drawerShape: Shape = MaterialTheme.shapes.large,
    drawerElevation: Dp = DrawerConstants.DefaultElevation,
    drawerBackgroundColor: Color = MaterialTheme.colors.surface,
    drawerContentColor: Color = contentColorFor(drawerBackgroundColor),
    drawerScrimColor: Color = DrawerConstants.defaultScrimColor,
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    bodyContent: @Composable (PaddingValues) -> Unit
) {
    scaffoldState.scaffoldGeometry.isFabDocked = isFloatingActionButtonDocked
    val child = @Composable { childModifier: Modifier ->
        Surface(modifier = childModifier, color = backgroundColor, contentColor = contentColor) {
            Column(Modifier.fillMaxSize()) {
                if (topBar != null) {
                    TopBarContainer(Modifier.zIndex(TopAppBarZIndex), scaffoldState, topBar)
                }
                Box(Modifier.weight(1f, fill = true)) {
                    ScaffoldContent(Modifier.fillMaxSize(), scaffoldState, bodyContent)
                    Column(Modifier.align(Alignment.BottomCenter)) {
                        snackbarHost(scaffoldState.snackbarHostState)
                        ScaffoldBottom(
                            scaffoldState = scaffoldState,
                            fabPos = floatingActionButtonPosition,
                            isFabDocked = isFloatingActionButtonDocked,
                            fab = floatingActionButton,
                            bottomBar = bottomBar
                        )
                    }
                }
            }
        }
    }

    if (drawerContent != null) {
        ModalDrawerLayout(
            modifier = modifier,
            drawerState = scaffoldState.drawerState,
            gesturesEnabled = scaffoldState.isDrawerGesturesEnabled,
            drawerContent = drawerContent,
            drawerShape = drawerShape,
            drawerElevation = drawerElevation,
            drawerBackgroundColor = drawerBackgroundColor,
            drawerContentColor = drawerContentColor,
            scrimColor = drawerScrimColor,
            bodyContent = { child(Modifier) }
        )
    } else {
        child(modifier)
    }
}

private fun FabPosition.toColumnAlign() =
    if (this == FabPosition.End) Alignment.End else Alignment.CenterHorizontally

/**
 * Scaffold part that is on the bottom. Includes FAB and BottomBar
 */
@Composable
private fun ScaffoldBottom(
    scaffoldState: ScaffoldState,
    fabPos: FabPosition,
    isFabDocked: Boolean,
    fab: @Composable (() -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null
) {
    if (isFabDocked && bottomBar != null && fab != null) {
        DockedBottomBar(
            fabPosition = fabPos,
            fab = { FabContainer(Modifier, scaffoldState, fab) },
            bottomBar = { BottomBarContainer(scaffoldState, bottomBar) }
        )
    } else {
        Column(Modifier.fillMaxWidth()) {
            if (fab != null) {
                FabContainer(
                    Modifier.align(fabPos.toColumnAlign())
                        .padding(start = FabSpacing, end = FabSpacing, bottom = FabSpacing),
                    scaffoldState,
                    fab
                )
            }
            if (bottomBar != null) {
                BottomBarContainer(scaffoldState, bottomBar)
            }
        }
    }
}

/**
 * Simple `Stack` implementation that places [fab] on top (z-axis) of [bottomBar], with the midpoint
 * of the [fab] aligned to the top edge of the [bottomBar].
 *
 * This is needed as we want the total height of the BottomAppBar to be equal to the height of
 * [bottomBar] + half the height of [fab], which is only possible with a custom layout.
 */
@Composable
private fun DockedBottomBar(
    fabPosition: FabPosition,
    fab: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    Layout(
        children = {
            bottomBar()
            fab()
        }
    ) { measurables, constraints ->
        val (appBarPlaceable, fabPlaceable) = measurables.map { it.measure(constraints) }

        val layoutWidth = appBarPlaceable.width
        // Total height is the app bar height + half the fab height
        val layoutHeight = appBarPlaceable.height + (fabPlaceable.height / 2)

        val appBarVerticalOffset = layoutHeight - appBarPlaceable.height
        val fabPosX = if (fabPosition == FabPosition.End) {
            layoutWidth - fabPlaceable.width - DockedFabEndSpacing.toIntPx()
        } else {
            (layoutWidth - fabPlaceable.width) / 2
        }

        layout(layoutWidth, layoutHeight) {
            appBarPlaceable.placeRelative(0, appBarVerticalOffset)
            fabPlaceable.placeRelative(fabPosX, 0)
        }
    }
}

@Composable
private fun ScaffoldContent(
    modifier: Modifier,
    scaffoldState: ScaffoldState,
    content: @Composable (PaddingValues) -> Unit
) {
    ScaffoldSlot(modifier) {
        val innerPadding = with(DensityAmbient.current) {
            val bottom = scaffoldState.scaffoldGeometry.bottomBarBounds?.height?.toDp() ?: 0.dp
            PaddingValues(bottom = bottom)
        }
        content(innerPadding)
    }
}

@Composable
private fun BottomBarContainer(
    scaffoldState: ScaffoldState,
    bottomBar: @Composable () -> Unit
) {
    BoundsAwareScaffoldSlot(
        Modifier,
        { scaffoldState.scaffoldGeometry.bottomBarBounds = it },
        slotContent = {
            Providers(AmbientScaffoldGeometry provides scaffoldState.scaffoldGeometry) {
                bottomBar()
            }
        }
    )
}

@Composable
private fun FabContainer(
    modifier: Modifier,
    scaffoldState: ScaffoldState,
    fab: @Composable () -> Unit
) {
    BoundsAwareScaffoldSlot(modifier, { scaffoldState.scaffoldGeometry.fabBounds = it }, fab)
}

@Composable
private fun TopBarContainer(
    modifier: Modifier,
    scaffoldState: ScaffoldState,
    topBar: @Composable () -> Unit
) {
    BoundsAwareScaffoldSlot(modifier, { scaffoldState.scaffoldGeometry.topBarBounds = it }, topBar)
}

@Composable
private fun BoundsAwareScaffoldSlot(
    modifier: Modifier,
    onBoundsKnown: (Rect?) -> Unit,
    slotContent: @Composable () -> Unit
) {
    onDispose {
        onBoundsKnown(null)
    }
    ScaffoldSlot(
        modifier = modifier.onGloballyPositioned { coords ->
            onBoundsKnown(coords.boundsInParent)
        },
        content = slotContent
    )
}

/**
 * Default slot implementation for Scaffold slots content
 */
@Composable
private fun ScaffoldSlot(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier) { content() }
}

private val FabSpacing = 16.dp
private val DockedFabEndSpacing = 16.dp
private const val TopAppBarZIndex = 1f