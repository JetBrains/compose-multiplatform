@file:OptIn(ExperimentalResourceApi::class)

package example.imageviewer.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import example.imageviewer.*
import example.imageviewer.icon.IconVisibility
import example.imageviewer.model.*
import example.imageviewer.painterResourceCached
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
    photoGallery: PhotoGallery,
    dependencies: Dependencies,
    onClickPreviewPicture: (PictureData) -> Unit,
    onMakeNewMemory: () -> Unit
) {
    val pictures = dependencies.pictures

    var selected: PictureData by remember { mutableStateOf(pictures.first()) }

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
                picture = selected, onClick = {
                    onClickPreviewPicture(selected)
                }
            )
            TopLayout(
                alignLeftContent = {},
                alignRightContent = {
                    CircularButton(painterResourceCached("list_view.png")) {
                        galleryPage.toggleGalleryStyle()
                    }
                },
            )
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            when (galleryPage.galleryStyle) {
                GalleryStyle.SQUARES -> SquaresGalleryView(
                    pictures,
                    selected,
                    onSelect = { selected = it },
                )

                GalleryStyle.LIST -> ListGalleryView(
                    pictures,
                    dependencies,
                    onSelect = { selected = it },
                    onFullScreen = {
                        onClickPreviewPicture(it)
                    }
                )
            }
            MakeNewMemoryMiniature(onMakeNewMemory)
        }
    }
    if (pictures.isEmpty()) {
        LoadingScreen(dependencies.localization.loading)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SquaresGalleryView(
    images: List<PictureData>,
    selectedImage: PictureData,
    onSelect: (PictureData) -> Unit,
) {
    LazyVerticalGrid(
        modifier = Modifier.padding(top = 4.dp),
        columns = GridCells.Adaptive(minSize = 130.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        itemsIndexed(images) { _, picture ->
            SquareThumbnail (
                picture = picture,
                onClick = { onSelect(picture) },
                isHighlighted = selectedImage === picture
            )
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun BoxScope.MakeNewMemoryMiniature(onClick: () -> Unit) {
    Column(modifier = Modifier.align(Alignment.BottomCenter)) {
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
                painter = painterResourceCached("plus.png"),
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
                    painter = painterResourceCached("eye.png"),
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
fun SquareThumbnail(
    picture: PictureData,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier.aspectRatio(1.0f).clickable(onClick = onClick)
    ) {
        Tooltip(picture.name) {
            ThumbnailImage(
                modifier = Modifier.fillMaxSize(),
                picture = picture,
            )
        }
        val tween = tween<Float>(
            durationMillis = AnimationConstants.DefaultDurationMillis * 3,
            delayMillis = 100,
            easing = LinearOutSlowInEasing,
        )
        AnimatedVisibility(isHighlighted, enter = fadeIn(tween), exit = fadeOut(tween)) {
            Box(Modifier.fillMaxSize().background(ImageviewerColors.uiLightBlack)) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
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
                    Icon(
                        imageVector = IconVisibility,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun ListGalleryView(
    pictures: SnapshotStateList<PictureData>,
    dependencies: Dependencies,
    onSelect: (PictureData) -> Unit,
    onFullScreen: (PictureData) -> Unit
) {
//    val notification = LocalNotification.current

    ScrollableColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        for ((idx, picWithThumb) in pictures.withIndex()) {
            Thumbnail(
                picWithThumb,
                onClickSelect = { onSelect(picWithThumb) },
                onClickFullScreen = { onFullScreen(picWithThumb) },
                onClickInfo = {
                    // dependencies.notification.
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
