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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import example.imageviewer.Dependencies
import example.imageviewer.ExternalImageViewerEvent
import example.imageviewer.model.GalleryEntryWithMetadata
import example.imageviewer.model.GalleryId
import example.imageviewer.model.GalleryPage
import example.imageviewer.model.PhotoGallery
import example.imageviewer.model.bigUrl
import example.imageviewer.notchPadding
import example.imageviewer.style.ImageviewerColors
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource


enum class GalleryStyle {
    SQUARES,
    LIST
}

@Composable
internal fun GalleryScreen(
    galleryPage: GalleryPage,
    photoGallery: PhotoGallery,
    dependencies: Dependencies,
    onClickPreviewPicture: (GalleryId) -> Unit,
    onMakeNewMemory: () -> Unit
) {
    val pictures by photoGallery.galleryStateFlow.collectAsState()
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
                getImage = { dependencies.imageRepository.loadContent(it.bigUrl) },
                picture = galleryPage.picture, onClick = {
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
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            when (galleryPage.galleryStyle) {
                GalleryStyle.SQUARES -> SquaresGalleryView(
                    pictures,
                    galleryPage.pictureId,
                    onSelect = { galleryPage.selectPicture(it) },
                )

                GalleryStyle.LIST -> ListGalleryView(
                    pictures,
                    dependencies,
                    onSelect = { galleryPage.selectPicture(it) },
                    onFullScreen = { onClickPreviewPicture(it) }
                )
            }
            MakeNewMemoryMiniature(onMakeNewMemory)
        }
    }
    if (pictures.isEmpty()) {
        LoadingScreen(dependencies.localization.loading)
    }
}

@Composable
private fun SquaresGalleryView(
    images: List<GalleryEntryWithMetadata>,
    selectedImage: GalleryId?,
    onSelect: (GalleryId) -> Unit,
) {
    Column {
        Spacer(Modifier.height(1.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 130.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            itemsIndexed(images) { idx, image ->
                val isSelected = image.id == selectedImage
                val (picture, bitmap) = image
                SquareMiniature(
                    image.thumbnail,
                    onClick = { onSelect(picture) },
                    isHighlighted = isSelected
                )
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun MakeNewMemoryMiniature(onClick: () -> Unit) {
    Column {
        Box(
            Modifier
                .clip(CircleShape)
                .width(52.dp)
                .background(ImageviewerColors.uiLightBlack)
                .aspectRatio(1.0f)
                .clickable {
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource("plus.png"),
                contentDescription = null,
                modifier = Modifier
                    .width(18.dp)
                    .height(18.dp),
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun SquareMiniature(image: ImageBitmap, isHighlighted: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.aspectRatio(1.0f).clickable { onClick() },
        contentAlignment = Alignment.BottomEnd
    ) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().clickable { onClick() }.then(
                if (isHighlighted) {
                    Modifier//.border(BorderStroke(5.dp, Color.White))
                } else Modifier
            ),
            contentScale = ContentScale.Crop
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
    pictures: List<GalleryEntryWithMetadata>,
    dependencies: Dependencies,
    onSelect: (GalleryId) -> Unit,
    onFullScreen: (GalleryId) -> Unit
) {
    ScrollableColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        for ((idx, picWithThumb) in pictures.withIndex()) {
            val (galleryId, picture, miniature) = picWithThumb
            Miniature(
                picture = picture,
                image = miniature,
                onClickSelect = {
                    onSelect(galleryId)
                },
                onClickFullScreen = {
                    onFullScreen(galleryId)
                },
                onClickInfo = {
                    dependencies.notification.notifyImageData(picture)
                },
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
