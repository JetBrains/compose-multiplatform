package example.imageviewer.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import example.imageviewer.Dependencies
import example.imageviewer.model.GalleryState
import example.imageviewer.model.Picture
import example.imageviewer.model.PictureWithThumbnail
import example.imageviewer.model.bigUrl
import example.imageviewer.style.ImageviewerColors
import example.imageviewer.style.ImageviewerColors.kotlinHorizontalGradientBrush
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

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

enum class GalleryStyle {
    SQUARES,
    LIST
}

fun GalleryStyle.toggled(): GalleryStyle {
    return if (this == GalleryStyle.SQUARES) GalleryStyle.LIST else GalleryStyle.SQUARES
}

@Composable
internal fun MainScreen(galleryScreenState: GalleryState, dependencies: Dependencies, onClickPreviewPicture: (Picture) -> Unit, onMakeNewMemory: () -> Unit) {
    var galleryStyle by remember { mutableStateOf(GalleryStyle.SQUARES) }
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        TitleBar(
            onRefresh = { galleryScreenState.refresh(dependencies) },
            onToggle = { galleryStyle = galleryStyle.toggled() },
            dependencies
        )
        if (needShowPreview()) {
            PreviewImage(
                getImage = { dependencies.imageRepository.loadContent(it.bigUrl) },
                picture = galleryScreenState.picture, onClick = {
                    galleryScreenState.picture?.let(onClickPreviewPicture)
                })
        }
        when (galleryStyle) {
            GalleryStyle.SQUARES -> SquaresGalleryView(
                galleryScreenState.picturesWithThumbnail,
                galleryScreenState.picturesWithThumbnail.getOrNull(galleryScreenState.currentPictureIndex),
                onSelect = { galleryScreenState.selectPicture(it) },
                onMakeNewMemory
            )

            GalleryStyle.LIST -> ListGalleryView(
                galleryScreenState.picturesWithThumbnail,
                dependencies,
                onSelect = { galleryScreenState.selectPicture(it) },
                onFullScreen = { onClickPreviewPicture(it) }
            )
        }
    }
    if (!galleryScreenState.isContentReady) {
        LoadingScreen(dependencies.localization.loading)
    }
}

@Composable
private fun SquaresGalleryView(
    images: List<PictureWithThumbnail>,
    selectedImage: PictureWithThumbnail?,
    onSelect: (Picture) -> Unit,
    onMakeNewMemory: () -> Unit,
) {
    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 128.dp)) {
        item {
            MakeNewMemoryMiniature(onMakeNewMemory)
        }
        itemsIndexed(images) { idx, image ->
            val isSelected = image == selectedImage
            val (picture, bitmap) = image
            SquareMiniature(bitmap, onClick = { onSelect(picture) }, isHighlighted = isSelected)
        }
    }
}

@Composable
private fun MakeNewMemoryMiniature(onClick: () -> Unit) {
    Box(
        Modifier.aspectRatio(1.0f)
            .clickable {
                onClick()
            }, contentAlignment = Alignment.Center
    ) {
        Text(
            "+",
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            fontSize = 50.sp
        )
    }
}

@Composable
internal fun SquareMiniature(image: ImageBitmap, isHighlighted: Boolean, onClick: () -> Unit) {
    Image(
        bitmap = image,
        contentDescription = null,
        modifier = Modifier.aspectRatio(1.0f).clickable { onClick() }.then(
            if (isHighlighted) {
                Modifier.border(BorderStroke(5.dp, Color.White))
            } else Modifier
        ),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun ListGalleryView(
    pictures: List<PictureWithThumbnail>,
    dependencies: Dependencies,
    onSelect: (Picture) -> Unit,
    onFullScreen: (Picture) -> Unit
) {
    GalleryHeader()
    Spacer(modifier = Modifier.height(10.dp))
    ScrollableColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        for ((idx, picWithThumb) in pictures.withIndex()) {
            val (picture, miniature) = picWithThumb
            Miniature(
                picture = picture,
                image = miniature,
                onClickSelect = {
                    onSelect(picture)
                },
                onClickFullScreen = {
                    onFullScreen(picture)
                },
                onClickInfo = {
                    dependencies.notification.notifyImageData(picture)
                },
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TitleBar(onRefresh: () -> Unit, onToggle: () -> Unit, dependencies: Dependencies) {
    TopAppBar(
        modifier = Modifier.background(brush = kotlinHorizontalGradientBrush),
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = ImageviewerColors.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        title = {
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
                        painter = painterResource("list_view.png"),
                        contentDescription = null,
                        modifier = Modifier.size(35.dp).clickable {
                            onToggle()
                        }
                    )
                }
                Surface(
                    color = ImageviewerColors.Transparent,
                    modifier = Modifier.padding(end = 20.dp).align(Alignment.CenterVertically),
                    shape = CircleShape
                ) {
                    Image(
                        painter = painterResource("refresh.png"),
                        contentDescription = null,
                        modifier = Modifier.size(35.dp).clickable {
                            onRefresh()
                        }
                    )
                }
            }
        })
}
