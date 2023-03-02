package example.imageviewer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import example.imageviewer.model.CameraPage
import example.imageviewer.model.FullScreenPage
import example.imageviewer.model.GalleryPage
import example.imageviewer.model.PhotoGallery
import example.imageviewer.model.MemoryPage
import example.imageviewer.model.Page
import example.imageviewer.model.bigUrl
import example.imageviewer.view.CameraScreen
import example.imageviewer.view.FullscreenImage
import example.imageviewer.view.GalleryScreen
import example.imageviewer.view.MemoryScreen
import example.imageviewer.view.NavigationStack
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
    val navigationStack = remember { NavigationStack<Page>(rootGalleryPage) }

    Surface(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(targetState = navigationStack.lastWithIndex(), transitionSpec = {
            val previousIdx = initialState.index
            val currentIdx = targetState.index
            val multiplier = if (previousIdx < currentIdx) 1 else -1
            slideInHorizontally { w -> multiplier * w } with
                    slideOutHorizontally { w -> multiplier * -1 * w }
        }) { (index, page) ->
            when (page) {
                is GalleryPage -> {
                    GalleryScreen(
                        page,
                        photoGallery,
                        dependencies,
                        onClickPreviewPicture = { previewPictureId ->
                            navigationStack.push(MemoryPage(previewPictureId))
                        },
                        onMakeNewMemory = {
                            navigationStack.push(CameraPage())
                        })
                }

                is FullScreenPage -> {
                    FullscreenImage(
                        galleryId = page.galleryId,
                        gallery = photoGallery,
                        getImage = { dependencies.imageRepository.loadContent(it.bigUrl) },
                        getFilter = { dependencies.getFilter(it) },
                        localization = dependencies.localization,
                        back = {
                            navigationStack.back()
                        }
                    )
                }

                is MemoryPage -> {
                    MemoryScreen(
                        page,
                        photoGallery,
                        onSelectRelatedMemory = { galleryId ->
                            navigationStack.push(MemoryPage(galleryId))
                        },
                        onBack = {
                            navigationStack.back()
                        },
                        onHeaderClick = { galleryId ->
                            navigationStack.push(FullScreenPage(galleryId))
                        })
                }

                is CameraPage -> {
                    CameraScreen(onBack = {
                        navigationStack.back()
                    })
                }
            }
        }
    }
}
