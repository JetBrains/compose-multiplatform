package example.imageviewer.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import example.imageviewer.style.DarkGray
import example.imageviewer.style.DarkGreen
import example.imageviewer.style.Foreground
import example.imageviewer.style.TranslucentBlack

@Composable
internal fun LoadingScreen(text: String = "") {
    Box(
        modifier = Modifier.fillMaxSize().background(color = TranslucentBlack)
    ) {
        Box(modifier = Modifier.align(Alignment.Center)) {
            Surface(color = DarkGray, elevation = 4.dp, shape = CircleShape) {
                CircularProgressIndicator(
                    modifier = Modifier.size(50.dp).padding(3.dp, 3.dp, 4.dp, 4.dp),
                    color = DarkGreen
                )
            }
        }
        Text(
            text = text,
            modifier = Modifier.align(Alignment.Center).offset(0.dp, 70.dp),
            style = MaterialTheme.typography.body1,
            color = Foreground
        )
    }
}
