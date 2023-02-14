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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import example.imageviewer.model.GalleryScreenState
import example.imageviewer.model.ScreenState
import example.imageviewer.model.bigUrl
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
    val galleryScreenState = remember { GalleryScreenState() }

    LaunchedEffect(Unit) {
        galleryScreenState.refresh(dependencies)
    }
    LaunchedEffect(Unit) {
        externalEvents?.collect {
            when (it) {
                ExternalImageViewerEvent.Foward -> galleryScreenState.nextImage()
                ExternalImageViewerEvent.Back -> galleryScreenState.previousImage()
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            galleryScreenState.screen == ScreenState.Miniatures,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            MainScreen(galleryScreenState, dependencies)
        }

        AnimatedVisibility(
            galleryScreenState.screen == ScreenState.FullScreen,
            enter = slideInHorizontally { -it },
            exit = slideOutHorizontally { -it }) {
            FullscreenImage(
                picture = galleryScreenState.picture,
                getImage = { dependencies.imageRepository.loadContent(it.bigUrl) },
                getFilter = { dependencies.getFilter(it) },
                localization = dependencies.localization,
                back = {
                    galleryScreenState.screen = ScreenState.Miniatures
                },
                nextImage = { galleryScreenState.nextImage() },
                previousImage = { galleryScreenState.previousImage() },
            )
        }
    }
}
