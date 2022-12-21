package example.imageviewer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.window.*
import example.imageviewer.*
import example.imageviewer.core.BitmapFilter
import example.imageviewer.core.FilterType
import example.imageviewer.model.*
import example.imageviewer.model.State
import example.imageviewer.style.Gray
import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

private val message: MutableState<String> = mutableStateOf("")
private val toastState: MutableState<Boolean> = mutableStateOf(false)

@Composable
internal fun ImageViewerIos() {
    val state = remember { mutableStateOf(State()) }
    val ioScope: CoroutineScope = rememberCoroutineScope { Dispatchers.Default }//todo?
    val dependencies = remember(ioScope) { getDependencies(ioScope) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Gray
    ) {
        ImageViewerCommon(
            state = state,
            dependencies = dependencies
        )
//        Toast(message.value, toastState)//todo
    }

}

class StubFilter():BitmapFilter {//todo
    override fun apply(bitmap: ImageBitmap): ImageBitmap {
        return bitmap
    }
}

private fun getDependencies(ioScope: CoroutineScope) = object : Dependencies {
    override val ioScope: CoroutineScope = ioScope
    override fun getFilter(type: FilterType): BitmapFilter = when (type) {
        FilterType.GrayScale -> StubFilter()
        FilterType.Pixel -> StubFilter()
        FilterType.Blur -> StubFilter()
    }

    override val localization: Localization = object : Localization {
        override val back: String get() = "todo"
        override val appName: String get() = "todo"
        override val loading: String get() = "todo"
        override val repoInvalid: String get() = "todo"
        override val repoEmpty: String get() = "todo"
        override val noInternet: String get() = "todo"
        override val loadImageUnavailable: String get() = "todo"
        override val lastImage: String get() = "todo"
        override val firstImage: String get() = "todo"
        override val picture: String get() = "todo"
        override val size: String get() = "todo"
        override val pixels: String get() = "todo"
        override val refreshUnavailable: String get() = "todo"
    }

    override val httpClient: HttpClient = HttpClient(Darwin)

    override val imageRepository: ContentRepository<ImageBitmap> =
        createRealRepository(httpClient)
            .adapter { it.toImageBitmap() }

    override val notification: Notification = object : PopupNotification(localization) {
        override fun showPopUpMessage(text: String) {
            message.value = text
            toastState.value = true
        }
    }
}
