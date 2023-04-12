import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.window.CanvasBasedWindow
import androidx.compose.ui.window.Window
import example.imageviewer.*
import example.imageviewer.ImageViewerCommon
import example.imageviewer.core.BitmapFilter
import example.imageviewer.core.FilterType
import example.imageviewer.model.*
import example.imageviewer.model.filtration.BlurFilter
import example.imageviewer.model.filtration.GrayScaleFilter
import example.imageviewer.model.filtration.PixelFilter
import example.imageviewer.style.ImageViewerTheme
import example.imageviewer.utils.ioDispatcher
import example.imageviewer.view.Toast
import example.imageviewer.view.ToastState
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.skiko.wasm.onWasmReady

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    onWasmReady {
        CanvasBasedWindow("ImageViewer") {
            ImageViewerWeb()
        }
    }
}

@Composable
internal fun ImageViewerWeb() {
    val toastState = remember { mutableStateOf<ToastState>(ToastState.Hidden) }
    val ioScope: CoroutineScope = rememberCoroutineScope { ioDispatcher }
    val dependencies = remember(ioScope) { getDependencies(ioScope, toastState) }

    ImageViewerTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            ImageViewerCommon(
                dependencies = dependencies
            )
            Toast(toastState)
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
fun getDependencies(ioScope: CoroutineScope, toastState: MutableState<ToastState>) = object : Dependencies {
    override val pictures: SnapshotStateList<PictureData> = mutableStateListOf(*resourcePictures)
    override val ioScope: CoroutineScope = ioScope
    override fun getFilter(type: FilterType): BitmapFilter = when (type) {
        FilterType.GrayScale -> GrayScaleFilter()
        FilterType.Pixel -> PixelFilter()
        FilterType.Blur -> BlurFilter()
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

    override val httpClient: WrappedHttpClient = object : WrappedHttpClient {
        private val ktorClient = HttpClient(JsClient())
        override suspend fun getAsBytes(urlString: String): ByteArray {
            return ktorClient.get(urlString).readBytes()
        }
    }

    override val imageRepository: ContentRepository<ImageBitmap> =
        createNetworkRepository(httpClient)
            .adapter { it.toImageBitmap() }

    override val notification: Notification = object : PopupNotification(localization) {
        override fun showPopUpMessage(text: String) {
            toastState.value = ToastState.Shown(text)
        }
    }
}