package visualeffects

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import graphics_2d.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

@Composable
fun Words() {
    val duration = 5000

    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val logoImg = painterResource(Res.drawable.compose_community_primary)

    val baseLogo = DpOffset(350.dp, 270.dp)

    val baseText = DpOffset(350.dp, 270.dp)

    val baseRu = DpOffset(100.dp, 100.dp)
    val baseEn = DpOffset(100.dp, 600.dp)
    val baseCh = DpOffset(600.dp, 100.dp)
    val baseJa = DpOffset(600.dp, 600.dp)

    val color1 = Color(0x6B, 0x57, 0xFF)
    val color2 = Color(0xFE, 0x28, 0x57)
    val color3 = Color(0xFD, 0xB6, 0x0D)
    val color4 = Color(0xFC, 0xF8, 0x4A)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Word(position = baseRu, angle = angle, scale = scale, text = "Ваш", color = color1)
        Word(position = baseEn, angle = angle, scale = scale, text = "Your", color = color2)
        Word(position = baseCh, angle = angle, scale = scale, text = "您的", color = color3)
        Word(position = baseJa, angle = angle, scale = scale, text = "あなたの", color = color4)
        Word(
            position = baseText,
            angle = 0f,
            scale = 6f,
            text = "    Compose\nMultiplatform",
            color = Color(52, 67, 235),
            alpha = 0.4f
        )

        val size = 80.dp * scale
        Image(
            logoImg, contentDescription = "Logo",
            modifier = Modifier
                .offset(baseLogo.x - size / 2, baseLogo.y - size / 2)
                .size(size)
                .rotate(angle * 2f)
        )
    }
}

@Composable
fun Word(
    position: DpOffset, angle: Float, scale: Float, text: String,
    color: Color, alpha: Float = 0.8f
) {
    Text(
        modifier = Modifier
            .offset(position.x, position.y)
            .rotate(angle)
            .scale(scale)
            .alpha(alpha),
        color = color,
        fontWeight = FontWeight.Bold,
        text = text,
    )
}

@Composable
fun FallingSnow() {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        repeat(50) {
            val size = remember { 20.dp + 10.dp * random() }
            val alpha = remember { 0.10f + 0.15f * random() }
            val sizePx = with(LocalDensity.current) { size.toPx() }
            val x = remember { (constraints.maxWidth * random()).toInt() }

            val infiniteTransition = rememberInfiniteTransition()
            val t by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(16000 + (16000 * random()).toInt(), easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
            val initialT = remember { random() }
            val actualT = (initialT + t) % 1f
            val y = (-sizePx + (constraints.maxHeight + sizePx) * actualT).toInt()

            Box(
                Modifier
                    .offset { IntOffset(x, y) }
                    .clip(CircleShape)
                    .alpha(alpha)
                    .background(Color.White)
                    .size(size)
            )

        }
    }
}

@Composable
fun Background() = Box(
    Modifier
        .fillMaxSize()
        .background(Color(0xFF6F97FF))
)

@Composable
fun RotatingWords() {
    Background()
    FallingSnow()
    Words()
}
