@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED")

package org.jetbrains.compose.codeeditor.codecompletion

import org.jetbrains.compose.codeeditor.AppTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.codeeditor.editor.OffsetState

private val popupPositionCorrection = IntOffset(-4, 0)

@Composable
internal fun CodeCompletionPopup(
    ccState: CodeCompletionState,
    offset: OffsetState
) {
    if (ccState.isVisible) {
        ccState.ccListState.scope = rememberCoroutineScope()

        Surface(
            elevation = 1.dp,
            border = BorderStroke(Dp.Hairline, AppTheme.colors.borderLight),
            modifier = Modifier
                .absoluteOffset { offset.value + ccState.popupPosition + popupPositionCorrection }
                .widthIn(min = 150.dp, max = 300.dp)
                .padding(vertical = 2.dp)
        ) {
            Column {
                CodeCompletionList(ccState.ccListState)
                Footer(ccState)
            }
        }
    }
}

@Composable
private fun CodeCompletionList(
    ccListState: CodeCompletionListState
) = Box(
    Modifier.heightIn(max = 208.dp)
) {
    var scrollWidth = 0

    if (ccListState.isMaxPageSizeExceeded()) {
        scrollWidth = 7
        VerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width((1 + scrollWidth).dp),
            adapter = rememberScrollbarAdapter(ccListState.lazyListState)
        )
    }

    LazyColumn(
        modifier = Modifier.padding(start = 2.dp, end = (2 + scrollWidth).dp),
        state = ccListState.lazyListState
    ) {
        items(ccListState.list, key = { it.id }) { item ->
            CodeCompletionElementRow(item)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CodeCompletionElementRow(
    element: CodeCompletionIndexedElement
) = Row(
    modifier = Modifier
        .background(color = if (element.selected) AppTheme.colors.secondary else Color.Transparent)
        .combinedClickable(
            onClick = element.onClick,
            onDoubleClick = element.onDoubleClick
        )
        .padding(top = 2.dp, bottom = 3.dp, start = 2.dp, end = 2.dp)
) {
    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.body1) {
        Text(
            element.element.name ?: "",
            color = AppTheme.colors.codeColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.width(8.dp))

        Text(
            element.element.tail ?: "",
            modifier = Modifier.weight(1f),
            color = AppTheme.colors.commentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.width(15.dp))

        Text(
            element.element.type ?: "",
            color = AppTheme.colors.commentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun Footer(
    ccState: CodeCompletionState
) {
    if (ccState.isLoading || ccState.ccListState.noSuggestions) {
        Box(
            modifier = Modifier
                .height(25.dp)
                .width(150.dp)
        ) {
            if (ccState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(14.dp)
                        .align(Alignment.CenterEnd),
                    color = AppTheme.colors.indicatorColor,
                    strokeWidth = 2.dp
                )
            } else if (ccState.ccListState.noSuggestions) {
                Text(
                    text = "No suggestions",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}
