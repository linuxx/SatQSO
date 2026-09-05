package com.thenetworkings.satqso.ui.passes

import com.thenetworkings.satqso.domain.OperatingMode
import com.thenetworkings.satqso.domain.PassSummary

internal val MinimumElevationCutoffs = listOf(0, 5, 10, 15, 20, 30)
internal val OperatingModeFilters = OperatingMode.values().toList()
internal const val DefaultMinimumElevationDegrees = 10
internal val LookAheadHourOptions = listOf(24, 48, 96, 168)
internal const val DefaultLookAheadHours = 24

internal fun lookAheadLabel(hours: Int): String = when (hours) {
    24 -> "24 hours"
    48 -> "2 days"
    96 -> "4 days"
    168 -> "7 days"
    else -> "$hours hours"
}

internal fun List<PassSummary>.filterByPassFilters(
    minimumElevationDegrees: Int,
    operatingModes: Set<OperatingMode>,
): List<PassSummary> =
    filter { pass ->
        pass.maxElevationDegrees >= minimumElevationDegrees &&
            pass.satellite.modes.any { it in operatingModes }
    }
