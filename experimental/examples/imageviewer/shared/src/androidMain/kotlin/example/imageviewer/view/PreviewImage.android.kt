package example.imageviewer.view

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
internal actual fun needShowPreview(): Boolean =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
