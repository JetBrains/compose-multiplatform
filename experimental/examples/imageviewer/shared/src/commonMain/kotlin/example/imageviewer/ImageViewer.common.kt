package example.imageviewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import example.imageviewer.model.ScreenState
import example.imageviewer.model.State
import example.imageviewer.model.bigUrl
import example.imageviewer.model.nextImage
import example.imageviewer.model.picture
import example.imageviewer.model.previousImage
import example.imageviewer.model.refresh
import example.imageviewer.view.FullscreenImage
import example.imageviewer.view.MainScreen

@Composable
internal fun ImageViewerCommon(state: MutableState<State>, dependencies: Dependencies) {
    state.refresh(dependencies)

    Surface(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            state.value.screen == ScreenState.Miniatures,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            MainScreen(state, dependencies)
        }

        AnimatedVisibility(
            state.value.screen == ScreenState.FullScreen,
            enter = slideInHorizontally { -it },
            exit = slideOutHorizontally { -it }) {
            FullscreenImage(
                picture = state.value.picture,
                getImage = { dependencies.imageRepository.loadContent(it.bigUrl) },
                getFilter = { dependencies.getFilter(it) },
                localization = dependencies.localization,
                back = { state.value = state.value.copy(screen = ScreenState.Miniatures) },
                nextImage = { state.nextImage() },
                previousImage = { state.previousImage() },
            )
        }
    }
}
