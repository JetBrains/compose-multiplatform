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
import kotlinx.cinterop.*
import platform.AVFoundation.*
import platform.AVFoundation.AVCaptureDeviceDiscoverySession.Companion.discoverySessionWithDeviceTypes
import platform.AVFoundation.AVCaptureDeviceInput.Companion.deviceInputWithDevice
import platform.CoreFoundation.*
import platform.CoreGraphics.CGImageRef
import platform.CoreGraphics.CGRect
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.*
import platform.ImageIO.*
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.UIImage
import platform.UIKit.UIView
import platform.UniformTypeIdentifiers.UTTypeJPEG
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

data class GeoPos(
    val latitude: Double,
    val longitude: Double,
)

fun fetchCoordinatesFrom(photoData: NSData): GeoPos {
    val imageSource = CGImageSourceCreateWithData(CFBridgingRetain(photoData) as CFDataRef, null)
    val imagePropertiesCF = CGImageSourceCopyPropertiesAtIndex(imageSource, 0, null)
    val gpsDictionary = CFDictionaryGetValue(imagePropertiesCF, kCGImagePropertyGPSDictionary) as CPointer<__CFDictionary>
    val longitude = CFBridgingRelease(CFDictionaryGetValue(gpsDictionary, kCGImagePropertyGPSLongitude))
    val latitude = CFBridgingRelease(CFDictionaryGetValue(gpsDictionary, kCGImagePropertyGPSLatitude))
    return GeoPos(latitude as Double, longitude as Double)
}

fun NSData.fetchImageFrom() : CGImageRef {
    return UIImage(data = this).CGImage!!
    val cfData = this as CFDataRef
    val imageSource = CGImageSourceCreateWithData(cfData, null)
    return CGImageSourceCreateImageAtIndex(imageSource, 0, null)!!
}

@Composable
private fun BoxScope.AuthorizedCamera() {
    val locationManager = remember {
        CLLocationManager().apply {
            desiredAccuracy = kCLLocationAccuracyBest
            requestWhenInUseAuthorization()
        }
    }
    fun attachedGPSTo(photoData: NSData): NSData {
        fun getLocationMetadata(): CFDictionaryRef {
            val metadata = CFDictionaryCreateMutable(null, 6, null, null)
            val location = locationManager.location
            if (location != null) {
                val latitude = location.coordinate.useContents { latitude }
                val longitude = location.coordinate.useContents { longitude }
                CFDictionaryAddValue(metadata, kCGImagePropertyGPSLatitude, CFBridgingRetain(latitude))
                CFDictionaryAddValue(metadata, kCGImagePropertyGPSLongitude, CFBridgingRetain(longitude))
                CFDictionaryAddValue(metadata, kCGImagePropertyGPSAltitudeRef, CFBridgingRetain(0))
                CFDictionaryAddValue(metadata, kCGImagePropertyGPSAltitude, CFBridgingRetain(location.altitude))
                CFDictionaryAddValue(metadata, kCGImagePropertyGPSTimeStamp, CFBridgingRetain(location.timestamp))
                CFDictionaryAddValue(metadata, kCGImagePropertyGPSDateStamp, CFBridgingRetain(location.timestamp))
            }
            return metadata as CFDictionaryRef
        }
        val imageSource = CGImageSourceCreateWithData(CFBridgingRetain(photoData) as CFDataRef, null)
        val imageProperties = CGImageSourceCopyPropertiesAtIndex(imageSource, 0, null)
        val mutableImagePropertiesCF = CFDictionaryCreateMutableCopy(kCFAllocatorDefault, 0, imageProperties)
        CFDictionaryAddValue(mutableImagePropertiesCF, kCGImagePropertyGPSDictionary, getLocationMetadata())
        val mutableImageProperties: NSMutableDictionary = CFBridgingRelease(mutableImagePropertiesCF) as NSMutableDictionary
        val updatedProperties = CFBridgingRetain(mutableImageProperties) as CFDictionaryRef
        val destPhotoData = CFDataCreateMutable(null, 0)
        val imageDestination = CGImageDestinationCreateWithData(destPhotoData, CFBridgingRetain(UTTypeJPEG.identifier) as CFStringRef, 1, null)
        CGImageDestinationAddImageFromSource(imageDestination, imageSource, 0, updatedProperties)
        CGImageDestinationFinalize(imageDestination)
        return CFBridgingRelease(destPhotoData) as NSData
    }
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
                val updatedData = attachedGPSTo(photoData = photoData)
                updatedData.fetchImageFrom()
                val geo = fetchCoordinatesFrom(updatedData)
                println("geo: $geo")
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
        //todo locationManager here?
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
