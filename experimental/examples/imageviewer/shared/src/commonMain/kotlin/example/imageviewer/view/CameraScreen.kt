package example.imageviewer.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign

@Composable
internal fun CameraScreen(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black).clickable { onBack() }, contentAlignment = Alignment.Center) {
        Text("Nothing here yet 📸", textAlign = TextAlign.Center, color = Color.White)
    }
}