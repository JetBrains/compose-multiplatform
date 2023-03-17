package example.imageviewer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import cnames.structs.__CFDictionary
import example.imageviewer.core.BitmapFilter
import example.imageviewer.core.FilterType
import example.imageviewer.model.*
import example.imageviewer.model.filtration.BlurFilter
import example.imageviewer.model.filtration.GrayScaleFilter
import example.imageviewer.model.filtration.PixelFilter
import example.imageviewer.style.ImageViewerTheme
import example.imageviewer.utils.ioDispatcher
import example.imageviewer.view.Toast
import example.imageviewer.view.ToastState
import example.imageviewer.view.toImageBitmap
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.CPointer
import kotlinx.coroutines.CoroutineScope
import platform.CoreFoundation.*
import platform.CoreGraphics.CGImageRef
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.ImageIO.*
import platform.UIKit.UIImage
import platform.UniformTypeIdentifiers.UTTypeJPEG

@Composable
internal fun ImageViewerIos() {
    val toastState = remember { mutableStateOf<ToastState>(ToastState.Hidden) }
    val ioScope: CoroutineScope = rememberCoroutineScope { ioDispatcher }
    val dependencies = remember(ioScope) { getDependencies(ioScope, toastState) }

    ImageViewerTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            ImageViewerCommon(
                dependencies = dependencies
            )
            Toast(toastState)
        }
    }
}

fun getDependencies(ioScope: CoroutineScope, toastState: MutableState<ToastState>) = object : Dependencies() {
    override val ioScope: CoroutineScope = ioScope
    override fun getFilter(type: FilterType): BitmapFilter = when (type) {
        FilterType.GrayScale -> GrayScaleFilter()
        FilterType.Pixel -> PixelFilter()
        FilterType.Blur -> BlurFilter()
    }

    override val localization: Localization = object : Localization {
        override val appName = "ImageViewer"
        override val loading = "Loading images..."
        override val repoEmpty = "Repository is empty."
        override val noInternet = "No internet access."
        override val repoInvalid = "List of images in current repository is invalid or empty."
        override val refreshUnavailable = "Cannot refresh images."
        override val loadImageUnavailable = "Cannot load full size image."
        override val lastImage = "This is last image."
        override val firstImage = "This is first image."
        override val picture = "Picture:"
        override val size = "Size:"
        override val pixels = "pixels."
        override val back = "Back"
    }

    override val httpClient: HttpClient = HttpClient(Darwin)

    override val notification: Notification = object : PopupNotification(localization) {
        override fun showPopUpMessage(text: String) {
            toastState.value = ToastState.Shown(text)
        }
    }

    override val imageStorage: ImageStorage = object : ImageStorage {
        val map: MutableMap<PictureData.Storage, NSData> = mutableMapOf()
        override suspend fun getImage(picture: PictureData.Storage): ImageBitmap {
            return map[picture]!!.fetchImageFrom()
        }

        override fun saveImage(picture: PictureData, image: PlatformStorableImage) {
            val updatedData: NSData = attachGPSTo(photoData = image.data, picture.geo)
            // todo attach name and description
        }
    }

}

fun fetchCoordinatesFrom(photoData: NSData): GeoPos {
    val imageSource = CGImageSourceCreateWithData(CFBridgingRetain(photoData) as CFDataRef, null)
    val imagePropertiesCF = CGImageSourceCopyPropertiesAtIndex(imageSource, 0, null)
    val gpsDictionary = CFDictionaryGetValue(imagePropertiesCF, kCGImagePropertyGPSDictionary) as CPointer<__CFDictionary>
    val longitude = CFBridgingRelease(CFDictionaryGetValue(gpsDictionary, kCGImagePropertyGPSLongitude))
    val latitude = CFBridgingRelease(CFDictionaryGetValue(gpsDictionary, kCGImagePropertyGPSLatitude))
    return GeoPos(latitude as Double, longitude as Double)
}

fun NSData.fetchImageFrom() : ImageBitmap {
    return UIImage(data = this).toImageBitmap()
//    val cfData = this as CFDataRef
//    val imageSource = CGImageSourceCreateWithData(cfData, null)
//    return CGImageSourceCreateImageAtIndex(imageSource, 0, null)!!
}

fun attachGPSTo(photoData: NSData, geo: GeoPos): NSData {
    fun getLocationMetadata(): CFDictionaryRef {
        val metadata = CFDictionaryCreateMutable(null, 6, null, null)
        CFDictionaryAddValue(metadata, kCGImagePropertyGPSLatitude, CFBridgingRetain(geo.latitude))
        CFDictionaryAddValue(metadata, kCGImagePropertyGPSLongitude, CFBridgingRetain(geo.longitude))
//            CFDictionaryAddValue(metadata, kCGImagePropertyGPSAltitude, CFBridgingRetain(location.altitude))
//            CFDictionaryAddValue(metadata, kCGImagePropertyGPSTimeStamp, CFBridgingRetain(location.timestamp))
//            CFDictionaryAddValue(metadata, kCGImagePropertyGPSDateStamp, CFBridgingRetain(location.timestamp))
        return metadata as CFDictionaryRef
    }
    val imageSource = CGImageSourceCreateWithData(CFBridgingRetain(photoData) as CFDataRef, null)
    val imageProperties = CGImageSourceCopyPropertiesAtIndex(imageSource, 0, null)
    val mutableImagePropertiesCF = CFDictionaryCreateMutableCopy(kCFAllocatorDefault, 0, imageProperties)
    CFDictionaryAddValue(mutableImagePropertiesCF, kCGImagePropertyGPSDictionary, getLocationMetadata())
    val mutableImageProperties: NSMutableDictionary = CFBridgingRelease(mutableImagePropertiesCF) as NSMutableDictionary
    val updatedProperties = CFBridgingRetain(mutableImageProperties) as CFDictionaryRef
    val destPhotoData = CFDataCreateMutable(null, 0)
    val imageDestination = CGImageDestinationCreateWithData(destPhotoData, CFBridgingRetain(
        UTTypeJPEG.identifier) as CFStringRef, 1, null)
    CGImageDestinationAddImageFromSource(imageDestination, imageSource, 0, updatedProperties)
    CGImageDestinationFinalize(imageDestination)
    return CFBridgingRelease(destPhotoData) as NSData
}

