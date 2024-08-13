package example.imageviewer.view

import android.annotation.SuppressLint
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import example.imageviewer.*
import example.imageviewer.icon.IconPhotoCamera
import example.imageviewer.model.GpsPosition
import example.imageviewer.model.PictureData
import example.imageviewer.model.createCameraPictureData
import imageviewer.shared.generated.resources.Res
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
actual fun CameraView(
    modifier: Modifier,
    onCapture: (picture: PictureData.Camera, image: PlatformStorableImage) -> Unit
) {
    val cameraPermissionState = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        )
    )
    if (cameraPermissionState.allPermissionsGranted) {
        CameraWithGrantedPermission(modifier, onCapture)
    } else {
        LaunchedEffect(Unit) {
            cameraPermissionState.launchMultiplePermissionRequest()
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun CameraWithGrantedPermission(
    modifier: Modifier,
    onCapture: (picture: PictureData.Camera, image: PlatformStorableImage) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewScope = rememberCoroutineScope()
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }

    val preview = Preview.Builder().build()
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

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    LaunchedEffect(isFrontCamera) {
        cameraProvider = suspendCoroutine<ProcessCameraProvider> { continuation ->
            ProcessCameraProvider.getInstance(context).also { cameraProvider ->
                cameraProvider.addListener({
                    continuation.resume(cameraProvider.get())
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
    val nameAndDescription = createNewPhotoNameAndDescription()
    var capturePhotoStarted by remember { mutableStateOf(false) }

    Box(modifier = modifier.pointerInput(isFrontCamera) {
        detectHorizontalDragGestures { change, dragAmount ->
            if (dragAmount.absoluteValue > 50.0) {
                isFrontCamera = !isFrontCamera
            }
        }
    }) {
        AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
        CircularButton(
            imageVector = IconPhotoCamera,
            modifier = Modifier.align(Alignment.BottomCenter).padding(36.dp),
            enabled = !capturePhotoStarted,
        ) {
            fun addLocationInfoAndReturnResult(imageBitmap: ImageBitmap) {
                fun sendToStorage(gpsPosition: GpsPosition) {
                    onCapture(
                        createCameraPictureData(
                            name = nameAndDescription.name,
                            description = nameAndDescription.description,
                            gps = gpsPosition
                        ),
                        AndroidStorableImage(imageBitmap)
                    )
                    capturePhotoStarted = false
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

            capturePhotoStarted = true
            imageCapture.takePicture(executor, object : OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val byteArray: ByteArray = image.planes[0].buffer.toByteArray()
                    val imageBitmap = byteArray.toImageBitmap()
                    image.close()
                    addLocationInfoAndReturnResult(imageBitmap)
                }
            })
            viewScope.launch {
                // TODO: There is a known issue with Android emulator
                //  https://partnerissuetracker.corp.google.com/issues/161034252
                //  After 5 seconds delay, let's assume that the bug appears and publish a prepared photo
                delay(5000)
                if (capturePhotoStarted) {
                    addLocationInfoAndReturnResult(
                        Res.readBytes("files/android-emulator-photo.jpg").toImageBitmap()
                    )
                }
            }
        }
        if (capturePhotoStarted) {
            CircularProgressIndicator(
                modifier = Modifier.size(80.dp).align(Alignment.Center),
                color = Color.White.copy(alpha = 0.7f),
                strokeWidth = 8.dp,
            )
        }
    }
}

private fun ByteBuffer.toByteArray(): ByteArray {
    rewind()    // Rewind the buffer to zero
    val data = ByteArray(remaining())
    get(data)   // Copy the buffer into a byte array
    return data // Return the byte array
}
