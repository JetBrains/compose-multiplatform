@file:OptIn(ExperimentalResourceApi::class)

package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import example.imageviewer.*
import example.imageviewer.model.*
import example.imageviewer.style.ImageviewerColors
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

enum class GalleryStyle {
    SQUARES,
    LIST
}

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun GalleryScreen(
    pictures: SnapshotStateList<PictureData>,
    selectedPictureIndex: MutableState<Int>,
    onClickPreviewPicture: (PictureData) -> Unit,
    onMakeNewMemory: () -> Unit
) {
    fun nextImage() {
        selectedPictureIndex.value =
            (selectedPictureIndex.value + 1).mod(pictures.size)
    }

    fun previousImage() {
        selectedPictureIndex.value =
            (selectedPictureIndex.value - 1).mod(pictures.size)
    }

    fun selectPicture(picture: PictureData) {
        selectedPictureIndex.value = pictures.indexOfFirst { it == picture }
    }

    val picture = pictures.getOrNull(selectedPictureIndex.value)

    var galleryStyle by remember { mutableStateOf(GalleryStyle.SQUARES) }
    val externalEvents = LocalInternalEvents.current
    LaunchedEffect(Unit) {
        externalEvents.collect {
            when (it) {
                ExternalImageViewerEvent.Foward -> nextImage()
                ExternalImageViewerEvent.Back -> previousImage()
                else -> {}
            }
        }
    }

    Column(modifier = Modifier.background(MaterialTheme.colors.background)) {
        Box {
            picture?.let {
                PreviewImage(
                    picture = it, onClick = {
                        onClickPreviewPicture(it)
                    }
                )
            }
            TopLayout(
                alignLeftContent = {},
                alignRightContent = {
                    CircularButton(painterResource("list_view.png")) {
                        galleryStyle = when (galleryStyle) {
                            GalleryStyle.SQUARES -> GalleryStyle.LIST
                            GalleryStyle.LIST -> GalleryStyle.SQUARES
                        }
                    }
                },
            )
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            when (galleryStyle) {
                GalleryStyle.SQUARES -> SquaresGalleryView(
                    images = pictures,
                    selectedImage = picture,
                    onSelect = { selectPicture(it) },
                )

                GalleryStyle.LIST -> ListGalleryView(
                    pictures = pictures,
                    onSelect = { selectPicture(it) },
                    onFullScreen = { onClickPreviewPicture(it) },
                )
            }
            CircularButton(
                image = painterResource("plus.png"),
                modifier = Modifier.align(Alignment.BottomCenter).padding(36.dp),
                onClick = onMakeNewMemory,
            )
        }
    }
}

@Composable
private fun SquaresGalleryView(
    images: List<PictureData>,
    selectedImage: PictureData?,
    onSelect: (PictureData) -> Unit,
) {
    LazyVerticalGrid(
        modifier = Modifier.padding(top = 4.dp),
        columns = GridCells.Adaptive(minSize = 130.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        itemsIndexed(images) { idx, picture ->
            val isSelected = picture == selectedImage
            SquareThumbnail(
                picture = picture,
                onClick = { onSelect(picture) },
                isHighlighted = isSelected
            )
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun SquareThumbnail(
    picture: PictureData,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier.aspectRatio(1.0f).clickable(onClick = onClick),
        contentAlignment = Alignment.BottomEnd
    ) {
        Tooltip(picture.name) {
            ThumbnailImage(
                modifier = Modifier.fillMaxSize(),
                picture = picture,
            )
        }
        if (isHighlighted) {
            Box(Modifier.fillMaxSize().background(ImageviewerColors.uiLightBlack))
            Box(
                Modifier
                    .padding(end = 4.dp, bottom = 4.dp)
                    .clip(CircleShape)
                    .width(32.dp)
                    .background(ImageviewerColors.uiLightBlack)
                    .aspectRatio(1.0f)
                    .clickable {
                        onClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource("eye.png"),
                    contentDescription = null,
                    modifier = Modifier
                        .width(17.dp)
                        .height(17.dp),
                )
            }
        }
    }
}

@Composable
private fun ListGalleryView(
    pictures: List<PictureData>,
    onSelect: (PictureData) -> Unit,
    onFullScreen: (PictureData) -> Unit,
) {
    val notification = LocalNotification.current
    ScrollableColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        for (p in pictures.withIndex()) {
            Thumbnail(
                picture = p.value,
                onClickSelect = {
                    onSelect(p.value)
                },
                onClickFullScreen = {
                    onFullScreen(p.value)
                },
                onClickInfo = {
                    notification.notifyImageData(p.value)
                },
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
