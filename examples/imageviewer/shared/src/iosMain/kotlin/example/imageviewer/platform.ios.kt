package example.imageviewer

import androidx.compose.ui.graphics.vector.ImageVector
import example.imageviewer.icon.IconIosShare
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.CoreFoundation.CFUUIDCreate
import platform.CoreFoundation.CFUUIDCreateString
import platform.Foundation.CFBridgingRelease
import platform.UIKit.UIImage

class IosStorableImage(
    val rawValue: UIImage
)

actual typealias PlatformStorableImage = IosStorableImage

@OptIn(ExperimentalForeignApi::class)
actual fun createUUID(): String =
    CFBridgingRelease(CFUUIDCreateString(null, CFUUIDCreate(null))) as String

actual val ioDispatcher = Dispatchers.IO

actual val isShareFeatureSupported: Boolean = true

actual val shareIcon: ImageVector = IconIosShare
