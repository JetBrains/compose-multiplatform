package example.imageviewer.view

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import example.imageviewer.LocalImageProvider
import example.imageviewer.LocalSharePicture
import example.imageviewer.filter.getPlatformContext
import example.imageviewer.icon.IconAutoFixHigh
import example.imageviewer.isShareFeatureSupported
import example.imageviewer.model.*
import example.imageviewer.shareIcon
import example.imageviewer.style.ImageviewerColors

@Composable
fun MemoryScreen(
    pictures: SnapshotStateList<PictureData>,
    memoryPage: MemoryPage,
    onSelectRelatedMemory: (pictureIndex: Int) -> Unit,
    onBack: (resetNavigation: Boolean) -> Unit,
    onHeaderClick: (index: Int) -> Unit,
) {
    val imageProvider = LocalImageProvider.current
    val sharePicture = LocalSharePicture.current
    var edit: Boolean by remember { mutableStateOf(false) }
    val picture = pictures.getOrNull(memoryPage.pictureIndex) ?: return
    var headerImage: ImageBitmap? by remember(picture) { mutableStateOf(null) }
    val platformContext = getPlatformContext()
    val verticalScrollEnableState = remember { mutableStateOf(true) }
    LaunchedEffect(picture) {
        headerImage = imageProvider.getImage(picture)
    }
    Box {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState, enabled = verticalScrollEnableState.value)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(393.dp)
                    .background(Color.White)
                    .graphicsLayer {
                        translationY = 0.5f * scrollState.value
                    },
                contentAlignment = Alignment.Center
            ) {
                headerImage?.let {
                    MemoryHeader(
                        it,
                        picture = picture,
                        onClick = { onHeaderClick(memoryPage.pictureIndex) }
                    )
                }
            }
            Box(modifier = Modifier.background(MaterialTheme.colors.background)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Headliner("Note")
                    Collapsible(picture.description, onEdit = { edit = true })
                    Headliner("Related memories")
                    val shuffledIndices = remember {
                        (pictures.indices.toList() - memoryPage.pictureIndex).shuffled().take(8)
                    }
                    LazyRow(
                        modifier = Modifier
                            .padding(10.dp, 0.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = shuffledIndices) { index ->
                            val relatedPicture = pictures.getOrNull(index)
                            if (relatedPicture != null) {
                                Box(Modifier.size(130.dp).clip(RoundedCornerShape(8.dp))) {
                                    SquareThumbnail(
                                        picture = relatedPicture,
                                        isHighlighted = false,
                                        onClick = { onSelectRelatedMemory(index) }
                                    )
                                }
                            }
                        }
                    }
                    Headliner("Place")
                    val locationShape = RoundedCornerShape(10.dp)
                    LocationVisualizer(
                        Modifier.padding(horizontal = 12.dp)
                            .clip(locationShape)
                            .border(1.dp, Color.Gray, locationShape)
                            .fillMaxWidth()
                            .height(200.dp),
                        gps = picture.gps,
                        title = picture.name,
                        parentScrollEnableState = verticalScrollEnableState,
                    )
                    Spacer(Modifier.height(50.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        IconWithText(Icons.Default.Delete, "Delete") {
                            imageProvider.delete(picture)
                            onBack(true)
                        }
                        IconWithText(Icons.Default.Edit, "Edit") {
                            edit = true
                        }
                        if (isShareFeatureSupported) {
                            IconWithText(shareIcon, "Share") {
                                sharePicture.share(platformContext, picture)
                            }
                        }
                    }
                    Spacer(Modifier.height(50.dp))
                }
            }
        }
        TopLayout(
            alignLeftContent = {
                BackButton {
                    onBack(false)
                }
            },
            alignRightContent = {},
        )
        if (edit) {
            EditMemoryDialog(picture.name, picture.description) { name, description ->
                imageProvider.edit(picture, name, description)
                edit = false
            }
        }
    }
}

@Composable
private fun IconWithText(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable {
            onClick()
        },
        horizontalArrangement = Arrangement.spacedBy(
            8.dp,
            Alignment.CenterHorizontally
        ),
        verticalAlignment = Alignment.Bottom
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
        )
        Text(
            text = text,
            textAlign = TextAlign.Left,
            color = ImageviewerColors.onBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun MemoryHeader(bitmap: ImageBitmap, picture: PictureData, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(modifier = Modifier.clickable(interactionSource, null, onClick = { onClick() })) {
        Image(
            bitmap,
            "Memory",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        MagicButtonOverlay(onClick)
        MemoryTextOverlay(picture)
    }
}

@Composable
fun BoxScope.MagicButtonOverlay(onClick: () -> Unit) {
    Column(
        modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
    ) {
        CircularButton(
            imageVector = IconAutoFixHigh,
            onClick = onClick,
        )
    }
}

@Composable
fun BoxScope.MemoryTextOverlay(picture: PictureData) {
    val shadowTextStyle = LocalTextStyle.current.copy(
        shadow = Shadow(
            color = Color.Black.copy(0.75f),
            offset = Offset(0f, 0f),
            blurRadius = 4f
        )
    )
    Column(
        modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 16.dp)
    ) {
        Text(
            text = picture.dateString,
            textAlign = TextAlign.Left,
            color = Color.White,
            fontSize = 20.sp,
            lineHeight = 22.sp,
            modifier = Modifier.fillMaxWidth(),
            fontWeight = FontWeight.SemiBold,
            style = shadowTextStyle
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = picture.name,
            textAlign = TextAlign.Left,
            color = Color.White,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Normal,
            style = shadowTextStyle
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Collapsible(s: String, onEdit: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    var isCollapsed by remember { mutableStateOf(true) }
    val text = if (isCollapsed) s.lines().first() + "... (see more)" else s
    Text(
        text,
        fontSize = 16.sp,
        modifier = Modifier
            .padding(10.dp, 0.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ImageviewerColors.noteBlockBackground)
            .padding(10.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ).combinedClickable(
                interactionSource = interactionSource, indication = null,
                onClick = {
                    isCollapsed = !isCollapsed
                },
                onLongClick = {
                    onEdit()
                }
            )
            .fillMaxWidth(),
    )
}

@Composable
fun Headliner(s: String) {
    Text(
        text = s,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        color = Color.Black,
        modifier = Modifier.padding(start = 12.dp, top = 32.dp, end = 12.dp, bottom = 16.dp)
    )
}
