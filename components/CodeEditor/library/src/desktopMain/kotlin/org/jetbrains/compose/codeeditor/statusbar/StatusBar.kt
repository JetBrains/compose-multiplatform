package org.jetbrains.compose.codeeditor.statusbar

import org.jetbrains.compose.codeeditor.AppTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun StatusBar(
    message: String,
    busyState: BusyState
) = Box(
    modifier = Modifier
        .height(28.dp)
        .fillMaxWidth()
        .drawBehind { // top border
            clipRect {
                drawLine(
                    brush = SolidColor(AppTheme.colors.borderLight),
                    cap = StrokeCap.Square,
                    start = Offset.Zero,
                    end = Offset(x = size.width, y = 0f)
                )
            }
        }
        .padding(vertical = 4.dp)
) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp).fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (busyState.isBusy) {
            Spacer(Modifier.width(8.dp))
            CircularProgressIndicator(
                modifier = Modifier.requiredSize(14.dp),
                color = AppTheme.colors.indicatorColor,
                strokeWidth = 2.dp
            )
        }
    }
}
