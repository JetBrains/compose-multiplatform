package example.imageviewer.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import example.imageviewer.*
import example.imageviewer.filter.PlatformContext
import example.imageviewer.model.PictureData
import example.imageviewer.style.ImageViewerTheme
import imageviewer.shared.generated.resources.Res
import imageviewer.shared.generated.resources.ic_imageviewer_round
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension
import java.awt.Toolkit

class ExternalNavigationEventBus {
    private val _events = MutableSharedFlow<ExternalImageViewerEvent>(
        replay = 0,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        extraBufferCapacity = 1,
    )
    val events = _events.asSharedFlow()

    fun produceEvent(event: ExternalImageViewerEvent) {
        _events.tryEmit(event)
    }
}

@Composable
fun ApplicationScope.ImageViewerDesktop() {
    val ioScope = rememberCoroutineScope { ioDispatcher }
    val toastState = remember { mutableStateOf<ToastState>(ToastState.Hidden) }
    val externalNavigationEventBus = remember { ExternalNavigationEventBus() }
    val dependencies = remember {
        getDependencies(toastState, ioScope, externalNavigationEventBus.events)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Image Viewer",
        state = WindowState(
            position = WindowPosition.Aligned(Alignment.Center),
            size = getPreferredWindowSize(720, 857)
        ),
        icon = painterResource(Res.drawable.ic_imageviewer_round),
        // https://github.com/JetBrains/compose-jb/issues/2741
        onKeyEvent = {
            if (it.type == KeyEventType.KeyUp) {
                when (it.key) {
                    Key.DirectionLeft -> externalNavigationEventBus.produceEvent(
                        ExternalImageViewerEvent.Previous
                    )

                    Key.DirectionRight -> externalNavigationEventBus.produceEvent(
                        ExternalImageViewerEvent.Next
                    )

                    Key.Escape -> externalNavigationEventBus.produceEvent(
                        ExternalImageViewerEvent.ReturnBack
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
                    dependencies = dependencies
                )
                Toast(toastState)
            }
        }
    }
}

private fun getDependencies(
    toastState: MutableState<ToastState>,
    ioScope: CoroutineScope,
    events: SharedFlow<ExternalImageViewerEvent>
) =
    object : Dependencies() {
        override val notification: Notification = object : PopupNotification(localization) {
            override fun showPopUpMessage(text: String) {
                toastState.value = ToastState.Shown(text)
            }
        }
        override val imageStorage: DesktopImageStorage = DesktopImageStorage(ioScope)
        override val sharePicture: SharePicture = object : SharePicture {
            override fun share(context: PlatformContext, picture: PictureData) {
                // On Desktop share feature not supported
            }
        }
        override val externalEvents = events
    }

private fun getPreferredWindowSize(desiredWidth: Int, desiredHeight: Int): DpSize {
    val screenSize: Dimension = Toolkit.getDefaultToolkit().screenSize
    val preferredWidth: Int = (screenSize.width * 0.8f).toInt()
    val preferredHeight: Int = (screenSize.height * 0.8f).toInt()
    val width: Int = if (desiredWidth < preferredWidth) desiredWidth else preferredWidth
    val height: Int = if (desiredHeight < preferredHeight) desiredHeight else preferredHeight
    return DpSize(width.dp, height.dp)
}
