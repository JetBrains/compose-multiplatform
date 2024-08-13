import SwiftUI

struct GradientTemplate<Content: View>: View {
    var title: String
    var content: () -> Content

    var body: some View {
        NavigationView {
            ZStack {
                surfaceColor()
                VStack {
                    gradient.ignoresSafeArea(edges: .top).frame(height: 0)
                    Spacer()
                }
                content()
                VStack {
                    Spacer()
                    Rectangle().frame(height: 0).background(gradient)
                }
            }
                .navigationTitle(title)
                .navigationBarTitleDisplayMode(.inline)
                .statusBar(hidden: false)
        }
    }
}
