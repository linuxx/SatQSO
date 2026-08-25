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

class LocationRepository(
    private val context: Context,
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): ObserverLocation {
        check(hasLocationPermission()) { "Location permission has not been granted." }

        val cached = fusedLocationClient.lastLocation.await()
        val location = cached ?: fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
            .await()
            ?: error("Unable to obtain current location.")

        return ObserverLocation(
            latitudeDegrees = location.latitude,
            longitudeDegrees = location.longitude,
            altitudeMeters = if (location.hasAltitude()) location.altitude else 0.0,
        )
    }
}
