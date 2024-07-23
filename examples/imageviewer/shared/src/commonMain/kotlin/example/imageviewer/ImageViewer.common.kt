package example.imageviewer

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
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
                if (navController.currentBackStack.value.size > 2) {
                    navController.popBackStack()
                }
            }
        }
    }

    val cbs by navController.currentBackStack.collectAsState()
    LaunchedEffect(cbs) {
        println(cbs.map { it.destination.route })
    }

    // TODO: The current version of compose-navigation has a bug that messes up the behavior of the animations. https://issuetracker.google.com/issues/355006319
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
                    if (navController.allowsNavigation(it)) {
                        navController.navigate(Memory(previewPictureIndex))
                    }
                },
                onMakeNewMemory = {
                    if (navController.allowsNavigation(it)) {
                        navController.navigate(Camera)
                    }
                }
            )
        }
        composable<Memory> {
            val memoryRoute = it.toRoute<Memory>()
            MemoryScreen(
                pictures = pictures,
                memoryPage = MemoryPage(memoryRoute.imageIndex),
                onSelectRelatedMemory = { pictureIndex ->
                    if (navController.allowsNavigation(it)) {
                        navController.navigate(Memory(pictureIndex))
                    }
                },
                onBack = { resetNavigation ->
                    if (navController.allowsNavigation(it)) {
                        navController.navigateUp()
                    }
                },
                onHeaderClick = { pictureIndex ->
                    if (navController.allowsNavigation(it)) {
                        navController.navigate(FullScreen(pictureIndex))
                    }
                },
            )
        }
        composable<FullScreen> {
            val fullScreenRoute = it.toRoute<FullScreen>()
            FullscreenImageScreen(
                picture = pictures[fullScreenRoute.imageIndex],
                back = {
                    if (navController.allowsNavigation(it)) {
                        navController.navigateUp()
                    }
                }
            )
        }
        composable<Camera> {
            CameraScreen(
                onBack = { resetSelectedPicture ->
                    if (navController.allowsNavigation(it)) {
                        navController.navigateUp()
                    }
                },
            )
        }
    }
}

fun NavController.allowsNavigation(backStackEntry: NavBackStackEntry): Boolean {
    if (this.currentBackStackEntry == backStackEntry) {
        return true
    } else {
        println("Away navigation not allowed for non-top back stack entry $backStackEntry")
        return false
    }
}