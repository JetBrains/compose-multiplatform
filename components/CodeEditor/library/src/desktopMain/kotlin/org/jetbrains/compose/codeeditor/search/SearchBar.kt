package org.jetbrains.compose.codeeditor.search

import org.jetbrains.compose.codeeditor.AppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerIcon
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect

@Composable
internal fun SearchBar(
    searchState: SearchState
) {
    if (!searchState.isVisible) return

    LaunchedEffect(Unit) {
        searchState.requestFocusAndSelect()
    }

    LaunchedEffect(searchState.searchString) {
        searchState.nextOnInput()
    }

    LaunchedEffect(searchState.selectedResultIndexByCaret) {
        searchState.onSelectedResultIndexByCaretChange()
    }

    Surface(
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .height(32.dp)
                .drawBehind { // bottom border todo: create a separate modifier for one side border
                    clipRect {
                        drawLine(
                            brush = SolidColor(AppTheme.colors.borderLight),
                            cap = StrokeCap.Square,
                            start = Offset(x = 0f, y = size.height),
                            end = Offset(x = size.width, y = size.height)
                        )
                    }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {

            CompositionLocalProvider(LocalTextSelectionColors provides AppTheme.colors.selectionColors) {
                BasicTextField(
                    value = searchState.fieldValue,
                    onValueChange = searchState::onFieldValueChange,
                    modifier = Modifier
                        .weight(.25f)
                        .height(30.dp)
                        .background(AppTheme.colors.textFieldBackground)
                        .drawBehind { // right border
                            clipRect {
                                drawLine(
                                    brush = SolidColor(AppTheme.colors.borderDark),
                                    cap = StrokeCap.Square,
                                    start = Offset(x = size.width, y = 0f),
                                    end = Offset(x = size.width, y = size.height)
                                )
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .focusRequester(searchState.focusRequester)
                        .onFocusChanged {
                            if (it.hasFocus) {
                                searchState.nextOnFocus()
                            }
                        }
                        .onPreviewKeyEvent(searchState.previewInnerKeyEventHandler::onKeyEvent),
                    textStyle = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colors.onBackground),
                    singleLine = true
                )
            }

            Text(
                text = searchState.status,
                modifier = Modifier
                    .width(64.dp)
                    .padding(horizontal = 5.dp),
                textAlign = TextAlign.Center,
                color = if (searchState.notFound) AppTheme.colors.errorColor else MaterialTheme.colors.onSurface,
                style = MaterialTheme.typography.overline
            )

            val buttonModifiers = remember {
                Modifier
                    .padding(1.dp, 2.dp, 1.dp, 3.dp)
                    .size(26.dp)
            }

            SearchBarButton(
                onClick = searchState::prev,
                modifier = buttonModifiers,
                enabled = searchState.resultIsNotEmpty
            ) {
                Text("↑")
            }

            SearchBarButton(
                onClick = searchState::next,
                modifier = buttonModifiers,
                enabled = searchState.resultIsNotEmpty
            ) {
                Text("↓")
            }

            Spacer(Modifier.weight(.75f))

            SearchBarButton(
                onClick = searchState::close,
                modifier = buttonModifiers
            ) {
                Text("×")
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchBarButton(
    onClick: () -> Unit,
    color: Color = LocalContentColor.current,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    var backgroundColor by remember { mutableStateOf(Color.Transparent) }
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        if (enabled) {
            val pressInteractions = mutableListOf<PressInteraction.Press>()
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> pressInteractions.add(interaction)
                    is PressInteraction.Release -> pressInteractions.remove(interaction.press)
                    is PressInteraction.Cancel -> pressInteractions.remove(interaction.press)
                }
                backgroundColor =
                    if (pressInteractions.isNotEmpty()) AppTheme.colors.buttonPress
                    else AppTheme.colors.buttonHover
            }
        } else {
            backgroundColor = Color.Transparent
        }
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .pointerMoveFilter(
                onEnter = {
                    if (enabled) backgroundColor = AppTheme.colors.buttonHover
                    false
                },
                onExit = {
                    backgroundColor = Color.Transparent
                    false
                }
            )
            .pointerIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default),
        enabled = enabled,
        shape = RoundedCornerShape(15),
        color = backgroundColor,
        contentColor = if (enabled) color else AppTheme.colors.buttonDisabled,
        interactionSource = interactionSource,
        indication = null
    ) {
        ProvideTextStyle(value = MaterialTheme.typography.button) {
            Box(
                contentAlignment = Alignment.Center,
                content = content
            )
        }
    }
}
