import UIKit
import SwiftUI
import shared
import AVKit
import WebKit

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
    
    var camera: AVCaptureDevice?
    
    var capturedImage: UIImage?
    var capturePhotoOutput: AVCapturePhotoOutput!
    var cameraPreviewLayer: AVCaptureVideoPreviewLayer!
    var webView: WKWebView!
    
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
        
        #if targetEnvironment(simulator)
        showAlert(for: .simulatorUsed)
        #else
        DispatchQueue.global().async {
            self.configureSessionIfAllowed()
        }
        #endif
    }
    
    private func configureSessionIfAllowed() {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            configureCamera()
            
        case .denied, .restricted:
            showAlert(for: .permissionDenied)
            
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
}

extension CameraUIViewController: AVCapturePhotoCaptureDelegate {
    func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        guard let photoData = photo.fileDataRepresentation() else { return }
        capturedImage = UIImage(data: photoData)
    }
}

fileprivate extension CameraUIViewController {
    enum AlertType {
        case simulatorUsed
        case permissionDenied
        case unknownAuthStatus
        
        var message: String {
            switch self {
            case .permissionDenied: return "Permission of camera usage should be granted"
            case .simulatorUsed: return "Camera is not available on simulator, please use real device"
            case .unknownAuthStatus: return "Unknown camera permission status"
            }
        }
    }
}
