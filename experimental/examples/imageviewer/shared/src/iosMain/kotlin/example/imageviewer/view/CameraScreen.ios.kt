package example.imageviewer.view

import androidx.compose.foundation.layout.Box
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

@Composable
internal actual fun CameraView(modifier: Modifier) {
    var cameraAccess by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> cameraAccess = true
            AVAuthorizationStatusDenied, AVAuthorizationStatusRestricted -> println("TODO describe to user")//todo
            AVAuthorizationStatusNotDetermined -> AVCaptureDevice.requestAccessForMediaType(
                mediaType = AVMediaTypeVideo
            ) {
                cameraAccess = true
            }
            //todo Stub on simulator
        }
    }

    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (cameraAccess) {
            val capturePhotoOutput = remember { AVCapturePhotoOutput() }
            val photoCaptureDelegate = remember {
                object : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
                    override fun captureOutput(
                        output: AVCapturePhotoOutput,
                        didFinishProcessingPhoto: AVCapturePhoto,
                        error: NSError?
                    ) {
                        val photoData = didFinishProcessingPhoto.fileDataRepresentation() ?: error("fileDataRepresentation is null")
                        val uiImage = UIImage(photoData)
                        //todo use this uiImage in main application
                    }
                }
            }
            val captureSession = remember {
                AVCaptureSession().also { captureSession->
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
                    captureSession.addInput(captureDeviceInput)
                    captureSession.addOutput(capturePhotoOutput)
                }
            }
            val cameraPreviewLayer = remember {
                AVCaptureVideoPreviewLayer(session = captureSession)
            }
            UIKitInteropView(
                modifier = modifier,
                background = Color.Black,
                update = {

                },
                resize = { view: UIView, rect: CValue<CGRect> ->
                    cameraPreviewLayer.connection?.apply {
                        videoOrientation = when (UIDevice.currentDevice.orientation) {
                            UIDeviceOrientation.UIDeviceOrientationPortrait -> AVCaptureVideoOrientationPortrait
                            UIDeviceOrientation.UIDeviceOrientationLandscapeLeft -> AVCaptureVideoOrientationLandscapeRight
                            UIDeviceOrientation.UIDeviceOrientationLandscapeRight -> AVCaptureVideoOrientationLandscapeLeft
                            UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown -> AVCaptureVideoOrientationPortraitUpsideDown
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
                Text("Compose Button - Take a photo")
            }
        } else {
            Text("Camera not available")
        }
    }
}
