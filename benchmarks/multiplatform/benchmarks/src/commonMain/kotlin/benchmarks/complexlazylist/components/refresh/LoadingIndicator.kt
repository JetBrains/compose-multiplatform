package benchmarks.complexlazylist.components.refresh

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max

@Composable
internal fun BoxScope.LoadingIndicatorDefault(
    modifier: Modifier,
    state: SwipeRefreshState,
    indicatorHeight: Dp
) {
    val height = max(30.dp, with(LocalDensity.current) {
        state.progress.offset.toDp()
    })
    var prev by remember { mutableStateOf(Offset(0f, 0f)) }
    var prevOffsetAngle by remember { mutableStateOf(0f) }
    var offsetAngle by remember { mutableStateOf(0f) }
    var startAngle by remember { mutableStateOf(prev.x) }
    var endAngle by remember { mutableStateOf(prev.y) }

    LaunchedEffect(state.loadState == REFRESHING || state.loadState == LOADING_MORE) {
        if (!(state.loadState == REFRESHING || state.loadState == LOADING_MORE)) {
            prevOffsetAngle = offsetAngle
            return@LaunchedEffect
        }
        animate(
            initialValue = prevOffsetAngle,
            targetValue = prevOffsetAngle + 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(5000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        ) { value, _ ->
            offsetAngle = value
        }
    }

    LaunchedEffect(state.loadState == REFRESHING || state.loadState == LOADING_MORE, endAngle) {
        if (!(state.loadState == REFRESHING || state.loadState == LOADING_MORE)) return@LaunchedEffect
        if ((endAngle - startAngle).toInt() == 270) {
            animate(
                initialValue = startAngle,
                targetValue = endAngle - 10f,
                animationSpec = tween(durationMillis = 700)
            ) { value, _ ->
                startAngle = value
            }
        }
    }

    LaunchedEffect(state.loadState == REFRESHING || state.loadState == LOADING_MORE, startAngle) {
        if (!(state.loadState == REFRESHING || state.loadState == LOADING_MORE)) if (!(state.loadState == REFRESHING || state.loadState == LOADING_MORE)) {
            prev = Offset(startAngle, endAngle)
            return@LaunchedEffect
        }
        if ((endAngle - startAngle).toInt() == 10
            || endAngle.toInt() == prev.y.toInt()
        ) {
            animate(
                initialValue = endAngle,
                targetValue = startAngle + 270f,
                animationSpec = tween(durationMillis = 700),
            ) { value, _ ->
                endAngle = value
            }
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(height), contentAlignment = Alignment.Center
    ) {
        if (state.isSwipeInProgress) {
            if (state.progress.offset <= with(LocalDensity.current) { indicatorHeight.toPx() }) {
                Text(text = if (state.progress.location == TOP) "下拉刷新" else "上拉加载更多")
            } else {
                Text(text = if (state.progress.location == TOP) "松开刷新" else "松开加载更多")
            }
        } else {
            AnimatedVisibility(
                visible = state.loadState == REFRESHING || state.loadState == LOADING_MORE,
                modifier = Modifier.align(Alignment.Center),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .align(alignment = Alignment.Center),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier
                            .padding(10.dp)
                            .height(20.dp).width(20.dp)
                            .aspectRatio(1f)
                            .rotate(offsetAngle)
                    ) {
                        Canvas(
                            modifier = Modifier.height(20.dp).width(20.dp)
                        ) {
                            drawArc(
                                color = Color.Gray,
                                startAngle = startAngle,
                                sweepAngle = endAngle - startAngle,
                                useCenter = false,
                                style = Stroke(width = 5f, cap = StrokeCap.Round)
                            )
                        }
                    }

                    Text(
                        modifier = Modifier.padding(10.dp),
                        text = "Loading...",
                        color = Color.Gray,
                        fontWeight = FontWeight.W500,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}