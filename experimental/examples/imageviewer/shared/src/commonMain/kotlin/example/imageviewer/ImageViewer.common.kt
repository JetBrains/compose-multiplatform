package example.imageviewer

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import example.imageviewer.model.*
import example.imageviewer.view.*
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
    CompositionLocalProvider(
        LocalizationLocal provides dependencies.localization,
        NotificationLocal provides dependencies.notification,
        ImageProviderLocal provides dependencies.imageProvider,
        ImageStorageLocal provides dependencies.imageStorage,
    ) {
        ImageViewerWithProvidedDependencies(dependencies.pictures, externalEvents)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun ImageViewerWithProvidedDependencies(
    pictures: SnapshotStateList<PictureData>,
    externalEvents: Flow<ExternalImageViewerEvent>
) {
    val rootGalleryPage = GalleryPage(pictures, externalEvents)
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
                        onBack = { navigationStack.back() },
                    )
                }
            }
        }
    }
}
