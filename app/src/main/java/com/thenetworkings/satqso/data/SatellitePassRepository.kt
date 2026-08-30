package com.thenetworkings.satqso.data

import com.thenetworkings.satqso.domain.ObserverLocation
import com.thenetworkings.satqso.domain.PassPredictor
import com.thenetworkings.satqso.domain.PassSummary

interface PassDataSource {
    suspend fun passes(
        observerLocation: ObserverLocation,
        start: java.time.Instant,
        end: java.time.Instant,
    ): List<PassSummary>
}

class SatellitePassRepository(
    private val tleDataSource: CelestrakTleDataSource,
    private val passPredictor: PassPredictor,
) : PassDataSource {
    override suspend fun passes(
        observerLocation: ObserverLocation,
        start: java.time.Instant,
        end: java.time.Instant,
    ): List<PassSummary> {
        val satellites = CuratedSatelliteCatalog.satellites
        val tles = tleDataSource.fetchTles(satellites.map { it.noradId }.toSet())

        return passPredictor.visiblePasses(
            satellites = satellites,
            tlesByNoradId = tles,
            observerLocation = observerLocation,
            start = start,
            end = end,
        )
    }
}
