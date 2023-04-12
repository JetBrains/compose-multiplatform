import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.window.Window
import example.imageviewer.*
import example.imageviewer.ImageViewerCommon
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
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Resource
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.wasm.unsafe.*
import kotlin.wasm.unsafe.withScopedMemoryAllocator
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import androidx.compose.ui.window.CanvasBasedWindow

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow("ImageViewer") {
        ImageViewerWeb()
    }
}

@Composable
internal fun ImageViewerWeb() {
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

@OptIn(ExperimentalResourceApi::class)
fun getDependencies(ioScope: CoroutineScope, toastState: MutableState<ToastState>) = object : Dependencies {
    override val pictures: SnapshotStateList<PictureData> = mutableStateListOf(*resourcePictures)
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

    override val httpClient: WrappedHttpClient = object : WrappedHttpClient {

        override suspend fun getAsBytes(urlString: String): ByteArray {
            return urlResource(urlString).readBytes()
        }
    }

    private fun ContentRepository<ImageBitmap>.cacheByUrlAdapter(): ContentRepository<ImageBitmap> {
        val original = this
        return object : ContentRepository<ImageBitmap> {
            val cache = mutableMapOf<String, ImageBitmap>()
            override suspend fun loadContent(url: String): ImageBitmap {
                return cache.getOrPut(url) {
                    original.loadContent(url)
                }
            }
        }
    }

    override val imageRepository: ContentRepository<ImageBitmap> =
        createNetworkRepository(httpClient)
            .adapter { it.toImageBitmap() }
            .cacheByUrlAdapter()

    override val notification: Notification = object : PopupNotification(localization) {
        override fun showPopUpMessage(text: String) {
            toastState.value = ToastState.Shown(text)
        }
    }
}

@ExperimentalResourceApi
private fun urlResource(path: String): Resource = JSResourceImpl(path)

@OptIn(ExperimentalResourceApi::class)
private abstract class AbstractResourceImpl(val path: String) : Resource {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return if (other is AbstractResourceImpl) {
            path == other.path
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }
}

@ExperimentalResourceApi
private class JSResourceImpl(path: String) : AbstractResourceImpl(path) {
    override suspend fun readBytes(): ByteArray {
        return suspendCoroutine { continuation ->
            val req = XMLHttpRequest()
            req.open("GET", path, true)
            req.responseType = "arraybuffer".asDynamic().unsafeCast<XMLHttpRequestResponseType>()

            req.onload = { _ ->
                val arrayBuffer = req.response
                if (arrayBuffer is ArrayBuffer) {
                    val size = arrayBuffer.byteLength
                    continuation.resume(arrayBuffer.toByteArray())
                } else {
                    continuation.resumeWithException(MissingResourceException(path))
                }
                null
            }
            req.send(null)
        }
    }
}

private class MissingResourceException constructor(path: String) :
    Exception("Missing resource with path: $path")


private fun ArrayBuffer.toByteArray(): ByteArray {
    val source = Int8Array(this, 0, byteLength)
    return jsInt8ArrayToKotlinByteArray(source)
}

@JsFun(
    """ (src, size, dstAddr) => {
        const mem8 = new Int8Array(wasmExports.memory.buffer, dstAddr, size);
        mem8.set(src);
    }
"""
)
internal external fun jsExportInt8ArrayToWasm(src: Int8Array, size: Int, dstAddr: Int)

internal fun jsInt8ArrayToKotlinByteArray(x: Int8Array): ByteArray {
    val size = x.length

    @OptIn(UnsafeWasmMemoryApi::class)
    return withScopedMemoryAllocator { allocator ->
        val memBuffer = allocator.allocate(size)
        val dstAddress = memBuffer.address.toInt()
        jsExportInt8ArrayToWasm(x, size, dstAddress)
        ByteArray(size) { i -> (memBuffer + i).loadByte() }
    }
}
