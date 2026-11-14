package ir.hesabyar.app.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import ir.hesabyar.core.design.components.ErrorBanner
import ir.hesabyar.core.design.components.HesabyarTopBar
import ir.hesabyar.core.design.components.PrimaryButton
import ir.hesabyar.core.design.components.QuantityField
import ir.hesabyar.core.design.components.SegmentedControl
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.accounting.AdjustStockCommand
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.format.PersianText
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.model.Product
import ir.hesabyar.domain.usecase.AdjustStockUseCase
import ir.hesabyar.domain.usecase.GetProductUseCase
import ir.hesabyar.domain.util.OperationIds
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StockAdjustUiState(
    val product: Product? = null,
    val increase: Boolean = true,
    val quantity: Double? = null,
    val reason: String = "",
    val saving: Boolean = false,
    val done: Boolean = false,
    val error: String? = null
) {
    val newStock: Double get() = ((product?.stockQuantity ?: 0.0) + if (increase) (quantity ?: 0.0) else -(quantity ?: 0.0))
}

@HiltViewModel
class StockAdjustViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProduct: GetProductUseCase,
    private val adjustStock: AdjustStockUseCase
) : ViewModel() {

    private val productId: Long = savedStateHandle.get<Long>("productId") ?: 0L
    private val operationId = OperationIds.new()
    private val _state = MutableStateFlow(StockAdjustUiState())
    val state = _state.asStateFlow()
    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val product = getProduct(productId)
            _state.update { it.copy(product = product) }
        }
    }

    fun setIncrease(value: Boolean) = _state.update { it.copy(increase = value) }
    fun setQuantity(value: Double?) = _state.update { it.copy(quantity = value) }
    fun setReason(value: String) = _state.update { it.copy(reason = value) }

    fun submit() {
        val current = _state.value
        val product = current.product ?: return
        val quantity = current.quantity ?: 0.0
        val error = when {
            quantity <= 0 -> "تعداد را وارد کنید"
            current.reason.isBlank() -> "دلیل اصلاح موجودی را وارد کنید"
            !current.increase && quantity > product.stockQuantity -> "موجودی نمی‌تواند منفی شود"
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(error = error) }
            viewModelScope.launch { _events.send(UiEvent(error, isError = true)) }
            return
        }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val command = AdjustStockCommand(
                businessId = product.businessId,
                operationId = operationId,
                date = JalaliDate.startOfDayMillis(System.currentTimeMillis()),
                productId = product.id,
                productName = product.name,
                currentStock = product.stockQuantity,
                averageCost = product.averageCost,
                delta = if (current.increase) quantity else -quantity,
                reason = current.reason.trim(),
                now = System.currentTimeMillis()
            )
            when (val result = adjustStock(command)) {
                is OperationResult.Success -> {
                    _state.update { it.copy(saving = false, done = true) }
                    _events.send(UiEvent("موجودی به‌روزرسانی شد"))
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
fun StockAdjustScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: StockAdjustViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.events.collect { snackbar.showSnackbar(it.message) } }
    LaunchedEffect(state.done) { if (state.done) onSaved() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HesabyarTopBar(title = "اصلاح موجودی", subtitle = state.product?.name, onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.error != null) ErrorBanner(state.error!!)
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "موجودی فعلی: ${PersianText.formatQuantity(state.product?.stockQuantity ?: 0.0)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    SegmentedControl(
                        options = listOf(true, false),
                        selected = state.increase,
                        onSelect = viewModel::setIncrease,
                        label = { if (it) "افزایش موجودی" else "کاهش موجودی" }
                    )
                    QuantityField(value = state.quantity, onValueChange = viewModel::setQuantity, label = "تعداد")
                    AppTextField(
                        value = state.reason,
                        onValueChange = viewModel::setReason,
                        label = "دلیل اصلاح *",
                        placeholder = "مثلاً شمارش انبار"
                    )
                    Text(
                        "موجودی پس از ثبت: ${PersianText.formatQuantity(state.newStock)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "این عملیات فقط موجودی را تغییر می‌دهد و روی صندوق اثری ندارد. سابقه انبار حفظ می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = HesabyarTheme.colors.onSurfaceSecondary
                    )
                }
            }
            PrimaryButton(text = "ثبت اصلاح موجودی", onClick = viewModel::submit, loading = state.saving)
            Spacer(Modifier.height(16.dp))
        }
    }
}
