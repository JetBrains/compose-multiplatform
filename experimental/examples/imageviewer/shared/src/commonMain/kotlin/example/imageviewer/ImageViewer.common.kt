package example.imageviewer

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import example.imageviewer.model.*
import example.imageviewer.view.*

enum class ExternalImageViewerEvent {
    Foward,
    Back,
    Escape,
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun ImageViewerCommon(
    dependencies: Dependencies
) {
    CompositionLocalProvider(
        LocalLocalization provides dependencies.localization,
        LocalNotification provides dependencies.notification,
        LocalImageProvider provides dependencies.imageProvider,
        LocalImageStorage provides dependencies.imageStorage,
        LocalInternalEvents provides dependencies.externalEvents
    ) {
        ImageViewerWithProvidedDependencies(dependencies.pictures)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun ImageViewerWithProvidedDependencies(
    pictures: SnapshotStateList<PictureData>
) {
    val rootGalleryPage = GalleryPage(pictures)
    val navigationStack = remember { NavigationStack<Page>(rootGalleryPage) }

    val externalEvents = LocalInternalEvents.current
    LaunchedEffect(Unit) {
        externalEvents.collect {
            if (it == ExternalImageViewerEvent.Escape) {
                navigationStack.back()
            }
        }
    }

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
                        pictures = pictures,
                        galleryPage = page,
                        onClickPreviewPicture = { previewPictureId ->
                            navigationStack.push(MemoryPage(previewPictureId))
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
                        onSelectRelatedMemory = { galleryId ->
                            navigationStack.push(MemoryPage(galleryId))
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
                        onBack = { resetSelectedPhoto ->
                            if (resetSelectedPhoto) {
                                rootGalleryPage.resetSelectedPicture()
                            }
                            navigationStack.back()
                        },
                    )
                }
            }
        }
    }
}
