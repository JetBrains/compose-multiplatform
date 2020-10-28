package example.imageviewer.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.onActive
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.GlobalScope
import example.imageviewer.style.ToastBackground
import example.imageviewer.style.Foreground
import androidx.compose.material.Surface

enum class ToastDuration(val value: Int) {
    Short(1000), Long(3000)
}

private var isShown: Boolean = false

@Composable
fun Toast(
    text: String,
    visibility: MutableState<Boolean> = mutableStateOf(false),
    duration: ToastDuration = ToastDuration.Long
) {
    if (isShown) {
        return
    }

    if (visibility.value) {
        isShown = true
        Box(
            modifier = Modifier.fillMaxSize().padding(bottom = 20.dp),
            alignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier.preferredSize(300.dp, 70.dp),
                color = ToastBackground,
                shape = RoundedCornerShape(4.dp)
            ) {
                Box(alignment = Alignment.Center) {
                    Text(
                        text = text,
                        color = Foreground
                    )
                }
                onActive {
                    GlobalScope.launch {
                        delay(duration.value.toLong())
                        isShown = false
                        visibility.value = false
                    }
                }
            }
        }
    }
}