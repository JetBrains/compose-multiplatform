package example.imageviewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import example.imageviewer.model.GalleryState
import example.imageviewer.model.ScreenState
import example.imageviewer.model.bigUrl
import example.imageviewer.model.nextImage
import example.imageviewer.model.picture
import example.imageviewer.model.previousImage
import example.imageviewer.model.refresh
import example.imageviewer.view.FullscreenImage
import example.imageviewer.view.MainScreen
import kotlinx.coroutines.flow.Flow

enum class ExternalImageViewerEvent {
    Foward,
    Back
}

@Composable
internal fun ImageViewerCommon(
    dependencies: Dependencies,
    externalEvents: Flow<ExternalImageViewerEvent>? = null
) {
    val galleryState = remember { mutableStateOf(GalleryState()) }
    LaunchedEffect(Unit) {
        galleryState.refresh(dependencies)
    }
    LaunchedEffect(Unit) {
        externalEvents?.collect {
            when (it) {
                ExternalImageViewerEvent.Foward -> galleryState.nextImage()
                ExternalImageViewerEvent.Back -> galleryState.previousImage()
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            galleryState.value.screen == ScreenState.Miniatures,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            MainScreen(galleryState, dependencies)
        }

        AnimatedVisibility(
            galleryState.value.screen == ScreenState.FullScreen,
            enter = slideInHorizontally { -it },
            exit = slideOutHorizontally { -it }) {
            FullscreenImage(
                picture = galleryState.value.picture,
                getImage = { dependencies.imageRepository.loadContent(it.bigUrl) },
                getFilter = { dependencies.getFilter(it) },
                localization = dependencies.localization,
                back = {
                    galleryState.value = galleryState.value.copy(screen = ScreenState.Miniatures)
                },
                nextImage = { galleryState.nextImage() },
                previousImage = { galleryState.previousImage() },
            )
        }
    }
}
