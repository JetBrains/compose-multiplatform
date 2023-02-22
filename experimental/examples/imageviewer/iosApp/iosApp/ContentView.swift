import UIKit
import SwiftUI
import shared

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

class CameraUIViewController: UIViewController {
    typealias VoidFunc = () -> ()
    var closeHandler: VoidFunc?
    
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
    
    @objc func capture() {
        
    }
    
    @objc func closeController() {
        closeHandler?()
    }
}
