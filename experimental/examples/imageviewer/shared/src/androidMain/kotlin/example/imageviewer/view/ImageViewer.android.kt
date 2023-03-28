package example.imageviewer.view

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import example.imageviewer.*
import example.imageviewer.shared.R
import example.imageviewer.style.ImageViewerTheme
import example.imageviewer.utils.ioDispatcher
import kotlinx.coroutines.CoroutineScope

@Composable
fun ImageViewerAndroid() {
    val context: Context = LocalContext.current
    val ioScope = rememberCoroutineScope { ioDispatcher }
    val dependencies = remember(context, ioScope) { getDependencies(context, ioScope) }
    ImageViewerTheme {
        ImageViewerCommon(dependencies)
    }
}

private fun getDependencies(context: Context, ioScope: CoroutineScope) = object : Dependencies() {
    override val notification: Notification = object : PopupNotification(localization) {
        override fun showPopUpMessage(text: String) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
    override val imageStorage: ImageStorage = AndroidImageStorage(pictures, ioScope)
}
