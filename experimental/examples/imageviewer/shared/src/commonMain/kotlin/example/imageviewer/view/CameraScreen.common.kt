package example.imageviewer.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun CameraScreen(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CameraView(Modifier.fillMaxSize())
            Box(Modifier.fillMaxWidth().height(30.dp).background(Color.Blue).align(Alignment.TopCenter))
            Button(
                modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
                onClick = {

                }) {
                Text("Compose Button - Take a photo")
            }
        }
        TopLayout(
            alignLeftContent = {
                Tooltip("TODO localization.back") {//todo
                    CircularButton(
                        painterResource("arrowleft.png"),
                        onClick = { onBack() }
                    )
                }
            },
            alignRightContent = {},
        )
    }
}

@Composable
internal expect fun CameraView(modifier: Modifier)
