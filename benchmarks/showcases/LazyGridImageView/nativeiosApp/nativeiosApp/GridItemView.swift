import SwiftUI
import UIKit

struct GridItemView: View {
    let imageName: String

    var body: some View {
        let imageUrl =  Bundle.main.url(forResource: imageName, withExtension: "jpg")
        ZStack {
            AsyncImage(url: imageUrl) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .frame(minWidth: 0, maxWidth: .infinity, minHeight: 0, maxHeight: .infinity)
                default:
                    Color.gray
                }
            }
        }
        .background(Color.white)
        .cornerRadius(8)
    }
}

struct GridItemView_Previews: PreviewProvider {
    static var previews: some View {
        GridItemView(imageName: "downloaded_image001")
            .frame(width: 150, height: 150)
            .previewLayout(.sizeThatFits)
    }
}
