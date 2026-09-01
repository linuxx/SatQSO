package com.thenetworkings.satqso.domain

import java.time.Instant

enum class OperatingMode(val label: String) {
    FmVoice("FM Voice"),
    SsbCw("SSB/CW"),
    Aprs("APRS"),
    Sstv("SSTV"),
    DigitalData("Digital/Data"),
    Telemetry("Telemetry"),
}

data class Satellite(
    val noradId: Int,
    val name: String,
    val modes: List<OperatingMode>,
    val uplink: String,
    val downlink: String,
    val notes: String,
    val altitudeKm: Int? = null,
    val launchDate: String? = null,
    val owner: String? = null,
    val website: String? = null,
)

data class Tle(
    val name: String,
    val line1: String,
    val line2: String,
)

data class ObserverLocation(
    val latitudeDegrees: Double,
    val longitudeDegrees: Double,
    val altitudeMeters: Double,
)

data class PassSummary(
    val satellite: Satellite,
    val aos: Instant,
    val los: Instant,
    val maxElevationDegrees: Double,
    val aosAzimuthDegrees: Double,
    val losAzimuthDegrees: Double,
    val track: List<PassTrackPoint> = emptyList(),
)

data class PassTrackPoint(
    val instant: Instant,
    val elevationDegrees: Double,
    val azimuthDegrees: Double,
    /** Positive while the satellite is receding from the observer. */
    val rangeRateMetersPerSecond: Double = 0.0,
)

interface PassPredictor {
    fun visiblePasses(
        satellites: List<Satellite>,
        tlesByNoradId: Map<Int, Tle>,
        observerLocation: ObserverLocation,
        start: Instant,
        end: Instant,
    ): List<PassSummary>
}
