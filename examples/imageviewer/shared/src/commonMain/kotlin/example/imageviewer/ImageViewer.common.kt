package example.imageviewer

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import example.imageviewer.model.*
import example.imageviewer.view.*

enum class ExternalImageViewerEvent {
    Next,
    Previous,
    ReturnBack,
}

@Composable
fun ImageViewerCommon(
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
fun ImageViewerWithProvidedDependencies(
    pictures: SnapshotStateList<PictureData>
) {
    // rememberSaveable is required to properly handle Android configuration changes (such as device rotation)
    val selectedPictureIndex = rememberSaveable { mutableStateOf(0) }
    val navigationStack = rememberSaveable(
        saver = listSaver<NavigationStack<Page>, Page>(
            restore = { NavigationStack(*it.toTypedArray()) },
            save = { it.stack },
        )
    ) {
        NavigationStack(GalleryPage())
    }

    val externalEvents = LocalInternalEvents.current
    LaunchedEffect(Unit) {
        externalEvents.collect {
            if (it == ExternalImageViewerEvent.ReturnBack) {
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
    }) { (_, page) ->
        when (page) {
            is GalleryPage -> {
                GalleryScreen(
                    pictures = pictures,
                    selectedPictureIndex = selectedPictureIndex,
                    onClickPreviewPicture = { previewPictureIndex ->
                        navigationStack.push(MemoryPage(previewPictureIndex))
                    }
                ) {
                    navigationStack.push(CameraPage())
                }
            }

            is FullScreenPage -> {
                FullscreenImageScreen(
                    picture = pictures[page.pictureIndex],
                    back = {
                        navigationStack.back()
                    }
                )
            }

            is MemoryPage -> {
                MemoryScreen(
                    pictures = pictures,
                    memoryPage = page,
                    onSelectRelatedMemory = { pictureIndex ->
                        navigationStack.push(MemoryPage(pictureIndex))
                    },
                    onBack = { resetNavigation ->
                        if (resetNavigation) {
                            selectedPictureIndex.value = 0
                            navigationStack.reset()
                        } else {
                            navigationStack.back()
                        }
                    },
                    onHeaderClick = { pictureIndex ->
                        navigationStack.push(FullScreenPage(pictureIndex))
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
