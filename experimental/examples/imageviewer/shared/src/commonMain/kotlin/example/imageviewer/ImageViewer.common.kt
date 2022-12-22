package example.imageviewer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import example.imageviewer.model.*
import example.imageviewer.style.Gray
import example.imageviewer.view.FullscreenImage
import example.imageviewer.view.MainScreen

@Composable
internal fun ImageViewerCommon(state: MutableState<State>, dependencies: Dependencies) {
    state.refresh(dependencies)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Gray
    ) {
        when (state.value.screen) {
            ScreenState.Miniatures -> {
                MainScreen(state, dependencies)
            }

            ScreenState.FullScreen -> {
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
}
