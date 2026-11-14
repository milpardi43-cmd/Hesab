package ir.hesabyar.app.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabyar.app.ui.common.UiEvent
import ir.hesabyar.core.design.components.AppCard
import ir.hesabyar.core.design.components.AppTextField
import ir.hesabyar.core.design.components.DropdownField
import ir.hesabyar.core.design.components.ErrorBanner
import ir.hesabyar.core.design.components.HesabyarTopBar
import ir.hesabyar.core.design.components.LoadingScreen
import ir.hesabyar.core.design.components.MoneyField
import ir.hesabyar.core.design.components.PrimaryButton
import ir.hesabyar.core.design.components.QuantityField
import ir.hesabyar.core.design.components.SectionHeader
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.model.Product
import ir.hesabyar.domain.model.ProductCategory
import ir.hesabyar.domain.repository.ProductRepository
import ir.hesabyar.domain.usecase.CheckProductLimitUseCase
import ir.hesabyar.domain.usecase.GetBusinessUseCase
import ir.hesabyar.domain.usecase.GetProductUseCase
import ir.hesabyar.domain.usecase.SaveProductUseCase
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductFormUiState(
    val loading: Boolean = true,
    val isEdit: Boolean = false,
    val name: String = "",
    val code: String = "",
    val categoryId: Long? = null,
    val purchasePrice: Long? = null,
    val sellingPrice: Long? = null,
    val stock: Double? = null,
    val minimumStock: Double? = null,
    val description: String = "",
    val existingStock: Double = 0.0,
    val categories: List<ProductCategory> = emptyList(),
    val saving: Boolean = false,
    val savedId: Long? = null,
    val error: String? = null
)

@HiltViewModel
class ProductFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProductRepository,
    private val getProduct: GetProductUseCase,
    private val saveProduct: SaveProductUseCase,
    private val getBusiness: GetBusinessUseCase,
    private val checkLimit: CheckProductLimitUseCase
) : ViewModel() {

    private val productId: Long = savedStateHandle.get<Long>("productId") ?: 0L
    private val _state = MutableStateFlow(ProductFormUiState())
    val state = _state.asStateFlow()
    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.observeCategories().collect { list ->
                _state.update { it.copy(categories = list) }
            }
        }
        viewModelScope.launch {
            val product = if (productId > 0) getProduct(productId) else null
            _state.update {
                if (product != null) {
                    it.copy(
                        loading = false,
                        isEdit = true,
                        name = product.name,
                        code = product.code.orEmpty(),
                        categoryId = product.categoryId,
                        purchasePrice = product.purchasePrice,
                        sellingPrice = product.sellingPrice,
                        stock = product.stockQuantity,
                        existingStock = product.stockQuantity,
                        minimumStock = product.minimumStock,
                        description = product.description.orEmpty()
                    )
                } else it.copy(loading = false)
            }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value, error = null) }
    fun setCode(value: String) = _state.update { it.copy(code = value) }
    fun setCategory(value: ProductCategory?) = _state.update { it.copy(categoryId = value?.id) }
    fun setPurchasePrice(value: Long?) = _state.update { it.copy(purchasePrice = value) }
    fun setSellingPrice(value: Long?) = _state.update { it.copy(sellingPrice = value) }
    fun setStock(value: Double?) = _state.update { it.copy(stock = value) }
    fun setMinimumStock(value: Double?) = _state.update { it.copy(minimumStock = value) }
    fun setDescription(value: String) = _state.update { it.copy(description = value) }

    fun createCategory(name: String) {
        viewModelScope.launch {
            if (name.isBlank()) return@launch
            val id = repository.createCategory(name)
            _state.update { it.copy(categoryId = id) }
        }
    }

    fun save() {
        val current = _state.value
        if (current.saving) return
        if (current.name.isBlank()) {
            _state.update { it.copy(error = "نام کالا را وارد کنید") }
            return
        }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            if (productId == 0L) {
                checkLimit()?.let { message ->
                    _state.update { it.copy(saving = false, error = message) }
                    _events.send(UiEvent(message, isError = true))
                    return@launch
                }
            }
            val businessId = getBusiness.current()?.id ?: 0L
            val product = Product(
                id = productId,
                businessId = businessId,
                name = current.name,
                code = current.code.takeIf { it.isNotBlank() },
                categoryId = current.categoryId,
                purchasePrice = current.purchasePrice ?: 0L,
                sellingPrice = current.sellingPrice ?: 0L,
                averageCost = current.purchasePrice ?: 0L,
                stockQuantity = if (productId == 0L) (current.stock ?: 0.0) else current.existingStock,
                minimumStock = current.minimumStock ?: 0.0,
                description = current.description.takeIf { it.isNotBlank() }
            )
            when (val result = saveProduct(product)) {
                is OperationResult.Success -> {
                    _state.update { it.copy(saving = false, savedId = result.data) }
                    _events.send(UiEvent(if (productId == 0L) "کالا ثبت شد" else "کالا به‌روزرسانی شد"))
                }
                is OperationResult.Failure -> {
                    _state.update { it.copy(saving = false, error = result.message) }
                    _events.send(UiEvent(result.message, isError = true))
                }
            }
        }
    }
}

@Composable
fun ProductFormScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: ProductFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showCategoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it.message) } }
    LaunchedEffect(state.savedId) { state.savedId?.let(onSaved) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HesabyarTopBar(title = if (state.isEdit) "ویرایش کالا" else "کالای جدید", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (state.loading) {
            LoadingScreen(Modifier.padding(padding))
        } else {
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.error != null) ErrorBanner(state.error!!)
                SectionHeader(title = "مشخصات کالا")
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppTextField(value = state.name, onValueChange = viewModel::setName, label = "نام کالا *")
                        AppTextField(value = state.code, onValueChange = viewModel::setCode, label = "کد / بارکد", placeholder = "اختیاری")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DropdownField(
                                label = "دسته‌بندی",
                                options = state.categories,
                                selected = state.categories.firstOrNull { it.id == state.categoryId },
                                optionLabel = { it.name },
                                onSelect = viewModel::setCategory,
                                modifier = Modifier.weight(1f),
                                placeholder = "بدون دسته"
                            )
                            TextButton(onClick = { showCategoryDialog = true }, modifier = Modifier.padding(top = 20.dp)) {
                                Text("دسته جدید")
                            }
                        }
                    }
                }
                SectionHeader(title = "قیمت‌ها")
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MoneyField(value = state.purchasePrice, onValueChange = viewModel::setPurchasePrice, label = "قیمت خرید")
                        MoneyField(value = state.sellingPrice, onValueChange = viewModel::setSellingPrice, label = "قیمت فروش")
                        Text(
                            "بهای تمام‌شده به‌صورت میانگین وزنی از خریدها محاسبه می‌شود و با هر خرید به‌روز می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = HesabyarTheme.colors.onSurfaceSecondary
                        )
                    }
                }
                SectionHeader(title = "موجودی")
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (state.isEdit) {
                            Text(
                                "موجودی فعلی: ${ir.hesabyar.domain.format.PersianText.formatQuantity(state.existingStock)} — برای تغییر موجودی از «اصلاح موجودی» در صفحه کالا استفاده کنید تا سابقه انبار حفظ شود.",
                                style = MaterialTheme.typography.bodySmall,
                                color = HesabyarTheme.colors.onSurfaceSecondary
                            )
                        } else {
                            QuantityField(value = state.stock, onValueChange = viewModel::setStock, label = "موجودی اولیه")
                        }
                        QuantityField(value = state.minimumStock, onValueChange = viewModel::setMinimumStock, label = "حداقل موجودی (هشدار)")
                    }
                }
                AppCard {
                    AppTextField(
                        value = state.description,
                        onValueChange = viewModel::setDescription,
                        label = "توضیحات",
                        placeholder = "اختیاری",
                        singleLine = false,
                        maxLines = 3
                    )
                }
                PrimaryButton(text = if (state.isEdit) "ذخیره تغییرات" else "ثبت کالا", onClick = viewModel::save, loading = state.saving)
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showCategoryDialog) {
        var categoryName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("دسته‌بندی جدید") },
            text = { AppTextField(value = categoryName, onValueChange = { categoryName = it }, label = "نام دسته") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createCategory(categoryName)
                        showCategoryDialog = false
                    },
                    enabled = categoryName.isNotBlank()
                ) { Text("ثبت") }
            },
            dismissButton = { TextButton(onClick = { showCategoryDialog = false }) { Text("انصراف") } }
        )
    }
}
