import Foundation

// MARK: - Domain models (mirrors the Android domain module)

enum TransactionKind: String, CaseIterable, Identifiable {
    case cashSale, creditSale, mixedSale, customerReceipt, expense, purchase, supplierPayment, saleReturn, accountTransfer, cashAdjustment
    var id: String { rawValue }

    var title: String {
        switch self {
        case .cashSale: return "فروش نقدی"
        case .creditSale: return "فروش نسیه"
        case .mixedSale: return "فروش نقد و نسیه"
        case .customerReceipt: return "دریافت از مشتری"
        case .expense: return "هزینه"
        case .purchase: return "خرید کالا"
        case .supplierPayment: return "پرداخت به تأمین‌کننده"
        case .saleReturn: return "برگشت از فروش"
        case .accountTransfer: return "انتقال بین حساب‌ها"
        case .cashAdjustment: return "اصلاح صندوق"
        }
    }

    var isIncome: Bool {
        switch self {
        case .cashSale, .creditSale, .mixedSale, .customerReceipt: return true
        default: return false
        }
    }
}

enum SalePaymentMode: String, CaseIterable, Identifiable {
    case cash, credit, mixed
    var id: String { rawValue }
    var title: String {
        switch self {
        case .cash: return "نقدی"
        case .credit: return "نسیه"
        case .mixed: return "ترکیبی"
        }
    }
}

enum PaymentMethod: String, CaseIterable, Identifiable {
    case cash, card, pos, transfer
    var id: String { rawValue }
    var title: String {
        switch self {
        case .cash: return "نقدی"
        case .card: return "کارت به کارت"
        case .pos: return "کارتخوان"
        case .transfer: return "حواله"
        }
    }
}

enum ContactKind: String, CaseIterable, Identifiable {
    case customer, supplier, both
    var id: String { rawValue }
    var title: String {
        switch self {
        case .customer: return "مشتری"
        case .supplier: return "تأمین‌کننده"
        case .both: return "مشتری و تأمین‌کننده"
        }
    }
    var isCustomer: Bool { self != .supplier }
    var isSupplier: Bool { self != .customer }
}

struct Product: Identifiable, Hashable {
    let id: UUID
    var name: String
    var category: String
    var purchasePrice: Int64
    var sellingPrice: Int64
    var averageCost: Int64
    var stock: Double
    var minimumStock: Double
    var isLowStock: Bool { minimumStock > 0 && stock <= minimumStock }
    var stockValue: Int64 { Int64(stock * Double(averageCost)) }
}

struct Contact: Identifiable, Hashable {
    let id: UUID
    var name: String
    var phone: String?
    var kind: ContactKind
    /// Positive = customer owes us, negative = we owe the supplier.
    var balance: Int64
    var lastActivity: Date?
}

struct Transaction: Identifiable, Hashable {
    let id: UUID
    var code: String
    var kind: TransactionKind
    var amount: Int64
    var paidAmount: Int64
    var remainingAmount: Int64
    var contactName: String?
    var accountTitle: String?
    var date: Date
    var note: String?
    var isReversed: Bool = false
    var items: [SaleLine] = []

    var paymentStatus: String {
        if remainingAmount <= 0 { return "تسویه" }
        if paidAmount > 0 { return "پرداخت جزئی" }
        return "نسیه"
    }
}

struct SaleLine: Identifiable, Hashable {
    let id: UUID
    var productName: String
    var quantity: Double
    var unitPrice: Int64
    var total: Int64 { Int64(quantity * Double(unitPrice)) }
}

struct Account: Identifiable, Hashable {
    let id: UUID
    var title: String
    var type: String
    var balance: Int64
}

struct DailySnapshot {
    var sales: Int64
    var cashReceived: Int64
    var expenses: Int64
    var profit: Int64
    var receivable: Int64
    var payable: Int64
}
