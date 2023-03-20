package example.imageviewer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import example.imageviewer.model.CameraPage
import example.imageviewer.model.FullScreenPage
import example.imageviewer.model.GalleryPage
import example.imageviewer.model.MemoryPage
import example.imageviewer.model.Page
import example.imageviewer.model.PhotoGallery
import example.imageviewer.model.bigUrl
import example.imageviewer.view.CameraScreen
import example.imageviewer.view.FullscreenImage
import example.imageviewer.view.GalleryScreen
import example.imageviewer.view.LocalDependencies
import example.imageviewer.view.MemoryScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

enum class ExternalImageViewerEvent {
    Foward,
    Back
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun ImageViewerCommon(
    dependencies: Dependencies,
    externalEvents: Flow<ExternalImageViewerEvent> = emptyFlow()
) {
    val photoGallery = remember { PhotoGallery(dependencies) }
    val rootGalleryPage = GalleryPage(photoGallery, externalEvents)

    Surface(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalDependencies provides dependencies) {
            Navigator(GalleryScreen(rootGalleryPage, photoGallery)) { navigator ->
                SlideTransition(navigator)
            }
        }
    }
}
