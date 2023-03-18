package example.imageviewer.storage

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

fun NSData.getImageBitmap() : ImageBitmap {
    return UIImage(data = this).toImageBitmap()
}

fun UIImage.toImageBitmap(): ImageBitmap {
    val pngRepresentation = UIImagePNGRepresentation(this)!!
    val byteArray = ByteArray(pngRepresentation.length.toInt()).apply {
        usePinned {
            memcpy(it.addressOf(0), pngRepresentation.bytes, pngRepresentation.length)
        }
    }
    return Image.makeFromEncoded(byteArray).toComposeImageBitmap()
}
