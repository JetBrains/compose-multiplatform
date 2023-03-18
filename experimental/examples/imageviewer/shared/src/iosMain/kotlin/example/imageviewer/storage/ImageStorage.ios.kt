package example.imageviewer.storage

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.ImageBitmap
import cnames.structs.__CFDictionary
import example.imageviewer.ImageStorage
import example.imageviewer.PlatformStorableImage
import example.imageviewer.model.GpsPosition
import example.imageviewer.model.PictureData
import kotlinx.cinterop.CPointer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import platform.CoreFoundation.*
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.ImageIO.*
import platform.UniformTypeIdentifiers.UTTypeJPEG

class IosImageStorage(val pictures: SnapshotStateList<PictureData>):ImageStorage {

    //todo remove inmemory storage map
    val map: MutableMap<PictureData.Camera, NSData> = mutableMapOf()

    init {
        // todo read PictureData from disk and add them to pictures
        // pictures.add()
    }

    override suspend fun getImage(picture: PictureData.Camera): ImageBitmap {
        return withContext(Dispatchers.Default) {
            map[picture]!!.getImageBitmap()
        }
    }

    override fun saveImage(picture: PictureData.Camera, image: PlatformStorableImage) {
        var saveData: NSData = attachGPSTo(photoData = image.data, picture.gps)
        saveData = attachTextMetadata(saveData, picture.name, picture.description)
        //todo save picture data to disk (name, description, gps)
        picture.fileName
        resizeImage(saveData)//todo save to disk as thumbnail ${picture.fileName}-small.jpg
        map[picture] = saveData
        pictures.add(picture)
    }

    fun resizeImage(original: NSData): NSData {
        return original//todo
    }
}

fun attachGPSTo(photoData: NSData, gps: GpsPosition): NSData {
    fun getLocationMetadata(): CFDictionaryRef {
        val metadata = CFDictionaryCreateMutable(null, 6, null, null)
        CFDictionaryAddValue(metadata, kCGImagePropertyGPSLatitude, CFBridgingRetain(gps.latitude))
        CFDictionaryAddValue(metadata, kCGImagePropertyGPSLongitude, CFBridgingRetain(gps.longitude))
//        CFDictionaryAddValue(metadata, kCGImagePropertyGPSTimeStamp, CFBridgingRetain(location.timestamp))
        return metadata as CFDictionaryRef
    }
    val imageSource = CGImageSourceCreateWithData(CFBridgingRetain(photoData) as CFDataRef, null)
    val imageProperties = CGImageSourceCopyPropertiesAtIndex(imageSource, 0, null)
    val mutableImagePropertiesCF = CFDictionaryCreateMutableCopy(kCFAllocatorDefault, 0, imageProperties)
    CFDictionaryAddValue(mutableImagePropertiesCF, kCGImagePropertyGPSDictionary, getLocationMetadata())
    val mutableImageProperties: NSMutableDictionary = CFBridgingRelease(mutableImagePropertiesCF) as NSMutableDictionary
    val updatedProperties = CFBridgingRetain(mutableImageProperties) as CFDictionaryRef
    val destPhotoData = CFDataCreateMutable(null, 0)
    val imageDestination = CGImageDestinationCreateWithData(
        data = destPhotoData,
        type = CFBridgingRetain(UTTypeJPEG.identifier) as CFStringRef,
        count = 1,
        options = null
    )
    CGImageDestinationAddImageFromSource(
        idst = imageDestination,
        isrc = imageSource,
        index = 0,
        properties = updatedProperties
    )
    CGImageDestinationFinalize(imageDestination)
    return CFBridgingRelease(destPhotoData) as NSData
}

fun fetchGpsFrom(photoData: NSData): GpsPosition {
    val imageSource = CGImageSourceCreateWithData(CFBridgingRetain(photoData) as CFDataRef, null)
    val imagePropertiesCF = CGImageSourceCopyPropertiesAtIndex(imageSource, 0, null)
    val gpsDictionary = CFDictionaryGetValue(imagePropertiesCF, kCGImagePropertyGPSDictionary) as CPointer<__CFDictionary>
    val longitude = CFBridgingRelease(CFDictionaryGetValue(gpsDictionary, kCGImagePropertyGPSLongitude))
    val latitude = CFBridgingRelease(CFDictionaryGetValue(gpsDictionary, kCGImagePropertyGPSLatitude))
    return GpsPosition(latitude as Double, longitude as Double)
}

fun attachTextMetadata(photoData: NSData, name: String, description: String): NSData {
    // todo save metadata to NSData
    return photoData
}

class TextMetadata(val name: String, description: String)
fun fetchTextMetadata(photoData: NSData): TextMetadata {
    //todo read metadata from NSData
    return TextMetadata(
        name = "some name",
        description = "some description"
    )
}
