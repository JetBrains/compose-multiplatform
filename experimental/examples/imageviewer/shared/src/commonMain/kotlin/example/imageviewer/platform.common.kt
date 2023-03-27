package example.imageviewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.*

expect fun Modifier.notchPadding(): Modifier


private val cache = mutableStateMapOf<String, Painter>()

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun painterResourceCached(res: String): Painter {
    return if (cache.containsKey(res)) {
        cache[res]!!
    } else {
        val rib = resource(res).rememberImageBitmap()
        if (rib !is LoadState.Success<ImageBitmap>) {
            BitmapPainter(rib.orEmpty())
        } else {
            cache[res] = BitmapPainter(rib.orEmpty())
            cache[res]!!
        }
    }
}
