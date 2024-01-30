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
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import example.imageviewer.ExternalImageViewerEvent
import example.imageviewer.LocalImageProvider
import example.imageviewer.LocalInternalEvents
import example.imageviewer.LocalNotification
import example.imageviewer.icon.IconMenu
import example.imageviewer.icon.IconVisibility
import example.imageviewer.model.PictureData
import example.imageviewer.style.ImageviewerColors
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

enum class GalleryStyle {
    SQUARES,
    LIST
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    pictures: SnapshotStateList<PictureData>,
    selectedPictureIndex: MutableState<Int>,
    onClickPreviewPicture: (index: Int) -> Unit,
    onMakeNewMemory: () -> Unit
) {
    val imageProvider = LocalImageProvider.current
    val viewScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = selectedPictureIndex.value,
        initialPageOffsetFraction = 0f,
        pageCount = { pictures.size },
    )
    LaunchedEffect(pagerState) {
        // Subscribe to page changes
        snapshotFlow { pagerState.currentPage }.collect { page ->
            selectedPictureIndex.value = page
        }
    }

    fun nextImage() {
        viewScope.launch {
            pagerState.animateScrollToPage(
                (pagerState.currentPage + 1).mod(pictures.size)
            )
        }
    }

    fun previousImage() {
        viewScope.launch {
            pagerState.animateScrollToPage(
                (pagerState.currentPage - 1).mod(pictures.size)
            )
        }
    }

    fun selectPicture(index: Int) {
        viewScope.launch {
            pagerState.animateScrollToPage(
                index,
                animationSpec = tween(
                    easing = LinearOutSlowInEasing,
                    durationMillis = AnimationConstants.DefaultDurationMillis * 2
                )
            )
        }
    }

    var galleryStyle by remember { mutableStateOf(GalleryStyle.SQUARES) }
    val externalEvents = LocalInternalEvents.current
    LaunchedEffect(Unit) {
        externalEvents.collect {
            when (it) {
                ExternalImageViewerEvent.Next -> nextImage()
                ExternalImageViewerEvent.Previous -> previousImage()
                else -> {}
            }
        }
    }
    Column(modifier = Modifier.background(MaterialTheme.colors.background)) {
        Box {
            Box(
                Modifier.fillMaxWidth().height(393.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.fillMaxSize()
                        .clickable {
                            onClickPreviewPicture(pagerState.currentPage)
                        }
                ) {
                    HorizontalPager(state = pagerState) { index ->
                        val picture = pictures[index]
                        var image: ImageBitmap? by remember(picture) { mutableStateOf(null) }
                        LaunchedEffect(picture) {
                            image = imageProvider.getImage(picture)
                        }
                        if (image != null) {
                            Box(Modifier.fillMaxSize().animatePageChanges(pagerState, index)) {
                                Image(
                                    bitmap = image!!,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                MemoryTextOverlay(picture)
                            }
                        }
                    }
                }
            }
            TopLayout(
                alignLeftContent = {},
                alignRightContent = {
                    CircularButton(
                        imageVector = IconMenu,
                        modifier = Modifier.testTag("toggleGalleryStyleButton")
                    ) {
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
                    pagerState = pagerState,
                    onSelect = { selectPicture(it) },
                )
                GalleryStyle.LIST -> ListGalleryView(
                    pictures = pictures,
                    onSelect = { selectPicture(it) },
                    onFullScreen = { onClickPreviewPicture(it) },
                )
            }
            CircularButton(
                Icons.Filled.Add,
                modifier = Modifier.align(Alignment.BottomCenter).padding(36.dp),
                onClick = onMakeNewMemory,
            )
        }
    }
}

@Composable
expect fun GalleryLazyVerticalGrid(
    columns: GridCells,
    modifier: Modifier,
    verticalArrangement: Arrangement.Vertical,
    horizontalArrangement: Arrangement.Horizontal,
    content: LazyGridScope.() -> Unit
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SquaresGalleryView(
    images: List<PictureData>,
    pagerState: PagerState,
    onSelect: (index: Int) -> Unit,
) {
    GalleryLazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 130.dp),
        modifier = Modifier.padding(top = 4.dp).testTag("squaresGalleryView"),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        itemsIndexed(images) { index, picture ->
            SquareThumbnail(
                picture = picture,
                onClick = { onSelect(index) },
                isHighlighted = pagerState.targetPage == index
            )
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
    pictures: List<PictureData>,
    onSelect: (index: Int) -> Unit,
    onFullScreen: (index: Int) -> Unit,
) {
    val notification = LocalNotification.current
    ScrollableColumn(
        modifier = Modifier.fillMaxSize().testTag("listGalleryView")
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        for (p in pictures.withIndex()) {
            Thumbnail(
                picture = p.value,
                onClickSelect = {
                    onSelect(p.index)
                },
                onClickFullScreen = {
                    onFullScreen(p.index)
                },
                onClickInfo = {
                    notification.notifyImageData(p.value)
                },
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.animatePageChanges(pagerState: PagerState, index: Int) =
    graphicsLayer {
        val x = (pagerState.currentPage - index + pagerState.currentPageOffsetFraction) * 2
        alpha = 1f - (x.absoluteValue * 0.7f).coerceIn(0f, 0.7f)
        val scale = 1f - (x.absoluteValue * 0.4f).coerceIn(0f, 0.4f)
        scaleX = scale
        scaleY = scale
        rotationY = x * 15f
    }
