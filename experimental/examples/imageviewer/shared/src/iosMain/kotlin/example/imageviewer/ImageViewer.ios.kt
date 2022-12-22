package example.imageviewer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.core.BitmapFilter
import example.imageviewer.core.FilterType
import example.imageviewer.model.ContentRepository
import example.imageviewer.model.State
import example.imageviewer.model.adapter
import example.imageviewer.model.createNetworkRepository
import example.imageviewer.style.Gray
import example.imageviewer.view.Toast
import example.imageviewer.view.ToastState
import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Composable
internal fun ImageViewerIos() {
    val toastState = remember { mutableStateOf<ToastState>(ToastState.Hidden) }
    val state = remember { mutableStateOf(State()) }
    val ioScope: CoroutineScope = rememberCoroutineScope { Dispatchers.Default }
    val dependencies = remember(ioScope) { getDependencies(ioScope, toastState) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Gray
    ) {
        ImageViewerCommon(
            state = state,
            dependencies = dependencies
        )
        Toast(toastState)
    }
}

class StubFilter : BitmapFilter {
    override fun apply(bitmap: ImageBitmap): ImageBitmap {
        return bitmap
    }
}

private fun getDependencies(ioScope: CoroutineScope, toastState: MutableState<ToastState>) = object : Dependencies {
    override val ioScope: CoroutineScope = ioScope
    override fun getFilter(type: FilterType): BitmapFilter = when (type) {
        FilterType.GrayScale -> StubFilter()
        FilterType.Pixel -> StubFilter()
        FilterType.Blur -> StubFilter()
    }

    override val localization: Localization = object : Localization {
        override val appName = "ImageViewer"
        override val loading = "Loading images..."
        override val repoEmpty = "Repository is empty."
        override val noInternet = "No internet access."
        override val repoInvalid = "List of images in current repository is invalid or empty."
        override val refreshUnavailable = "Cannot refresh images."
        override val loadImageUnavailable = "Cannot load full size image."
        override val lastImage = "This is last image."
        override val firstImage = "This is first image."
        override val picture = "Picture:"
        override val size = "Size:"
        override val pixels = "pixels."
        override val back = "Back"
    }

    override val httpClient: HttpClient = HttpClient(Darwin)

    override val imageRepository: ContentRepository<ImageBitmap> =
        createNetworkRepository(httpClient)
            .adapter { it.toImageBitmap() }

    override val notification: Notification = object : PopupNotification(localization) {
        override fun showPopUpMessage(text: String) {
            toastState.value = ToastState.Shown(text)
        }
    }
}
