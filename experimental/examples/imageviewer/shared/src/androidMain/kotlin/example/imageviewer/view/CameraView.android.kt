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
import androidx.compose.material.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import example.imageviewer.AndroidStorableImage
import example.imageviewer.ImageStorage
import example.imageviewer.model.GpsPosition
import example.imageviewer.model.PictureData
import example.imageviewer.toImageBitmap
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private val executor = Executors.newSingleThreadExecutor()

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal actual fun CameraView(modifier: Modifier, storage: ImageStorage) {
    val cameraPermissionState = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    )
    if (cameraPermissionState.allPermissionsGranted) {
        CameraWithGrantedPermission(modifier, storage)
    } else {
        LaunchedEffect(Unit) {
            cameraPermissionState.launchMultiplePermissionRequest()
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun CameraWithGrantedPermission(modifier: Modifier, storage: ImageStorage) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val preview = Preview.Builder().build()
    val previewView = remember { PreviewView(context) }
    val imageCapture: ImageCapture = remember { ImageCapture.Builder().build() }
    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()

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

    Box(contentAlignment = Alignment.BottomCenter, modifier = modifier) {
        AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
        Button(onClick = {
            imageCapture.takePicture(executor, object : OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val byteArray: ByteArray = image.planes[0].buffer.toByteArray()
                    val imageBitmap = byteArray.toImageBitmap()
                    image.close()
                    val lastLocation: Task<Location> =
                        LocationServices.getFusedLocationProviderClient(context).lastLocation
                    val gpsPosition = if (lastLocation.isSuccessful) {
                        GpsPosition(lastLocation.result.latitude, lastLocation.result.longitude)
                    } else {
                        GpsPosition(0.0, 0.0)
                    }
                    storage.saveImage(
                        PictureData.Camera(
                            id = UUID.randomUUID().toString(),
                            name = "Kotlin Conf",
                            description = "Kotlin Conf photo description",
                            gps = gpsPosition
                        ),
                        AndroidStorableImage(imageBitmap)
                    )
                }
            })
        }) {
            Text("Photo")
        }
    }
}

private fun ByteBuffer.toByteArray(): ByteArray {
    rewind()    // Rewind the buffer to zero
    val data = ByteArray(remaining())
    get(data)   // Copy the buffer into a byte array
    return data // Return the byte array
}
