package example.imageviewer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
internal fun ImageViewerCommon(dependencies: Dependencies) {
    val content = ContentState(
        getFilter = {
            dependencies.getFilter(it)
        },
        state = remember { mutableStateOf(ContentStateData()) },
        notification = dependencies.notification,
        repository = dependencies.imageRepository,
        localization = dependencies.localization,
    ).apply {
        initData()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Gray
    ) {
        when (AppState.screenState()) {
            ScreenType.MainScreen -> {
                MainScreen(content)
            }
            ScreenType.FullscreenImage -> {
                FullscreenImage(content)
            }
        }
    }
}
