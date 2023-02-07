package example.imageviewer.view

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import example.imageviewer.Dependencies
import example.imageviewer.model.*
import example.imageviewer.model.State
import example.imageviewer.style.*
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.orEmpty
import org.jetbrains.compose.resources.rememberImageBitmap
import org.jetbrains.compose.resources.resource

@Composable
internal fun GalleryHeader() {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(10.dp).fillMaxWidth()
    ) {
        Text(
            "My Gallery",
            fontSize = 25.sp,
            color = MaterialTheme.colorScheme.onBackground,
            fontStyle = FontStyle.Italic
        )
    }
}

@Composable
internal fun MainScreen(state: MutableState<State>, dependencies: Dependencies) {
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        TopContent(state, dependencies)
        GalleryHeader()
        Spacer(modifier = Modifier.height(10.dp))
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
                Spacer(modifier = Modifier.height(10.dp))
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
        PreviewImage(
            state = state,
            getImage = { dependencies.imageRepository.loadContent(it.bigUrl) })
    }
}

val kotlinHorizontalGradientBrush = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF7F52FF),
        Color(0xFFC811E2),
        Color(0xFFE54857)
    )
)

@OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TitleBar(state: MutableState<State>, dependencies: Dependencies) {
    TopAppBar(
        modifier = Modifier.background(brush = kotlinHorizontalGradientBrush),
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = Color.White
        ),
        title = {
//            Text("UI Components")
            Row(Modifier.height(50.dp)) {
                Text(
                    dependencies.localization.appName,
                    modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
                    fontWeight = FontWeight.Bold
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
