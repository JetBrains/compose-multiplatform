package example.imageviewer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.core.BitmapFilter
import example.imageviewer.core.FilterType
import example.imageviewer.model.*
import example.imageviewer.style.Gray
import example.imageviewer.view.FullscreenImage
import example.imageviewer.view.MainScreen

interface Dependencies {
    fun getFilter(type: FilterType): BitmapFilter
    val localization: Localization
    val imageRepository: ContentRepository<NetworkRequest, ImageBitmap>
    val notification: Notification
}

@Composable
internal fun ImageViewerCommon(
    dependencies: Dependencies,
    state: MutableState<ContentStateData>
) {
    val content = ContentState(
        state = state,
        dependencies = dependencies,
    ).apply {
        initData()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Gray
    ) {
        when (state.value.screen) {
            ScreenState.Miniatures -> {
                MainScreen(content)
            }

            ScreenState.FullScreen -> {
                FullscreenImage(
                    picture = state.value.picture!!,
                    getImage = { dependencies.imageRepository.loadContent(NetworkRequest(it.bigUrl)) },
                    getFilter = { dependencies.getFilter(it) },
                    localization = dependencies.localization,
                    back = { state.value = state.value.copy(screen = ScreenState.Miniatures) },
                    nextImage = { content.state.nextImage() },
                    previousImage = { content.state.previousImage() },
                )
            }
        }
    }
}
