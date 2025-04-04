import SwiftUI
import UIKit

struct GridItemView: View {
    let imageName: String

    var body: some View {
        ZStack {
            Image(imageName)
                .resizable()
                .frame(minWidth: 0, maxWidth: .infinity, minHeight: 0, maxHeight: .infinity)
        }
        .background(Color.white)
        .cornerRadius(8)
    }
}

struct GridItemView_Previews: PreviewProvider {
    static var previews: some View {
        GridItemView(imageName: "image001")
            .frame(width: 150, height: 150)
            .previewLayout(.sizeThatFits)
    }
}
