import SwiftUI

@main
struct HesabyarApp: App {
    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(\.layoutDirection, .rightToLeft)
                .preferredColorScheme(.light)
        }
    }
}

struct RootView: View {
    @StateObject private var store = AppStore()
    @State private var selection = 0

    var body: some View {
        TabView(selection: $selection) {
            DashboardView()
                .environmentObject(store)
                .tabItem { Label("خانه", systemImage: "house.fill") }
                .tag(0)

            TransactionsView()
                .environmentObject(store)
                .tabItem { Label("تراکنش‌ها", systemImage: "list.bullet.rectangle.portrait") }
                .tag(1)

            ProductsView()
                .environmentObject(store)
                .tabItem { Label("کالاها", systemImage: "shippingbox.fill") }
                .tag(2)

            ContactsView()
                .environmentObject(store)
                .tabItem { Label("اشخاص", systemImage: "person.2.fill") }
                .tag(3)

            ReportsView()
                .environmentObject(store)
                .tabItem { Label("گزارش‌ها", systemImage: "chart.bar.fill") }
                .tag(4)
        }
        .tint(Brand.deepGreen)
    }
}
