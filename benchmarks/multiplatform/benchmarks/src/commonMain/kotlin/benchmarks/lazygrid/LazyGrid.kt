package benchmarks.lazygrid

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlin.coroutines.suspendCoroutine

@Composable
fun LazyGrid(smoothScroll: Boolean = false, withLaunchedEffectInItem: Boolean = false) {
    val itemCount = 12000
    val entries = remember {List(itemCount) { Entry("$it") }}
    val state = rememberLazyGridState()

    MaterialTheme {
        Column {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "IamLazy" },
                state = state
            ) {
                items(entries) {
                    ListCell(it, withLaunchedEffectInItem)
                }
            }
        }

    }

    var curItem by remember { mutableStateOf(0) }
    var direct by remember { mutableStateOf(true) }
    if (smoothScroll) {
        LaunchedEffect(Unit) {
            while (isActive) {
                withFrameMillis { }
                curItem = state.firstVisibleItemIndex
                if (curItem == 0) direct = true
                if (curItem > itemCount - 100) direct = false
                state.scrollBy(if (direct) 55f else -55f)
            }
        }
    } else {
        LaunchedEffect(Unit) {
            while(isActive) {
                withFrameMillis {}
                curItem += if (direct) 50 else -50
                if (curItem >= itemCount) {
                    direct = false
                    curItem = itemCount - 1
                } else if (curItem <= 0) {
                    direct = true
                    curItem = 0
                }
                state.scrollToItem(curItem)
            }
        }
    }
}

data class Entry(val contents: String)

@Composable
private fun ListCell(entry: Entry, withLaunchedEffect: Boolean = false) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = entry.contents,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(16.dp)
        )

        if (withLaunchedEffect) {
            LaunchedEffect(Unit) {
                // Never resumed to imitate some async task running in an item's scope
                suspendCoroutine {  }
            }
        }
    }
}