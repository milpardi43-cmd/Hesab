import SwiftUI

// MARK: - Brand palette (matches Android Color.kt)

enum Brand {
    static let deepGreen = Color(hex: 0x0E5A43)
    static let deepGreenDark = Color(hex: 0x083D2D)
    static let mint = Color(hex: 0x22A06B)
    static let income = Color(hex: 0x1F9D62)
    static let expense = Color(hex: 0xD64545)
    static let warning = Color(hex: 0xE08A00)
    static let mist = Color(hex: 0xF4F6F3)
    static let hairline = Color(hex: 0xE1E6E1)
    static let secondaryText = Color(hex: 0x5C6661)
    static let paper = Color.white
    static let receivable = Color(hex: 0x16794F)
    static let payable = Color(hex: 0xB43B3B)
}

extension Color {
    init(hex: UInt32) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: 1
        )
    }
}

enum AppFont {
    static func regular(_ size: CGFloat) -> Font { .custom("Vazirmatn", size: size) }
    static func medium(_ size: CGFloat) -> Font { .custom("Vazirmatn-Medium", size: size) }
    static func semibold(_ size: CGFloat) -> Font { .custom("Vazirmatn-SemiBold", size: size) }
    static func bold(_ size: CGFloat) -> Font { .custom("Vazirmatn-Bold", size: size) }
    static func heavy(_ size: CGFloat) -> Font { .custom("Vazirmatn-ExtraBold", size: size) }
}

// MARK: - Reusable components

struct AppCard<Content: View>: View {
    var padding: CGFloat = 16
    @ViewBuilder var content: Content

    var body: some View {
        content
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Brand.paper)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(Brand.hairline, lineWidth: 1)
            )
    }
}

struct MoneyText: View {
    let amount: Int64
    var font: Font = AppFont.semibold(17)
    var color: Color = .primary
    var showCurrency = false
    var signed = false

    var body: some View {
        Text(signed ? PersianFormat.signedMoney(amount) : (showCurrency ? PersianFormat.money(amount) : PersianFormat.number(amount)))
            .font(font)
            .foregroundStyle(color)
            .lineLimit(1)
            .minimumScaleFactor(0.7)
    }
}

struct StatTile: View {
    let title: String
    let amount: Int64
    let icon: String
    let tint: Color

    var body: some View {
        AppCard(padding: 14) {
            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 8) {
                    Image(systemName: icon)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(tint)
                        .frame(width: 28, height: 28)
                        .background(tint.opacity(0.14), in: Circle())
                    Text(title)
                        .font(AppFont.medium(12))
                        .foregroundStyle(Brand.secondaryText)
                        .lineLimit(1)
                }
                MoneyText(amount: amount, font: AppFont.semibold(17), color: .primary)
            }
        }
    }
}

struct StatusPill: View {
    let text: String
    let color: Color

    var body: some View {
        Text(text)
            .font(AppFont.medium(10))
            .foregroundStyle(color)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(color.opacity(0.12), in: Capsule())
    }
}

struct SectionTitle: View {
    let title: String
    var action: String? = nil
    var onAction: (() -> Void)? = nil

    var body: some View {
        HStack {
            Text(title).font(AppFont.semibold(17))
            Spacer()
            if let action, let onAction {
                Button(action, action: onAction)
                    .font(AppFont.medium(13))
                    .foregroundStyle(Brand.deepGreen)
            }
        }
    }
}

struct EmptyHint: View {
    let icon: String
    let title: String
    let message: String
    var body: some View {
        VStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 30, weight: .regular))
                .foregroundStyle(Brand.deepGreen)
                .frame(width: 68, height: 68)
                .background(Brand.deepGreen.opacity(0.1), in: Circle())
            Text(title).font(AppFont.semibold(16))
            Text(message)
                .font(AppFont.regular(13))
                .foregroundStyle(Brand.secondaryText)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 28)
    }
}

struct PrimaryButtonStyle: ButtonStyle {
    var background: Color = Brand.deepGreen
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(AppFont.semibold(15))
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(background.opacity(configuration.isPressed ? 0.85 : 1), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}
