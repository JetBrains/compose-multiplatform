package example.imageviewer.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitInteropView
import androidx.compose.ui.unit.dp
import example.imageviewer.IosStorableImage
import example.imageviewer.LocalLocalization
import example.imageviewer.PlatformStorableImage
import example.imageviewer.model.GpsPosition
import example.imageviewer.model.PictureData
import example.imageviewer.model.createCameraPictureData
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import kotlinx.datetime.Clock
import platform.AVFoundation.*
import platform.AVFoundation.AVCaptureDeviceDiscoverySession.Companion.discoverySessionWithDeviceTypes
import platform.AVFoundation.AVCaptureDeviceInput.Companion.deviceInputWithDevice
import platform.CoreGraphics.CGRect
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.*
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
internal actual fun CameraView(
    modifier: Modifier,
    onCapture: (picture: PictureData.Camera, image: PlatformStorableImage) -> Unit
) {
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
                AuthorizedCamera(onCapture)
            }
        }
    }
}

@Composable
private fun BoxScope.AuthorizedCamera(onCapture: (picture: PictureData.Camera, image: PlatformStorableImage) -> Unit) {
    val locationManager = remember {
        CLLocationManager().apply {
            desiredAccuracy = kCLLocationAccuracyBest
            requestWhenInUseAuthorization()
        }
    }
    val capturePhotoOutput = remember { AVCapturePhotoOutput() }
    var actualOrientation by remember {
        mutableStateOf(
            AVCaptureVideoOrientationPortrait
        )
    }
    var capturePhotoStarted by remember { mutableStateOf(false) }
    val photoCaptureDelegate = remember {
        object : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
            override fun captureOutput(
                output: AVCapturePhotoOutput,
                didFinishProcessingPhoto: AVCapturePhoto,
                error: NSError?
            ) {
                val photoData = didFinishProcessingPhoto.fileDataRepresentation()
                if (photoData != null) {
                    val location = locationManager.location
                    val geoPos = if (location != null) {
                        GpsPosition(
                            latitude = location.coordinate.useContents { latitude },
                            longitude = location.coordinate.useContents { longitude }
                        )
                    } else {
                        GpsPosition(0.0, 0.0)
                    }
                    val uiImage = UIImage(photoData)
                    onCapture(
                        createCameraPictureData(
                            name = "Kotlin Conf",
                            description = "Kotlin Conf photo description",
                            gps = geoPos),
                        IosStorableImage(uiImage)
                    )
                }
                capturePhotoStarted = false
            }
        }
    }
    val camera: AVCaptureDevice? = remember {
        discoverySessionWithDeviceTypes(
            deviceTypes = deviceTypes,
            mediaType = AVMediaTypeVideo,
            position = AVCaptureDevicePositionFront,
        ).devices.firstOrNull() as? AVCaptureDevice
    }
    if (camera != null) {
        //todo locationManager here?
        //todo location maybe null if user decline location permission
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

        DisposableEffect(Unit) {
            class OrientationListener : NSObject() {
                @ObjCAction
                fun orientationDidChange(arg: NSNotification) {
                    val cameraConnection = cameraPreviewLayer.connection
                    if (cameraConnection != null) {
                        actualOrientation = when (UIDevice.currentDevice.orientation) {
                            UIDeviceOrientation.UIDeviceOrientationPortrait ->
                                AVCaptureVideoOrientationPortrait

                            UIDeviceOrientation.UIDeviceOrientationLandscapeLeft ->
                                AVCaptureVideoOrientationLandscapeRight

                            UIDeviceOrientation.UIDeviceOrientationLandscapeRight ->
                                AVCaptureVideoOrientationLandscapeLeft

                            UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown ->
                                AVCaptureVideoOrientationPortrait

                            else -> cameraConnection.videoOrientation
                        }
                        cameraConnection.videoOrientation = actualOrientation
                    }
                    capturePhotoOutput.connectionWithMediaType(AVMediaTypeVideo)?.videoOrientation =
                        actualOrientation
                }
            }
            val listener = OrientationListener()
            val notificationName = platform.UIKit.UIDeviceOrientationDidChangeNotification
            NSNotificationCenter.defaultCenter.addObserver(
                observer = listener,
                selector = NSSelectorFromString(OrientationListener::orientationDidChange.name + ":"),
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
        UIKitInteropView(
            modifier = Modifier.fillMaxSize(),
            background = Color.Black,
            resize = { view: UIView, rect: CValue<CGRect> ->
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
            modifier = Modifier.align(Alignment.BottomCenter).padding(44.dp),
//            enabled = !capturePhotoStarted,//todo
            onClick = {
                capturePhotoStarted = true
                val photoSettings = AVCapturePhotoSettings.photoSettingsWithFormat(
                    format = mapOf(AVVideoCodecKey to AVVideoCodecTypeJPEG)
                )
                if (camera.position == AVCaptureDevicePositionFront) {
                    capturePhotoOutput.connectionWithMediaType(AVMediaTypeVideo)?.automaticallyAdjustsVideoMirroring = false
                    capturePhotoOutput.connectionWithMediaType(AVMediaTypeVideo)?.videoMirrored = true
                }
                capturePhotoOutput.capturePhotoWithSettings(
                    settings = photoSettings,
                    delegate = photoCaptureDelegate
                )
            }
        ) {
            Text(LocalLocalization.current.takePhoto)
        }
        if (capturePhotoStarted) {
            CircularProgressIndicator(
                modifier = Modifier.size(80.dp).align(Alignment.Center),
                color = Color.White.copy(alpha = 0.7f),
                strokeWidth = 8.dp,
            )
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
