package com.thenetworkings.satqso.ui.passes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thenetworkings.satqso.data.PassDisplayPreferences
import com.thenetworkings.satqso.data.PassDataSource
import com.thenetworkings.satqso.domain.ObserverLocation
import com.thenetworkings.satqso.domain.OperatingMode
import com.thenetworkings.satqso.domain.PassSummary
import com.thenetworkings.satqso.location.LocationDataSource
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PassListUiState(
    val isLoading: Boolean = false,
    val needsLocationPermission: Boolean = true,
    val showManualLocationEditor: Boolean = false,
    val showFilters: Boolean = false,
    val passes: List<PassSummary> = emptyList(),
    val selectedPass: PassSummary? = null,
    val observerLocation: ObserverLocation? = null,
    val unfilteredPassCount: Int = 0,
    val minimumElevationDegrees: Int = DefaultMinimumElevationDegrees,
    val lookAheadHours: Int = DefaultLookAheadHours,
    val selectedOperatingModes: Set<OperatingMode> = OperatingModeFilters.toSet(),
    val errorMessage: String? = null,
)

class PassListViewModel(
    private val locationRepository: LocationDataSource,
    private val passRepository: PassDataSource,
    private val passDisplayPreferences: PassDisplayPreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PassListUiState())
    val uiState: StateFlow<PassListUiState> = _uiState.asStateFlow()
    private var allPasses: List<PassSummary> = emptyList()

    init {
        val minimumElevationDegrees = passDisplayPreferences.minimumElevationDegrees()
            .takeIf { it in MinimumElevationCutoffs }
            ?: DefaultMinimumElevationDegrees
        val selectedOperatingModes = passDisplayPreferences.operatingModes()
            .ifEmpty { OperatingModeFilters.toSet() }
        val lookAheadHours = passDisplayPreferences.lookAheadHours()
            .takeIf { it in LookAheadHourOptions }
            ?: DefaultLookAheadHours
        _uiState.update {
            it.copy(
                minimumElevationDegrees = minimumElevationDegrees,
                selectedOperatingModes = selectedOperatingModes,
                lookAheadHours = lookAheadHours,
            )
        }
        refresh()
    }

    fun refresh() {
        if (!locationRepository.hasLocationPermission() && !locationRepository.hasManualLocation()) {
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
                val start = Instant.now()
                val end = start.plus(Duration.ofHours(_uiState.value.lookAheadHours.toLong()))
                location to passRepository.passes(location, start, end)
            }.onSuccess { (location, passes) ->
                allPasses = passes
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        observerLocation = location,
                        passes = passes.filterByPassFilters(
                            minimumElevationDegrees = it.minimumElevationDegrees,
                            operatingModes = it.selectedOperatingModes,
                        ),
                        unfilteredPassCount = passes.size,
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

    fun setMinimumElevationDegrees(value: Int) {
        if (value !in MinimumElevationCutoffs) return
        passDisplayPreferences.saveMinimumElevationDegrees(value)
        _uiState.update {
            it.copy(
                minimumElevationDegrees = value,
                passes = allPasses.filterByPassFilters(
                    minimumElevationDegrees = value,
                    operatingModes = it.selectedOperatingModes,
                ),
            )
        }
    }

    fun setLookAheadHours(value: Int) {
        if (value !in LookAheadHourOptions) return
        passDisplayPreferences.saveLookAheadHours(value)
        _uiState.update { it.copy(lookAheadHours = value) }
        refresh()
    }

    fun toggleOperatingMode(mode: OperatingMode) {
        _uiState.update {
            val selectedModes = if (mode in it.selectedOperatingModes) {
                it.selectedOperatingModes - mode
            } else {
                it.selectedOperatingModes + mode
            }.ifEmpty { OperatingModeFilters.toSet() }

            passDisplayPreferences.saveOperatingModes(selectedModes)
            it.copy(
                selectedOperatingModes = selectedModes,
                passes = allPasses.filterByPassFilters(
                    minimumElevationDegrees = it.minimumElevationDegrees,
                    operatingModes = selectedModes,
                ),
            )
        }
    }

    fun selectPass(pass: PassSummary) {
        _uiState.update { it.copy(selectedPass = pass) }
    }

    fun closePassDetails() {
        _uiState.update { it.copy(selectedPass = null) }
    }

    fun showFilters() {
        _uiState.update { it.copy(showFilters = true) }
    }

    fun dismissFilters() {
        _uiState.update { it.copy(showFilters = false) }
    }

    fun showManualLocationEditor() {
        _uiState.update { it.copy(showManualLocationEditor = true) }
    }

    fun dismissManualLocationEditor() {
        _uiState.update { it.copy(showManualLocationEditor = false) }
    }

    fun saveManualLocation(latitudeText: String, longitudeText: String, altitudeText: String): String? {
        val latitude = latitudeText.toDoubleOrNull()
            ?.takeIf { it.isFinite() && it in -90.0..90.0 }
            ?: return "Enter a latitude between -90 and 90."
        val longitude = longitudeText.toDoubleOrNull()
            ?.takeIf { it.isFinite() && it in -180.0..180.0 }
            ?: return "Enter a longitude between -180 and 180."
        val altitude = altitudeText.ifBlank { "0" }.toDoubleOrNull()?.takeIf { it.isFinite() }
            ?: return "Enter a valid altitude in meters."

        locationRepository.saveManualLocation(
            ObserverLocation(
                latitudeDegrees = latitude,
                longitudeDegrees = longitude,
                altitudeMeters = altitude,
            ),
        )
        _uiState.update { it.copy(showManualLocationEditor = false) }
        refresh()
        return null
    }

    class Factory(
        private val locationRepository: LocationDataSource,
        private val passRepository: PassDataSource,
        private val passDisplayPreferences: PassDisplayPreferences,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PassListViewModel(locationRepository, passRepository, passDisplayPreferences) as T
    }
}
