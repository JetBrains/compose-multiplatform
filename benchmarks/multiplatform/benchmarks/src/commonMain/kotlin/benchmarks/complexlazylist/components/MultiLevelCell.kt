package benchmarks.complexlazylist.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import benchmarks.complexlazylist.models.IBaseViewModel
import benchmarks.complexlazylist.models.ICompositionItem
import benchmarks.complexlazylist.models.ICompositionModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun DecoratedCell(model: IBaseViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.LightGray.copy(1f),
                        Color.LightGray.copy(0.5f),
                        Color.LightGray.copy(0f)
                    ),
                    start = Offset(320f, 0f),
                    end = Offset(320f, 164f)
                )
            ),
        verticalArrangement = Arrangement.spacedBy((-60).dp)
    ) {
        CompositionView(model)
        Box(
            modifier = Modifier.fillMaxWidth()
                .heightIn(164.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(64.dp)
                    .padding(8.dp)
                    .border(1.dp, Color.Cyan.copy(0.3f), RoundedCornerShape(4.dp))
                    .padding(8.dp)
                    .background(color = Color.Cyan.copy(alpha = 0.1f))
            ) {

                Avatar(
                    modifier = Modifier
                        .padding(4.dp)
                        .align(Alignment.CenterVertically)
                )

                TextInfo(
                    Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                )

                FollowBtn(
                    Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}

@Composable
internal fun CompositionView(model: IBaseViewModel) {
    if (model !is ICompositionModel)
        return
    Box(
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White).padding(10.dp)
    ) {
        model.overlyTopLeft?.let { overlyTopLeft(it) }
        model.overlyView1?.let { overlyView1(it) }
        model.overlyView2?.let { overlyView2(it) }
        model.overlyView3?.let { overlyView3(it) }
        model.overlyTopRight?.let { overlyTopRight(it) }
        model.label?.let { overlyLabel(it) }
    }
}

@Composable
internal fun overlyTopLeft(compositionItem: ICompositionItem) {
    compositionItem.bgColor?.let { Color.DarkGray }?.let { ColorPainter(it) }?.let {
        compositionItem.alpha?.toFloat()?.let { it1 ->
            compositionItem.radius?.toFloat()?.let { RoundedCornerShape(it.dp) }?.let { it2 ->
                Modifier
                    .padding(start = 0.dp, top = 0.dp)
                    .width(50.dp)
                    .height(50.dp)
                    .alpha(it1)
                    .clip(shape = it2)
                    .shadow(3.dp)
            }
        }?.let { it2 ->
            Image(
                painter = it,
                modifier = it2,
                contentDescription = null
            )
        }
    }
}

@Composable
internal fun overlyView1(compositionItem: ICompositionItem) {
    compositionItem.bgColor?.let { Color.Red }?.let { ColorPainter(it) }?.let {
        compositionItem.alpha?.toFloat()?.let { it1 ->
            compositionItem.radius?.toFloat()?.let { RoundedCornerShape(it.dp) }?.let { it2 ->
                Modifier
                    .padding(start = 20.dp, top = 20.dp)
                    .width(150.dp)
                    .height(100.dp)
                    .alpha(it1)
                    .clip(shape = it2)
                    .shadow(3.dp)
            }
        }?.let { it2 ->
            Image(
                painter = it,
                modifier = it2,
                contentDescription = null
            )
        }
    }
}

@Composable
internal fun overlyView2(compositionItem: ICompositionItem) {
    compositionItem.bgColor?.let { Color.Yellow }?.let { ColorPainter(it) }?.let {
        compositionItem.alpha?.toFloat()?.let { it1 ->
            compositionItem.radius?.toFloat()?.let { RoundedCornerShape(it.dp) }?.let { it2 ->
                Modifier
                    .padding(start = 100.dp, top = 50.dp)
                    .width(150.dp)
                    .height(100.dp)
                    .alpha(it1)
                    .clip(shape = it2)
                    .shadow(3.dp)
            }
        }?.let { it2 ->
            Image(
                painter = it,
                modifier = it2,
                contentDescription = null
            )
        }
    }
}

@Composable
internal fun overlyView3(compositionItem: ICompositionItem) {
    compositionItem.bgColor?.let { Color.Green }?.let { ColorPainter(it) }?.let {
        compositionItem.alpha?.toFloat()?.let { it1 ->
            compositionItem.radius?.toFloat()?.let { RoundedCornerShape(it.dp) }?.let { it2 ->
                Modifier
                    .padding(start = 110.dp, top = 60.dp)
                    .width(130.dp)
                    .height(80.dp)
                    .alpha(it1)
                    .clip(shape = it2)
                    .shadow(3.dp)
            }
        }?.let { it2 ->
            Image(
                painter = it,
                modifier = it2,
                contentDescription = null
            )
        }
    }
}

@Composable
internal fun overlyTopRight(compositionItem: ICompositionItem) {
    compositionItem.bgColor?.let { Color.LightGray }?.let { ColorPainter(it) }?.let {
        compositionItem.alpha?.toFloat()?.let { it1 ->
            compositionItem.radius?.toFloat()?.let { RoundedCornerShape(it.dp) }?.let { it2 ->
                Modifier
                    .padding(start = 160.dp, top = 15.dp)
                    .width(90.dp)
                    .height(60.dp)
                    .alpha(it1)
                    .clip(shape = it2)
                    .shadow(3.dp)
            }
        }?.let { it2 ->
            Image(
                painter = it,
                modifier = it2,
                contentDescription = null
            )
        }
    }
}

@Composable
internal fun overlyLabel(compositionItem: ICompositionItem) {
    Box(modifier = Modifier.fillMaxSize().padding(), contentAlignment = Alignment.TopCenter) {
        compositionItem.text?.let {
            Text(
                text = it,
                modifier = Modifier.background(Color.Black.copy(0.5f)).alpha(0.5f)
                    .padding(start = 10.dp, end = 10.dp).width(200.dp).height(100.dp),
                color = Color.Cyan
            )
        }
    }
}

@Composable
internal fun Avatar(modifier: Modifier) {
    Image(
        modifier = modifier
            .size(50.dp)
            .clip(CircleShape)
            .border(
                shape = CircleShape,
                border = BorderStroke(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Blue, Color.Yellow, Color.Green, Color.Red),
                        start = Offset(0f, 0f),
                        end = Offset(100f, 100f)
                    )
                )
            )
            .border(
                shape = CircleShape,
                border = BorderStroke(4.dp, SolidColor(Color.White))
            ),
        painter = ColorPainter(color = Color.Gray),
        contentDescription = null, // decorative
    )
}

@Composable
internal fun TextInfo(modifier: Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "name",
            color = Color.Black,
            maxLines = 1,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 0.15.sp
            )
        )
        Text(
            text = "desc",
            color = Color.Black.copy(alpha = 0.75f),
            maxLines = 1,
            style = TextStyle( // here
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                letterSpacing = 0.25.sp
            )
        )
    }
}

@Composable
internal fun FollowBtn(modifier: Modifier) {
    var checked by remember { mutableStateOf(false) }
    val backgroundShape: Shape = remember { RoundedCornerShape(4.dp) }
    var clickCount by remember { mutableStateOf(1) }
    val offsetX by animateDpAsState(
        if (checked) (-80).dp else (-10).dp,
        animationSpec = tween(durationMillis = 3000)
    )

    var alpha by remember { mutableStateOf(1f) }
    var scale by remember { mutableStateOf(1f) }
    val scope = rememberCoroutineScope()
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.offset(x = offsetX),
    ) {
        Text(
            text = if (checked) "unfollow" else "follow",
            style = typography.body1.copy(color = Color.White),
            textAlign = TextAlign.Center,
            modifier = modifier
                .width(100.dp)
                .clickable(onClick = {
                    clickCount++
                    checked = !checked
                    scope.launch {
                        coroutineScope {
                            launch {
                                animate(1f, 0.5f) { value, _ -> alpha = value }
                            }
                            launch {
                                animate(1f, 0.5f) { value, _ -> scale = value }
                            }
                        }
                        coroutineScope {
                            launch {
                                animate(0.5f, 1f) { value, _ -> alpha = value }
                            }
                            launch {
                                animate(0.5f, 1f) { value, _ -> scale = value }
                            }
                        }
                    }
                })
                .shadow(5.dp, shape = backgroundShape)
                .clip(backgroundShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Red,
                            Color.Yellow,
                        ),
                        startY = 0f,
                        endY = 80f
                    )
                )
                .padding(6.dp)
                .scale(scale)
                .alpha(alpha)
        )
    }
}
