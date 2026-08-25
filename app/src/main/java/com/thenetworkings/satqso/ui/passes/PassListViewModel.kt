package com.thenetworkings.satqso.ui.passes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thenetworkings.satqso.data.SatellitePassRepository
import com.thenetworkings.satqso.domain.PassSummary
import com.thenetworkings.satqso.location.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PassListUiState(
    val isLoading: Boolean = false,
    val needsLocationPermission: Boolean = true,
    val passes: List<PassSummary> = emptyList(),
    val errorMessage: String? = null,
)

class PassListViewModel(
    private val locationRepository: LocationRepository,
    private val passRepository: SatellitePassRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PassListUiState())
    val uiState: StateFlow<PassListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (!locationRepository.hasLocationPermission()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    needsLocationPermission = true,
                    errorMessage = null,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    needsLocationPermission = false,
                    errorMessage = null,
                )
            }

            runCatching {
                val location = locationRepository.currentLocation()
                passRepository.todayPasses(location)
            }.onSuccess { passes ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        passes = passes,
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Unable to calculate passes.",
                    )
                }
            }
        }
    }

    class Factory(
        private val locationRepository: LocationRepository,
        private val passRepository: SatellitePassRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PassListViewModel(locationRepository, passRepository) as T
    }
}
