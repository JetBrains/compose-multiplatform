package org.jetbrains.compose.demo.widgets.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.demo.widgets.platform.VerticalScrollbar
import org.jetbrains.compose.demo.widgets.platform.pointerMoveFilter
import org.jetbrains.compose.demo.widgets.theme.WidgetGalleryTheme
import org.jetbrains.compose.demo.widgets.ui.utils.PanelState
import org.jetbrains.compose.demo.widgets.ui.utils.ResizablePanel
import org.jetbrains.compose.demo.widgets.ui.utils.VerticalSplittable
import org.jetbrains.compose.demo.widgets.ui.utils.withoutWidthConstraints

@Composable
fun MainView() {
    DisableSelection {
        WidgetGalleryTheme() {
            WidgetsPanel()
        }
    }
}

@Composable
fun WidgetsPanel() {
    val widgetsTypeState = rememberSaveable { mutableStateOf(WidgetsType.sortedValues.first()) }
    val panelState = remember { PanelState() }

    val animatedSize = if (panelState.splitter.isResizing) {
        if (panelState.isExpanded) panelState.expandedSize else panelState.collapsedSize
    } else {
        animateDpAsState(
            if (panelState.isExpanded) panelState.expandedSize else panelState.collapsedSize,
            SpringSpec(stiffness = Spring.StiffnessLow)
        ).value
    }

    VerticalSplittable(
        Modifier.fillMaxSize(),
        panelState.splitter,
        onResize = {
            panelState.expandedSize =
                (panelState.expandedSize + it).coerceAtLeast(panelState.expandedSizeMin)
        }
    ) {
        ResizablePanel(
            Modifier.width(animatedSize).fillMaxHeight(),
            title = "Widgets",
            state = panelState
        ) {
            WidgetsListView(widgetsTypeState)
        }

        Box {
            Column {
                WidgetsView(
                    widgetsTypeState,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WidgetsListView(widgetsTypeState: MutableState<WidgetsType>) {
    Box {
        with(LocalDensity.current) {
            val scrollState = rememberLazyListState()

            val fontSize = 14.sp
            val lineHeight = fontSize.toDp() * 1.5f

            val sortedItems = WidgetsType.sortedValues
            LazyColumn(
                modifier = Modifier.fillMaxSize().withoutWidthConstraints(),
                state = scrollState
            ) {
                items(sortedItems) {
                    WidgetsListItemViewImpl(it, widgetsTypeState, fontSize, lineHeight)
                }
            }

            VerticalScrollbar(
                Modifier.align(Alignment.CenterEnd),
                scrollState,
                sortedItems.size,
                lineHeight
            )
        }
    }

}

@Composable
private fun WidgetsListItemViewImpl(
    widgetsType: WidgetsType,
    widgetsTypeState: MutableState<WidgetsType>,
    fontSize: TextUnit,
    height: Dp
) {
    val isCurrent = widgetsTypeState.value == widgetsType

    Row(
        modifier = Modifier
            .wrapContentHeight()
            .clickable { widgetsTypeState.value = widgetsType }
            .height(height)
            .padding(start = 16.dp)
    ) {
        var inFocus by remember { mutableStateOf(false) }
        val textColor = LocalContentColor.current.let {
            when {
                isCurrent -> it
                inFocus -> it.copy(alpha = 0.6f)
                else -> it.copy(alpha = 0.4f)
            }
        }

        Text(
            text = widgetsType.readableName,
            color = textColor,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .clipToBounds()
                .pointerMoveFilter(
                    onEnter = {
                        inFocus = true
                        true
                    },
                    onExit = {
                        inFocus = false
                        true
                    }
                ),
            softWrap = true,
            fontSize = fontSize,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
}
