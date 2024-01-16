import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Indicators(
    count: Int,
    size: Int,
    spacer: Int,
    selectedColor: Color,
    unselectedColor: Color,
    modifier: Modifier = Modifier,
    selectedIndex: Int = 0,
    selectedLength: Int = 50
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(count) {
            val isSelected = selectedIndex == it
            IndicatorView(
                isSelected = isSelected,
                spacer = spacer,
                size = size,
                selectedLength = selectedLength,
                selectedColor = selectedColor,
                unselectedColor = unselectedColor
            )
        }
    }
}

@Composable
private fun IndicatorView(
    isSelected: Boolean,
    spacer: Int,
    size: Int,
    selectedLength: Int,
    selectedColor: Color,
    unselectedColor: Color
) {
    val color: Color by animateColorAsState(
        targetValue = if (isSelected) selectedColor else unselectedColor,
        animationSpec = tween(durationMillis = 1000)
    )
    val width: Dp by animateDpAsState(
        targetValue = if (isSelected) selectedLength.dp else size.dp,
        animationSpec = tween(durationMillis = 1000)
    )

    Box(
        modifier = Modifier
            .padding(horizontal = spacer.dp / 2)
            .size(width = width, height = size.dp)
            .clip(CircleShape)
            .background(color)
    )
}