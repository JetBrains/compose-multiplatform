package example.imageviewer.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitInteropView
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import platform.AVFoundation.*
import platform.AVFoundation.AVCaptureDeviceDiscoverySession.Companion.discoverySessionWithDeviceTypes
import platform.AVFoundation.AVCaptureDeviceInput.Companion.deviceInputWithDevice
import platform.CoreGraphics.CGRectMake
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreMedia.CMTime
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.MapKit.MKMapView
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIScreen
import platform.UIKit.UIView
import platform.darwin.NSObject

private val capturePhotoOutput = AVCapturePhotoOutput()
private val photoCaptureDelegate = object : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
    override fun captureOutput(
        output: AVCapturePhotoOutput,
        didFinishProcessingPhoto: AVCapturePhoto,
        error: NSError?
    ) {
        println("before captureOutput")
        val photoData = didFinishProcessingPhoto.fileDataRepresentation() ?: error("fileDataRepresentation is null")
        val uiImage = UIImage(photoData)
        uiImage.size.useContents { println("w: $width, h: $height") }
        //super.captureOutput(output, didFinishProcessingPhoto, error)
    }
}

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
        }
    }

    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (cameraAccess) {
            //todo Stub on simulator
            UIKitInteropView(
                modifier = modifier,
                background = Color.Black,
                update = { println("UIKitInteropView, update") },
            ) {
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
                captureSession.addInput(captureDeviceInput)
                captureSession.addOutput(capturePhotoOutput)

                val cameraPreviewLayer = AVCaptureVideoPreviewLayer(session = captureSession)
                val cameraContainer = UIView()
                cameraContainer.layer.addSublayer(cameraPreviewLayer)
                cameraPreviewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
                cameraPreviewLayer.frame = UIScreen.mainScreen.applicationFrame

                captureSession.startRunning()
                cameraContainer
            }
        } else {
            Text("Camera not available")
        }

        Button(
            modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
            onClick = {
                val photoSettings = AVCapturePhotoSettings.photoSettingsWithFormat(format = mapOf(AVVideoCodecKey to AVVideoCodecTypeJPEG))
                photoSettings.setHighResolutionPhotoEnabled(true)
                val currentMetadata = photoSettings.metadata
                //todo location metadata
                //photoSettings.metadata = currentMetadata.merging(getLocationMetadata(), uniquingKeysWith: { _, geoMetaDataKey -> Any in return geoMetaDataKey })
                capturePhotoOutput.setHighResolutionCaptureEnabled(true)
                capturePhotoOutput.capturePhotoWithSettings(settings = photoSettings, delegate = photoCaptureDelegate)
            }) {
            androidx.compose.material3.Text("Compose Button - Take a photo")
        }
    }
}

