package ir.hesabyar.app.ui.products

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabyar.core.design.components.EmptyState
import ir.hesabyar.core.design.components.HesabyarTopBar
import ir.hesabyar.core.design.components.MoneyText
import ir.hesabyar.core.design.components.SearchField
import ir.hesabyar.core.design.components.StatusBadge
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.format.PersianText
import ir.hesabyar.domain.model.Product
import ir.hesabyar.domain.model.ProductCategory
import ir.hesabyar.domain.model.ProductFilter
import ir.hesabyar.domain.usecase.GetCategoriesUseCase
import ir.hesabyar.domain.usecase.GetProductsUseCase
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProductsViewModel @Inject constructor(
    getProducts: GetProductsUseCase,
    getCategories: GetCategoriesUseCase
) : ViewModel() {

    private val _filter = MutableStateFlow(ProductFilter())
    val filter: StateFlow<ProductFilter> = _filter.asStateFlow()

    val products: StateFlow<List<Product>> = _filter
        .flatMapLatest { getProducts(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<ProductCategory>> = getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) = _filter.update { it.copy(query = value) }
    fun setCategory(id: Long?) = _filter.update { it.copy(categoryId = id) }
    fun toggleLowStock() = _filter.update { it.copy(onlyLowStock = !it.onlyLowStock) }
}

@Composable
fun ProductsScreen(
    onOpenProduct: (Long) -> Unit,
    onAddProduct: () -> Unit,
    viewModel: ProductsViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HesabyarTopBar(title = "کالاها", subtitle = "موجودی، قیمت و بهای تمام‌شده") },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProduct,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("کالای جدید") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchField(
                    value = filter.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = "جست‌وجوی نام یا کد کالا"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filter.categoryId == null && !filter.onlyLowStock,
                            onClick = { viewModel.setCategory(null); if (filter.onlyLowStock) viewModel.toggleLowStock() },
                            label = { Text("همه") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filter.onlyLowStock,
                            onClick = { viewModel.toggleLowStock() },
                            label = { Text("کم‌موجود") }
                        )
                    }
                    items(categories, key = { it.id }) { category ->
                        FilterChip(
                            selected = filter.categoryId == category.id,
                            onClick = { viewModel.setCategory(if (filter.categoryId == category.id) null else category.id) },
                            label = { Text(category.name) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (products.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.Inventory2,
                    title = "کالایی ثبت نشده",
                    message = "کالاها را با قیمت خرید و فروش ثبت کنید تا موجودی و سود فروش به‌صورت خودکار محاسبه شود.",
                    actionLabel = "افزودن کالا",
                    onAction = onAddProduct
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductRow(product, onClick = { onOpenProduct(product.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductRow(product: Product, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Box(
                Modifier.size(42.dp).then(Modifier).padding(0.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.size(6.dp))
            Column(Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        product.categoryName ?: "بدون دسته",
                        style = MaterialTheme.typography.labelSmall,
                        color = HesabyarTheme.colors.onSurfaceSecondary
                    )
                    if (product.isLowStock) StatusBadge("کم‌موجود", HesabyarTheme.colors.warning)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                MoneyText(amount = product.sellingPrice, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    "موجودی: ${PersianText.formatQuantity(product.stockQuantity)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (product.isLowStock) HesabyarTheme.colors.warning else HesabyarTheme.colors.onSurfaceSecondary
                )
            }
        }
    }
}
