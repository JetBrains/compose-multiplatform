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

    override val localization: Localization = object : Localization {
        override val back get() = context.getString(R.string.back)
        override val appName get() = context.getString(R.string.app_name)
        override val loading get() = context.getString(R.string.loading)
        override val repoInvalid get() = context.getString(R.string.repo_invalid)
        override val repoEmpty get() = context.getString(R.string.repo_empty)
        override val noInternet get() = context.getString(R.string.no_internet)
        override val loadImageUnavailable get() = context.getString(R.string.load_image_unavailable)
        override val lastImage get() = context.getString(R.string.last_image)
        override val firstImage get() = context.getString(R.string.first_image)
        override val picture get() = context.getString(R.string.picture)
        override val size get() = context.getString(R.string.size)
        override val pixels get() = context.getString(R.string.pixels)
        override val refreshUnavailable get() = context.getString(R.string.refresh_unavailable)
    }

    override val notification: Notification = object : PopupNotification(localization) {
        override fun showPopUpMessage(text: String) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
    override val imageStorage: ImageStorage = AndroidImageStorage(pictures, ioScope)
}
