package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
        val scrollState = rememberScrollState()
        Column(Modifier.verticalScroll(scrollState)) {
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
                Spacer(modifier = Modifier.height(5.dp))
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
        Spacer(modifier = Modifier.height(5.dp))
    }
    Spacer(modifier = Modifier.height(5.dp))
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun TitleBar(state: MutableState<State>, dependencies: Dependencies) {
    TopAppBar(
        backgroundColor = DarkGreen,
        title = {
            Row(Modifier.height(50.dp)) {
                Text(
                    dependencies.localization.appName,
                    color = Foreground,
                    modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
                )
                Surface(
                    color = Transparent,
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
