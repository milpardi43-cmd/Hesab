package ir.hesabyar.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabyar.core.design.theme.DeepGreenDark
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.usecase.GetBusinessUseCase
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val getBusiness: GetBusinessUseCase
) : androidx.lifecycle.ViewModel() {
    suspend fun hasBusiness(): Boolean = getBusiness.hasBusiness()
}

@Composable
fun SplashScreen(
    onReady: (Boolean) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        delay(1300)
        onReady(viewModel.hasBusiness())
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, DeepGreenDark))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ح",
                    color = Color.White,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.height(20.dp))
            Text("حسابیار", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(
                "حساب‌وکتاب فروشگاهت، همیشه همراهت",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(36.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                color = Color.White.copy(alpha = 0.85f),
                strokeWidth = 2.5.dp
            )
        }
        Text(
            "نسخه ۱٫۰٫۰",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.65f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp)
        )
    }
}

private data class OnboardingPage(val icon: ImageVector, val title: String, val description: String)

private val PAGES = listOf(
    OnboardingPage(
        Icons.Rounded.ReceiptLong,
        "فروش و هزینه را سریع ثبت کن",
        "در چند ثانیه فروش نقدی یا نسیه، خرید کالا و هزینه‌های روزانه را ثبت کنید؛ حسابیار محاسبات را دقیق انجام می‌دهد."
    ),
    OnboardingPage(
        Icons.Rounded.Inventory2,
        "انبار و حساب‌ها همیشه شفاف",
        "موجودی کالاها، بهای تمام‌شده، صندوق و حساب بانکی به‌صورت خودکار به‌روز می‌شوند و هیچ عددی گم نمی‌شود."
    ),
    OnboardingPage(
        Icons.Rounded.BarChart,
        "بدان چقدر سود کرده‌ای",
        "گزارش روزانه و ماهانه، بدهی مشتریان، طلب تأمین‌کنندگان و سود واقعی فروشگاه را در یک نگاه ببینید."
    )
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onFinish) { Text("رد کردن") }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { pageIndex ->
            val page = PAGES[pageIndex]
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(page.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(66.dp))
                }
                Spacer(Modifier.height(32.dp))
                Text(page.title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Text(
                    page.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = HesabyarTheme.colors.onSurfaceSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.Center) {
            PAGES.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(7.dp)
                        .width(if (index == pagerState.currentPage) 22.dp else 7.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                )
            }
        }
        Button(
            onClick = {
                if (pagerState.currentPage == PAGES.lastIndex) {
                    onFinish()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 16.dp).height(54.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(if (pagerState.currentPage == PAGES.lastIndex) "شروع استفاده" else "ادامه")
        }
    }
}
