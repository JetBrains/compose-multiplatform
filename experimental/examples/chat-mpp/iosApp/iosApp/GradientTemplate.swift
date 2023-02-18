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
                VStack {
                    content().frame(maxHeight: .infinity)
                    Rectangle().fill(Color.clear).frame(height: 0).background(gradient)
                }.ignoresSafeArea(.keyboard, edges: .bottom)
            }
                    .navigationTitle(title)
                    .navigationBarTitleDisplayMode(.inline)
                    .statusBar(hidden: false)
        }
            .toolbar(.visible, for: .tabBar)
    }
}
