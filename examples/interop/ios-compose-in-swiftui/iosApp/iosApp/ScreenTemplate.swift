import SwiftUI

struct ScreenTemplate<Content: View>: View {
    var title: String
    var content: () -> Content

    var body: some View {
        NavigationView {
            ZStack {
                VStack {
                    Spacer()
                }
                content()
                VStack {
                    Spacer()
                }
            }
                .navigationTitle(title)
                .navigationBarTitleDisplayMode(.inline)
                .statusBar(hidden: false)
        }
    }
}
