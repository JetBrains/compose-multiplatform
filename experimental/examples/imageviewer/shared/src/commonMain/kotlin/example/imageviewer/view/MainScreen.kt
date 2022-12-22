package example.imageviewer.view

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import example.imageviewer.Dependencies
import example.imageviewer.model.*
import example.imageviewer.model.State
import example.imageviewer.style.*
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.orEmpty
import org.jetbrains.compose.resources.rememberImageBitmap
import org.jetbrains.compose.resources.resource

@Composable
internal fun MainScreen(state: MutableState<State>, dependencies: Dependencies) {
    Column {
        TopContent(state, dependencies)
        ScrollableColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            for (i in state.value.pictures.indices) {
                val picture = state.value.pictures[i]
                Miniature(
                    picture = picture,
                    image = state.value.miniatures[picture],
                    onClickSelect = {
                        state.setSelectedIndex(i)
                    },
                    onClickFullScreen = {
                        state.toFullscreen(i)
                    },
                    onClickInfo = {
                        dependencies.notification.notifyImageData(picture)
                    },
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
    if (!state.value.isContentReady) {
        LoadingScreen(dependencies.localization.loading)
    }
}

@Composable
private fun TopContent(state: MutableState<State>, dependencies: Dependencies) {
    TitleBar(state, dependencies)
    if (needShowPreview()) {
        PreviewImage(state = state, getImage = { dependencies.imageRepository.loadContent(it.bigUrl) })
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun TitleBar(state: MutableState<State>, dependencies: Dependencies) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        title = {
            Row(Modifier.height(50.dp)) {
                Text(
                    dependencies.localization.appName,
                    modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
                )
                Surface(
                    color = ImageviewerColors.Transparent,
                    modifier = Modifier.padding(end = 20.dp).align(Alignment.CenterVertically),
                    shape = CircleShape
                ) {
                    Image(
                        bitmap = resource("refresh.png").rememberImageBitmap().orEmpty(),
                        contentDescription = null,
                        modifier = Modifier.size(35.dp).clickable {
                            state.refresh(dependencies)
                        }
                    )
                }
            }
        })
}
