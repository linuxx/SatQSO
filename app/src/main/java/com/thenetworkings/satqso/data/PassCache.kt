package com.thenetworkings.satqso.data

import android.content.Context
import com.thenetworkings.satqso.domain.ObserverLocation
import com.thenetworkings.satqso.domain.OperatingMode
import com.thenetworkings.satqso.domain.PassSummary
import com.thenetworkings.satqso.domain.PassTrackPoint
import com.thenetworkings.satqso.domain.Satellite
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

data class CachedPasses(val passes: List<PassSummary>, val observerLocation: ObserverLocation, val savedAt: Instant)

interface PassCache {
    fun read(): CachedPasses?
    fun write(value: CachedPasses)
}

class SharedPreferencesPassCache(context: Context) : PassCache {
    private val preferences = context.getSharedPreferences("pass_cache", Context.MODE_PRIVATE)

    override fun read(): CachedPasses? = runCatching {
        val root = JSONObject(preferences.getString("data", null) ?: return null)
        val location = root.getJSONObject("location")
        val array = root.getJSONArray("passes")
        CachedPasses(
            passes = List(array.length()) { array.getJSONObject(it).toPassSummary() },
            observerLocation = ObserverLocation(location.getDouble("latitude"), location.getDouble("longitude"), location.getDouble("altitude")),
            savedAt = Instant.ofEpochMilli(root.getLong("savedAt")),
        )
    }.getOrNull()

    override fun write(value: CachedPasses) {
        val root = JSONObject()
            .put("savedAt", value.savedAt.toEpochMilli())
            .put("location", JSONObject()
                .put("latitude", value.observerLocation.latitudeDegrees)
                .put("longitude", value.observerLocation.longitudeDegrees)
                .put("altitude", value.observerLocation.altitudeMeters))
            .put("passes", JSONArray(value.passes.map { it.toJson() }))
        preferences.edit().putString("data", root.toString()).apply()
    }
}

private fun PassSummary.toJson(): JSONObject = JSONObject()
    .put("satellite", JSONObject()
        .put("noradId", satellite.noradId).put("name", satellite.name)
        .put("modes", JSONArray(satellite.modes.map { it.name }))
        .put("uplink", satellite.uplink).put("downlink", satellite.downlink).put("notes", satellite.notes)
        .putNullable("altitudeKm", satellite.altitudeKm).putNullable("launchDate", satellite.launchDate)
        .putNullable("owner", satellite.owner).putNullable("website", satellite.website))
    .put("aos", aos.toEpochMilli()).put("los", los.toEpochMilli())
    .put("maxElevation", maxElevationDegrees).put("aosAzimuth", aosAzimuthDegrees).put("losAzimuth", losAzimuthDegrees)
    .put("track", JSONArray(track.map {
        JSONObject()
            .put("instant", it.instant.toEpochMilli())
            .put("elevation", it.elevationDegrees)
            .put("azimuth", it.azimuthDegrees)
            .put("rangeRate", it.rangeRateMetersPerSecond)
    }))

private fun JSONObject.toPassSummary(): PassSummary {
    val satelliteJson = getJSONObject("satellite")
    val modesJson = satelliteJson.getJSONArray("modes")
    val trackJson = getJSONArray("track")
    return PassSummary(
        satellite = Satellite(
            noradId = satelliteJson.getInt("noradId"), name = satelliteJson.getString("name"),
            modes = List(modesJson.length()) { OperatingMode.valueOf(modesJson.getString(it)) },
            uplink = satelliteJson.getString("uplink"), downlink = satelliteJson.getString("downlink"), notes = satelliteJson.getString("notes"),
            altitudeKm = satelliteJson.optInt("altitudeKm").takeUnless { satelliteJson.isNull("altitudeKm") },
            launchDate = satelliteJson.optString("launchDate").takeUnless { satelliteJson.isNull("launchDate") },
            owner = satelliteJson.optString("owner").takeUnless { satelliteJson.isNull("owner") },
            website = satelliteJson.optString("website").takeUnless { satelliteJson.isNull("website") },
        ),
        aos = Instant.ofEpochMilli(getLong("aos")), los = Instant.ofEpochMilli(getLong("los")),
        maxElevationDegrees = getDouble("maxElevation"), aosAzimuthDegrees = getDouble("aosAzimuth"), losAzimuthDegrees = getDouble("losAzimuth"),
        track = List(trackJson.length()) { trackJson.getJSONObject(it).let { point ->
            PassTrackPoint(
                instant = Instant.ofEpochMilli(point.getLong("instant")),
                elevationDegrees = point.getDouble("elevation"),
                azimuthDegrees = point.getDouble("azimuth"),
                rangeRateMetersPerSecond = point.optDouble("rangeRate", 0.0),
            )
        } },
    )
}

private fun JSONObject.putNullable(key: String, value: Any?): JSONObject = put(key, value ?: JSONObject.NULL)
