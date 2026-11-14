package ir.hesabyar.app.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.model.Account
import ir.hesabyar.domain.model.CustomerReportRow
import ir.hesabyar.domain.model.DailyReport
import ir.hesabyar.domain.model.MonthlyReport
import ir.hesabyar.domain.model.ProductReportRow
import ir.hesabyar.domain.model.SalesTrend
import ir.hesabyar.domain.model.SupplierReportRow
import ir.hesabyar.domain.repository.AccountRepository
import ir.hesabyar.domain.usecase.GetCustomerReportUseCase
import ir.hesabyar.domain.usecase.GetDailyReportUseCase
import ir.hesabyar.domain.usecase.GetMonthlyReportUseCase
import ir.hesabyar.domain.usecase.GetProductReportUseCase
import ir.hesabyar.domain.usecase.GetSalesTrendUseCase
import ir.hesabyar.domain.usecase.GetSupplierReportUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ReportTab(val label: String) {
    DAILY("روزانه"),
    MONTHLY("ماهانه"),
    CUSTOMERS("مشتریان"),
    SUPPLIERS("تأمین‌کنندگان"),
    PRODUCTS("کالاها"),
    ACCOUNTS("حساب‌ها")
}

data class ReportsUiState(
    val tab: ReportTab = ReportTab.DAILY,
    val date: Long = JalaliDate.startOfDayMillis(System.currentTimeMillis()),
    val year: Int = JalaliDate.today().year,
    val month: Int = JalaliDate.today().month,
    val daily: DailyReport? = null,
    val monthly: MonthlyReport? = null,
    val customers: List<CustomerReportRow> = emptyList(),
    val suppliers: List<SupplierReportRow> = emptyList(),
    val products: List<ProductReportRow> = emptyList(),
    val trend: SalesTrend? = null,
    val accounts: List<Account> = emptyList(),
    val loading: Boolean = false
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val getDaily: GetDailyReportUseCase,
    private val getMonthly: GetMonthlyReportUseCase,
    private val getCustomers: GetCustomerReportUseCase,
    private val getSuppliers: GetSupplierReportUseCase,
    private val getProducts: GetProductReportUseCase,
    private val getTrend: GetSalesTrendUseCase,
    accounts: AccountRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReportsUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            accounts.observeAccounts().collect { list -> _state.update { it.copy(accounts = list) } }
        }
        refreshAll()
    }

    fun setTab(tab: ReportTab) = _state.update { it.copy(tab = tab) }

    fun setDate(date: Long) {
        _state.update { it.copy(date = date) }
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val report = getDaily(date)
            _state.update { it.copy(daily = report, loading = false) }
        }
    }

    fun previousMonth() {
        var year = _state.value.year
        var month = _state.value.month - 1
        if (month < 1) {
            month = 12
            year -= 1
        }
        _state.update { it.copy(year = year, month = month) }
        loadMonthly()
    }

    fun nextMonth() {
        var year = _state.value.year
        var month = _state.value.month + 1
        if (month > 12) {
            month = 1
            year += 1
        }
        _state.update { it.copy(year = year, month = month) }
        loadMonthly()
    }

    private fun loadMonthly() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val current = _state.value
            val report = getMonthly(current.year, current.month)
            _state.update { it.copy(monthly = report, loading = false) }
        }
    }

    private fun refreshAll() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val current = _state.value
            val daily = getDaily(current.date)
            val monthly = getMonthly(current.year, current.month)
            val customers = getCustomers()
            val suppliers = getSuppliers()
            val products = getProducts()
            val trend = getTrend(7)
            _state.update {
                it.copy(
                    daily = daily,
                    monthly = monthly,
                    customers = customers,
                    suppliers = suppliers,
                    products = products,
                    trend = trend,
                    loading = false
                )
            }
        }
    }
}
