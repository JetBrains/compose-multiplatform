package example.imageviewer.view

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import example.imageviewer.*
import example.imageviewer.core.BitmapFilter
import example.imageviewer.core.FilterType
import example.imageviewer.model.*
import example.imageviewer.model.filtration.BlurFilter
import example.imageviewer.model.filtration.GrayScaleFilter
import example.imageviewer.model.filtration.PixelFilter
import example.imageviewer.shared.R
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Composable
fun ImageViewerAndroid() {
    val context: Context = LocalContext.current
    val ioScope = rememberCoroutineScope { SupervisorJob() + Dispatchers.IO }
    val dependencies = remember(context) { getDependencies(context, ioScope) }
    ImageViewerCommon(dependencies)
}

private fun getDependencies(context: Context, ioScope: CoroutineScope) = object : Dependencies {
    override fun getFilter(type: FilterType): BitmapFilter =
        when (type) {
            FilterType.GrayScale -> GrayScaleFilter()
            FilterType.Pixel -> PixelFilter()
            FilterType.Blur -> BlurFilter(context)
        }

    override val localization: Localization = object : Localization {
        override val appName: String = context.getString(R.string.app_name)
        override val loading: String = context.getString(R.string.loading)

    }

    override val imageRepository: ContentRepository<NetworkRequest, ImageBitmap> =
        createRealRepository(HttpClient(CIO))
            .decorateWithDiskCache(ioScope, context.cacheDir)
            .adapter { it.toImageBitmap() }

    override val notification: Notification = object : Notification {

        override fun notifyInvalidRepo() = showPopUpMessage(
            context.getString(R.string.repo_invalid)
        )

        override fun notifyRepoIsEmpty() = showPopUpMessage(
            context.getString(R.string.repo_empty)
        )

        override fun notifyNoInternet() = showPopUpMessage(
            context.getString(R.string.no_internet)
        )

        override fun notifyLoadImageUnavailable() = showPopUpMessage(
            "${context.getString(R.string.no_internet)}\n${context.getString(R.string.load_image_unavailable)}"
        )

        override fun notifyLastImage() = showPopUpMessage(
            context.getString(R.string.last_image)
        )

        override fun notifyFirstImage() = showPopUpMessage(
            context.getString(R.string.first_image)
        )

        override fun notifyRefreshUnavailable() = showPopUpMessage(
            "${context.getString(R.string.no_internet)}\n${context.getString(R.string.refresh_unavailable)}"
        )

        override fun notifyImageData(picture: Picture) = showPopUpMessage(
            "${context.getString(R.string.picture)} " +
                    "${picture.name} \n" +
                    "${context.getString(R.string.size)} " +
                    "${picture.width}x${picture.height} " +
                    "${context.getString(R.string.pixels)}"
        )

        private fun showPopUpMessage(text: String) {
            Toast.makeText(
                context,
                text,
                Toast.LENGTH_SHORT
            ).show()
        }

    }
}
