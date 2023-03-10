import UIKit
import SwiftUI
import shared

public func sendMessage(_ text: String) {
    Main_iosKt.sendMessage(text: text)
}

public func gradient3Colors() -> [Color] {
    return Main_iosKt.gradient3Colors().map { hex in
        Color(getCGColor(hex.intValue)).opacity(1.0)
    }
}

public func surfaceColor() -> Color {
    Color(getCGColor(Int(Main_iosKt.surfaceColor())))
}

private func getCGColor(_ argb: Int) -> CGColor {
    func clr(_ component: Int) -> CGFloat {
        CGFloat(component & 0xff) / 255.0
    }

    return CGColor(red: clr(argb >> 16), green: clr(argb >> 8), blue: clr(argb), alpha: clr(argb >> 24))
}
