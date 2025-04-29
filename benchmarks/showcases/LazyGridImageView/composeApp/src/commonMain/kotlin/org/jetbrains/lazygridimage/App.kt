package org.jetbrains.lazygridimage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lazygridimage.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalResourceApi::class)
@Composable
@Preview
fun App() {
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    // State for auto-scrolling
    var autoScroll by remember { mutableStateOf(false) }
    var scrollingDown by remember { mutableStateOf(true) }
    var currentIndex by remember { mutableStateOf(0) }

    val uris = remember {
        val availableResources: MutableList<String> = mutableListOf()
        for (index in 1..999) {
            try {
                val resUri = Res.getUri("files/downloaded_image${index.toString().padStart(3, '0')}.jpg")
                availableResources.add(resUri)
            } catch (e: Exception) {
                //ignore
            }
        }
        List(999) { index ->
            availableResources[index % availableResources.size]
        }
    }

    val numOfColumns = 3

    // Auto-scrolling effect
    LaunchedEffect(autoScroll) {
        if (autoScroll) {
            while (true) {
                delay(100)

                if (scrollingDown) {
                    if (currentIndex < uris.size - numOfColumns - 1) {
                        currentIndex += numOfColumns
                    } else {
                        scrollingDown = false
                    }
                } else {
                    if (currentIndex > 0) {
                        currentIndex -= numOfColumns
                    } else {
                        scrollingDown = true
                    }
                }

                coroutineScope.launch {
                    gridState.animateScrollToItem(
                        index = currentIndex,
                    )
                }
            }
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Checkbox for auto-scrolling
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Checkbox(
                        checked = autoScroll,
                        onCheckedChange = { autoScroll = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Auto Scroll")
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(numOfColumns),
                    contentPadding = PaddingValues(4.dp),
                    modifier = Modifier.fillMaxSize(),
                    state = gridState
                ) {
                    items(uris) { uri ->
                        ImageCard(
                            uri,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImageCard(
    uri: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f) // Square aspect ratio
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
