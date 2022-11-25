package example.imageviewer.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import example.imageviewer.style.DarkGray

@Composable
fun SplashUI() {
    Box(Modifier.fillMaxSize().background(DarkGray)) {
        Text(
            // TODO implement common resources
            "Image Viewer",
            Modifier.align(Alignment.Center),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 100.sp
        )
    }
}
