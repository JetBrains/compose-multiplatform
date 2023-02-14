package example.imageviewer.view

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val availableFilters = FilterType.values().toList()
    var selectedFilters by remember { mutableStateOf(emptySet<FilterType>()) }

    val originalImageState = remember(picture) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(picture) {
        if (picture != null) {
            originalImageState.value = getImage(picture)
        }
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
    Box(Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.background)) {
        Column {
            FullscreenImageBar(
                localization,
                picture?.name,
                back,
                availableFilters,
                selectedFilters,
                onSelectFilter = {
                    if (it !in selectedFilters) {
                        selectedFilters += it
                    } else {
                        selectedFilters -= it
                    }
                })
            if (imageWithFilter != null) {
                val imageSize = IntSize(imageWithFilter.width, imageWithFilter.height)
                val scalableState = remember(imageSize) { ScalableState(imageSize) }
                val visiblePartOfImage: IntRect = scalableState.visiblePart
                Slider(
                    modifier = Modifier.fillMaxWidth(),
                    value = scalableState.scale,
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

        FloatingActionButton(
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
            containerColor = ImageviewerColors.KotlinGradient0,
            onClick = previousImage
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowLeft,
                contentDescription = "Previous",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        FloatingActionButton(
            modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
            containerColor = ImageviewerColors.KotlinGradient0,
            onClick = nextImage
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = "Next",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
private fun FullscreenImageBar(
    localization: Localization,
    pictureName: String?,
    onBack: () -> Unit,
    filters: List<FilterType>,
    selectedFilters: Set<FilterType>,
    onSelectFilter: (FilterType) -> Unit
) {
    TopAppBar(
        modifier = Modifier.background(brush = ImageviewerColors.kotlinHorizontalGradientBrush),
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = ImageviewerColors.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        title = {
            Text("${localization.picture} ${pictureName ?: "Unknown"}")
        },
        navigationIcon = {
            Tooltip(localization.back) {
                Image(
                    resource("back.png").rememberImageBitmap().orEmpty(),
                    contentDescription = null,
                    modifier = Modifier.size(38.dp)
                        .clip(CircleShape)
                        .clickable { onBack() }
                )
            }
        },
        actions = {
            for (type in filters) {
                FilterButton(active = type in selectedFilters,
                    type,
                    onClick = {
                        onSelectFilter(type)
                    })
            }
        }
    )
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
