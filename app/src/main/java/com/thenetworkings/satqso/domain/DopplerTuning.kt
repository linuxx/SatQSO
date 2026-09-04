package com.thenetworkings.satqso.domain

import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val SPEED_OF_LIGHT_METERS_PER_SECOND = 299_792_458.0
private val downlinkFrequencyPattern = Regex(
    """(\d+(?:\.\d+)?)(?:\s*-\s*(\d+(?:\.\d+)?))?\s*MHz""",
    RegexOption.IGNORE_CASE,
)

data class DownlinkTuningPoint(
    val instant: Instant,
    val frequencyHertz: Long,
)

/** Returns the center of a fixed downlink or advertised downlink range, in hertz. */
fun downlinkCenterFrequencyHertz(downlink: String): Double? {
    val match = downlinkFrequencyPattern.find(downlink) ?: return null
    val lowMegahertz = match.groupValues[1].toDoubleOrNull() ?: return null
    val highMegahertz = match.groupValues[2].toDoubleOrNull() ?: lowMegahertz
    return ((lowMegahertz + highMegahertz) / 2.0) * 1_000_000.0
}

/**
 * Provides 3 to 9 evenly spaced receiver-tuning recommendations. Short passes use fewer
 * steps; longer passes add steps so the radio is not left on one Doppler correction too long.
 */
fun PassSummary.downlinkTuningPoints(): List<DownlinkTuningPoint> {
    val centerFrequencyHertz = downlinkCenterFrequencyHertz(satellite.downlink) ?: return emptyList()
    val sortedTrack = track.sortedBy { it.instant }
    if (sortedTrack.isEmpty()) return emptyList()

    val durationSeconds = Duration.between(aos, los).seconds.coerceAtLeast(0)
    val pointCount = ((durationSeconds / 120.0).roundToInt() + 1).coerceIn(3, 9)
    return List(pointCount) { index ->
        val fraction = if (pointCount == 1) 0.0 else index.toDouble() / (pointCount - 1)
        val instant = aos.plusMillis((durationSeconds * 1_000.0 * fraction).roundToLong())
        val rangeRate = sortedTrack.rangeRateAt(instant)
        DownlinkTuningPoint(
            instant = instant,
            frequencyHertz = dopplerAdjustedDownlinkHertz(centerFrequencyHertz, rangeRate),
        )
    }
}

/**
 * Returns the expected received one-way downlink frequency using the first-order Doppler model.
 * A positive range rate means the satellite is receding, so the received frequency decreases.
 */
fun dopplerAdjustedDownlinkHertz(
    nominalFrequencyHertz: Double,
    rangeRateMetersPerSecond: Double,
): Long = (nominalFrequencyHertz * (1.0 - rangeRateMetersPerSecond / SPEED_OF_LIGHT_METERS_PER_SECOND))
    .roundToLong()

private fun List<PassTrackPoint>.rangeRateAt(instant: Instant): Double {
    val before = lastOrNull { !it.instant.isAfter(instant) } ?: first()
    val after = firstOrNull { !it.instant.isBefore(instant) } ?: last()
    val spanMillis = Duration.between(before.instant, after.instant).toMillis()
    if (spanMillis <= 0L) return before.rangeRateMetersPerSecond
    val fraction = Duration.between(before.instant, instant).toMillis().toDouble() / spanMillis
    return before.rangeRateMetersPerSecond +
        (after.rangeRateMetersPerSecond - before.rangeRateMetersPerSecond) * fraction.coerceIn(0.0, 1.0)
}
