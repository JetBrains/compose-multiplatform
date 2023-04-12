package example.imageviewer.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.*
import example.imageviewer.*
import example.imageviewer.Notification
import example.imageviewer.core.BitmapFilter
import example.imageviewer.core.FilterType
import example.imageviewer.model.*
import example.imageviewer.model.filtration.BlurFilter
import example.imageviewer.model.filtration.GrayScaleFilter
import example.imageviewer.model.filtration.PixelFilter
import example.imageviewer.style.ImageViewerTheme
import example.imageviewer.utils.decorateWithDiskCache
import example.imageviewer.utils.getPreferredWindowSize
import example.imageviewer.utils.ioDispatcher
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File

class ExternalNavigationEventBus {
    private val _events = MutableSharedFlow<ExternalImageViewerEvent>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_LATEST
    )
    val events = _events.asSharedFlow()

    fun produceEvent(event: ExternalImageViewerEvent) {
        _events.tryEmit(event)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ApplicationScope.ImageViewerDesktop() {
    val toastState = remember { mutableStateOf<ToastState>(ToastState.Hidden) }
    val ioScope: CoroutineScope = rememberCoroutineScope { ioDispatcher }
    val dependencies = remember(ioScope) { getDependencies(ioScope, toastState) }
    val externalNavigationEventBus = remember { ExternalNavigationEventBus() }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Image Viewer",
        state = WindowState(
            position = WindowPosition.Aligned(Alignment.Center),
            size = getPreferredWindowSize(720, 857)
        ),
        icon = painterResource("ic_imageviewer_round.png"),
        // https://github.com/JetBrains/compose-jb/issues/2741
        onKeyEvent = {
            if (it.type == KeyEventType.KeyUp) {
                when (it.key) {
                    Key.DirectionLeft -> externalNavigationEventBus.produceEvent(
                        ExternalImageViewerEvent.Back
                    )

                    Key.DirectionRight -> externalNavigationEventBus.produceEvent(
                        ExternalImageViewerEvent.Foward
                    )
                }
            }
            false
        }
    ) {
        ImageViewerTheme {
            Surface(
                modifier = Modifier.fillMaxSize()
            ) {
                ImageViewerCommon(
                    dependencies = dependencies,
                    externalEvents = externalNavigationEventBus.events
                )
                Toast(toastState)
            }
        }
    }
}

private fun getDependencies(ioScope: CoroutineScope, toastState: MutableState<ToastState>) =
    object : Dependencies {
        override val pictures: SnapshotStateList<PictureData> = mutableStateListOf(*resourcePictures)
        override val ioScope: CoroutineScope = ioScope
        override fun getFilter(type: FilterType): BitmapFilter = when (type) {
            FilterType.GrayScale -> GrayScaleFilter()
            FilterType.Pixel -> PixelFilter()
            FilterType.Blur -> BlurFilter()
        }

        override val localization: Localization = object : Localization {
            override val back: String get() = ResString.back
            override val appName: String get() = ResString.appName
            override val loading: String get() = ResString.loading
            override val repoInvalid: String get() = ResString.repoInvalid
            override val repoEmpty: String get() = ResString.repoEmpty
            override val noInternet: String get() = ResString.noInternet
            override val loadImageUnavailable: String get() = ResString.loadImageUnavailable
            override val lastImage: String get() = ResString.lastImage
            override val firstImage: String get() = ResString.firstImage
            override val picture: String get() = ResString.picture
            override val size: String get() = ResString.size
            override val pixels: String get() = ResString.pixels
            override val refreshUnavailable: String get() = ResString.refreshUnavailable
        }

        override val httpClient: WrappedHttpClient = object : WrappedHttpClient {
            val ktorClient = HttpClient(CIO)
            override suspend fun getAsBytes(urlString: String): ByteArray {
                return ktorClient.get(urlString).readBytes()
            }
        }

        val userHome: String? = System.getProperty("user.home")
        override val imageRepository: ContentRepository<ImageBitmap> =
            createNetworkRepository(httpClient)
                .run {
                    if (userHome != null) {
                        decorateWithDiskCache(
                            ioScope,
                            File(userHome).resolve("Pictures").resolve("imageviewer")
                        )
                    } else {
                        this
                    }
                }
                .adapter { it.toImageBitmap() }

        override val notification: Notification = object : PopupNotification(localization) {
            override fun showPopUpMessage(text: String) {
                toastState.value = ToastState.Shown(text)
            }
        }
    }
