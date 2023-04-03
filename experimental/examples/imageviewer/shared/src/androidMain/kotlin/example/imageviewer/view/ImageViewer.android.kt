package example.imageviewer.view

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import example.imageviewer.*
import example.imageviewer.filter.PlatformContext
import example.imageviewer.model.PictureData
import example.imageviewer.storage.AndroidImageStorage
import example.imageviewer.style.ImageViewerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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
    override val imageStorage: AndroidImageStorage = AndroidImageStorage(pictures, ioScope, context)
    override val sharePicture: SharePicture = object : SharePicture {
        override fun share(context: PlatformContext, picture: PictureData) {
            ioScope.launch {
                val shareIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(
                        Intent.EXTRA_STREAM,
                        imageStorage.getUri(context.androidContext, picture)
                    )
                    type = "image/jpeg"
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                withContext(Dispatchers.Main) {
                    context.androidContext.startActivity(Intent.createChooser(shareIntent, null))
                }
            }
        }
    }
}
