import SwiftUI

struct DashboardView: View {
    @EnvironmentObject private var store: AppStore
    @State private var showSale = false
    @State private var showExpense = false

    private let columns = [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 14) {
                    header
                    heroCard
                    quickActions
                    statsGrid
                    recentTransactions
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 24)
            }
            .background(Brand.mist.ignoresSafeArea())
            .navigationBarHidden(true)
            .sheet(isPresented: $showSale) { SaleSheet().environmentObject(store) }
            .sheet(isPresented: $showExpense) { ExpenseSheet().environmentObject(store) }
        }
    }

    private var header: some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 2) {
                Text("حسابیار").font(AppFont.heavy(24)).foregroundStyle(Brand.deepGreen)
                Text("\(JalaliDate.today) · فروشگاه آفتاب").font(AppFont.regular(12)).foregroundStyle(Brand.secondaryText)
            }
            Spacer()
            Button {
                // Future: notification center
            } label: {
                Image(systemName: "bell")
                    .font(.system(size: 18, weight: .medium))
                    .foregroundStyle(Brand.deepGreen)
                    .frame(width: 42, height: 42)
                    .background(Brand.paper, in: Circle())
                    .overlay(Circle().stroke(Brand.hairline, lineWidth: 1))
            }
        }
        .padding(.top, 6)
    }

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("موجودی کل حساب‌ها").font(AppFont.regular(12)).foregroundStyle(.white.opacity(0.8))
                    Text(PersianFormat.money(store.cashBalance)).font(AppFont.heavy(26)).foregroundStyle(.white)
                }
                Spacer()
                Image(systemName: "wallet.bifold.fill")
                    .font(.system(size: 30, weight: .semibold))
                    .foregroundStyle(.white.opacity(0.9))
            }
            HStack(spacing: 8) {
                ForEach(store.accounts) { account in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(account.title).font(AppFont.medium(10)).foregroundStyle(.white.opacity(0.75))
                        Text(PersianFormat.number(account.balance)).font(AppFont.semibold(12)).foregroundStyle(.white)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(8)
                    .background(.white.opacity(0.12), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                }
            }
        }
        .padding(18)
        .background(
            LinearGradient(colors: [Brand.deepGreen, Brand.deepGreenDark], startPoint: .topLeading, endPoint: .bottomTrailing),
            in: RoundedRectangle(cornerRadius: 24, style: .continuous)
        )
        .overlay(alignment: .topLeading) {
            Circle().fill(.white.opacity(0.06)).frame(width: 160, height: 160).offset(x: -40, y: -70)
        }
        .clipped()
    }

    private var quickActions: some View {
        HStack(spacing: 8) {
            quickAction(title: "فروش", icon: "cart.badge.plus", tint: Brand.deepGreen) { showSale = true }
            quickAction(title: "هزینه", icon: "receipt", tint: Brand.expense) { showExpense = true }
            quickAction(title: "خرید", icon: "shippingbox.and.arrow.backward", tint: Brand.warning) { showSale = false }
            quickAction(title: "دریافت", icon: "arrow.down.circle", tint: Brand.income) { showSale = false }
        }
    }

    private func quickAction(title: String, icon: String, tint: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 6) {
                Image(systemName: icon).font(.system(size: 19, weight: .semibold)).foregroundStyle(tint)
                Text(title).font(AppFont.medium(11)).foregroundStyle(.primary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(Brand.paper, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(Brand.hairline, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }

    private var statsGrid: some View {
        LazyVGrid(columns: columns, spacing: 10) {
            StatTile(title: "فروش امروز", amount: store.snapshot.sales, icon: "cart", tint: Brand.income)
            StatTile(title: "دریافت امروز", amount: store.snapshot.cashReceived, icon: "arrow.down", tint: Brand.deepGreen)
            StatTile(title: "طلب از مشتریان", amount: store.receivable, icon: "person.crop.circle.badge.exclamationmark", tint: Brand.receivable)
            StatTile(title: "بدهی به تأمین‌کننده", amount: store.payable, icon: "person.crop.circle.badge.minus", tint: Brand.payable)
        }
    }

    private var recentTransactions: some View {
        VStack(spacing: 10) {
            SectionTitle(title: "آخرین تراکنش‌ها", action: "همه", onAction: {})
            if store.transactions.isEmpty {
                EmptyHint(icon: "tray", title: "هنوز تراکنشی نیست", message: "اولین فروش را ثبت کنید تا اینجا نمایش داده شود.")
            } else {
                VStack(spacing: 0) {
                    ForEach(Array(store.transactions.prefix(5).enumerated()), id: \.element.id) { index, tx in
                        TransactionRow(transaction: tx)
                        if index < min(store.transactions.count, 5) - 1 {
                            Divider().overlay(Brand.hairline)
                        }
                    }
                }
                .padding(.horizontal, 14)
                .background(Brand.paper, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: 20, style: .continuous).stroke(Brand.hairline, lineWidth: 1))
            }
        }
    }
}

struct TransactionRow: View {
    let transaction: Transaction

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(tint)
                .frame(width: 38, height: 38)
                .background(tint.opacity(0.12), in: Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(transaction.kind.title).font(AppFont.medium(13))
                Text(transaction.contactName ?? transaction.note ?? transaction.accountTitle ?? "")
                    .font(AppFont.regular(11))
                    .foregroundStyle(Brand.secondaryText)
                    .lineLimit(1)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 3) {
                Text(PersianFormat.number(transaction.amount))
                    .font(AppFont.semibold(14))
                    .foregroundStyle(transaction.kind.isIncome ? Brand.income : Brand.expense)
                Text(transaction.remainingAmount > 0 ? "نسیه" : transaction.paymentStatus)
                    .font(AppFont.medium(10))
                    .foregroundStyle(transaction.remainingAmount > 0 ? Brand.warning : Brand.secondaryText)
            }
        }
        .padding(.vertical, 10)
    }

    private var icon: String {
        switch transaction.kind {
        case .cashSale, .creditSale, .mixedSale: return "cart.fill"
        case .customerReceipt: return "arrow.down.circle.fill"
        case .expense: return "receipt.fill"
        case .purchase: return "shippingbox.fill"
        case .supplierPayment: return "arrow.up.circle.fill"
        case .saleReturn: return "arrow.uturn.backward.circle.fill"
        case .accountTransfer: return "arrow.left.arrow.right"
        case .cashAdjustment: return "slider.horizontal.3"
        }
    }

    private var tint: Color {
        transaction.kind.isIncome ? Brand.income : Brand.expense
    }
}
