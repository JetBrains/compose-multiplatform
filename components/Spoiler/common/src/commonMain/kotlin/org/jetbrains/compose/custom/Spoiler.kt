import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

/**
 * Hides the composable content by default and reveals it when a click occurred.
 *
 * @param hideInitial Set to true to initially show the content of the [Spoiler]
 * @param label The clickable label composable of the [Spoiler]
 * @param content The content to be displayed when the [Spoiler] is unveiled
 */
@Composable
fun Spoiler(
    modifier: Modifier = Modifier,
    hideInitial: Boolean = true,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    label: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    var show by remember { mutableStateOf(!hideInitial) }
    Box(modifier.clickable { show = !show }) {
        ColumnScope.label()
    }
    val columnModifier = if (show) Modifier else Modifier.alpha(0F).size(0.dp, 0.dp)
    Column(columnModifier, horizontalAlignment = horizontalAlignment) {
        ColumnScope.content()
    }
}
