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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import example.imageviewer.Dependencies
import example.imageviewer.ExternalImageViewerEvent
import example.imageviewer.ImageProvider
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
    galleryPage: GalleryPage,
    dependencies: Dependencies,
    onClickPreviewPicture: (PictureData) -> Unit,
    onMakeNewMemory: () -> Unit
) {
    LaunchedEffect(Unit) {
        galleryPage.externalEvents.collect {
            when (it) {
                ExternalImageViewerEvent.Foward -> galleryPage.nextImage()
                ExternalImageViewerEvent.Back -> galleryPage.previousImage()
            }
        }
    }

    Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        Box {
            PreviewImage(
                getImage = { dependencies.imageProvider.getImage(it) },
                picture = galleryPage.galleryEntry, onClick = {
                    galleryPage.pictureId?.let(onClickPreviewPicture)
                }
            )
            TopLayout(
                alignLeftContent = {},
                alignRightContent = {
                    CircularButton(painterResource("list_view.png")) {
                        galleryPage.toggleGalleryStyle()
                    }
                },
            )
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            when (galleryPage.galleryStyle) {
                GalleryStyle.SQUARES -> SquaresGalleryView(
                    images = globalPictures,
                    selectedImage = galleryPage.pictureId,
                    onSelect = { galleryPage.selectPicture(it) },
                    imageProvider = dependencies.imageProvider
                )

                GalleryStyle.LIST -> ListGalleryView(
                    pictures = globalPictures,
                    dependencies = dependencies,
                    onSelect = { galleryPage.selectPicture(it) },
                    onFullScreen = { onClickPreviewPicture(it) },
                )
            }
            CircularButton(
                image = painterResource("plus.png"),
                modifier = Modifier.align(Alignment.BottomCenter).padding(48.dp),
                onClick = onMakeNewMemory,
            )
        }
    }
    if (globalPictures.isEmpty()) {
        LoadingScreen(dependencies.localization.loading)
    }
}

@Composable
private fun SquaresGalleryView(
    images: List<PictureData>,
    selectedImage: PictureData?,
    onSelect: (PictureData) -> Unit,
    imageProvider: ImageProvider,
) {
    Column {
        Spacer(Modifier.height(4.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 130.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            itemsIndexed(images) { idx, picture ->
                val isSelected = picture == selectedImage
                SquareMiniature(
                    picture = picture,
                    imageProvider = imageProvider,
                    onClick = { onSelect(picture) },
                    isHighlighted = isSelected
                )
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun SquareMiniature(picture: PictureData, imageProvider: ImageProvider, isHighlighted: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.aspectRatio(1.0f).clickable(onClick = onClick),
        contentAlignment = Alignment.BottomEnd
    ) {
        MiniatureImage(
            modifier = Modifier.fillMaxSize(),
            picture = picture,
            imageProvider = imageProvider,
        )
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
    dependencies: Dependencies,
    onSelect: (PictureData) -> Unit,
    onFullScreen: (PictureData) -> Unit,
) {
    ScrollableColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        for (p in pictures.withIndex()) {
            Miniature(
                picture = p.value,
                onClickSelect = {
                    onSelect(p.value)
                },
                onClickFullScreen = {
                    onFullScreen(p.value)
                },
                onClickInfo = {
                    dependencies.notification.notifyImageData(p.value)
                },
                imageProvider = dependencies.imageProvider
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
