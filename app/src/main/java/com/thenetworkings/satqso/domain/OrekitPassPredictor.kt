package com.thenetworkings.satqso.domain

import java.time.Instant
import java.util.Date
import kotlin.math.max
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
        var activePass: MutablePass? = null
        val sampleStepSeconds = 60L

        while (!current.isAfter(end)) {
            val sample = sampleAt(propagator, observer, current)
            if (sample.elevationDegrees > 0.0) {
                val pass = activePass ?: MutablePass(
                    aos = current,
                    aosAzimuthDegrees = sample.azimuthDegrees,
                    maxElevationDegrees = sample.elevationDegrees,
                    losAzimuthDegrees = sample.azimuthDegrees,
                ).also { activePass = it }

                if (sample.elevationDegrees > pass.maxElevationDegrees) {
                    pass.maxElevationDegrees = sample.elevationDegrees
                }
                pass.los = current
                pass.losAzimuthDegrees = sample.azimuthDegrees
            } else if (activePass != null) {
                activePass?.toSummary(satellite)?.let(passes::add)
                activePass = null
            }
            current = current.plusSeconds(sampleStepSeconds)
        }
        activePass?.toSummary(satellite)?.let(passes::add)
        return passes
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

    private data class MutablePass(
        val aos: Instant,
        var los: Instant = aos,
        val aosAzimuthDegrees: Double,
        var losAzimuthDegrees: Double,
        var maxElevationDegrees: Double,
    ) {
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
}
