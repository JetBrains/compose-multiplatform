package benchmarks.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Transition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import compose_benchmarks.benchmarks.generated.resources.Res
import compose_benchmarks.benchmarks.generated.resources.compose_multiplatform
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
@Composable
fun AnimatedVisibility() {
    MaterialTheme {
        var showImage by remember { mutableStateOf(false) }
        var transition: Transition<EnterExitState>? = null
        LaunchedEffect(showImage) {
            do {
                withFrameNanos {}
            } while (transition?.isRunning == true)

            showImage = !showImage
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedVisibility(showImage) {
                transition = this.transition
                Image(
                    painterResource(Res.drawable.compose_multiplatform),
                    null
                )
            }
        }
    }
}