import SwiftUI

struct ProductsView: View {
    @EnvironmentObject private var store: AppStore
    @State private var query = ""
    @State private var selected: Product?

    private var filtered: [Product] {
        query.isEmpty ? store.products : store.products.filter { $0.name.localizedCaseInsensitiveContains(query) || $0.category.localizedCaseInsensitiveContains(query) }
    }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(filtered) { product in
                        Button { selected = product } label: {
                            HStack(spacing: 10) {
                                Image(systemName: "shippingbox.fill")
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundStyle(Brand.deepGreen)
                                    .frame(width: 40, height: 40)
                                    .background(Brand.deepGreen.opacity(0.1), in: Circle())
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(product.name).font(AppFont.medium(13)).lineLimit(1)
                                    HStack(spacing: 6) {
                                        Text("موجودی \(PersianFormat.quantity(product.stock))")
                                        Text("·")
                                        Text(PersianFormat.money(product.sellingPrice))
                                    }
                                    .font(AppFont.regular(11))
                                    .foregroundStyle(Brand.secondaryText)
                                }
                                Spacer()
                                if product.isLowStock { StatusPill(text: "کم‌موجود", color: Brand.warning) }
                            }
                            .padding(.vertical, 3)
                        }
                        .buttonStyle(.plain)
                        .listRowBackground(Brand.paper)
                        .listRowSeparatorTint(Brand.hairline)
                    }
                } header: {
                    HStack {
                        Text("\(filtered.count) کالا").font(AppFont.medium(12))
                        Spacer()
                        Text("ارزش انبار: \(PersianFormat.money(store.products.reduce(0) { $0 + $1.stockValue }))")
                            .font(AppFont.medium(12))
                            .foregroundStyle(Brand.deepGreen)
                    }
                }
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .background(Brand.mist.ignoresSafeArea())
            .navigationTitle("کالاها")
            .searchable(text: $query, prompt: "جست‌وجوی کالا")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        // Future: product form
                    } label: { Image(systemName: "plus") }
                }
            }
            .sheet(item: $selected) { product in
                ProductDetailSheet(product: product)
            }
        }
    }
}

struct ProductDetailSheet: View {
    let product: Product
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 14) {
                    AppCard {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(product.name).font(AppFont.heavy(20))
                            Text(product.category).font(AppFont.regular(12)).foregroundStyle(Brand.secondaryText)
                            Divider().overlay(Brand.hairline)
                            row("قیمت فروش", PersianFormat.money(product.sellingPrice))
                            row("قیمت خرید", PersianFormat.money(product.purchasePrice))
                            row("میانگین بهای تمام‌شده", PersianFormat.money(product.averageCost))
                            row("موجودی", PersianFormat.quantity(product.stock))
                            row("حداقل موجودی", PersianFormat.quantity(product.minimumStock))
                            row("ارزش موجودی", PersianFormat.money(product.stockValue))
                        }
                    }
                    if product.isLowStock {
                        AppCard {
                            HStack {
                                Image(systemName: "exclamationmark.triangle.fill").foregroundStyle(Brand.warning)
                                Text("این کالا به حداقل موجودی رسیده است. برای جلوگیری از اتمام، سفارش خرید ثبت کنید.")
                                    .font(AppFont.regular(12))
                                    .foregroundStyle(Brand.secondaryText)
                            }
                        }
                    }
                }
                .padding(16)
            }
            .background(Brand.mist.ignoresSafeArea())
            .navigationTitle("جزئیات کالا")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("بستن") { dismiss() } } }
        }
    }

    private func row(_ title: String, _ value: String) -> some View {
        HStack {
            Text(title).font(AppFont.regular(13)).foregroundStyle(Brand.secondaryText)
            Spacer()
            Text(value).font(AppFont.medium(13))
        }
    }
}
