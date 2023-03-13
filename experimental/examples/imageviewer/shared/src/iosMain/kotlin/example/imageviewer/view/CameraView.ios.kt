package example.imageviewer.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitInteropView
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.CValue
import platform.AVFoundation.*
import platform.AVFoundation.AVCaptureDeviceDiscoverySession.Companion.discoverySessionWithDeviceTypes
import platform.AVFoundation.AVCaptureDeviceInput.Companion.deviceInputWithDevice
import platform.CoreGraphics.CGRect
import platform.Foundation.NSError
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.UIImage
import platform.UIKit.UIView
import platform.darwin.NSObject

private sealed interface CameraAccess {
    object Undefined : CameraAccess
    object Denied : CameraAccess
    object Authorized : CameraAccess
}

@Composable
internal actual fun CameraView(modifier: Modifier) {
    var cameraAccess: CameraAccess by remember { mutableStateOf(CameraAccess.Undefined) }
    LaunchedEffect(Unit) {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> {
                cameraAccess = CameraAccess.Authorized
            }

            AVAuthorizationStatusDenied, AVAuthorizationStatusRestricted -> {
                cameraAccess = CameraAccess.Denied
            }

            AVAuthorizationStatusNotDetermined -> {
                AVCaptureDevice.requestAccessForMediaType(
                    mediaType = AVMediaTypeVideo
                ) { success ->
                    cameraAccess = if (success) CameraAccess.Authorized else CameraAccess.Denied
                }
            }
        }
    }
    Box(
        Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (cameraAccess) {
            CameraAccess.Undefined -> {
                // Waiting for the user to accept permission
            }

            CameraAccess.Denied -> {
                Text("Camera access denied", color = Color.White)
            }

            CameraAccess.Authorized -> {
                AuthorizedCamera()
            }
        }
    }
}

@Composable
private fun BoxScope.AuthorizedCamera() {
    val capturePhotoOutput = remember { AVCapturePhotoOutput() }
    val photoCaptureDelegate = remember {
        object : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
            override fun captureOutput(
                output: AVCapturePhotoOutput,
                didFinishProcessingPhoto: AVCapturePhoto,
                error: NSError?
            ) {
                val photoData = didFinishProcessingPhoto.fileDataRepresentation()
                    ?: error("fileDataRepresentation is null")
                val uiImage = UIImage(photoData)
                //todo pass image to gallery page
            }
        }
    }
    val camera: AVCaptureDevice? = remember {
        discoverySessionWithDeviceTypes(
            deviceTypes = listOf(AVCaptureDeviceTypeBuiltInWideAngleCamera),
            mediaType = AVMediaTypeVideo,
            position = AVCaptureDevicePositionFront,
        ).devices.firstOrNull() as? AVCaptureDevice
    }
    if (camera != null) {
        val captureSession: AVCaptureSession = remember {
            AVCaptureSession().also { captureSession ->
                captureSession.sessionPreset = AVCaptureSessionPresetPhoto
                val captureDeviceInput: AVCaptureDeviceInput =
                    deviceInputWithDevice(device = camera, error = null)!!
                captureSession.addInput(captureDeviceInput)
                captureSession.addOutput(capturePhotoOutput)
            }
        }
        val cameraPreviewLayer = remember {
            AVCaptureVideoPreviewLayer(session = captureSession)
        }
        UIKitInteropView(
            modifier = Modifier.fillMaxSize(),
            background = Color.Black,
            resize = { view: UIView, rect: CValue<CGRect> ->
                cameraPreviewLayer.connection?.apply {
                    videoOrientation = when (UIDevice.currentDevice.orientation) {
                        UIDeviceOrientation.UIDeviceOrientationPortrait ->
                            AVCaptureVideoOrientationPortrait

                        UIDeviceOrientation.UIDeviceOrientationLandscapeLeft ->
                            AVCaptureVideoOrientationLandscapeRight

                        UIDeviceOrientation.UIDeviceOrientationLandscapeRight ->
                            AVCaptureVideoOrientationLandscapeLeft

                        UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown ->
                            AVCaptureVideoOrientationPortraitUpsideDown

                        else -> videoOrientation
                    }
                }
                CATransaction.begin()
                CATransaction.setValue(true, kCATransactionDisableActions)
                view.layer.setFrame(rect)
                cameraPreviewLayer.setFrame(rect)
                CATransaction.commit()
            },
        ) {
            val cameraContainer = UIView()
            cameraContainer.layer.addSublayer(cameraPreviewLayer)
            cameraPreviewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
            captureSession.startRunning()
            cameraContainer
        }
        Button(
            modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
            onClick = {
                val photoSettings = AVCapturePhotoSettings.photoSettingsWithFormat(
                    format = mapOf(AVVideoCodecKey to AVVideoCodecTypeJPEG)
                )
                photoSettings.setHighResolutionPhotoEnabled(true)
                capturePhotoOutput.setHighResolutionCaptureEnabled(true)
                capturePhotoOutput.capturePhotoWithSettings(
                    settings = photoSettings,
                    delegate = photoCaptureDelegate
                )
            }) {
            Text("Compose Button - take a photo 📸")
        }
    } else {
        SimulatorStub()
    }
}

@Composable
private fun SimulatorStub() {
    Text(
        """
            Camera is not available on simulator.
            Please try to run on a real iOS device.
        """.trimIndent(), color = Color.White
    )
}
