import SwiftUI
import Foundation
import UIKit

struct ContentView: View {
    // State to hold the list of image names
    @State private var imageNames: [String] = []

    // Define grid layout with 3 columns
    private let columns = [
        GridItem(.flexible()),
        GridItem(.flexible()),
        GridItem(.flexible())
    ]

    var body: some View {
        ScrollView {
            // Grid with 3 columns
            LazyVGrid(columns: columns, spacing: 8) {
                ForEach(0..<imageNames.count, id: \.self) { index in
                    GridItemView(imageName: imageNames[index])
                        .aspectRatio(1, contentMode: .fill)
                }
            }
            .padding(8)
        }
        .onAppear {
            // Load image names when the view appears
            loadImageNames()
        }
    }

    private func loadImageNames() {
        var existingImages: [String] = []
        (1...1000).forEach { index in
            let name = "image\(String(format: "%03d", index))"
            if (UIImage(named: name) != nil) {
                existingImages.append(name)
            }
        }
        
        if (existingImages.count == 0) {
            return
        }
     
        imageNames = (0..<1000).map { index in
            existingImages[index % existingImages.count]
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
