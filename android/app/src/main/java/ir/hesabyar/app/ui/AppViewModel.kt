package ir.hesabyar.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabyar.domain.model.AppSettings
import ir.hesabyar.domain.model.Business
import ir.hesabyar.domain.usecase.GetBusinessUseCase
import ir.hesabyar.domain.usecase.GetSettingsUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class AppViewModel @Inject constructor(
    getSettings: GetSettingsUseCase,
    getBusiness: GetBusinessUseCase
) : ViewModel() {
    val settings: StateFlow<AppSettings> = getSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val business: StateFlow<Business?> = getBusiness()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
