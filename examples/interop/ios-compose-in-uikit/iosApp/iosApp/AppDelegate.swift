import Foundation
import UIKit
import shared

@main
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        UITabBarItem.appearance().setTitleTextAttributes([NSAttributedString.Key.font: UIFont.systemFont(ofSize: 15)], for: .normal)

        let composeViewController = Main_iosKt.ComposeEntryPoint()
        composeViewController.title = "Compose Multiplatform inside UIKit"

        let anotherViewController = UIKitViewController()
        anotherViewController.title = "UIKit"

        // Set up the UITabBarController
        let tabBarController = UITabBarController()
        tabBarController.viewControllers = [
            // Wrap them in a UINavigationController for the titles
            UINavigationController(rootViewController: composeViewController),
            UINavigationController(rootViewController: anotherViewController)
        ]
        tabBarController.tabBar.items?[0].title = "Compose"
        tabBarController.tabBar.items?[1].title = "UIKit"

        // Set the tab bar controller as the window's root view controller and make it visible
        window = UIWindow(frame: UIScreen.main.bounds)
        window?.rootViewController = tabBarController
        window?.makeKeyAndVisible()

        return true
    }

}

class UIKitViewController: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()
        let label = UILabel()
        label.translatesAutoresizingMaskIntoConstraints = false
        label.text = "UIKitViewController"
        label.textAlignment = .center
        label.numberOfLines = 0
        view.addSubview(label)
        view.backgroundColor = UIColor { collection in
            switch collection.userInterfaceStyle {
            case .light: return UIColor.white
            case .dark: return UIColor.black
            default: return UIColor.white
            }
        }

        NSLayoutConstraint.activate([
            label.centerYAnchor.constraint(equalTo: self.view.centerYAnchor),
            label.leadingAnchor.constraint(equalTo: self.view.leadingAnchor, constant: 20),
            label.trailingAnchor.constraint(equalTo: self.view.trailingAnchor, constant: -20)
        ])
    }

}
