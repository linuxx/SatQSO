package com.thenetworkings.satqso.domain

import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Test

class OrekitPassPredictorTest {
    @Test
    fun calculatesVisiblePassesWithoutAndroidDependencies() {
        val predictor = OrekitPassPredictor()
        val satellite = Satellite(
            noradId = 25544,
            name = "ISS",
            modes = listOf(OperatingMode.Aprs),
            uplink = "145.990 MHz FM",
            downlink = "145.800 MHz FM",
            notes = "Test fixture.",
        )
        val tle = Tle(
            name = "ISS (ZARYA)",
            line1 = "1 25544U 98067A   24234.51782528  .00012345  00000+0  12345-3 0  9991",
            line2 = "2 25544  51.6416 247.4627 0006703 130.5360 325.0288 15.72125391563537",
        )

        val passes = predictor.visiblePasses(
            satellites = listOf(satellite),
            tlesByNoradId = mapOf(25544 to tle),
            observerLocation = ObserverLocation(
                latitudeDegrees = 38.8977,
                longitudeDegrees = -77.0365,
                altitudeMeters = 0.0,
            ),
            start = Instant.parse("2024-08-21T00:00:00Z"),
            end = Instant.parse("2024-08-22T00:00:00Z"),
        )

        assertTrue(passes.isNotEmpty())
        assertTrue(passes.all { it.maxElevationDegrees > 0.0 })
    }
}
