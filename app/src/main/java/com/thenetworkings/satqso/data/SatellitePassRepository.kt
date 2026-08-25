package com.thenetworkings.satqso.data

import com.thenetworkings.satqso.domain.ObserverLocation
import com.thenetworkings.satqso.domain.PassPredictor
import com.thenetworkings.satqso.domain.PassSummary
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

class SatellitePassRepository(
    private val tleDataSource: CelestrakTleDataSource,
    private val passPredictor: PassPredictor,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    suspend fun todayPasses(observerLocation: ObserverLocation): List<PassSummary> {
        val satellites = CuratedSatelliteCatalog.satellites
        val tles = tleDataSource.fetchTles(satellites.map { it.noradId }.toSet())
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(clock).atStartOfDay(zone).toInstant()
        val end = start.plusSeconds(24 * 60 * 60)

        return passPredictor.visiblePasses(
            satellites = satellites,
            tlesByNoradId = tles,
            observerLocation = observerLocation,
            start = start,
            end = end,
        )
    }
}
