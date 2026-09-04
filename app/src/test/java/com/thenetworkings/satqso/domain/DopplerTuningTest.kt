package com.thenetworkings.satqso.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DopplerTuningTest {
    @Test
    fun findsCenterOfFixedAndRangedDownlinks() {
        assertEquals(436_795_000.0, downlinkCenterFrequencyHertz("436.795 MHz FM")!!, 0.1)
        assertEquals(145_900_000.0, downlinkCenterFrequencyHertz("145.870-145.930 MHz USB/CW")!!, 0.1)
    }

    @Test
    fun raisesReceiveFrequencyWhileSatelliteApproaches() {
        val nominal = 436_795_000.0

        val approaching = dopplerAdjustedDownlinkHertz(nominal, rangeRateMetersPerSecond = -7_000.0)
        val receding = dopplerAdjustedDownlinkHertz(nominal, rangeRateMetersPerSecond = 7_000.0)

        assertTrue(approaching > nominal)
        assertTrue(receding < nominal)
    }

    @Test
    fun appliesTheExpectedOneWayShiftForPositiveAndNegativeRangeRate() {
        val nominal = 436_795_000.0

        assertEquals(436_805_199L, dopplerAdjustedDownlinkHertz(nominal, -7_000.0))
        assertEquals(436_784_801L, dopplerAdjustedDownlinkHertz(nominal, 7_000.0))
        assertEquals(436_795_000L, dopplerAdjustedDownlinkHertz(nominal, 0.0))
    }

    @Test
    fun addsMoreTuningPointsForLongerPasses() {
        val start = Instant.parse("2026-01-01T00:00:00Z")
        val satellite = Satellite(
            noradId = 1,
            name = "Test",
            modes = listOf(OperatingMode.FmVoice),
            uplink = "145.000 MHz",
            downlink = "436.795 MHz",
            notes = "",
        )
        fun pass(minutes: Long) = PassSummary(
            satellite = satellite,
            aos = start,
            los = start.plusSeconds(minutes * 60),
            maxElevationDegrees = 30.0,
            aosAzimuthDegrees = 0.0,
            losAzimuthDegrees = 180.0,
            track = listOf(
                PassTrackPoint(start, 0.0, 0.0, -5_000.0),
                PassTrackPoint(start.plusSeconds(minutes * 60), 0.0, 180.0, 5_000.0),
            ),
        )

        val shortPass = pass(minutes = 4).downlinkTuningPoints()
        val longPass = pass(minutes = 16).downlinkTuningPoints()

        assertEquals(3, shortPass.size)
        assertEquals(9, longPass.size)
        assertTrue(shortPass.first().frequencyHertz > shortPass.last().frequencyHertz)
    }
}
