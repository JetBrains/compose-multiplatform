package org.jetbrains.compose.codeeditor.editor.text

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.codeeditor.AppTheme

@Composable
internal fun LineNumbers(
    textState: TextState,
    textFieldState: EditorTextFieldState,
    scrollState: ScrollState,
    onWidthChange: (Int) -> Unit = {}
) {
    with(LocalDensity.current) {
        Surface(
            color = AppTheme.colors.backgroundMedium,
        ) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(IntrinsicSize.Max)
                        .drawBehind {
                            clipRect {
                                drawLine(
                                    brush = SolidColor(AppTheme.colors.borderMedium),
                                    cap = StrokeCap.Square,
                                    start = Offset(x = size.width, y = 0f),
                                    end = Offset(x = size.width, y = size.height)
                                )
                            }
                        }
                        .onSizeChanged { onWidthChange(it.width + Paddings.textFieldLeftPadding.roundToPx()) }
                ) {
                    for (i in 1..textFieldState.lineCount) {
                        Text(
                            text = i.toString(),
                            modifier = Modifier
                                .offset(y = (
                                    getVerticalPosition(
                                        top = textState.getLineTop(i - 1),
                                        bottom = textState.getLineBottom(i - 1),
                                        height = textState.lineHeight
                                    ) - scrollState.value).toDp())
                                .widthIn(min = 25.dp)
                                .fillMaxWidth()
                                .padding(Paddings.lineNumbersPadding),
                            style = MaterialTheme.typography.body1,
                            textAlign = TextAlign.Right
                        )
                    }
                }
            }
        }
    }

}

private fun getVerticalPosition(top: Float, bottom: Float, height: Float) =
    if (height > 0) top + (bottom - top - height) / 2
    else top
