package example.imageviewer

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import example.imageviewer.model.*
import example.imageviewer.view.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

enum class ExternalImageViewerEvent {
    Next,
    Previous,
}

@Composable
fun ImageViewerCommon(
    dependencies: Dependencies
) {
    CompositionLocalProvider(
        LocalNotification provides dependencies.notification,
        LocalImageProvider provides dependencies.imageProvider,
        LocalInternalEvents provides dependencies.externalEvents,
        LocalSharePicture provides dependencies.sharePicture,
    ) {
        ImageViewerWithProvidedDependencies(dependencies.pictures)
    }
}

private val pageNavConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(GalleryPage::class, GalleryPage.serializer())
            subclass(MemoryPage::class, MemoryPage.serializer())
            subclass(FullScreenPage::class, FullScreenPage.serializer())
            subclass(CameraPage::class, CameraPage.serializer())
        }
    }
}

private val pageTransitionSpec: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
    val from = initialState.key
    val to = targetState.key
    if (from is GalleryPage && to is MemoryPage) {
        fadeIn() togetherWith fadeOut(tween(durationMillis = 500, delayMillis = 500))
    } else {
        slideInHorizontally { w -> w } togetherWith slideOutHorizontally { w -> -w }
    }
}

private val pagePopTransitionSpec: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
    val from = initialState.key
    val to = targetState.key
    if (from is MemoryPage && to is GalleryPage) {
        fadeIn() togetherWith fadeOut(tween(delayMillis = 150))
    } else {
        slideInHorizontally { w -> -w } togetherWith slideOutHorizontally { w -> w }
    }
}

private val pagePredictivePopTransitionSpec: AnimatedContentTransitionScope<Scene<NavKey>>.(Int) -> ContentTransform = {
    val from = initialState.key
    val to = targetState.key
    if (from is MemoryPage && to is GalleryPage) {
        fadeIn() togetherWith fadeOut(tween(delayMillis = 150))
    } else {
        slideInHorizontally { w -> -w } togetherWith slideOutHorizontally { w -> w }
    }
}

@Composable
fun ImageViewerWithProvidedDependencies(
    pictures: SnapshotStateList<PictureData>
) {
    // rememberSaveable is required to properly handle Android configuration changes (such as device rotation)
    val selectedPictureIndex = rememberSaveable { mutableStateOf(0) }
    val backStack = rememberNavBackStack(pageNavConfiguration, GalleryPage)

    // Always keep at least one entry on the back stack.
    fun popBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    // Avoid pushing a duplicate entry when the same destination is triggered twice in a row
    // (e.g. a double-tap/double-click during the crossfade transition).
    fun navigateTo(page: Page) {
        if (backStack.lastOrNull() != page) {
            backStack.add(page)
        }
    }

    fun resetToGallery() {
        selectedPictureIndex.value = 0
        while (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { popBack() },
        transitionSpec = pageTransitionSpec,
        popTransitionSpec = pagePopTransitionSpec,
        predictivePopTransitionSpec = pagePredictivePopTransitionSpec,
        entryProvider = entryProvider {
            entry<GalleryPage> {
                GalleryScreen(
                    pictures = pictures,
                    selectedPictureIndex = selectedPictureIndex,
                    onClickPreviewPicture = { previewPictureIndex ->
                        navigateTo(MemoryPage(previewPictureIndex))
                    }
                ) {
                    navigateTo(CameraPage)
                }
            }

            entry<FullScreenPage> { page ->
                FullscreenImageScreen(
                    picture = pictures[page.pictureIndex],
                    back = {
                        popBack()
                    }
                )
            }

            entry<MemoryPage> { page ->
                MemoryScreen(
                    pictures = pictures,
                    memoryPage = page,
                    onSelectRelatedMemory = { pictureIndex ->
                        navigateTo(MemoryPage(pictureIndex))
                    },
                    onBack = { resetNavigation ->
                        if (resetNavigation) {
                            resetToGallery()
                        } else {
                            popBack()
                        }
                    },
                    onHeaderClick = { pictureIndex ->
                        navigateTo(FullScreenPage(pictureIndex))
                    },
                )
            }

            entry<CameraPage> {
                CameraScreen(
                    onBack = { resetSelectedPicture ->
                        if (resetSelectedPicture) {
                            selectedPictureIndex.value = 0
                        }
                        popBack()
                    },
                )
            }
        }
    )
}
