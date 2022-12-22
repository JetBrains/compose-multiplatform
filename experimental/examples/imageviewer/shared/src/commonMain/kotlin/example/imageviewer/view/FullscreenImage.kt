package example.imageviewer.view

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import example.imageviewer.Localization
import example.imageviewer.core.BitmapFilter
import example.imageviewer.core.FilterType
import example.imageviewer.model.*
import example.imageviewer.style.*
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.orEmpty
import org.jetbrains.compose.resources.rememberImageBitmap
import org.jetbrains.compose.resources.resource

@Composable
internal fun FullscreenImage(
    picture: Picture?,
    getImage: suspend (Picture) -> ImageBitmap,
    getFilter: (FilterType) -> BitmapFilter,
    localization: Localization,
    back: () -> Unit,
    nextImage: () -> Unit,
    previousImage: () -> Unit,
) {
    val filtersState = remember { mutableStateOf(emptySet<FilterType>()) }

    val originalImageState = remember(picture) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(picture) {
        if (picture != null) {
            originalImageState.value = getImage(picture)
        }
    }

    val originalImage = originalImageState.value
    val filters = filtersState.value
    val imageWithFilter = remember(originalImage, filters) {
        if (originalImage != null) {
            var result: ImageBitmap = originalImage
            for (filter in filters.map { getFilter(it) }) {
                result = filter.apply(result)
            }
            result
        } else {
            null
        }
    }

    Box(Modifier.fillMaxSize().background(color = MaterialTheme.colors.background)) {
        Column {
            Toolbar(picture?.name ?: "", filtersState, localization, back)
            if (imageWithFilter != null) {
                val imageSize = IntSize(imageWithFilter.width, imageWithFilter.height)
                val scalableState = remember(imageSize) { mutableStateOf(ScalableState(imageSize)) }
                val visiblePartOfImage: IntRect = scalableState.value.visiblePart
                Slider(
                    modifier = Modifier.fillMaxWidth(),
                    value = scalableState.value.scale,
                    valueRange = MIN_SCALE..MAX_SCALE,
                    onValueChange = { scalableState.setScale(it) },
                )
                Box(
                    modifier = Modifier.fillMaxSize()
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
                        contentDescription = null
                    )
                }
            } else {
                LoadingScreen()
            }
        }

        FloatingActionButton(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp), onClick = previousImage) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowLeft,
                contentDescription = "Previous",
                tint = MaterialTheme.colors.primary
            )
        }
        FloatingActionButton(modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp), onClick = nextImage) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = "Next",
                tint = MaterialTheme.colors.primary
            )
        }
    }

}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun Toolbar(
    title: String,
    filtersState: MutableState<Set<FilterType>>,
    localization: Localization,
    back: () -> Unit
) {
    val backButtonInteractionSource = remember { MutableInteractionSource() }
    val backButtonHover by backButtonInteractionSource.collectIsHoveredAsState()
    Surface(
//        color = MiniatureColor,
        modifier = Modifier.height(44.dp)
    ) {
        Row(modifier = Modifier.padding(end = 30.dp)) {
            Surface(
                color = Color.Transparent,
                modifier = Modifier.padding(start = 20.dp).align(Alignment.CenterVertically),
                shape = CircleShape
            ) {
                Tooltip(localization.back) {
                    Image(
                        resource("back.png").rememberImageBitmap().orEmpty(),
                        contentDescription = null,
                        modifier = Modifier.size(38.dp)
                            .hoverable(backButtonInteractionSource)
                            .background(color = ImageviewerColors.buttonBackground(backButtonHover))
                            .clickable { back() }
                    )
                }
            }
            Text(
                title,
                maxLines = 1,
                modifier = Modifier.padding(start = 30.dp).weight(1f)
                    .align(Alignment.CenterVertically),
                style = MaterialTheme.typography.body1
            )

            Surface(
                color = Color(255, 255, 255, 40),
                modifier = Modifier.size(154.dp, 38.dp)
                    .align(Alignment.CenterVertically),
                shape = CircleShape
            ) {
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    for (type in FilterType.values()) {
                        FilterButton(filtersState.value.contains(type), type, onClick = {
                            filtersState.value = if (filtersState.value.contains(type)) {
                                filtersState.value - type
                            } else {
                                filtersState.value + type
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterButton(
    active: Boolean,
    type: FilterType,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val filterButtonHover by interactionSource.collectIsHoveredAsState()
    Box(
        modifier = Modifier.background(color = ImageviewerColors.Transparent).clip(CircleShape)
    ) {
        Tooltip(type.toString()) {
            Image(
                getFilterImage(active, type = type),
                contentDescription = null,
                Modifier.size(38.dp)
                    .hoverable(interactionSource)
                    .background(color = ImageviewerColors.buttonBackground(filterButtonHover))
                    .clickable { onClick() }
            )
        }
    }
    Spacer(Modifier.width(20.dp))
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun getFilterImage(active: Boolean, type: FilterType): ImageBitmap {
    return when (type) {
        FilterType.GrayScale -> if (active) {
            resource("grayscale_on.png").rememberImageBitmap().orEmpty()
        } else {
            resource("grayscale_off.png").rememberImageBitmap().orEmpty()
        }

        FilterType.Pixel -> if (active) {
            resource("pixel_on.png").rememberImageBitmap().orEmpty()
        } else {
            resource("pixel_off.png").rememberImageBitmap().orEmpty()
        }

        FilterType.Blur -> if (active) {
            resource("blur_on.png").rememberImageBitmap().orEmpty()
        } else {
            resource("blur_off.png").rememberImageBitmap().orEmpty()
        }
    }
}
