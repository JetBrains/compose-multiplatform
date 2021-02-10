package example.imageviewer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import example.imageviewer.view.BuildAppUI
import example.imageviewer.model.ContentState
import example.imageviewer.model.ImageRepository

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val content = ContentState.applyContent(
            this@MainActivity,
            "https://raw.githubusercontent.com/JetBrains/compose-jb/master/artwork/imageviewerrepo/fetching.list"
        )
        
        setContent {
            BuildAppUI(content)
        }
    }
}