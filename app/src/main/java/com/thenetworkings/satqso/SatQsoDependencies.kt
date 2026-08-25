package com.thenetworkings.satqso

import android.content.Context
import com.thenetworkings.satqso.data.CelestrakTleDataSource
import com.thenetworkings.satqso.data.SatellitePassRepository
import com.thenetworkings.satqso.domain.OrekitPassPredictor
import com.thenetworkings.satqso.location.LocationRepository
import okhttp3.OkHttpClient

class SatQsoDependencies(context: Context) {
    private val appContext = context.applicationContext
    private val httpClient = OkHttpClient()

    val locationRepository = LocationRepository(appContext)
    val passRepository = SatellitePassRepository(
        tleDataSource = CelestrakTleDataSource(httpClient),
        passPredictor = OrekitPassPredictor(),
    )
}
