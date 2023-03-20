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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import example.imageviewer.Dependencies
import example.imageviewer.ExternalImageViewerEvent
import example.imageviewer.model.CameraPage
import example.imageviewer.model.GalleryEntryWithMetadata
import example.imageviewer.model.GalleryId
import example.imageviewer.model.GalleryPage
import example.imageviewer.model.MemoryPage
import example.imageviewer.model.PhotoGallery
import example.imageviewer.model.bigUrl
import example.imageviewer.style.ImageviewerColors
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource


enum class GalleryStyle {
    SQUARES,
    LIST
}

internal class GalleryScreen(
    val page: GalleryPage,
    val photoGallery: PhotoGallery,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val dependencies = LocalDependencies.current
        GalleryScreen(
            page,
            photoGallery,
            dependencies,
            onClickPreviewPicture = { previewPictureId ->
                navigator.push(MemoryScreen(previewPictureId, photoGallery))
            },
            onMakeNewMemory = {
                navigator.push(CameraScreen)
            }
        )
    }

}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun GalleryScreen(
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
            CircularButton(
                image = painterResource("plus.png"),
                modifier = Modifier.align(Alignment.BottomCenter).padding(48.dp),
                onClick = onMakeNewMemory,
            )
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
        Spacer(Modifier.height(4.dp))
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
        Spacer(modifier = Modifier.height(10.dp))
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
