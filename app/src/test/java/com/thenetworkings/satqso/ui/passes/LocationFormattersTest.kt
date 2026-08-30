package com.thenetworkings.satqso.ui.passes

import com.thenetworkings.satqso.domain.ObserverLocation
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationFormattersTest {
    @Test
    fun calculatesSixCharacterMaidenheadLocator() {
        val location = ObserverLocation(
            latitudeDegrees = 35.0456,
            longitudeDegrees = -85.3097,
            altitudeMeters = 0.0,
        )

        assertEquals("EM75IB", location.maidenheadLocator())
    }

    @Test
    fun calculatesFourCharacterMaidenheadGrid() {
        val location = ObserverLocation(
            latitudeDegrees = 35.0456,
            longitudeDegrees = -85.3097,
            altitudeMeters = 0.0,
        )

        assertEquals("EM75", location.maidenheadGrid())
    }

    @Test
    fun formatsCoordinatesWithHemisphereSuffixes() {
        val location = ObserverLocation(
            latitudeDegrees = 35.5,
            longitudeDegrees = -85.25,
            altitudeMeters = 0.0,
        )

        assertEquals("35.5000 N, 85.2500 W", location.formattedCoordinates())
    }
}
