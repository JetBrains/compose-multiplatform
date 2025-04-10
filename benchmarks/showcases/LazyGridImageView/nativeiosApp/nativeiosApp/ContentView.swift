import SwiftUI
import Foundation
import UIKit
import Combine

struct ContentView: View {
    // in testing mode it is allowed to have not all images downloaded
    private let testingMode: Bool = false

    @State private var imageNames: [String] = []
    @State private var autoScroll: Bool = false
    @State private var scrollPosition: Int = 0
    @State private var scrollingDown: Bool = true
    @State private var timer: AnyCancellable?

    private let columns = [
        GridItem(.flexible()),
        GridItem(.flexible()),
        GridItem(.flexible())
    ]

    var body: some View {
        VStack {
            // Toggle for auto-scrolling
            Toggle(isOn: $autoScroll) {
                Text("Auto Scroll")
            }
            .padding(.horizontal)
            .onChange(of: autoScroll) { newValue in
                if newValue {
                    startAutoScroll()
                } else {
                    stopAutoScroll()
                }
            }

            ScrollViewReader { proxy in
                ScrollView {
                    // Grid with 3 columns
                    LazyVGrid(columns: columns, spacing: 8) {
                        ForEach(0..<imageNames.count, id: \.self) { index in
                            GridItemView(imageName: imageNames[index])
                                .aspectRatio(1, contentMode: .fill)
                                .id(index) // Add id for ScrollViewReader
                        }
                    }
                    .padding(8)
                }
                .onChange(of: scrollPosition) { newPosition in
                    // Scroll to the new position when scrollPosition changes
                    withAnimation(.linear) {
                        proxy.scrollTo(newPosition, anchor: .top)
                    }
                }
            }
        }
        .onAppear {
            // Load image names when the view appears
            loadImageNames()
        }
        .onDisappear {
            // Stop auto-scrolling when view disappears
            stopAutoScroll()
        }
    }


    private func loadImageNames() {
        let numOfImages = 999
        if testingMode {
            var existingImages: [String] = []
            (1...numOfImages).forEach { index in
                let name = "downloaded_image\(String(format: "%03d", index))"
                if (UIImage(named: name) != nil) {
                    existingImages.append(name)
                }
            }

            if (existingImages.count == 0) {
                return
            }

            imageNames = (0..<numOfImages).map { index in
                existingImages[index % existingImages.count]
            }
        } else {
            imageNames = (0..<numOfImages).map { index in
                "downloaded_image\(String(format: "%03d", index + 1))"
            }
        }
    }

    private func startAutoScroll() {
        // Cancel any existing timer
        stopAutoScroll()

        timer = Timer.publish(every: 0.1, on: .main, in: .common)
            .autoconnect()
            .sink { _ in
                self.updateScrollPosition()
            }
    }

    private func stopAutoScroll() {
        timer?.cancel()
        timer = nil
    }

    private func updateScrollPosition() {
        if scrollingDown {
            // Scrolling down
            if scrollPosition < imageNames.count - columns.count - 1 {
                scrollPosition += columns.count
            } else {
                // Reached bottom, change direction
                scrollingDown = false
            }
        } else {
            // Scrolling up
            if scrollPosition > 0 {
                scrollPosition -= columns.count
            } else {
                // Reached top, change direction
                scrollingDown = true
            }
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
