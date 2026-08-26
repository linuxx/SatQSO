package com.thenetworkings.satqso.ui.passes

import com.thenetworkings.satqso.domain.OperatingMode
import com.thenetworkings.satqso.domain.PassSummary
import com.thenetworkings.satqso.domain.Satellite
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class PassFiltersTest {
    @Test
    fun filtersPassesBelowMinimumElevation() {
        val passes = listOf(
            pass(maxElevationDegrees = 4.9),
            pass(maxElevationDegrees = 10.0),
            pass(maxElevationDegrees = 42.0),
        )

        val filtered = passes.filterByPassFilters(
            minimumElevationDegrees = 10,
            operatingModes = OperatingMode.values().toSet(),
        )

        assertEquals(listOf(10.0, 42.0), filtered.map { it.maxElevationDegrees })
    }

    @Test
    fun filtersPassesByOperatingMode() {
        val passes = listOf(
            pass(maxElevationDegrees = 42.0, modes = listOf(OperatingMode.FmVoice)),
            pass(maxElevationDegrees = 42.0, modes = listOf(OperatingMode.Aprs)),
            pass(maxElevationDegrees = 42.0, modes = listOf(OperatingMode.SsbCw)),
        )

        val filtered = passes.filterByPassFilters(
            minimumElevationDegrees = 10,
            operatingModes = setOf(OperatingMode.FmVoice, OperatingMode.SsbCw),
        )

        assertEquals(listOf(OperatingMode.FmVoice, OperatingMode.SsbCw), filtered.map { it.satellite.modes.first() })
    }

    private fun pass(
        maxElevationDegrees: Double,
        modes: List<OperatingMode> = listOf(OperatingMode.FmVoice),
    ) = PassSummary(
        satellite = Satellite(
            noradId = 27607,
            name = "SO-50",
            modes = modes,
            uplink = "145.850 MHz FM",
            downlink = "436.795 MHz FM",
            notes = "FM repeater satellite.",
        ),
        aos = Instant.EPOCH,
        los = Instant.EPOCH.plusSeconds(600),
        maxElevationDegrees = maxElevationDegrees,
        aosAzimuthDegrees = 180.0,
        losAzimuthDegrees = 45.0,
    )
}
