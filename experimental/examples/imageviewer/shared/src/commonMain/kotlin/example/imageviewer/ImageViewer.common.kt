package example.imageviewer

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import example.imageviewer.model.*
import example.imageviewer.view.*

enum class ExternalImageViewerEvent {
    Foward,
    Back,
    Escape,
}

@Composable
internal fun ImageViewerCommon(
    dependencies: Dependencies
) {
    CompositionLocalProvider(
        LocalLocalization provides dependencies.localization,
        LocalNotification provides dependencies.notification,
        LocalImageProvider provides dependencies.imageProvider,
        LocalInternalEvents provides dependencies.externalEvents,
        LocalSharePicture provides dependencies.sharePicture,
    ) {
        ImageViewerWithProvidedDependencies(dependencies.pictures)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun ImageViewerWithProvidedDependencies(
    pictures: SnapshotStateList<PictureData>
) {
    val selectedPictureIndex: MutableState<Int> = mutableStateOf(0)
    val navigationStack = remember { NavigationStack<Page>(GalleryPage()) }
    val externalEvents = LocalInternalEvents.current
    LaunchedEffect(Unit) {
        externalEvents.collect {
            if (it == ExternalImageViewerEvent.Escape) {
                navigationStack.back()
            }
        }
    }

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
                    pictures = pictures,
                    selectedPictureIndex = selectedPictureIndex,
                    onClickPreviewPicture = { previewPictureId ->
                        navigationStack.push(MemoryPage(mutableStateOf(previewPictureId)))
                    }
                ) {
                    navigationStack.push(CameraPage())
                }
            }

            is FullScreenPage -> {
                FullscreenImageScreen(
                    picture = page.picture,
                    back = {
                        navigationStack.back()
                    }
                )
            }

            is MemoryPage -> {
                MemoryScreen(
                    pictures = pictures,
                    memoryPage = page,
                    onSelectRelatedMemory = { picture ->
                        navigationStack.push(MemoryPage(mutableStateOf(picture)))
                    },
                    onBack = {
                        navigationStack.back()
                    },
                    onHeaderClick = { galleryId ->
                        navigationStack.push(FullScreenPage(galleryId))
                    },
                )
            }

            is CameraPage -> {
                CameraScreen(
                    onBack = { resetSelectedPicture ->
                        if (resetSelectedPicture) {
                            selectedPictureIndex.value = 0
                        }
                        navigationStack.back()
                    },
                )
            }
        }
    }
}
