import SwiftUI
import UIKit

struct ComposeInsideSwiftUIScreen: View {
    var body: some View {
        ZStack {
            ComposeLayer()
            ChatFooterScrimLayer()
            ChatHeaderLayer()
            TextInputLayer()
        }.onTapGesture {
            UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
        }
        .onAppear {
            UIApplication.shared.statusBarStyle = .lightContent
        }
        .onDisappear {
            UIApplication.shared.statusBarStyle = .default
        }
    }
}

struct ComposeLayer: View {
    var body: some View {
        ComposeViewControllerToSwiftUI()
            .ignoresSafeArea()
    }
}

struct ChatHeaderLayer: View {
    static let extraTopInset: CGFloat = 100

    var body: some View {
        VStack(spacing: 0) {
            ZStack(alignment: .top) {
                LinearGradient(
                    colors: [
                        Color.black.opacity(0.75),
                        Color.black.opacity(0.45),
                        Color.black.opacity(0)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 200)
                .ignoresSafeArea(edges: .top)
                .allowsHitTesting(false)

                VStack(spacing: 10) {
                    HStack {
                        ChatBackButton()
                        Spacer()
                        OverlappingAvatarGroup()
                        Spacer()
                        VideoCallButton()
                    }
                    ChatTitlePill(title: "The Composers Chat")
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
            }

            Spacer()
        }
    }
}

private struct ChatBackButton: View {
    var body: some View {
        Button(action: {}) {
            HStack(spacing: 6) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 15, weight: .semibold))
                Text("2")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(.black)
                    .frame(width: 18, height: 18)
                    .background(Circle().fill(Color.white))
            }
            .foregroundColor(.white)
            .padding(.leading, 12)
            .padding(.trailing, 8)
            .padding(.vertical, 8)
            .modifier(LiquidGlassCapsule())
        }
        .buttonStyle(.plain)
    }
}

private struct VideoCallButton: View {
    var body: some View {
        Button(action: {}) {
            Image(systemName: "video.fill")
                .font(.system(size: 17, weight: .medium))
                .foregroundColor(.white)
                .frame(width: 44, height: 44)
                .modifier(LiquidGlassCircle())
        }
        .buttonStyle(.plain)
    }
}

private struct ChatTitlePill: View {
    let title: String

    var body: some View {
        Button(action: {}) {
            HStack(spacing: 4) {
                Text(title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.white)
                Image(systemName: "chevron.right")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(.white.opacity(0.7))
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 6)
            .modifier(LiquidGlassCapsule())
        }
        .buttonStyle(.plain)
    }
}

private struct OverlappingAvatarGroup: View {
    private let size: CGFloat = 26
    private let overlap: CGFloat = 9

    var body: some View {
        ZStack {
            avatar("stock2")
                .offset(x: -overlap, y: overlap * 0.6)
            avatar("stock3")
                .offset(x: overlap, y: overlap * 0.6)
            avatar("stock1")
                .offset(y: -overlap * 0.6)
        }
        .frame(width: size + overlap * 2, height: size + overlap * 2)
    }

    // Path mirrors where the Compose Multiplatform Gradle plugin copies shared module resources into the app bundle; it'll need updating if the shared module's name ever changes.
    private static let composeResourcesDir = "compose-resources/composeResources/chat_mpp.shared.generated.resources/drawable"

    private func avatar(_ imageName: String) -> some View {
        Group {
            if let path = Bundle.main.path(forResource: imageName, ofType: "jpg", inDirectory: Self.composeResourcesDir),
               let uiImage = UIImage(contentsOfFile: path) {
                Image(uiImage: uiImage)
                    .resizable()
                    .scaledToFill()
            } else {
                Color.gray
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .overlay(Circle().stroke(Color.white.opacity(0.85), lineWidth: 1.5))
    }
}

private struct LiquidGlassCapsule: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content.glassEffect(.regular.tint(.black.opacity(0.35)).interactive(), in: Capsule())
        } else {
            content.background(Capsule().fill(.ultraThinMaterial))
        }
    }
}

private struct LiquidGlassCircle: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content.glassEffect(.regular.tint(.black.opacity(0.35)).interactive(), in: Circle())
        } else {
            content.background(Circle().fill(.ultraThinMaterial))
        }
    }
}

private struct ChatFooterScrimLayer: View {
    var body: some View {
        LinearGradient(
            colors: [
                Color.black.opacity(0),
                Color.black.opacity(0.2),
                Color.black.opacity(0.4)
            ],
            startPoint: .top,
            endPoint: .bottom
        )
        .frame(height: 80)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
        .ignoresSafeArea(edges: .bottom)
        .allowsHitTesting(false)
    }
}

struct TextInputLayer: View {
    @State private var textState: String = ""
    @FocusState private var textFieldFocused: Bool

    var body: some View {
        VStack {
            Spacer()
            HStack {
                TextField("Type message…", text: $textState, axis: .vertical)
                    .focused($textFieldFocused)
                    .lineLimit(3)
                if (!textState.isEmpty) {
                    Button(action: {
                        sendMessage(textState)
                        textFieldFocused = false
                        textState = ""
                    }) {
                        Image(systemName: "arrow.up.circle.fill")
                            .tint(Color(red: 0.671, green: 0.365, blue: 0.792))
                    }
                }
            }.padding(15).background(RoundedRectangle(cornerRadius: 200).fill(.white).opacity(0.95)).padding(15)
        }
    }
}
