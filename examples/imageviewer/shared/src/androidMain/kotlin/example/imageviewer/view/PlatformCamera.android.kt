package example.imageviewer.view

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import example.imageviewer.*
import example.imageviewer.model.GpsPosition
import example.imageviewer.model.PictureData
import example.imageviewer.model.createCameraPictureData
import imageviewer.shared.generated.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.absoluteValue

private val executor = Executors.newSingleThreadExecutor()

@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun rememberPlatformCameraState(): PlatformCameraState {
    val cameraPermissionState = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        )
    )
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.allPermissionsGranted) {
            cameraPermissionState.launchMultiplePermissionRequest()
        }
    }
    return when {
        cameraPermissionState.allPermissionsGranted -> PlatformCameraState.Ready(rememberAndroidCamera())
        cameraPermissionState.shouldShowRationale ->
            PlatformCameraState.Unavailable("Camera permission is required to take photos.")

        else -> PlatformCameraState.Pending
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun rememberAndroidCamera(): PlatformCamera {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewScope = rememberCoroutineScope()

    val preview = remember { Preview.Builder().build() }
    val previewView = remember { PreviewView(context) }
    val imageCapture: ImageCapture = remember { ImageCapture.Builder().build() }
    var isFrontCamera by rememberSaveable { mutableStateOf(false) }
    val cameraSelector = remember(isFrontCamera) {
        val lensFacing =
            if (isFrontCamera) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
        CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
    }

    BindCameraLifecycle(context, lifecycleOwner, cameraSelector, preview, previewView, imageCapture)

    val nameAndDescription = createNewPhotoNameAndDescription()

    return remember(context, previewView, imageCapture, viewScope, nameAndDescription) {
        AndroidPlatformCamera(
            context = context,
            previewView = previewView,
            imageCapture = imageCapture,
            viewScope = viewScope,
            nameAndDescription = nameAndDescription,
            isFrontCamera = { isFrontCamera },
            onToggleFrontCamera = { isFrontCamera = !isFrontCamera },
        )
    }
}

@Composable
private fun BindCameraLifecycle(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    cameraSelector: CameraSelector,
    preview: Preview,
    previewView: PreviewView,
    imageCapture: ImageCapture,
) {
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    LaunchedEffect(cameraSelector) {
        cameraProvider = suspendCoroutine<ProcessCameraProvider> { continuation ->
            ProcessCameraProvider.getInstance(context).also { provider ->
                provider.addListener({
                    continuation.resume(provider.get())
                }, executor)
            }
        }
        cameraProvider?.unbindAll()
        cameraProvider?.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }
}

private class AndroidPlatformCamera(
    private val context: Context,
    private val previewView: PreviewView,
    private val imageCapture: ImageCapture,
    private val viewScope: CoroutineScope,
    private val nameAndDescription: NameAndDescription,
    private val isFrontCamera: () -> Boolean,
    private val onToggleFrontCamera: () -> Unit,
) : PlatformCamera {

    @Composable
    override fun Preview(modifier: Modifier) {
        AndroidView(
            factory = { previewView },
            modifier = modifier.pointerInput(isFrontCamera()) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount.absoluteValue > 50.0) {
                        onToggleFrontCamera()
                    }
                }
            },
        )
    }

    override fun capture(onResult: (picture: PictureData.Camera, image: PlatformStorableImage) -> Unit) {
        val callback = ImageCaptureCallback(context, nameAndDescription, onResult)
        imageCapture.takePicture(executor, callback)
        viewScope.launch {
            // TODO: There is a known issue with Android emulator
            //  https://partnerissuetracker.corp.google.com/issues/161034252
            //  After 5 seconds delay, let's assume that the bug appears and publish a prepared photo
            delay(5000)
            if (!callback.isResultDelivered) {
                callback.deliverFallbackResult(
                    Res.readBytes("files/android-emulator-photo.jpg").toImageBitmap()
                )
            }
        }
    }
}

private class ImageCaptureCallback(
    private val context: Context,
    private val nameAndDescription: NameAndDescription,
    private val onResult: (picture: PictureData.Camera, image: PlatformStorableImage) -> Unit,
) : OnImageCapturedCallback() {
    private var resultDelivered = false
    val isResultDelivered: Boolean get() = resultDelivered

    override fun onCaptureSuccess(image: ImageProxy) {
        val byteArray: ByteArray = image.planes[0].buffer.toByteArray()
        val imageBitmap = byteArray.toImageBitmap()
        image.close()
        deliverResult(imageBitmap)
    }

    fun deliverFallbackResult(imageBitmap: ImageBitmap) {
        deliverResult(imageBitmap)
    }

    private fun deliverResult(imageBitmap: ImageBitmap) {
        if (resultDelivered) return
        resultDelivered = true

        fun sendToStorage(gpsPosition: GpsPosition) {
            onResult(
                createCameraPictureData(
                    name = nameAndDescription.name,
                    description = nameAndDescription.description,
                    gps = gpsPosition
                ),
                AndroidStorableImage(imageBitmap)
            )
        }
        LocationServices.getFusedLocationProviderClient(context)
            .getCurrentLocation(CurrentLocationRequest.Builder().build(), null)
            .apply {
                addOnSuccessListener {
                    sendToStorage(GpsPosition(it.latitude, it.longitude))
                }
                addOnFailureListener {
                    sendToStorage(GpsPosition(0.0, 0.0))
                }
            }
    }
}

private fun ByteBuffer.toByteArray(): ByteArray {
    rewind()    // Rewind the buffer to zero
    val data = ByteArray(remaining())
    get(data)   // Copy the buffer into a byte array
    return data // Return the byte array
}
