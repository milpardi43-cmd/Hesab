package ir.hesabyar.app.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabyar.core.design.components.AppCard
import ir.hesabyar.core.design.components.EmptyState
import ir.hesabyar.core.design.components.HesabyarTopBar
import ir.hesabyar.core.design.components.InfoRow
import ir.hesabyar.core.design.components.LoadingScreen
import ir.hesabyar.core.design.components.MoneyText
import ir.hesabyar.core.design.components.SecondaryButton
import ir.hesabyar.core.design.components.StatusBadge
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.format.PersianText
import ir.hesabyar.domain.model.Product
import ir.hesabyar.domain.model.StockMovement
import ir.hesabyar.domain.usecase.GetStockMovementsUseCase
import ir.hesabyar.domain.usecase.ObserveProductUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeProduct: ObserveProductUseCase,
    getMovements: GetStockMovementsUseCase
) : ViewModel() {
    private val productId: Long = savedStateHandle.get<Long>("productId") ?: 0L

    val product: StateFlow<Product?> = observeProduct(productId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val movements: StateFlow<List<StockMovement>> = getMovements(productId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun ProductDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onAdjustStock: (Long) -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val product by viewModel.product.collectAsStateWithLifecycle()
    val movements by viewModel.movements.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HesabyarTopBar(
                title = product?.name ?: "جزئیات کالا",
                onBack = onBack,
                actions = {
                    product?.let {
                        IconButton(onClick = { onEdit(it.id) }) { Icon(Icons.Rounded.Edit, contentDescription = "ویرایش") }
                    }
                }
            )
        }
    ) { padding ->
        val current = product
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding)) { LoadingScreen() }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(54.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                            }
                            Spacer(Modifier.size(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(current.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    current.categoryName ?: "بدون دسته",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HesabyarTheme.colors.onSurfaceSecondary
                                )
                            }
                            if (current.isLowStock) StatusBadge("کم‌موجود", HesabyarTheme.colors.warning)
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text("موجودی فعلی", style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
                                Text(
                                    PersianText.formatQuantity(current.stockQuantity),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = if (current.isLowStock) HesabyarTheme.colors.warning else MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text("ارزش انبار", style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
                                MoneyText(amount = current.stockValue, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
                item {
                    AppCard {
                        Column {
                            InfoRow("قیمت خرید", value = PersianText.formatMoney(current.purchasePrice))
                            InfoRow("قیمت فروش", value = PersianText.formatMoney(current.sellingPrice))
                            InfoRow("بهای تمام‌شده (میانگین)", value = PersianText.formatMoney(current.averageCost))
                            InfoRow("حداقل موجودی", value = PersianText.formatQuantity(current.minimumStock))
                            current.code?.let { InfoRow("کد کالا", value = PersianText.toPersianDigits(it)) }
                            current.description?.takeIf { it.isNotBlank() }?.let { InfoRow("توضیحات", value = it) }
                        }
                    }
                }
                item {
                    SecondaryButton(
                        text = "اصلاح موجودی",
                        onClick = { onAdjustStock(current.id) },
                        icon = Icons.Rounded.Tune,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item { Text("گردش انبار", style = MaterialTheme.typography.titleMedium) }
                if (movements.isEmpty()) {
                    item { EmptyState(title = "گردشی ثبت نشده", message = "با خرید یا فروش، گردش موجودی این کالا نمایش داده می‌شود.") }
                } else {
                    items(movements, key = { it.id }) { movement ->
                        AppCard(contentPadding = PaddingValues(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(movement.type.label, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        JalaliDate.formatLongDate(movement.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HesabyarTheme.colors.onSurfaceSecondary
                                    )
                                    movement.note?.let {
                                        Text(it, style = MaterialTheme.typography.labelSmall, color = HesabyarTheme.colors.onSurfaceSecondary)
                                    }
                                }
                                Text(
                                    text = (if (movement.quantityChange > 0) "+" else "") + PersianText.formatQuantity(movement.quantityChange),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (movement.quantityChange >= 0) HesabyarTheme.colors.income else HesabyarTheme.colors.expense
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
