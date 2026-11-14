import SwiftUI

struct SaleSheet: View {
    @EnvironmentObject private var store: AppStore
    @Environment(\.dismiss) private var dismiss

    @State private var quantities: [UUID: Double] = [:]
    @State private var mode: SalePaymentMode = .cash
    @State private var paidText = ""
    @State private var contactId: UUID?
    @State private var showProductPicker = false
    @State private var showSuccess = false

    private var lines: [SaleLine] {
        store.products.compactMap { product in
            let quantity = quantities[product.id] ?? 0
            guard quantity > 0 else { return nil }
            return SaleLine(id: product.id, productName: product.name, quantity: quantity, unitPrice: product.sellingPrice)
        }
    }

    private var total: Int64 { lines.reduce(0) { $0 + $1.total } }
    private var paid: Int64 {
        switch mode {
        case .cash: return total
        case .credit: return 0
        case .mixed: return min(total, Int64(PersianFormat.number(paidText).replacingOccurrences(of: "٬", with: "").replacingOccurrences(of: " تومان", with: "")) ?? 0)
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 14) {
                    AppCard {
                        VStack(alignment: .leading, spacing: 10) {
                            SectionTitle(title: "کالاها", action: "افزودن", onAction: { showProductPicker = true })
                            if lines.isEmpty {
                                EmptyHint(icon: "shippingbox", title: "کالایی انتخاب نشده", message: "برای ثبت فروش، حداقل یک کالا اضافه کنید.")
                            } else {
                                ForEach(lines) { line in
                                    HStack {
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(line.productName).font(AppFont.medium(13)).lineLimit(1)
                                            Text("\(PersianFormat.quantity(line.quantity)) × \(PersianFormat.number(line.unitPrice))")
                                                .font(AppFont.regular(11))
                                                .foregroundStyle(Brand.secondaryText)
                                        }
                                        Spacer()
                                        MoneyText(amount: line.total, font: AppFont.semibold(13))
                                    }
                                    Divider().overlay(Brand.hairline)
                                }
                            }
                        }
                    }

                    if !lines.isEmpty {
                        AppCard {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("روش پرداخت").font(AppFont.medium(13)).foregroundStyle(Brand.secondaryText)
                                Picker("روش پرداخت", selection: $mode) {
                                    ForEach(SalePaymentMode.allCases) { option in
                                        Text(option.title).tag(option)
                                    }
                                }
                                .pickerStyle(.segmented)

                                if mode != .cash {
                                    Picker("مشتری", selection: $contactId) {
                                        Text("انتخاب مشتری").tag(UUID?.none)
                                        ForEach(store.contacts.filter { $0.kind.isCustomer }) { contact in
                                            Text(contact.name).tag(UUID?.some(contact.id))
                                        }
                                    }
                                }

                                if mode == .mixed {
                                    TextField("مبلغ پرداخت‌شده", text: $paidText)
                                        .keyboardType(.numberPad)
                                        .font(AppFont.regular(14))
                                        .padding(12)
                                        .background(Brand.mist, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                }
                            }
                        }
                    }

                    AppCard {
                        VStack(spacing: 8) {
                            HStack {
                                Text("جمع فاکتور").font(AppFont.medium(14))
                                Spacer()
                                MoneyText(amount: total, font: AppFont.heavy(20), color: Brand.deepGreen)
                            }
                            HStack {
                                Text("مانده نسیه").font(AppFont.medium(13)).foregroundStyle(Brand.secondaryText)
                                Spacer()
                                MoneyText(amount: max(0, total - paid), font: AppFont.semibold(15), color: Brand.warning)
                            }
                        }
                    }

                    Button("ثبت فروش") {
                        let contact = store.contacts.first(where: { $0.id == contactId })
                        store.addSale(lines: lines, mode: mode, contact: contact, paid: paid)
                        showSuccess = true
                    }
                    .buttonStyle(PrimaryButtonStyle())
                    .disabled(lines.isEmpty)
                    .opacity(lines.isEmpty ? 0.5 : 1)
                }
                .padding(16)
            }
            .background(Brand.mist.ignoresSafeArea())
            .navigationTitle("ثبت فروش")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("بستن") { dismiss() }
                }
            }
            .sheet(isPresented: $showProductPicker) {
                ProductQuantityPicker(quantities: $quantities)
                    .environmentObject(store)
            }
            .alert("فروش ثبت شد", isPresented: $showSuccess) {
                Button("عالی") { dismiss() }
            } message: {
                Text("فاکتور به مبلغ \(PersianFormat.money(total)) ثبت و موجودی کالاها به‌روزرسانی شد.")
            }
        }
    }
}

struct ProductQuantityPicker: View {
    @EnvironmentObject private var store: AppStore
    @Binding var quantities: [UUID: Double]
    @Environment(\.dismiss) private var dismiss
    @State private var query = ""

    private var filtered: [Product] {
        query.isEmpty ? store.products : store.products.filter { $0.name.localizedCaseInsensitiveContains(query) }
    }

    var body: some View {
        NavigationStack {
            List {
                ForEach(filtered) { product in
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(product.name).font(AppFont.medium(14))
                                Text("موجودی: \(PersianFormat.quantity(product.stock)) · \(PersianFormat.money(product.sellingPrice))")
                                    .font(AppFont.regular(11))
                                    .foregroundStyle(Brand.secondaryText)
                            }
                            Spacer()
                            if product.isLowStock { StatusPill(text: "کم‌موجود", color: Brand.warning) }
                        }
                        HStack(spacing: 14) {
                            Button { decrease(product) } label: {
                                Image(systemName: "minus.circle.fill").font(.system(size: 24)).foregroundStyle(Brand.secondaryText)
                            }
                            .buttonStyle(.plain)
                            Text(PersianFormat.quantity(quantities[product.id] ?? 0))
                                .font(AppFont.semibold(15))
                                .frame(minWidth: 36)
                            Button { increase(product) } label: {
                                Image(systemName: "plus.circle.fill").font(.system(size: 24)).foregroundStyle(Brand.deepGreen)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
            .searchable(text: $query, prompt: "جست‌وجوی کالا")
            .navigationTitle("انتخاب کالا")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("انجام شد") { dismiss() }
                }
            }
        }
    }

    private func increase(_ product: Product) {
        let current = quantities[product.id] ?? 0
        quantities[product.id] = min(product.stock, current + 1)
    }

    private func decrease(_ product: Product) {
        let current = quantities[product.id] ?? 0
        quantities[product.id] = max(0, current - 1)
    }
}

struct ExpenseSheet: View {
    @EnvironmentObject private var store: AppStore
    @Environment(\.dismiss) private var dismiss
    @State private var title = ""
    @State private var amountText = ""
    @State private var saved = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 14) {
                TextField("عنوان هزینه", text: $title)
                    .font(AppFont.regular(14))
                    .padding(14)
                    .background(Brand.paper, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                TextField("مبلغ (تومان)", text: $amountText)
                    .keyboardType(.numberPad)
                    .font(AppFont.regular(14))
                    .padding(14)
                    .background(Brand.paper, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                Button("ثبت هزینه") {
                    let cleaned = amountText.replacingOccurrences(of: ",", with: "").replacingOccurrences(of: "٬", with: "")
                    store.addExpense(title: title.isEmpty ? "هزینه" : title, amount: Int64(cleaned) ?? 0)
                    saved = true
                }
                .buttonStyle(PrimaryButtonStyle(background: Brand.expense))
                .disabled(Int64(amountText) == nil && Int64(amountText.replacingOccurrences(of: ",", with: "")) == nil)
                Spacer()
            }
            .padding(16)
            .background(Brand.mist.ignoresSafeArea())
            .navigationTitle("ثبت هزینه")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("بستن") { dismiss() } } }
            .alert("هزینه ثبت شد", isPresented: $saved) {
                Button("باشه") { dismiss() }
            }
        }
    }
}
