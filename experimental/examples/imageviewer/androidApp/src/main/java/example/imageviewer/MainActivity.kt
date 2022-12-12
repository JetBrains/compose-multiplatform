package example.imageviewer

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import example.imageviewer.core.FilterType
import example.imageviewer.view.AppUI
import example.imageviewer.model.ContentState
import example.imageviewer.model.ContentStateData
import example.imageviewer.model.ImageRepository
import example.imageviewer.model.Notification
import example.imageviewer.model.filtration.BlurFilter
import example.imageviewer.model.filtration.GrayScaleFilter
import example.imageviewer.model.filtration.PixelFilter
import example.imageviewer.shared.R
import example.imageviewer.view.showPopUpMessage

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val content = ContentState(
            repository = ImageRepository("https://raw.githubusercontent.com/JetBrains/compose-jb/master/artwork/imageviewerrepo/fetching.list"),
            getFilter = {
                when (it) {
                    FilterType.GrayScale -> GrayScaleFilter()
                    FilterType.Pixel -> PixelFilter()
                    FilterType.Blur -> BlurFilter(this)
                }
            },
            state = mutableStateOf(ContentStateData()),
            notification = AndroidNotification(contextProvider = { this }),
            cacheDirProvider = {this.cacheDir.absolutePath}
        ).apply {
            initData()
        }

        setContent {
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
