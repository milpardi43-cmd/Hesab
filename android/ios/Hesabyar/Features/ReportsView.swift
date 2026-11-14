import SwiftUI
import Charts

struct ReportsView: View {
    @EnvironmentObject private var store: AppStore
    @State private var period = 7

    private var series: [(day: String, sales: Int64)] {
        let calendar = Calendar.current
        return (0..<period).reversed().map { offset in
            let date = calendar.date(byAdding: .day, value: -offset, to: Date())!
            let sales = store.transactions
                .filter { calendar.isDate($0.date, inSameDayAs: date) && [.cashSale, .creditSale, .mixedSale].contains($0.kind) }
                .reduce(0) { $0 + $1.amount }
            return (JalaliDate.short(date).suffix(5).description, sales)
        }
    }

    private var topProducts: [(name: String, sold: Double)] {
        var totals: [String: Double] = [:]
        for tx in store.transactions where [.cashSale, .creditSale, .mixedSale].contains(tx.kind) {
            for line in tx.items {
                totals[line.productName, default: 0] += line.quantity
            }
        }
        return totals.sorted { $0.value > $1.value }.prefix(4).map { ($0.key, $0.value) }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 14) {
                    AppCard {
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Text("روند فروش").font(AppFont.semibold(16))
                                Spacer()
                                Picker("دوره", selection: $period) {
                                    Text("۷ روز").tag(7)
                                    Text("۱۴ روز").tag(14)
                                    Text("۳۰ روز").tag(30)
                                }
                                .pickerStyle(.segmented)
                                .frame(width: 190)
                            }
                            if series.allSatisfy({ $0.sales == 0 }) {
                                EmptyHint(icon: "chart.bar", title: "داده‌ای برای نمایش نیست", message: "با ثبت فروش، نمودار روند شکل می‌گیرد.")
                            } else {
                                Chart(series, id: \.day) { point in
                                    BarMark(
                                        x: .value("روز", point.day),
                                        y: .value("فروش", point.sales)
                                    )
                                    .foregroundStyle(Brand.deepGreen)
                                    .cornerRadius(5)
                                }
                                .frame(height: 190)
                                .chartYAxis {
                                    AxisMarks(position: .leading) { value in
                                        AxisGridLine().foregroundStyle(Brand.hairline)
                                        AxisValueLabel {
                                            if let amount = value.as(Int64.self) {
                                                Text(PersianFormat.number(amount / 1_000_000) + "م")
                                                    .font(AppFont.regular(9))
                                            }
                                        }
                                    }
                                }
                                .chartXAxis {
                                    AxisMarks { value in
                                        AxisValueLabel {
                                            if let label = value.as(String.self) {
                                                Text(label).font(AppFont.regular(9))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AppCard {
                        VStack(alignment: .leading, spacing: 10) {
                            Text("خلاصه امروز").font(AppFont.semibold(16))
                            summaryRow("فروش", store.snapshot.sales, Brand.income)
                            summaryRow("دریافت نقدی", store.snapshot.cashReceived, Brand.deepGreen)
                            summaryRow("هزینه‌ها", store.snapshot.expenses, Brand.expense)
                            summaryRow("سود تقریبی", store.snapshot.profit, Brand.deepGreen)
                            Divider().overlay(Brand.hairline)
                            summaryRow("طلب از مشتریان", store.receivable, Brand.receivable)
                            summaryRow("بدهی به تأمین‌کنندگان", store.payable, Brand.payable)
                        }
                    }

                    AppCard {
                        VStack(alignment: .leading, spacing: 10) {
                            Text("پرفروش‌ترین کالاها").font(AppFont.semibold(16))
                            if topProducts.isEmpty {
                                Text("هنوز فروشی ثبت نشده است.").font(AppFont.regular(12)).foregroundStyle(Brand.secondaryText)
                            } else {
                                ForEach(Array(topProducts.enumerated()), id: \.offset) { index, item in
                                    HStack {
                                        Text("\(index + 1)").font(AppFont.bold(12)).foregroundStyle(Brand.deepGreen)
                                            .frame(width: 22, height: 22).background(Brand.deepGreen.opacity(0.1), in: Circle())
                                        Text(item.name).font(AppFont.regular(13)).lineLimit(1)
                                        Spacer()
                                        Text(PersianFormat.quantity(item.sold)).font(AppFont.semibold(13))
                                    }
                                }
                            }
                        }
                    }

                    AppCard {
                        VStack(alignment: .leading, spacing: 10) {
                            Text("بدهی‌های مهم").font(AppFont.semibold(16))
                            ForEach(store.contacts.filter { $0.balance != 0 }.sorted { abs($0.balance) > abs($1.balance) }.prefix(4)) { contact in
                                HStack {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(contact.name).font(AppFont.medium(13))
                                        Text(contact.balance > 0 ? "طلب ما" : "بدهی ما")
                                            .font(AppFont.regular(10))
                                            .foregroundStyle(Brand.secondaryText)
                                    }
                                    Spacer()
                                    MoneyText(amount: abs(contact.balance), font: AppFont.semibold(13), color: contact.balance > 0 ? Brand.receivable : Brand.payable)
                                }
                            }
                        }
                    }
                }
                .padding(16)
            }
            .background(Brand.mist.ignoresSafeArea())
            .navigationTitle("گزارش‌ها")
        }
    }

    private func summaryRow(_ title: String, _ amount: Int64, _ color: Color) -> some View {
        HStack {
            Text(title).font(AppFont.regular(13)).foregroundStyle(Brand.secondaryText)
            Spacer()
            MoneyText(amount: amount, font: AppFont.semibold(14), color: color)
        }
    }
}
