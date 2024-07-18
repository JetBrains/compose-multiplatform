package example.imageviewer.view

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// On the desktop, include a scrollbar
@Composable
actual fun GalleryLazyVerticalGrid(
    columns: GridCells,
    modifier: Modifier,
    verticalArrangement: Arrangement.Vertical,
    horizontalArrangement: Arrangement.Horizontal,
    content: LazyGridScope.() -> Unit
) {
    Box(
        modifier = modifier
    ) {
        val scrollState = rememberLazyGridState()
        val adapter = rememberScrollbarAdapter(scrollState)
        LazyVerticalGrid(
            columns = columns,
            modifier = Modifier.fillMaxSize(),
            state = scrollState,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            content = content
        )

        Box(
            modifier = Modifier.matchParentSize()
        ){
            VerticalScrollbar(
                adapter = adapter,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}