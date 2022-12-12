package example.imageviewer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import example.imageviewer.core.FilterType
import example.imageviewer.view.AppUI
import example.imageviewer.model.ContentState
import example.imageviewer.model.ContentStateData
import example.imageviewer.model.ImageRepository
import example.imageviewer.model.filtration.BlurFilter
import example.imageviewer.model.filtration.GrayScaleFilter
import example.imageviewer.model.filtration.PixelFilter

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val content = ContentState(
            repository = ImageRepository("https://raw.githubusercontent.com/JetBrains/compose-jb/master/artwork/imageviewerrepo/fetching.list"),
            contextProvider = { this },
            getFilter = {
                when (it) {
                    FilterType.GrayScale -> GrayScaleFilter()
                    FilterType.Pixel -> PixelFilter()
                    FilterType.Blur -> BlurFilter(this)
                }
            },
            state = mutableStateOf(ContentStateData())
        ).apply {
            initData()
        }

        setContent {
            AppUI(content)
        }
    }
}
