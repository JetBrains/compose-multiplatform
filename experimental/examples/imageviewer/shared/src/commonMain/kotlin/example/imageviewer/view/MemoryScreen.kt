package example.imageviewer.view

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onSelectRelatedMemory: (GalleryId) -> Unit,
    onBack: () -> Unit,
    onHeaderClick: (GalleryId) -> Unit
) {
    val pictures by photoGallery.galleryStateFlow.collectAsState()
    val picture = pictures.first { it.id == memoryPage.galleryId }
    Column {
        TopAppBar(
            modifier = Modifier.background(brush = ImageviewerColors.kotlinHorizontalGradientBrush),
            colors = TopAppBarDefaults.smallTopAppBarColors(
                containerColor = ImageviewerColors.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            ),
            title = {
                Text("")
            },
            navigationIcon = {
                Tooltip("Back") {
                    Image(
                        painterResource("back.png"),
                        contentDescription = null,
                        modifier = Modifier.size(38.dp)
                            .clip(CircleShape)
                            .clickable { onBack() }
                    )
                }
            },
        )
        val scrollState = memoryPage.scrollState
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color.White)
                    .graphicsLayer {
                        translationY = 0.5f * scrollState.value
                    },
                contentAlignment = Alignment.Center
            ) {
                MemoryHeader(picture.thumbnail, onClick = { onHeaderClick(memoryPage.galleryId) })
            }
            Box(modifier = Modifier.background(ImageviewerColors.kotlinHorizontalGradientBrush)) {
                Column {
                    Headliner("Where it happened")
                    LocationVisualizer()
                    Headliner("What happened")
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
                    Text(
                        "Delete this memory",
                        Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                    Spacer(Modifier.height(50.dp))
                }
            }
        }
    }
}

@Composable
private fun MemoryHeader(bitmap: ImageBitmap, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(modifier = Modifier.clickable(interactionSource, null, onClick = { onClick() })) {
        Image(
            bitmap,
            "Memory",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(modifier = Modifier.align(Alignment.Center)) {
            Text(
                "Your Memory",
                textAlign = TextAlign.Center,
                color = Color.White,
                fontSize = 50.sp,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(30.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .border(
                        width = 2.dp,
                        color = Color.Black,
                        shape = RoundedCornerShape(100.dp)
                    )
                    .clip(
                        RoundedCornerShape(100.dp)
                    )
                    .background(Color.Black.copy(alpha = 0.7f)).padding(10.dp)
            ) {
                Text(
                    "19th of April 2023",
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
            }
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
        modifier = Modifier
            .padding(10.dp, 0.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .padding(10.dp)
            .animateContentSize()
            .clickable(interactionSource = interctionSource, indication = null) {
                isCollapsed = !isCollapsed
            },
    )
}

@Composable
internal fun Headliner(s: String) {
    Text(
        text = s,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        color = Color.White,
        modifier = Modifier.padding(10.dp, 30.dp, 10.dp, 10.dp)
    )
}

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun LocationVisualizer() {
    Image(
        painterResource("dummy_map.png"),
        "Map",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxWidth().height(200.dp)
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
        LazyRow(modifier = Modifier.fillMaxSize()) {
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
    SquareMiniature(
        galleryEntry.thumbnail,
        false,
        onClick = { onSelectRelatedMemory(galleryEntry.id) })
}