package example.imageviewer.view

import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitInteropView
import androidx.compose.ui.unit.dp
import platform.AVFoundation.*
import platform.AVFoundation.AVCaptureDeviceDiscoverySession.Companion.discoverySessionWithDeviceTypes
import platform.AVFoundation.AVCaptureDeviceInput.Companion.deviceInputWithDevice
import platform.CoreGraphics.CGRectMake
import platform.MapKit.MKMapView
import platform.UIKit.UIApplication
import platform.UIKit.UIScreen
import platform.UIKit.UIView

@Composable
internal actual fun CameraView(modifier: Modifier) {
    var cameraAccess by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> cameraAccess = true
            AVAuthorizationStatusDenied, AVAuthorizationStatusRestricted -> println("TODO describe to user")//todo
            AVAuthorizationStatusNotDetermined -> AVCaptureDevice.requestAccessForMediaType(mediaType = AVMediaTypeVideo) {
                cameraAccess = true
            }
        }
    }

    if (cameraAccess) {
        UIKitInteropView(modifier) {
            val captureSession = AVCaptureSession()
            captureSession.sessionPreset = AVCaptureSessionPresetPhoto
            val position = if (true) AVCaptureDevicePositionFront else AVCaptureDevicePositionBack
            val camera: AVCaptureDevice = discoverySessionWithDeviceTypes(
                deviceTypes = listOf(
                    AVCaptureDeviceTypeBuiltInWideAngleCamera
                ),
                mediaType = AVMediaTypeVideo,
                position = position,
            ).devices.first() as AVCaptureDevice
            val captureDeviceInput: AVCaptureDeviceInput =
                deviceInputWithDevice(device = camera, error = null)!!
            val capturePhotoOutput = AVCapturePhotoOutput()
            captureSession.addInput(captureDeviceInput)
            captureSession.addOutput(capturePhotoOutput)

            val cameraContainer = UIView()
            val cameraPreviewLayer = AVCaptureVideoPreviewLayer(session = captureSession)
            cameraContainer.layer.addSublayer(cameraPreviewLayer)
            cameraPreviewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
            cameraPreviewLayer.frame = UIScreen.mainScreen.applicationFrame

            captureSession.startRunning()
            cameraContainer
        }
    } else {
        Text("Camera not available")
    }
}

