package example.imageviewer.view

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import example.imageviewer.Localization
import example.imageviewer.model.GalleryEntryWithMetadata
import example.imageviewer.model.GalleryId
import example.imageviewer.model.MemoryPage
import example.imageviewer.model.PhotoGallery
import example.imageviewer.style.ImageviewerColors
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun MemoryScreen(
    memoryPage: MemoryPage,
    photoGallery: PhotoGallery,
    localization: Localization,
    onSelectRelatedMemory: (GalleryId) -> Unit,
    onBack: () -> Unit,
    onHeaderClick: (GalleryId) -> Unit
) {
    val pictures by photoGallery.galleryStateFlow.collectAsState()
    val picture = pictures.first { it.id == memoryPage.galleryId }
    Box {
        val scrollState = memoryPage.scrollState
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
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
                MemoryHeader(picture.thumbnail, onClick = { onHeaderClick(memoryPage.galleryId) })
            }
            Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                Column {
                    Headliner("Place")
                    LocationVisualizer(Modifier.padding(horizontal = 12.dp).clip(RoundedCornerShape(10.dp)))
                    Headliner("Note")
                    Collapsible(
                        """
                        I took a picture with my iPhone 14 at 17:45. The picture ended up being 3024 x 4032 pixels. ✨
                        
                        I took multiple additional photos of the same subject, but they turned out not quite as well, so I decided to keep this specific one as a memory.
                        
                        I might upload this picture to Unsplash at some point, since other people might also enjoy this picture. So it would make sense to not keep it to myself! 😄
                        """.trimIndent()
                    )
                    Headliner("Related memories")
                    RelatedMemoriesVisualizer(pictures, onSelectRelatedMemory)
                    Spacer(Modifier.height(50.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            8.dp,
                            Alignment.CenterHorizontally
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painterResource("trash.png"),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Delete Memory",
                            textAlign = TextAlign.Left,
                            color = ImageviewerColors.onBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    Spacer(Modifier.height(50.dp))
                }
            }
        }
        TopLayout(
            alignLeftContent = {
                Tooltip(localization.back) {
                    CircularButton(
                        painterResource("arrowleft.png"),
                        onClick = { onBack() }
                    )
                }
            },
            alignRightContent = {},
        )
    }
}

@Composable
private fun MemoryHeader(bitmap: ImageBitmap, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val shadowTextStyle = LocalTextStyle.current.copy(
        shadow = Shadow(
            color = Color.Black,
            offset = Offset(4f, 4f),
            blurRadius = 4f
        )
    )
    Box(modifier = Modifier.clickable(interactionSource, null, onClick = { onClick() })) {
        Image(
            bitmap,
            "Memory",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 16.dp)
        ) {
            Text(
                "Your Memory",
                textAlign = TextAlign.Left,
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.SemiBold,
                style = shadowTextStyle
            )
            Spacer(Modifier.height(5.dp))

            Text(
                "19th of April 2023",
                textAlign = TextAlign.Left,
                color = Color.White,
                fontWeight = FontWeight.Normal,
                style = shadowTextStyle
            )
        }
    }
}

@Composable
internal fun Collapsible(s: String) {
    val interctionSource = remember { MutableInteractionSource() }
    var isCollapsed by remember { mutableStateOf(true) }
    val text = if (isCollapsed) s.lines().first() + "... (see more)" else s
    Text(
        text,
        fontSize = 12.sp,
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
            )
            .clickable(interactionSource = interctionSource, indication = null) {
                isCollapsed = !isCollapsed
            },
    )
}

@Composable
internal fun Headliner(s: String) {
    Text(
        text = s,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        color = Color.Black,
        modifier = Modifier.padding(start = 12.dp, top = 32.dp, end = 12.dp, bottom = 16.dp)
    )
}

@Composable
internal fun RelatedMemoriesVisualizer(
    ps: List<GalleryEntryWithMetadata>,
    onSelectRelatedMemory: (GalleryId) -> Unit
) {
    Box(
        modifier = Modifier.padding(10.dp, 0.dp).clip(RoundedCornerShape(10.dp)).fillMaxWidth()
            .height(200.dp)
    ) {
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(ps) { idx, item ->
                RelatedMemory(idx, item, onSelectRelatedMemory)
            }
        }
    }
}

@Composable
internal fun RelatedMemory(
    index: Int,
    galleryEntry: GalleryEntryWithMetadata,
    onSelectRelatedMemory: (GalleryId) -> Unit
) {
    Box(Modifier.size(130.dp).clip(RoundedCornerShape(8.dp))) {
        SquareMiniature(
            galleryEntry.thumbnail,
            false,
            onClick = { onSelectRelatedMemory(galleryEntry.id) })
    }
}