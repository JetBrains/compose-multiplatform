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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import example.imageviewer.model.CameraPage
import example.imageviewer.model.FullScreenPage
import example.imageviewer.model.GalleryState
import example.imageviewer.model.MemoryPage
import example.imageviewer.model.Page
import example.imageviewer.model.bigUrl
import example.imageviewer.view.CameraScreen
import example.imageviewer.view.FullscreenImage
import example.imageviewer.view.MainScreen
import example.imageviewer.view.MemoryView
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
    val galleryState = remember { GalleryState() }
    val navigationStack = remember { NavigationStack<Page>(listOf(galleryState)) }

    LaunchedEffect(Unit) {
        galleryState.refresh(dependencies)
    }
    LaunchedEffect(Unit) {
        externalEvents.collect {
            when (it) {
                ExternalImageViewerEvent.Foward -> galleryState.nextImage()
                ExternalImageViewerEvent.Back -> galleryState.previousImage()
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(targetState = navigationStack.lastWithIndex(), transitionSpec = {
            val previousIdx = initialState.index
            val currentIdx = targetState.index
            val multiplier = if (previousIdx < currentIdx) 1 else -1
            slideInHorizontally { w -> multiplier * w } with
                    slideOutHorizontally { w -> multiplier * -1 * w }
        }) { (index, targetState) ->
            when (targetState) {
                is GalleryState -> {
                    MainScreen(targetState, dependencies, onClickPreviewPicture = { fullScreenPicture ->
                        val idx = targetState.picturesWithThumbnail.indexOfFirst { it.picture == fullScreenPicture }
                        navigationStack.push(MemoryPage(idx))
                    }, onMakeNewMemory = {
                        navigationStack.push(CameraPage())
                    })
                }

                is FullScreenPage -> {
                    FullscreenImage(
                        picture = targetState.picture,
                        getImage = { dependencies.imageRepository.loadContent(it.bigUrl) },
                        getFilter = { dependencies.getFilter(it) },
                        localization = dependencies.localization,
                        back = {
                            navigationStack.back()
                        },
                        nextImage = { /*fullGalleryScreenState.nextImage()*/ },
                        previousImage = { /*fullGalleryScreenState.previousImage()*/ },
                    )
                }

                is MemoryPage -> {
                    MemoryView(targetState, galleryState.picturesWithThumbnail, onSelectRelatedMemory = {
                        navigationStack.push(MemoryPage(it))
                    }, onBack = {
                        navigationStack.back()
                    }, onHeaderClick = {
                        navigationStack.push(FullScreenPage(galleryState.picturesWithThumbnail[it].picture))
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
