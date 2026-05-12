package example.imageviewer

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import example.imageviewer.view.ImageViewerAndroid
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : AppCompatActivity() {

    val externalEvents = MutableSharedFlow<ExternalImageViewerEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageViewerAndroid(externalEvents)
        }
        onBackPressedDispatcher.addCallback {
            externalEvents.tryEmit(ExternalImageViewerEvent.ReturnBack)
        }
    }

}
