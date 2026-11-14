import Foundation

/// Number, money and Jalali date formatting shared by all features.
enum PersianFormat {
    static let locale = Locale(identifier: "fa_IR")

    private static let moneyFormatter: NumberFormatter = {
        let formatter = NumberFormatter()
        formatter.locale = locale
        formatter.numberStyle = .decimal
        formatter.groupingSeparator = "٬"
        formatter.maximumFractionDigits = 0
        return formatter
    }()

    private static let quantityFormatter: NumberFormatter = {
        let formatter = NumberFormatter()
        formatter.locale = locale
        formatter.numberStyle = .decimal
        formatter.maximumFractionDigits = 3
        formatter.minimumFractionDigits = 0
        return formatter
    }()

    static func number(_ value: Int64) -> String {
        moneyFormatter.string(from: NSNumber(value: value)) ?? "\(value)"
    }

    static func money(_ value: Int64) -> String {
        "\(number(value)) تومان"
    }

    static func signedMoney(_ value: Int64) -> String {
        let prefix = value > 0 ? "+" : ""
        return "\(prefix)\(money(value))"
    }

    static func quantity(_ value: Double) -> String {
        quantityFormatter.string(from: NSNumber(value: value)) ?? "\(value)"
    }
}

/// Jalali (Solar Hijri) calendar helpers using Foundation's Persian calendar.
enum JalaliDate {
    static var calendar: Calendar = {
        var calendar = Calendar(identifier: .persian)
        calendar.locale = Locale(identifier: "fa_IR")
        calendar.timeZone = .current
        return calendar
    }()

    static var dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.calendar = calendar
        formatter.locale = Locale(identifier: "fa_IR")
        formatter.dateFormat = "d MMMM yyyy"
        return formatter
    }()

    static var shortFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.calendar = calendar
        formatter.locale = Locale(identifier: "fa_IR")
        formatter.dateFormat = "yyyy/MM/dd"
        return formatter
    }()

    static func long(_ date: Date) -> String { dateFormatter.string(from: date) }
    static func short(_ date: Date) -> String { shortFormatter.string(from: date) }

    static var today: String { long(Date()) }

    static func monthName(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.calendar = calendar
        formatter.locale = Locale(identifier: "fa_IR")
        formatter.dateFormat = "MMMM yyyy"
        return formatter.string(from: date)
    }
}
