package com.thenetworkings.satqso.ui.passes

import com.thenetworkings.satqso.domain.OperatingMode
import com.thenetworkings.satqso.domain.PassSummary

internal val MinimumElevationCutoffs = listOf(0, 5, 10, 15)
internal val OperatingModeFilters = OperatingMode.values().toList()
internal const val DefaultMinimumElevationDegrees = 10
internal val LookAheadHourOptions = listOf(6, 12, 24, 48)
internal const val DefaultLookAheadHours = 24

internal fun List<PassSummary>.filterByPassFilters(
    minimumElevationDegrees: Int,
    operatingModes: Set<OperatingMode>,
): List<PassSummary> =
    filter { pass ->
        pass.maxElevationDegrees >= minimumElevationDegrees &&
            pass.satellite.modes.any { it in operatingModes }
    }
