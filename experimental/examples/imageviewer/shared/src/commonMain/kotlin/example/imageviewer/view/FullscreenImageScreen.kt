package example.imageviewer.view

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import example.imageviewer.ImageProvider
import example.imageviewer.Localization
import example.imageviewer.core.BitmapFilter
import example.imageviewer.core.FilterType
import example.imageviewer.model.*
import example.imageviewer.style.*
import org.jetbrains.compose.resources.*

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun FullscreenImageScreen(
    picture: PictureData,
    imageProvider: ImageProvider,
    getFilter: (FilterType) -> BitmapFilter,
    localization: Localization,
    back: () -> Unit,
) {
    val availableFilters = FilterType.values().toList()
    var selectedFilters by remember { mutableStateOf(emptySet<FilterType>()) }

    val originalImageState = remember(picture) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(picture) {
        originalImageState.value = imageProvider.getImage(picture)
    }

    val originalImage = originalImageState.value
    val imageWithFilter = remember(originalImage, selectedFilters) {
        if (originalImage != null) {
            var result: ImageBitmap = originalImage
            for (filter in selectedFilters.map { getFilter(it) }) {
                result = filter.apply(result)
            }
            result
        } else {
            null
        }
    }
    Box(Modifier.fillMaxSize().background(color = ImageviewerColors.fullScreenImageBackground)) {
        if (imageWithFilter != null) {
            val scalableState = remember { ScalableState() }
            scalableState.updateImageSize(imageWithFilter.width, imageWithFilter.height)
            val visiblePartOfImage: IntRect = scalableState.visiblePart
            Box(
                Modifier.fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        scalableState.changeBoxSize(coordinates.size)
                    }
                    .addUserInput(scalableState)
            ) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = BitmapPainter(
                        imageWithFilter,
                        srcOffset = visiblePartOfImage.topLeft,
                        srcSize = visiblePartOfImage.size
                    ),
                    contentDescription = null,
                )
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(ImageviewerColors.filterButtonsBackground)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FilterButtons(
                        picture = picture,
                        filters = availableFilters,
                        selectedFilters = selectedFilters,
                        onSelectFilter = {
                            if (it !in selectedFilters) {
                                selectedFilters += it
                            } else {
                                selectedFilters -= it
                            }
                        },
                        getFilter = getFilter,
                        imageProvider = imageProvider,
                    )
                    ZoomControllerView(Modifier, scalableState)
                }
            }
        } else {
            LoadingScreen()
        }

        TopLayout(
            alignLeftContent = {
                Tooltip(localization.back) {
                    CircularButton(
                        painterResource("arrowleft.png"),
                        onClick = { back() }
                    )
                }
            },
            alignRightContent = {},
        )
    }
}

@Composable
private fun FilterButtons(
    picture: PictureData,
    filters: List<FilterType>,
    selectedFilters: Set<FilterType>,
    onSelectFilter: (FilterType) -> Unit,
    getFilter: (FilterType) -> BitmapFilter,
    imageProvider: ImageProvider,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        for (type in filters) {
            Tooltip(type.toString()) {
                ThumbnailImage(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .border(
                            color = if (type in selectedFilters) Color.White else Color.Gray,
                            width = 3.dp,
                            shape = CircleShape
                        )
                        .clickable {
                            onSelectFilter(type)
                        },
                    picture = picture,
                    imageProvider = imageProvider,
                    filter = remember { { getFilter(type).apply(it) } }
                )
            }
        }
    }
}
