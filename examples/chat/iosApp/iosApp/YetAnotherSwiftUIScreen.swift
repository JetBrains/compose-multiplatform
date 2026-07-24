import SwiftUI

struct YetAnotherSwiftUIScreen: View {
    var body: some View {
        NavigationView {
            VStack {
                Text("Yet another SwiftUI screen")
            }
        }
        .navigationTitle("Swift UI View")
        .navigationBarTitleDisplayMode(.inline)
    }
}
