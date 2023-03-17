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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import example.imageviewer.model.*
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
    val rootGalleryPage = GalleryPage(externalEvents)
    val navigationStack = remember { NavigationStack<Page>(rootGalleryPage) }

    Surface(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(targetState = navigationStack.lastWithIndex(), transitionSpec = {
            val previousIdx = initialState.index
            val currentIdx = targetState.index
            val multiplier = if (previousIdx < currentIdx) 1 else -1
            if (initialState.value is GalleryPage && targetState.value is MemoryPage) {
                fadeIn() with fadeOut(tween(durationMillis = 500, 500))
            } else if (initialState.value is MemoryPage && targetState.value is GalleryPage) {
                fadeIn() with fadeOut(tween(delayMillis = 150))
            } else {
                slideInHorizontally { w -> multiplier * w } with
                        slideOutHorizontally { w -> multiplier * -1 * w }
            }
        }) { (index, page) ->
            when (page) {
                is GalleryPage -> {
                    GalleryScreen(
                        page,
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
                        galleryId = page.picture,
                        getImage = { dependencies.imageProvider.getImage(it) },
                        getFilter = { dependencies.getFilter(it) },
                        localization = dependencies.localization,
                        back = {
                            navigationStack.back()
                        }
                    )
                }

                is MemoryPage -> {
                    MemoryScreen(
                        memoryPage = page,
                        getImage = { dependencies.imageProvider.getImage(it) },
                        localization = dependencies.localization,
                        onSelectRelatedMemory = { galleryId ->
                            navigationStack.push(MemoryPage(galleryId))
                        },
                        onBack = {
                            navigationStack.back()
                        },
                        onHeaderClick = { galleryId ->
                            navigationStack.push(FullScreenPage(galleryId))
                        },
                        imageProvider = dependencies.imageProvider
                    )
                }

                is CameraPage -> {
                    CameraScreen(
                        localization = dependencies.localization,
                        onBack = { navigationStack.back() }
                    )
                }
            }
        }
    }
}
