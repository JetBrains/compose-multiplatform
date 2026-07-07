package example.imageviewer.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitView
import example.imageviewer.IosStorableImage
import example.imageviewer.NameAndDescription
import example.imageviewer.PlatformStorableImage
import example.imageviewer.createNewPhotoNameAndDescription
import example.imageviewer.model.GpsPosition
import example.imageviewer.model.PictureData
import example.imageviewer.model.createCameraPictureData
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.AVFoundation.*
import platform.AVFoundation.AVCaptureDeviceDiscoverySession.Companion.discoverySessionWithDeviceTypes
import platform.AVFoundation.AVCaptureDeviceInput.Companion.deviceInputWithDevice
import platform.CoreGraphics.CGRectZero
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
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

private val deviceTypes = listOf(
    AVCaptureDeviceTypeBuiltInWideAngleCamera,
    AVCaptureDeviceTypeBuiltInDualWideCamera,
    AVCaptureDeviceTypeBuiltInDualCamera,
    AVCaptureDeviceTypeBuiltInUltraWideCamera,
    AVCaptureDeviceTypeBuiltInDuoCamera
)

@Composable
private fun rememberCameraAccess(): CameraAccess {
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
    return cameraAccess
}

@Composable
actual fun rememberPlatformCameraState(): PlatformCameraState {
    return when (rememberCameraAccess()) {
        CameraAccess.Undefined -> PlatformCameraState.Pending
        CameraAccess.Denied -> PlatformCameraState.Unavailable("Camera access denied")
        CameraAccess.Authorized -> {
            val camera = rememberDiscoveredCamera()
            if (camera == null) {
                PlatformCameraState.Unavailable(
                    """
                    Camera is not available on simulator.
                    Please try to run on a real iOS device.
                    """.trimIndent()
                )
            } else {
                PlatformCameraState.Ready(rememberAuthorizedCamera(camera))
            }
        }
    }
}

@Composable
private fun rememberDiscoveredCamera(): AVCaptureDevice? = remember {
    discoverySessionWithDeviceTypes(
        deviceTypes = deviceTypes,
        mediaType = AVMediaTypeVideo,
        position = AVCaptureDevicePositionFront,
    ).devices.firstOrNull() as? AVCaptureDevice
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
private fun TrackCameraOrientation(
    cameraPreviewLayer: AVCaptureVideoPreviewLayer,
    capturePhotoOutput: AVCapturePhotoOutput,
) {
    DisposableEffect(cameraPreviewLayer, capturePhotoOutput) {
        val listener = OrientationListener(cameraPreviewLayer, capturePhotoOutput)
        val notificationName = platform.UIKit.UIDeviceOrientationDidChangeNotification
        NSNotificationCenter.defaultCenter.addObserver(
            observer = listener,
            selector = NSSelectorFromString(
                OrientationListener::orientationDidChange.name + ":"
            ),
            name = notificationName,
            `object` = null
        )
        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(
                observer = listener,
                name = notificationName,
                `object` = null
            )
        }
    }
}

@OptIn(BetaInteropApi::class)
private class OrientationListener(
    private val cameraPreviewLayer: AVCaptureVideoPreviewLayer,
    private val capturePhotoOutput: AVCapturePhotoOutput,
) : NSObject() {
    private var actualOrientation = AVCaptureVideoOrientationPortrait

    @Suppress("UNUSED_PARAMETER")
    @ObjCAction
    fun orientationDidChange(arg: NSNotification) {
        val cameraConnection = cameraPreviewLayer.connection
        if (cameraConnection != null) {
            actualOrientation = UIDevice.currentDevice.orientation.toAVCaptureVideoOrientation()
                ?: cameraConnection.videoOrientation
            cameraConnection.videoOrientation = actualOrientation
        }
        capturePhotoOutput.connectionWithMediaType(AVMediaTypeVideo)
            ?.videoOrientation = actualOrientation
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
private fun rememberAuthorizedCamera(camera: AVCaptureDevice): PlatformCamera {
    val capturePhotoOutput = remember { AVCapturePhotoOutput() }
    val locationManager = remember {
        CLLocationManager().apply {
            desiredAccuracy = kCLLocationAccuracyBest
            requestWhenInUseAuthorization()
        }
    }
    val nameAndDescription = createNewPhotoNameAndDescription()

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

    TrackCameraOrientation(cameraPreviewLayer, capturePhotoOutput)

    return remember(camera, capturePhotoOutput, captureSession, cameraPreviewLayer, locationManager, nameAndDescription) {
        IosPlatformCamera(
            camera = camera,
            capturePhotoOutput = capturePhotoOutput,
            captureSession = captureSession,
            cameraPreviewLayer = cameraPreviewLayer,
            locationManager = locationManager,
            nameAndDescription = nameAndDescription,
        )
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class IosPlatformCamera(
    private val camera: AVCaptureDevice,
    private val capturePhotoOutput: AVCapturePhotoOutput,
    private val captureSession: AVCaptureSession,
    private val cameraPreviewLayer: AVCaptureVideoPreviewLayer,
    private val locationManager: CLLocationManager,
    private val nameAndDescription: NameAndDescription,
) : PlatformCamera {

    @Composable
    override fun Preview(modifier: Modifier) {
        UIKitView(
            modifier = modifier.fillMaxSize().background(Color.Black),
            factory = {
                val cameraContainer = object : UIView(frame = CGRectZero.readValue()) {
                    override fun layoutSubviews() {
                        CATransaction.begin()
                        CATransaction.setValue(true, kCATransactionDisableActions)
                        layer.setFrame(frame)
                        cameraPreviewLayer.setFrame(frame)
                        CATransaction.commit()
                    }
                }
                cameraContainer.layer.addSublayer(cameraPreviewLayer)
                cameraPreviewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
                captureSession.startRunning()
                cameraContainer
            },
        )
    }

    override fun capture(onResult: (picture: PictureData.Camera, image: PlatformStorableImage) -> Unit) {
        val photoCaptureDelegate = PhotoCaptureDelegate(locationManager, nameAndDescription, onResult)
        val photoSettings = AVCapturePhotoSettings.photoSettingsWithFormat(
            format = mapOf(AVVideoCodecKey to AVVideoCodecTypeJPEG)
        )
        if (camera.position == AVCaptureDevicePositionFront) {
            capturePhotoOutput.connectionWithMediaType(AVMediaTypeVideo)
                ?.automaticallyAdjustsVideoMirroring = false
            capturePhotoOutput.connectionWithMediaType(AVMediaTypeVideo)
                ?.videoMirrored = true
        }
        capturePhotoOutput.capturePhotoWithSettings(
            settings = photoSettings,
            delegate = photoCaptureDelegate
        )
    }
}

private class PhotoCaptureDelegate(
    private val locationManager: CLLocationManager,
    private val nameAndDescription: NameAndDescription,
    private val onResult: (picture: PictureData.Camera, image: PlatformStorableImage) -> Unit,
) : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
    override fun captureOutput(
        output: AVCapturePhotoOutput,
        didFinishProcessingPhoto: AVCapturePhoto,
        error: NSError?
    ) {
        val photoData = didFinishProcessingPhoto.fileDataRepresentation()
        if (photoData != null) {
            val gps = locationManager.location?.toGps() ?: GpsPosition(0.0, 0.0)
            val uiImage = UIImage(photoData)
            onResult(
                createCameraPictureData(
                    name = nameAndDescription.name,
                    description = nameAndDescription.description,
                    gps = gps
                ),
                IosStorableImage(uiImage)
            )
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun CLLocation.toGps() =
    GpsPosition(
        latitude = coordinate.useContents { latitude },
        longitude = coordinate.useContents { longitude }
    )

private fun UIDeviceOrientation.toAVCaptureVideoOrientation(): AVCaptureVideoOrientation? = when (this) {
    UIDeviceOrientation.UIDeviceOrientationPortrait -> AVCaptureVideoOrientationPortrait
    UIDeviceOrientation.UIDeviceOrientationLandscapeLeft -> AVCaptureVideoOrientationLandscapeRight
    UIDeviceOrientation.UIDeviceOrientationLandscapeRight -> AVCaptureVideoOrientationLandscapeLeft
    UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown -> AVCaptureVideoOrientationPortrait
    else -> null
}
