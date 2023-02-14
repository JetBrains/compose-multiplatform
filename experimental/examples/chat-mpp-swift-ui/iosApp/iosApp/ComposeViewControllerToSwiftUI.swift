/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import UIKit
import SwiftUI
import shared

struct ComposeViewControllerToSwiftUI: UIViewControllerRepresentable {
    private let onInteract: (InteractResult) -> ()
    init(onInteract: @escaping (InteractResult) -> ()) {
        self.onInteract = onInteract
    }

    func makeUIViewController(context: Context) -> UIViewController {
        return Main_iosKt.MainViewController(/*onInteract: { interactData in
            onInteract(InteractResult.init(Color(getCGColor(Int(interactData.swiftUIColor))), interactData.darkTheme))
        }*/)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {

    }

    func sizeThatFits(_ proposal: ProposedViewSize, uiViewController: UIViewControllerType, context: Context) -> CGSize? {
        return uiViewController.view.frame.size
    }
}

public struct InteractResult {
    public let swiftUIColor:Color
    public let darkTheme:Bool

    public init(_ swiftUIColor: Color, _ darkTheme: Bool) {
        self.swiftUIColor = swiftUIColor
        self.darkTheme = darkTheme
    }
}

func getCGColor(_ argb:Int) -> CGColor {
    func clr(_ component: Int) -> CGFloat {
        CGFloat(component & 0xff) / 255.0
    }
    return CGColor(red: clr(argb >> 16), green: clr(argb >> 8), blue: clr(argb), alpha: clr(argb >> 24))
}

public func sendMessage(_ text:String) {
    Main_iosKt.sendMessage(text: text)
}
