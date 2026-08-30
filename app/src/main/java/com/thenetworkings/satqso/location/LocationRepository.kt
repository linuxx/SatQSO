package com.thenetworkings.satqso.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.thenetworkings.satqso.domain.ObserverLocation
import kotlinx.coroutines.tasks.await

interface LocationDataSource {
    fun hasLocationPermission(): Boolean
    fun hasManualLocation(): Boolean
    fun saveManualLocation(location: ObserverLocation)
    fun clearManualLocation()
    suspend fun currentLocation(): ObserverLocation
}

class LocationRepository(
    private val context: Context,
) : LocationDataSource {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    override fun hasManualLocation(): Boolean =
        preferences.contains(LATITUDE_KEY) && preferences.contains(LONGITUDE_KEY)

    override fun saveManualLocation(location: ObserverLocation) {
        preferences.edit()
            .putString(LATITUDE_KEY, location.latitudeDegrees.toString())
            .putString(LONGITUDE_KEY, location.longitudeDegrees.toString())
            .putString(ALTITUDE_KEY, location.altitudeMeters.toString())
            .apply()
    }

    override fun clearManualLocation() {
        preferences.edit()
            .remove(LATITUDE_KEY)
            .remove(LONGITUDE_KEY)
            .remove(ALTITUDE_KEY)
            .apply()
    }

    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(): ObserverLocation {
        manualLocation()?.let { return it }

        if (!hasLocationPermission()) {
            error("Location permission has not been granted.")
        }

        val location = runCatching {
            val cached = fusedLocationClient.lastLocation.await()
            cached ?: fusedLocationClient
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
                .await()
                ?: error("Unable to obtain current location.")
        }.getOrElse { throwable ->
            return manualLocation() ?: throw throwable
        }

        return ObserverLocation(
            latitudeDegrees = location.latitude,
            longitudeDegrees = location.longitude,
            altitudeMeters = if (location.hasAltitude()) location.altitude else 0.0,
        )
    }

    private fun manualLocation(): ObserverLocation? {
        val latitude = preferences.getString(LATITUDE_KEY, null)?.toDoubleOrNull() ?: return null
        val longitude = preferences.getString(LONGITUDE_KEY, null)?.toDoubleOrNull() ?: return null
        val altitude = preferences.getString(ALTITUDE_KEY, null)?.toDoubleOrNull() ?: 0.0
        return ObserverLocation(latitude, longitude, altitude)
    }

    private companion object {
        const val PREFERENCES_NAME = "manual_location"
        const val LATITUDE_KEY = "latitude"
        const val LONGITUDE_KEY = "longitude"
        const val ALTITUDE_KEY = "altitude"
    }
}
