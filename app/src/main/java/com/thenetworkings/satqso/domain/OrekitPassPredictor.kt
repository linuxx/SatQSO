package com.thenetworkings.satqso.domain

import java.time.Instant
import java.time.Duration
import java.util.Date
import kotlin.math.max
import kotlin.math.roundToLong
import org.hipparchus.util.FastMath
import org.orekit.bodies.GeodeticPoint
import org.orekit.bodies.OneAxisEllipsoid
import org.orekit.frames.FramesFactory
import org.orekit.frames.TopocentricFrame
import org.orekit.propagation.analytical.tle.TLE
import org.orekit.propagation.analytical.tle.TLEPropagator
import org.orekit.time.AbsoluteDate
import org.orekit.time.TimeScalesFactory
import org.orekit.utils.Constants
import org.orekit.utils.IERSConventions

class OrekitPassPredictor : PassPredictor {
    init {
        OrekitData.initialize()
    }

    private val utc = TimeScalesFactory.getUTC()
    private val earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, false)
    private val earth = OneAxisEllipsoid(
        Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
        Constants.WGS84_EARTH_FLATTENING,
        earthFrame,
    )

    override fun visiblePasses(
        satellites: List<Satellite>,
        tlesByNoradId: Map<Int, Tle>,
        observerLocation: ObserverLocation,
        start: Instant,
        end: Instant,
    ): List<PassSummary> {
        val observer = TopocentricFrame(
            earth,
            GeodeticPoint(
                FastMath.toRadians(observerLocation.latitudeDegrees),
                FastMath.toRadians(observerLocation.longitudeDegrees),
                observerLocation.altitudeMeters,
            ),
            "observer",
        )

        return satellites
            .flatMap { satellite ->
                val tle = tlesByNoradId[satellite.noradId] ?: return@flatMap emptyList()
                passesForSatellite(satellite, tle, observer, start, end)
            }
            .sortedBy { it.aos }
    }

    private fun passesForSatellite(
        satellite: Satellite,
        tle: Tle,
        observer: TopocentricFrame,
        start: Instant,
        end: Instant,
    ): List<PassSummary> {
        val propagator = TLEPropagator.selectExtrapolator(TLE(tle.line1, tle.line2))
        val passes = mutableListOf<PassSummary>()
        var current = start
        var previous = TimedSkySample(current, sampleAt(propagator, observer, current))
        var activePass: MutablePass? = previous.takeIf { it.sample.elevationDegrees > 0.0 }
            ?.let { sample ->
                MutablePass(
                    aos = sample.instant,
                    aosAzimuthDegrees = sample.sample.azimuthDegrees,
                    maxElevationDegrees = sample.sample.elevationDegrees,
                    losAzimuthDegrees = sample.sample.azimuthDegrees,
                )
            }
        val sampleStepSeconds = 60L

        while (current.isBefore(end)) {
            current = minOf(current.plusSeconds(sampleStepSeconds), end)
            val sample = TimedSkySample(current, sampleAt(propagator, observer, current))

            when {
                previous.sample.elevationDegrees <= 0.0 && sample.sample.elevationDegrees > 0.0 -> {
                    val aos = findHorizonCrossing(propagator, observer, previous, sample)
                    activePass = MutablePass(
                        aos = aos.instant,
                        aosAzimuthDegrees = aos.sample.azimuthDegrees,
                        maxElevationDegrees = aos.sample.elevationDegrees,
                        losAzimuthDegrees = aos.sample.azimuthDegrees,
                    ).also { it.record(sample) }
                }
                previous.sample.elevationDegrees > 0.0 && sample.sample.elevationDegrees <= 0.0 -> {
                    val los = findHorizonCrossing(propagator, observer, previous, sample)
                    activePass?.apply {
                        this.los = los.instant
                        losAzimuthDegrees = los.sample.azimuthDegrees
                    }?.toSummary(satellite)?.let(passes::add)
                    activePass = null
                }
                sample.sample.elevationDegrees > 0.0 -> activePass?.record(sample)
            }
            previous = sample
        }
        activePass?.toSummary(satellite)?.let(passes::add)
        return passes
    }

    private fun findHorizonCrossing(
        propagator: TLEPropagator,
        observer: TopocentricFrame,
        before: TimedSkySample,
        after: TimedSkySample,
    ): TimedSkySample {
        var lower = before
        var upper = after
        while (Duration.between(lower.instant, upper.instant) > BOUNDARY_PRECISION) {
            val midpoint = lower.instant.plusMillis(
                Duration.between(lower.instant, upper.instant).toMillis() / 2,
            )
            val midpointSample = TimedSkySample(midpoint, sampleAt(propagator, observer, midpoint))
            if ((lower.sample.elevationDegrees > 0.0) == (midpointSample.sample.elevationDegrees > 0.0)) {
                lower = midpointSample
            } else {
                upper = midpointSample
            }
        }

        val intervalMillis = Duration.between(lower.instant, upper.instant).toMillis()
        val elevationRange = upper.sample.elevationDegrees - lower.sample.elevationDegrees
        val fraction = if (elevationRange == 0.0) 0.5 else -lower.sample.elevationDegrees / elevationRange
        val crossing = lower.instant.plusMillis((intervalMillis * fraction.coerceIn(0.0, 1.0)).roundToLong())
        return TimedSkySample(crossing, sampleAt(propagator, observer, crossing))
    }

    private fun sampleAt(
        propagator: TLEPropagator,
        observer: TopocentricFrame,
        instant: Instant,
    ): SkySample {
        val date = AbsoluteDate(Date.from(instant), utc)
        val position = propagator.getPVCoordinates(date, earthFrame).position
        val elevation = FastMath.toDegrees(observer.getElevation(position, earthFrame, date))
        val azimuth = FastMath.toDegrees(observer.getAzimuth(position, earthFrame, date)).normalizeDegrees()
        return SkySample(elevationDegrees = elevation, azimuthDegrees = azimuth)
    }

    private data class SkySample(
        val elevationDegrees: Double,
        val azimuthDegrees: Double,
    )

    private data class TimedSkySample(
        val instant: Instant,
        val sample: SkySample,
    )

    private data class MutablePass(
        val aos: Instant,
        var los: Instant = aos,
        val aosAzimuthDegrees: Double,
        var losAzimuthDegrees: Double,
        var maxElevationDegrees: Double,
    ) {
        fun record(timedSample: TimedSkySample) {
            if (timedSample.sample.elevationDegrees > maxElevationDegrees) {
                maxElevationDegrees = timedSample.sample.elevationDegrees
            }
            los = timedSample.instant
            losAzimuthDegrees = timedSample.sample.azimuthDegrees
        }

        fun toSummary(satellite: Satellite): PassSummary? {
            val durationSeconds = max(0L, los.epochSecond - aos.epochSecond)
            return if (durationSeconds >= 120L) {
                PassSummary(
                    satellite = satellite,
                    aos = aos,
                    los = los,
                    maxElevationDegrees = maxElevationDegrees,
                    aosAzimuthDegrees = aosAzimuthDegrees,
                    losAzimuthDegrees = losAzimuthDegrees,
                )
            } else {
                null
            }
        }
    }

    private fun Double.normalizeDegrees(): Double = if (this < 0.0) this + 360.0 else this

    private companion object {
        val BOUNDARY_PRECISION: Duration = Duration.ofSeconds(1)
    }
}
