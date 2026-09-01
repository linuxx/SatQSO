package com.thenetworkings.satqso

import android.content.Context
import com.thenetworkings.satqso.data.CelestrakTleDataSource
import com.thenetworkings.satqso.data.SatellitePassRepository
import com.thenetworkings.satqso.data.SharedPreferencesPassDisplayPreferences
import com.thenetworkings.satqso.data.SharedPreferencesPassCache
import com.thenetworkings.satqso.data.SharedPreferencesTleCache
import com.thenetworkings.satqso.domain.OrekitPassPredictor
import com.thenetworkings.satqso.location.LocationRepository
import com.thenetworkings.satqso.location.OrientationRepository
import okhttp3.OkHttpClient

class SatQsoDependencies(context: Context) {
    private val appContext = context.applicationContext
    private val httpClient = OkHttpClient()

    val locationRepository = LocationRepository(appContext)
    val orientationRepository = OrientationRepository(appContext)
    val passDisplayPreferences = SharedPreferencesPassDisplayPreferences(appContext)
    val passCache = SharedPreferencesPassCache(appContext)
    val passRepository = SatellitePassRepository(
        tleDataSource = CelestrakTleDataSource(
            httpClient = httpClient,
            cache = SharedPreferencesTleCache(appContext),
        ),
        passPredictor = OrekitPassPredictor(),
    )
}
