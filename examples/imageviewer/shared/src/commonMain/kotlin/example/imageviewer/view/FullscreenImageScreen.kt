package example.imageviewer.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import example.imageviewer.LocalImageProvider
import example.imageviewer.filter.FilterType
import example.imageviewer.filter.getFilter
import example.imageviewer.filter.getPlatformContext
import example.imageviewer.model.PictureData
import example.imageviewer.model.ScalableState
import example.imageviewer.style.ImageviewerColors
import imageviewer.shared.generated.resources.Res
import imageviewer.shared.generated.resources.back
import org.jetbrains.compose.resources.stringResource

@Composable
fun FullscreenImageScreen(
    picture: PictureData,
    back: () -> Unit,
) {
    val imageProvider = LocalImageProvider.current
    val availableFilters = FilterType.values().toList()
    var selectedFilters by remember { mutableStateOf(emptySet<FilterType>()) }

    val originalImageState = remember(picture) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(picture) {
        originalImageState.value = imageProvider.getImage(picture)
    }

    val platformContext = getPlatformContext()
    val originalImage = originalImageState.value
    val imageWithFilter = remember(originalImage, selectedFilters) {
        if (originalImage != null) {
            var result: ImageBitmap = originalImage
            for (filter in selectedFilters.map { getFilter(it) }) {
                result = filter.invoke(result, platformContext)
            }
            result
        } else {
            null
        }
    }
    Box(Modifier.fillMaxSize().background(color = ImageviewerColors.fullScreenImageBackground)) {
        if (imageWithFilter != null) {
            val scalableState = remember { ScalableState() }

            ScalableImage(
                scalableState,
                imageWithFilter,
                modifier = Modifier.fillMaxSize().clipToBounds(),
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
                )
                ZoomControllerView(Modifier, scalableState)
            }
        }

        TopLayout(
            alignLeftContent = {
                Tooltip(stringResource(Res.string.back)) {
                    BackButton(back)
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
) {
    val platformContext = getPlatformContext()
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
                    filter = remember { { getFilter(type).invoke(it, platformContext) } }
                )
            }
        }
    }
}
