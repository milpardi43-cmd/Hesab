import Foundation

/// In-memory store for the iOS preview build.
/// The next milestone replaces this with SwiftData models that mirror the Android Room schema.
final class AppStore: ObservableObject {
    @Published var products: [Product]
    @Published var contacts: [Contact]
    @Published var transactions: [Transaction]
    @Published var accounts: [Account]

    init(
        products: [Product] = SampleData.products,
        contacts: [Contact] = SampleData.contacts,
        transactions: [Transaction] = SampleData.transactions,
        accounts: [Account] = SampleData.accounts
    ) {
        self.products = products
        self.contacts = contacts
        self.transactions = transactions
        self.accounts = accounts
    }

    var cashBalance: Int64 { accounts.reduce(0) { $0 + $1.balance } }

    var receivable: Int64 { contacts.filter { $0.balance > 0 }.reduce(0) { $0 + $1.balance } }
    var payable: Int64 { contacts.filter { $0.balance < 0 }.reduce(0) { $0 + (-$1.balance) } }

    var todayTransactions: [Transaction] {
        transactions.filter { Calendar.current.isDateInToday($0.date) }
    }

    var snapshot: DailySnapshot {
        let sales = todayTransactions
            .filter { [.cashSale, .creditSale, .mixedSale].contains($0.kind) }
            .reduce(0) { $0 + $1.amount }
        let received = todayTransactions.filter { $0.kind == .customerReceipt }.reduce(0) { $0 + $1.amount }
        let expenses = todayTransactions.filter { $0.kind == .expense }.reduce(0) { $0 + $1.amount }
        return DailySnapshot(
            sales: sales,
            cashReceived: received,
            expenses: expenses,
            profit: Int64(Double(sales) * 0.28) - expenses,
            receivable: receivable,
            payable: payable
        )
    }

    func addSale(lines: [SaleLine], mode: SalePaymentMode, contact: Contact?, paid: Int64) {
        let total = lines.reduce(0) { $0 + $1.total }
        let paidAmount: Int64 = mode == .cash ? total : (mode == .credit ? 0 : min(paid, total))
        let kind: TransactionKind = mode == .cash ? .cashSale : (mode == .credit ? .creditSale : .mixedSale)
        let transaction = Transaction(
            id: UUID(),
            code: "T-\(Int.random(in: 1000...9999))",
            kind: kind,
            amount: total,
            paidAmount: paidAmount,
            remainingAmount: total - paidAmount,
            contactName: contact?.name,
            accountTitle: "صندوق",
            date: Date(),
            note: nil,
            items: lines
        )
        transactions.insert(transaction, at: 0)
        if paidAmount > 0, let index = accounts.firstIndex(where: { $0.title == "صندوق" }) {
            accounts[index].balance += paidAmount
        }
        if total - paidAmount > 0, let contactId = contact?.id, let index = contacts.firstIndex(where: { $0.id == contactId }) {
            contacts[index].balance += total - paidAmount
        }
        for line in lines {
            if let index = products.firstIndex(where: { $0.name == line.productName }) {
                products[index].stock = max(0, products[index].stock - line.quantity)
            }
        }
    }

    func addExpense(title: String, amount: Int64) {
        transactions.insert(
            Transaction(
                id: UUID(),
                code: "E-\(Int.random(in: 1000...9999))",
                kind: .expense,
                amount: amount,
                paidAmount: amount,
                remainingAmount: 0,
                contactName: nil,
                accountTitle: "صندوق",
                date: Date(),
                note: title
            ),
            at: 0
        )
        if let index = accounts.firstIndex(where: { $0.title == "صندوق" }) {
            accounts[index].balance -= amount
        }
    }
}

enum SampleData {
    static let accounts: [Account] = [
        Account(id: UUID(), title: "صندوق", type: "نقدی", balance: 18_450_000),
        Account(id: UUID(), title: "بانک ملت", type: "بانک", balance: 42_800_000),
        Account(id: UUID(), title: "کارتخوان", type: "کارتخوان", balance: 6_250_000)
    ]

    static let products: [Product] = [
        Product(id: UUID(), name: "روغن آفتابگردان ۱٫۸ لیتری", category: "خواربار", purchasePrice: 720_000, sellingPrice: 890_000, averageCost: 725_000, stock: 18, minimumStock: 5),
        Product(id: UUID(), name: "برنج ایرانی هاشمی ۵ کیلویی", category: "خواربار", purchasePrice: 3_850_000, sellingPrice: 4_450_000, averageCost: 3_870_000, stock: 7, minimumStock: 4),
        Product(id: UUID(), name: "چای کیسه‌ای ۱۰۰ عددی", category: "نوشیدنی", purchasePrice: 480_000, sellingPrice: 620_000, averageCost: 485_000, stock: 3, minimumStock: 6),
        Product(id: UUID(), name: "شیر پرچرب ۱ لیتری", category: "لبنیات", purchasePrice: 320_000, sellingPrice: 395_000, averageCost: 322_000, stock: 24, minimumStock: 10),
        Product(id: UUID(), name: "دستمال کاغذی ۳۰۰ برگ", category: "بهداشتی", purchasePrice: 210_000, sellingPrice: 285_000, averageCost: 212_000, stock: 42, minimumStock: 12)
    ]

    static let contacts: [Contact] = [
        Contact(id: UUID(), name: "علی رضایی", phone: "09121234567", kind: .customer, balance: 4_250_000, lastActivity: Date().addingTimeInterval(-3600)),
        Contact(id: UUID(), name: "مریم احمدی", phone: "09351112233", kind: .customer, balance: 1_180_000, lastActivity: Date().addingTimeInterval(-86400)),
        Contact(id: UUID(), name: "پخش مواد غذایی البرز", phone: "02155443322", kind: .supplier, balance: -8_900_000, lastActivity: Date().addingTimeInterval(-172800)),
        Contact(id: UUID(), name: "شرکت لبنیات دامداران", phone: "02177889900", kind: .supplier, balance: -2_350_000, lastActivity: Date().addingTimeInterval(-259200))
    ]

    static let transactions: [Transaction] = {
        let today = Date()
        let yesterday = Calendar.current.date(byAdding: .day, value: -1, to: today)!
        return [
            Transaction(
                id: UUID(), code: "T-14030824-0003", kind: .cashSale, amount: 1_780_000, paidAmount: 1_780_000,
                remainingAmount: 0, contactName: nil, accountTitle: "صندوق", date: today,
                note: "فروش روزانه", items: [
                    SaleLine(id: UUID(), productName: "روغن آفتابگردان ۱٫۸ لیتری", quantity: 2, unitPrice: 890_000)
                ]
            ),
            Transaction(
                id: UUID(), code: "T-14030824-0002", kind: .creditSale, amount: 2_450_000, paidAmount: 500_000,
                remainingAmount: 1_950_000, contactName: "علی رضایی", accountTitle: nil, date: today,
                note: "خرید خانوادگی", items: [
                    SaleLine(id: UUID(), productName: "برنج ایرانی هاشمی ۵ کیلویی", quantity: 1, unitPrice: 4_450_000)
                ]
            ),
            Transaction(
                id: UUID(), code: "T-14030824-0001", kind: .expense, amount: 350_000, paidAmount: 350_000,
                remainingAmount: 0, contactName: nil, accountTitle: "صندوق", date: today, note: "کرایه حمل بار"
            ),
            Transaction(
                id: UUID(), code: "T-14030823-0007", kind: .customerReceipt, amount: 1_000_000, paidAmount: 1_000_000,
                remainingAmount: 0, contactName: "مریم احمدی", accountTitle: "صندوق", date: yesterday, note: "تسویه بخشی از بدهی"
            ),
            Transaction(
                id: UUID(), code: "T-14030823-0006", kind: .purchase, amount: 12_500_000, paidAmount: 3_600_000,
                remainingAmount: 8_900_000, contactName: "پخش مواد غذایی البرز", accountTitle: "بانک ملت", date: yesterday,
                note: "خرید هفتگی"
            )
        ]
    }()
}
