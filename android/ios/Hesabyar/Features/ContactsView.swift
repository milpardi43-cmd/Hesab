import SwiftUI

struct ContactsView: View {
    @EnvironmentObject private var store: AppStore
    @State private var query = ""
    @State private var kind: ContactKind? = nil

    private var filtered: [Contact] {
        store.contacts.filter { contact in
            let matchesKind: Bool
            switch kind {
            case .customer?: matchesKind = contact.kind.isCustomer
            case .supplier?: matchesKind = contact.kind.isSupplier
            case .both?: matchesKind = contact.kind == .both
            case nil: matchesKind = true
            }
            let matchesQuery = query.isEmpty || contact.name.localizedCaseInsensitiveContains(query) || (contact.phone?.contains(query) ?? false)
            return matchesKind && matchesQuery
        }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                HStack(spacing: 8) {
                    chip("همه", selected: kind == nil) { kind = nil }
                    chip("مشتریان", selected: kind == .customer) { kind = .customer }
                    chip("تأمین‌کنندگان", selected: kind == .supplier) { kind = .supplier }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)

                List(filtered) { contact in
                    HStack(spacing: 10) {
                        Text(String(contact.name.prefix(1)))
                            .font(AppFont.bold(16))
                            .foregroundStyle(Brand.deepGreen)
                            .frame(width: 42, height: 42)
                            .background(Brand.deepGreen.opacity(0.12), in: Circle())
                        VStack(alignment: .leading, spacing: 3) {
                            Text(contact.name).font(AppFont.medium(14))
                            Text(contact.phone ?? contact.kind.title).font(AppFont.regular(11)).foregroundStyle(Brand.secondaryText)
                        }
                        Spacer()
                        VStack(alignment: .trailing, spacing: 2) {
                            MoneyText(
                                amount: abs(contact.balance),
                                font: AppFont.semibold(14),
                                color: contact.balance > 0 ? Brand.receivable : (contact.balance < 0 ? Brand.payable : Brand.secondaryText)
                            )
                            Text(contact.balance > 0 ? "بدهکار" : (contact.balance < 0 ? "بستانکار" : "تسویه"))
                                .font(AppFont.medium(10))
                                .foregroundStyle(Brand.secondaryText)
                        }
                    }
                    .padding(.vertical, 3)
                    .listRowBackground(Brand.paper)
                    .listRowSeparatorTint(Brand.hairline)
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
            }
            .background(Brand.mist.ignoresSafeArea())
            .navigationTitle("اشخاص")
            .searchable(text: $query, prompt: "جست‌وجوی نام یا موبایل")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        // Future: contact form
                    } label: { Image(systemName: "plus") }
                }
            }
        }
    }

    private func chip(_ title: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(title, action: action)
            .font(AppFont.medium(12))
            .padding(.horizontal, 14)
            .padding(.vertical, 7)
            .background(selected ? Brand.deepGreen : Brand.paper, in: Capsule())
            .foregroundStyle(selected ? .white : .primary)
            .overlay(Capsule().stroke(selected ? Color.clear : Brand.hairline, lineWidth: 1))
    }
}
