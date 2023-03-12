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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.interop.UIKitInteropView
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.*
import platform.AVFoundation.*
import platform.AVFoundation.AVCaptureDeviceDiscoverySession.Companion.discoverySessionWithDeviceTypes
import platform.AVFoundation.AVCaptureDeviceInput.Companion.deviceInputWithDevice
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreMedia.CMTime
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.MapKit.MKMapView
import platform.UIKit.*
import platform.darwin.NSObject
import platform.posix.memcpy

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
                        uiImage.size.useContents { println("uiImage, w: $width, h: $height") }
                        cameraImages.add(uiImage.toImageBitmap())
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
                    view.layer.setFrame(rect)
                    cameraPreviewLayer.setFrame(rect)
                },
            ) {
                val cameraContainer = UIView()
                cameraContainer.layer.addSublayer(cameraPreviewLayer)
                cameraPreviewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
                cameraPreviewLayer.frame = UIScreen.mainScreen.applicationFrame

                captureSession.startRunning()
                cameraContainer
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
        } else {
            Text("Camera not available")
        }
    }
}

private fun UIImage.toImageBitmap(): ImageBitmap {
    //todo https://github.com/touchlab/DroidconKotlin/blob/fe5b7e8bb6cdf5d00eeaf7ee13f1f96b71857e8f/shared-ui/src/iosMain/kotlin/co/touchlab/droidcon/ui/util/ToSkiaImage.kt
    val pngRepresentation = UIImagePNGRepresentation(this)!!
    val byteArray = ByteArray(pngRepresentation.length.toInt()).apply {
        usePinned {
            memcpy(it.addressOf(0), pngRepresentation.bytes, pngRepresentation.length)
        }
    }
    return org.jetbrains.skia.Image.makeFromEncoded(byteArray).toComposeImageBitmap()
}
