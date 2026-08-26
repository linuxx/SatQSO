package com.thenetworkings.satqso.ui.passes

import com.thenetworkings.satqso.domain.ObserverLocation
import java.util.Locale
import kotlin.math.floor

internal fun ObserverLocation.formattedCoordinates(): String =
    "${latitudeDegrees.formatCoordinate("N", "S")}, ${longitudeDegrees.formatCoordinate("E", "W")}"

internal fun ObserverLocation.maidenheadLocator(): String {
    val longitude = (longitudeDegrees + 180.0).coerceIn(0.0, 359.999999)
    val latitude = (latitudeDegrees + 90.0).coerceIn(0.0, 179.999999)

    val fieldLongitude = floor(longitude / 20.0).toInt()
    val fieldLatitude = floor(latitude / 10.0).toInt()
    val squareLongitude = floor((longitude % 20.0) / 2.0).toInt()
    val squareLatitude = floor(latitude % 10.0).toInt()
    val subsquareLongitude = floor(((longitude % 2.0) / 2.0) * 24.0).toInt()
    val subsquareLatitude = floor((latitude % 1.0) * 24.0).toInt()

    return buildString {
        append(('A'.code + fieldLongitude).toChar())
        append(('A'.code + fieldLatitude).toChar())
        append(squareLongitude)
        append(squareLatitude)
        append(('A'.code + subsquareLongitude).toChar())
        append(('A'.code + subsquareLatitude).toChar())
    }
}

private fun Double.formatCoordinate(positiveSuffix: String, negativeSuffix: String): String {
    val suffix = if (this >= 0) positiveSuffix else negativeSuffix
    return "%.4f %s".format(Locale.US, kotlin.math.abs(this), suffix)
}
