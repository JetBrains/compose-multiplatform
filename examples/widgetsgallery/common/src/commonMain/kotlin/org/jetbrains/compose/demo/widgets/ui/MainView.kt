package org.jetbrains.compose.demo.widgets.ui

import androidx.compose.animation.animate
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.selection.DisableSelection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import org.jetbrains.compose.demo.widgets.platform.VerticalScrollbar
import org.jetbrains.compose.demo.widgets.platform.pointerMoveFilter
import org.jetbrains.compose.demo.widgets.theme.WidgetGalleryTheme
import org.jetbrains.compose.demo.widgets.ui.utils.PanelState
import org.jetbrains.compose.demo.widgets.ui.utils.ResizablePanel
import org.jetbrains.compose.demo.widgets.ui.utils.VerticalSplittable
import org.jetbrains.compose.demo.widgets.ui.utils.withoutWidthConstraints
import kotlin.ranges.coerceAtLeast

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
    val widgetsTypeState = savedInstanceState { WidgetsType.sortedValues.first() }
    val panelState = remember { PanelState() }

    val animatedSize = if (panelState.splitter.isResizing) {
        if (panelState.isExpanded) panelState.expandedSize else panelState.collapsedSize
    } else {
        animate(
            if (panelState.isExpanded) panelState.expandedSize else panelState.collapsedSize,
            SpringSpec(stiffness = Spring.StiffnessLow)
        )
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
        with(AmbientDensity.current) {
            val scrollState = rememberLazyListState()

            val fontSize = 14.sp
            val lineHeight = fontSize.toDp() * 1.5f

            val items = WidgetsType.sortedValues
            LazyColumnFor(
                items,
                modifier = Modifier.fillMaxSize().withoutWidthConstraints(),
                state = scrollState,
                itemContent = { WidgetsListItemViewImpl(it, widgetsTypeState, fontSize, lineHeight) }
            )

            VerticalScrollbar(
                Modifier.align(Alignment.CenterEnd),
                scrollState,
                items.size,
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
        val textColor = AmbientContentColor.current.let {
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
