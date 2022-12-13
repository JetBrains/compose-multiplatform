package example.imageviewer

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import example.imageviewer.core.FilterType
import example.imageviewer.model.*
import example.imageviewer.view.AppUI
import example.imageviewer.model.filtration.BlurFilter
import example.imageviewer.model.filtration.GrayScaleFilter
import example.imageviewer.model.filtration.PixelFilter
import example.imageviewer.shared.R
import example.imageviewer.view.showPopUpMessage
import kotlinx.coroutines.Dispatchers

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val content = ContentState(
                getFilter = {
                    when (it) {
                        FilterType.GrayScale -> GrayScaleFilter()
                        FilterType.Pixel -> PixelFilter()
                        FilterType.Blur -> BlurFilter(this)
                    }
                },
                state = remember { mutableStateOf(ContentStateData()) },
                notification = AndroidNotification(contextProvider = { this }),
                repository = rememberImageRepository(rememberCoroutineScope { Dispatchers.Default })
            ).apply {
                initData()
            }
            AppUI(content)
        }
    }
}

class AndroidNotification(val contextProvider: () -> Context): Notification {
    val context get() = contextProvider()
    fun getString(id: Int): String {
        return context.getString(id)
    }

    override fun notifyInvalidRepo() {
        showPopUpMessage(
            getString(R.string.repo_invalid),
            context
        )
    }

    override fun notifyRepoIsEmpty() {
        showPopUpMessage(
            getString(R.string.repo_empty),
            context
        )
    }

    override fun notifyNoInternet() {
        showPopUpMessage(
            getString(R.string.no_internet),
            context
        )
    }

    override fun notifyLoadImageUnavailable() {
        showPopUpMessage(
            "${getString(R.string.no_internet)}\n${getString(R.string.load_image_unavailable)}",
            context
        )
    }

    override fun notifyLastImage() {
        showPopUpMessage(
            getString(R.string.last_image),
            context
        )
    }

    override fun notifyFirstImage() {
        showPopUpMessage(
            getString(R.string.first_image),
            context
        )
    }

    override fun notifyRefreshUnavailable() {
        showPopUpMessage(
            "${getString(R.string.no_internet)}\n${getString(R.string.refresh_unavailable)}",
            context
        )
    }

}
