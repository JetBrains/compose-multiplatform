import UIKit
import SwiftUI
import shared
import AVKit
import CoreLocation

struct ComposeView: UIViewControllerRepresentable {
    private let openCamera: () -> ()
    init(openCamera: @escaping () -> ()) {
        self.openCamera = openCamera
    }

    func makeUIViewController(context: Context) -> UIViewController {
        Main_iosKt.MainViewController(openCamera: self.openCamera)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}


struct ContentView: View {
    @State var cameraScreen:Bool = false
    var body: some View {
        if(cameraScreen) {
            CameraScreen(closeHandler: { cameraScreen = false })
        } else {
            ComposeView(openCamera: { cameraScreen = true })
                    .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
        }
    }
}


struct CameraScreen: View {
    private let closeHandler: () -> ()
    
    init(closeHandler: @escaping () -> Void) {
        self.closeHandler = closeHandler
    }
    
    var body: some View {
        VStack {
            Text("Camera screen")
            UIKitCamera(closeHandler: closeHandler)
        }
    }
}


struct UIKitCamera : UIViewControllerRepresentable {
    private let closeHandler: () -> ()
    
    init(closeHandler: @escaping () -> Void) {
        self.closeHandler = closeHandler
    }
    
    func makeUIViewController(context: Context) -> UIViewController {
        let vc = CameraUIViewController()
        vc.closeHandler = self.closeHandler
        return vc
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {

    }
}


final class CameraUIViewController: UIViewController {
    typealias VoidFunc = () -> ()
    var closeHandler: VoidFunc?
    
    let captureSession = AVCaptureSession()
    let locationManager = CLLocationManager()
    
    var camera: AVCaptureDevice?
    
    var lastCapturedImage: UIImage?
    var capturePhotoOutput: AVCapturePhotoOutput!
    var cameraPreviewLayer: AVCaptureVideoPreviewLayer!
    
    private let imageStorage = ImageStorage()
    
    private let cameraContainer = UIView()
    private let captureButton: UIButton = {
        let button = UIButton()
        button.contentHorizontalAlignment = .fill
        button.contentVerticalAlignment = .fill
        button.setImage(UIImage.init(systemName: "camera.circle.fill")!.withTintColor(.gray, renderingMode: .alwaysOriginal), for: .normal)
        return button
    }()
    private var closeButton: UIButton = {
        let button = UIButton()
        button.contentHorizontalAlignment = .fill
        button.contentVerticalAlignment = .fill
        button.setImage(UIImage.init(systemName: "xmark.circle.fill")!.withTintColor(.gray, renderingMode: .alwaysOriginal), for: .normal)
        return button
    }()
    
    override func loadView() {
        super.loadView()
        let contentView = UIView()
        contentView.backgroundColor = .purple
        self.view = contentView

        [cameraContainer, captureButton, closeButton].forEach{
            contentView.addSubview($0)
            $0.translatesAutoresizingMaskIntoConstraints = false
        }
        
        [cameraContainer.topAnchor.constraint(equalTo: contentView.topAnchor),
         cameraContainer.bottomAnchor.constraint(equalTo: contentView.bottomAnchor),
         cameraContainer.leadingAnchor.constraint(equalTo: contentView.leadingAnchor),
         cameraContainer.trailingAnchor.constraint(equalTo: contentView.trailingAnchor)]
            .forEach { $0.isActive = true }
        
        [captureButton.centerXAnchor.constraint(equalTo: cameraContainer.centerXAnchor),
         captureButton.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -48),
         captureButton.heightAnchor.constraint(equalToConstant: 56),
         captureButton.widthAnchor.constraint(equalToConstant: 56)]
            .forEach { $0.isActive = true }
        
        [closeButton.leadingAnchor.constraint(equalTo: cameraContainer.leadingAnchor, constant: 16),
         closeButton.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 16),
         closeButton.heightAnchor.constraint(equalToConstant: 36),
         closeButton.widthAnchor.constraint(equalToConstant: 36)]
            .forEach { $0.isActive = true }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        closeButton.addTarget(self, action: #selector(closeController), for: .touchUpInside)
        captureButton.addTarget(self, action: #selector(capture), for: .touchUpInside)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        #if targetEnvironment(simulator)
        showAlert(for: .simulatorUsed)
        #else
        DispatchQueue.global().async {
            self.configureCaptureSessionIfAllowed()
            self.configureLocationServicesIfAllowed()
        }
        #endif
    }
    
    private func configureCaptureSessionIfAllowed() {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            configureCamera()
            
        case .denied, .restricted:
            showAlert(for: .cameraPermissionDenied)
            
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video, completionHandler: { permissionGranted in
                if permissionGranted {
                    DispatchQueue.main.async {
                        self.configureCamera()
                    }
                }
            })
            
        @unknown default:
            showAlert(for: .unknownAuthStatus)
        }
    }
    
    private func configureCamera() {
        captureSession.sessionPreset = .photo
        
        AVCaptureDevice.DiscoverySession(deviceTypes: [.builtInDualCamera, .builtInDualWideCamera, .builtInTelephotoCamera, .builtInTripleCamera, .builtInTrueDepthCamera, .builtInUltraWideCamera, .builtInWideAngleCamera], mediaType: .video, position: .back).devices.forEach{ device in
            if device.position == .back {
                camera = device
            }
        }
        
        guard let camera = camera, let captureDeviceInput = try? AVCaptureDeviceInput(device: camera) else { return }
        
        capturePhotoOutput = AVCapturePhotoOutput()
        
        captureSession.addInput(captureDeviceInput)
        captureSession.addOutput(capturePhotoOutput)
        
        DispatchQueue.main.async {
            self.cameraPreviewLayer = AVCaptureVideoPreviewLayer(session: self.captureSession)
            self.cameraContainer.layer.addSublayer(self.cameraPreviewLayer)
            self.cameraPreviewLayer?.videoGravity = .resizeAspectFill
            self.cameraPreviewLayer?.frame = self.view.layer.frame
        }
        
        captureSession.startRunning()
    }
    
    @objc private func capture() {
        let photoSettings = AVCapturePhotoSettings(format: [AVVideoCodecKey: AVVideoCodecType.jpeg])
        photoSettings.isHighResolutionPhotoEnabled = true
        let currentMetadata = photoSettings.metadata
//        photoSettings.metadata = currentMetadata.merging(getLocationMetadata(), uniquingKeysWith: { _, geoMetaDataKey -> Any in return geoMetaDataKey })
        
        capturePhotoOutput.isHighResolutionCaptureEnabled = true
        capturePhotoOutput.capturePhoto(with: photoSettings, delegate: self)
    }
    
    @objc private func closeController() {
        closeHandler?()
    }
    
    private func showAlert(for type: AlertType) {
        // TODO: This won't work due to CameraUIViewController.view is not in the windows hierarchy. Navigation should be fixed.
        let alert = UIAlertController(title: "Warning", message: type.message, preferredStyle: .alert)
        alert.addAction(.init(title: "Ok", style: .default, handler: { [weak self] action in
            self?.closeHandler?()
        }))
        
        self.present(alert, animated: true)
    }
    
    private func getLocationMetadata() -> [String: Any] {
        var metadata = [String: Any]()
        
        guard let location = locationManager.location else {
            print("ImageViewer: couldn't get location, geo metadata will be empty")
            return [:]
        }
        
        metadata = [
            kCGImagePropertyGPSLatitude as String: location.coordinate.latitude,
            kCGImagePropertyGPSLongitude as String: location.coordinate.longitude,
            kCGImagePropertyGPSAltitudeRef as String: 0,
            kCGImagePropertyGPSAltitude as String: location.altitude,
            kCGImagePropertyGPSTimeStamp as String: location.timestamp,
            kCGImagePropertyGPSDateStamp as String: location.timestamp,
        ]
        
        return metadata
    }
}

extension CameraUIViewController: AVCapturePhotoCaptureDelegate {
    func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        guard let photoData = photo.fileDataRepresentation() else { return }
        lastCapturedImage = UIImage(data: photoData)
        imageStorage.set(imageData: photoData)
    }
}

extension CameraUIViewController: CLLocationManagerDelegate {
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        switch status {
        case .authorizedAlways, .authorizedWhenInUse:
            configureLocationServices()
        default:
            print("ImageViewer: location permission wasn't granted, photos will be without location information")
            return
        }
    }
    
    private func configureLocationServicesIfAllowed() {
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.requestWhenInUseAuthorization()
    }
    
    private func configureLocationServices() {
        if CLLocationManager.locationServicesEnabled() {
            locationManager.startUpdatingLocation()
        }
    }
}

fileprivate extension CameraUIViewController {
    enum AlertType {
        case simulatorUsed
        case cameraPermissionDenied
        case unknownAuthStatus
        
        var message: String {
            switch self {
            case .cameraPermissionDenied: return "Permission of camera usage should be granted"
            case .simulatorUsed: return "Camera is not available on simulator, please use real device"
            case .unknownAuthStatus: return "Unknown camera permission status"
            }
        }
    }
}

fileprivate class ImageStorage {
    private let fm = FileManager.default
    private let df: DateFormatter = {
        let df = DateFormatter()
        df.dateFormat = "yyyy-MM-dd-mm-ss.SSS"
        return df
    }()
    
    private static let relativePath = "ImageViewer/takenPhotos/"
    
    init() {
        guard let pathUrl = getRelativePathUrl() else {
            return
        }
        
        let fileExists = fm.fileExists(atPath: pathUrl.path)
        guard !fileExists else {
            return
        }
        
        do {
            try fm.createDirectory(at: pathUrl, withIntermediateDirectories: true)
        } catch {
            print("ImageViewer: Error while creating directory for storaging photos, please see description:\n\(error.localizedDescription)")
        }
    }
    
    func set(imageData: Data) {
        let fileName = df.string(from: Date()) + "_taken.jpg"
        guard let fileUrl = makeFileUrl(for: fileName) else { return }
        do {
            try imageData.write(to: fileUrl)
        } catch {
            print("ImageViewer: Error while saving photo, please see description:\n\(error.localizedDescription)")
        }
    }
    
    func getAll() -> [UIImage]? {
        guard let imagesUrl = getRelativePathUrl() else { return nil }
        
        var resultArray = [UIImage?]()
        do {
            let imageUrls = try fm.contentsOfDirectory(atPath: imagesUrl.path)
            for imageUrlString in imageUrls {
                DispatchQueue.global(qos: .utility).async {
                    let image = self.get(imagePathString: imageUrlString)
                    DispatchQueue.global().async(flags: .barrier) {
                        resultArray.append(image)
                    }
                }
            }
        } catch {
            print("ImageViewer: Error while fetching images from fileStorage, see description:\n\(error.localizedDescription)")
            return nil
        }
        return resultArray.compactMap{$0}
    }
    
    func get(imageName: String) -> UIImage? {
        guard let url = makeFileUrl(for: imageName), let data = try? Data(contentsOf: url) else { return nil }
        return UIImage(data: data)
    }
    
    func get(imagePathString: String) -> UIImage? {
        guard let url = URL(string: imagePathString), let data = try? Data(contentsOf: url) else { return nil }
        return UIImage(data: data)
    }
    
    private func makeFileUrl(for name: String) -> URL? {
        guard let url = getRelativePathUrl() else { return nil }
        if #available(iOS 16, *) {
            return url.appending(component: name)
        } else {
            return url.appendingPathComponent(name)
        }
    }
    
    private func getRelativePathUrl() -> URL? {
        guard let url = fm.urls(for: .documentDirectory, in: .userDomainMask).first else { return nil }
        if #available(iOS 16, *) {
            return url.appending(component: Self.relativePath)
        } else {
            return url.appendingPathComponent(Self.relativePath)
        }
    }
}
