package example.imageviewer.view

import android.annotation.SuppressLint
import android.location.Location
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import example.imageviewer.*
import example.imageviewer.model.GpsPosition
import example.imageviewer.model.PictureData
import example.imageviewer.model.createCameraPictureData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.resource
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private val executor = Executors.newSingleThreadExecutor()

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal actual fun CameraView(
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

@OptIn(ExperimentalResourceApi::class)
@SuppressLint("MissingPermission")
@Composable
private fun CameraWithGrantedPermission(
    modifier: Modifier,
    onCapture: (picture: PictureData.Camera, image: PlatformStorableImage) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val preview = Preview.Builder().build()
    val previewView = remember { PreviewView(context) }
    val imageCapture: ImageCapture = remember { ImageCapture.Builder().build() }
    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()
    val viewScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val cameraProvider = suspendCoroutine<ProcessCameraProvider> { continuation ->
            ProcessCameraProvider.getInstance(context).also { cameraProvider ->
                cameraProvider.addListener({
                    continuation.resume(cameraProvider.get())
                }, executor)
            }
        }
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }
    val nameAndDescription = createNewPhotoNameAndDescription()
    var capturePhotoStarted by remember { mutableStateOf(false) }
    Box(contentAlignment = Alignment.BottomCenter, modifier = modifier) {
        AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
        Button(
            enabled = !capturePhotoStarted,
            onClick = {
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
                    // TODO: There is known issue with Android emulator
                    //  https://partnerissuetracker.corp.google.com/issues/161034252
                    //  After 5 seconds delay, let's assume that bug appear and publish prepared photo
                    delay(5000)
                    if (capturePhotoStarted) {
                        addLocationInfoAndReturnResult(
                            resource("android-emulator-photo.jpg").readBytes().toImageBitmap()
                        )
                    }
                }
            }) {
            Text(LocalLocalization.current.takePhoto, color = Color.White)
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
