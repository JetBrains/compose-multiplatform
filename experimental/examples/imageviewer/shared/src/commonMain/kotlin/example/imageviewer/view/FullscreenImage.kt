package example.imageviewer.view

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import example.imageviewer.LocalImageProvider
import example.imageviewer.Localization
import example.imageviewer.core.BitmapFilter
import example.imageviewer.core.FilterType
import example.imageviewer.model.*
import example.imageviewer.painterResourceCached
import example.imageviewer.style.*
import org.jetbrains.compose.resources.ExperimentalResourceApi

@Composable
internal fun FullscreenImage(
    picture: PictureData,
    getFilter: (FilterType) -> BitmapFilter,
    localization: Localization,
    back: () -> Unit,
) {
    val imageProvider = LocalImageProvider.current
    val availableFilters = FilterType.values().toList()
    var selectedFilters by remember { mutableStateOf(emptySet<FilterType>()) }

    val originalImageState = remember(picture) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(picture) {
        if (picture != null) {
            originalImageState.value = imageProvider.getImage(picture)
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
    Box(Modifier.fillMaxSize().background(color = ImageviewerColors.fullScreenImageBackground)) {
        Column {
            FullscreenImageBar(
                localization,
                picture.name,
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
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    val imageSize = IntSize(imageWithFilter.width, imageWithFilter.height)
                    val scalableState = remember(imageSize) { ScalableState(imageSize) }
                    val visiblePartOfImage: IntRect = scalableState.visiblePart
                    Column {
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
                    }
                    Box(
                        Modifier.clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(ImageviewerColors.fullScreenImageBackground).padding(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            FilterButtons(availableFilters, selectedFilters, {
                                if (it !in selectedFilters) {
                                    selectedFilters += it
                                } else {
                                    selectedFilters -= it
                                }
                            })
                        }
                    }
                }
            } else {
                LoadingScreen()
            }
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
    TopLayout(
        alignLeftContent = {
            Tooltip(localization.back) {
                CircularButton(
                    painterResourceCached("arrowleft.png"),
                    onClick = { onBack() }
                )
            }
        },
        alignRightContent = {},
    )
}

@Composable
private fun FilterButtons(
    filters: List<FilterType>,
    selectedFilters: Set<FilterType>,
    onSelectFilter: (FilterType) -> Unit
) {
    for (type in filters) {
        FilterButton(active = type in selectedFilters,
            type,
            onClick = {
                onSelectFilter(type)
            })
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
                Modifier.size(40.dp)
                    .hoverable(interactionSource)
                    .background(color = ImageviewerColors.buttonBackground(filterButtonHover))
                    .clickable { onClick() }
            )
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun getFilterImage(active: Boolean, type: FilterType): Painter {
    return when (type) {
        FilterType.GrayScale -> if (active) {
            painterResourceCached("grayscale_on.png")
        } else {
            painterResourceCached("grayscale_off.png")
        }

        FilterType.Pixel -> if (active) {
            painterResourceCached("pixel_on.png")
        } else {
            painterResourceCached("pixel_off.png")
        }

        FilterType.Blur -> if (active) {
            painterResourceCached("blur_on.png")
        } else {
            painterResourceCached("blur_off.png")
        }
    }
}
