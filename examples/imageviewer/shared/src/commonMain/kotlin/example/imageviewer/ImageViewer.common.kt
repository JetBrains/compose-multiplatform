package example.imageviewer

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import example.imageviewer.model.MemoryPage
import example.imageviewer.model.PictureData
import example.imageviewer.view.CameraScreen
import example.imageviewer.view.FullscreenImageScreen
import example.imageviewer.view.GalleryScreen
import example.imageviewer.view.MemoryScreen
import kotlinx.coroutines.delay

enum class ExternalImageViewerEvent {
    Next,
    Previous,
    ReturnBack,
}

@Composable
fun ImageViewerCommon(
    dependencies: Dependencies,
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ImageViewerWithProvidedDependencies(
    pictures: SnapshotStateList<PictureData>,
) {
    // rememberSaveable is required to properly handle Android configuration changes (such as device rotation)
    val selectedPictureIndex = rememberSaveable { mutableStateOf(0) }

    val externalEvents = LocalInternalEvents.current
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        externalEvents.collect {
            if (it == ExternalImageViewerEvent.ReturnBack) {
                navController.navigateUp()
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            println(navController.currentBackStack.value.toList().map {
                it.destination.route
            })
        }
    }

    NavHost(
        navController = navController,
        startDestination = Gallery,
        modifier = Modifier.fillMaxSize(),
        enterTransition = enterT,
        exitTransition = exitT,
        popExitTransition = popExitT,
        popEnterTransition = popEnterT
    ) {
        composable<Gallery> {
            GalleryScreen(
                pictures = pictures,
                selectedPictureIndex = selectedPictureIndex,
                onClickPreviewPicture = { previewPictureIndex ->
                    navController.navigate(Memory(previewPictureIndex))
                },
                onMakeNewMemory = {
                    navController.navigate(Camera)
                }
            )
        }
        composable<Memory> {
            val memoryRoute = it.toRoute<Memory>()
            MemoryScreen(
                pictures = pictures,
                memoryPage = MemoryPage(memoryRoute.imageIndex),
                onSelectRelatedMemory = { pictureIndex ->
                    navController.navigate(Memory(pictureIndex))
                },
                onBack = { resetNavigation ->
                    // TODO: There's an annoying problem here where navigating too quickly
                    // somehow breaks navigation. Unclear at the moment why.
                    val didNav = navController.navigateUp()
                    println("navigating up! $didNav")
                },
                onHeaderClick = { pictureIndex ->
                    navController.navigate(FullScreen(pictureIndex))
                },
            )
        }
        composable<FullScreen> {
            val fullScreenRoute = it.toRoute<FullScreen>()
            FullscreenImageScreen(
                picture = pictures[fullScreenRoute.imageIndex],
                back = {
                    navController.navigateUp()
                }
            )
        }
        composable<Camera> {
            CameraScreen(
                onBack = { resetSelectedPicture ->
                    navController.navigateUp()
                },
            )
        }
    }
}
