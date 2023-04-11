@file:Suppress("FunctionName")

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OpenedCell(cell: Cell) {
    Text(
        text = cell.bombsNear.toString(),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun CellWithIcon(src: String, alt: String) {
    Image(
        painter = loadImage(src),
        contentDescription = alt,
        modifier = Modifier.fillMaxSize().padding(Dp(4.0f))
    )
}

@Composable
fun Mine() {
    CellWithIcon(src = "assets/mine.png", alt = "Bomb")
}

@Composable
fun Flag() {
    CellWithIcon(src = "assets/flag.png", alt = "Flag")
}

@Composable
fun IndicatorWithIcon(iconPath: String, alt: String, value: Int) {
    Box(modifier = Modifier.background(Color(0x8e, 0x6e, 0x0e))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp, 40.dp)) {
                CellWithIcon(iconPath, alt)
            }

            Box(modifier = Modifier.size(56.dp, 36.dp)) {
                Text(
                    text = value.toString(),
                    fontSize = 24.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun NewGameButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .background(color = Color(0x42, 0x8e, 0x04))
            .border(width = 1.dp, color = Color.White)
            .clickable { onClick() }
    ) {
        Text(
            text,
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier.padding(4.dp)
        )
    }
}