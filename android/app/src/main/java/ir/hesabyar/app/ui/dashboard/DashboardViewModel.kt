package ir.hesabyar.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabyar.domain.model.DashboardData
import ir.hesabyar.domain.usecase.GetDashboardUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class DashboardViewModel @Inject constructor(
    getDashboard: GetDashboardUseCase
) : ViewModel() {
    val state: StateFlow<DashboardData?> = getDashboard()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
