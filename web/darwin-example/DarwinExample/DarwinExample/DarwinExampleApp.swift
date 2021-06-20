//
//  DarwinExampleApp.swift
//  DarwinExample
//
//  Created by Tadeas Kriz on 16.06.2021.
//

import SwiftUI
import darwin_example

@main
struct DarwinExampleApp: App {
    var body: some Scene {
        WindowGroup {
            ZStack {
                ComposableView()
            }
        }
    }
}

class RootView: UIView {
    override func layoutSubviews() {
        super.layoutSubviews()

        subviews.forEach { subview in
            subview.frame = bounds
        }
    }
}

struct ComposableView: UIViewRepresentable {
    func makeUIView(context: Context) -> some UIView {
        return RootView()
    }

    func updateUIView(_ uiView: UIViewType, context: Context) {
        IosApp().attachMain(view: uiView)
    }
}
