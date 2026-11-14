package ir.hesabyar.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.hesabyar.core.design.components.AppCard
import ir.hesabyar.core.design.components.AppLogo
import ir.hesabyar.core.design.components.HesabyarTopBar
import ir.hesabyar.core.design.components.SectionHeader
import ir.hesabyar.core.design.theme.HesabyarTheme

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HesabyarTopBar(title = "درباره حسابیار", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    AppLogo(size = 84.dp)
                    Spacer(Modifier.height(10.dp))
                    Text("حسابیار", style = MaterialTheme.typography.headlineMedium)
                    Text("حساب‌وکتاب کسب‌وکار شما، ساده و دقیق", style = MaterialTheme.typography.bodyMedium, color = HesabyarTheme.colors.onSurfaceSecondary)
                    Text("نسخه ۱٫۰٫۰", style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
                }
            }
            item {
                AppCard {
                    Column {
                        Text("داستان ما", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "حسابیار برای فروشگاه‌های کوچک ایران ساخته شده است؛ جایی که صاحب کسب‌وکار باید در چند ثانیه فروش را ثبت کند، موجودی را بداند و بداند چقدر سود کرده است — بدون نیاز به دانش حسابداری و بدون اینترنت.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HesabyarTheme.colors.onSurfaceSecondary
                        )
                    }
                }
            }
            item {
                SectionHeader(title = "چه چیزهایی دارید؟")
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeatureRow(Icons.Rounded.ReceiptLong, "فروش نقدی، نسیه و ترکیبی با محاسبه خودکار سود")
                        FeatureRow(Icons.Rounded.Inventory2, "انبار، میانگین بهای تمام‌شده و هشدار کمبود")
                        FeatureRow(Icons.Rounded.AccountBalanceWallet, "صندوق، بانک و انتقال وجه با سابقه کامل")
                        FeatureRow(Icons.Rounded.BarChart, "گزارش روزانه و ماهانه، بدهی مشتریان و تأمین‌کنندگان")
                        FeatureRow(Icons.Rounded.CloudOff, "کاملاً آفلاین با پشتیبان‌گیری و بازیابی")
                    }
                }
            }
            item {
                SectionHeader(title = "حریم خصوصی")
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text("داده‌های شما روی دستگاه خودتان می‌ماند", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
                        }
                        Text(
                            "حسابیار اطلاعات فروشگاه را به هیچ سرور خارجی ارسال نمی‌کند. پشتیبان‌گیری فقط زمانی انجام می‌شود که خودتان فایل را بسازید یا بازیابی کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = HesabyarTheme.colors.onSurfaceSecondary
                        )
                    }
                }
            }
            item {
                SectionHeader(title = "پشتیبانی")
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("پشتیبانی حسابیار", style = MaterialTheme.typography.bodyLarge)
                        Text("support@hesabyar.app", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        Text("ساخته‌شده برای کسب‌وکارهای ایرانی", style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
                    }
                }
            }
            item {
                Text(
                    "© ${ir.hesabyar.domain.calendar.JalaliDate.today().year} حسابیار — تمامی حقوق محفوظ است.",
                    style = MaterialTheme.typography.labelSmall,
                    color = HesabyarTheme.colors.onSurfaceSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = HesabyarTheme.colors.income, modifier = Modifier.size(18.dp))
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp).padding(start = 6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
    }
}
