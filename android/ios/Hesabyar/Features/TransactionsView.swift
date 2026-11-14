import SwiftUI

struct TransactionsView: View {
    @EnvironmentObject private var store: AppStore
    @State private var query = ""
    @State private var filter: Filter = .all

    enum Filter: String, CaseIterable, Identifiable {
        case all, sales, expenses, debts
        var id: String { rawValue }
        var title: String {
            switch self {
            case .all: return "همه"
            case .sales: return "فروش"
            case .expenses: return "هزینه‌ها"
            case .debts: return "نسیه‌ها"
            }
        }
    }

    private var filtered: [Transaction] {
        store.transactions.filter { tx in
            let matchesQuery = query.isEmpty ||
                (tx.contactName?.localizedCaseInsensitiveContains(query) ?? false) ||
                (tx.note?.localizedCaseInsensitiveContains(query) ?? false) ||
                tx.code.localizedCaseInsensitiveContains(query)
            let matchesFilter: Bool
            switch filter {
            case .all: matchesFilter = true
            case .sales: matchesFilter = [.cashSale, .creditSale, .mixedSale].contains(tx.kind)
            case .expenses: matchesFilter = [.expense, .purchase, .supplierPayment].contains(tx.kind)
            case .debts: matchesFilter = tx.remainingAmount > 0
            }
            return matchesQuery && matchesFilter
        }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(Filter.allCases) { option in
                            Button(option.title) { filter = option }
                                .font(AppFont.medium(12))
                                .padding(.horizontal, 14)
                                .padding(.vertical, 7)
                                .background(filter == option ? Brand.deepGreen : Brand.paper, in: Capsule())
                                .foregroundStyle(filter == option ? .white : .primary)
                                .overlay(Capsule().stroke(filter == option ? Color.clear : Brand.hairline, lineWidth: 1))
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                }

                if filtered.isEmpty {
                    Spacer()
                    EmptyHint(icon: "magnifyingglass", title: "تراکنشی پیدا نشد", message: "فیلتر یا عبارت جست‌وجو را تغییر دهید.")
                    Spacer()
                } else {
                    List(filtered) { tx in
                        TransactionRow(transaction: tx)
                            .listRowBackground(Brand.paper)
                            .listRowSeparatorTint(Brand.hairline)
                    }
                    .listStyle(.plain)
                    .scrollContentBackground(.hidden)
                }
            }
            .background(Brand.mist.ignoresSafeArea())
            .navigationTitle("تراکنش‌ها")
            .searchable(text: $query, prompt: "جست‌وجوی تراکنش")
        }
    }
}
