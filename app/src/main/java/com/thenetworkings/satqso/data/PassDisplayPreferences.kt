package com.thenetworkings.satqso.data

import android.content.Context
import com.thenetworkings.satqso.domain.OperatingMode

interface PassDisplayPreferences {
    fun minimumElevationDegrees(): Int
    fun saveMinimumElevationDegrees(value: Int)
    fun operatingModes(): Set<OperatingMode>
    fun saveOperatingModes(values: Set<OperatingMode>)
}

class SharedPreferencesPassDisplayPreferences(context: Context) : PassDisplayPreferences {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun minimumElevationDegrees(): Int =
        preferences.getInt(KEY_MINIMUM_ELEVATION_DEGREES, DEFAULT_MINIMUM_ELEVATION_DEGREES)

    override fun saveMinimumElevationDegrees(value: Int) {
        preferences.edit()
            .putInt(KEY_MINIMUM_ELEVATION_DEGREES, value)
            .apply()
    }

    override fun operatingModes(): Set<OperatingMode> {
        val storedNames = preferences.getStringSet(KEY_OPERATING_MODES, null)
            ?: return OperatingMode.values().toSet()
        val modesByName = OperatingMode.values().associateBy { it.name }

        return storedNames.mapNotNull { modesByName[it] }
            .toSet()
            .ifEmpty { OperatingMode.values().toSet() }
    }

    override fun saveOperatingModes(values: Set<OperatingMode>) {
        preferences.edit()
            .putStringSet(KEY_OPERATING_MODES, values.map { it.name }.toSet())
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "pass_display_preferences"
        const val KEY_MINIMUM_ELEVATION_DEGREES = "minimum_elevation_degrees"
        const val KEY_OPERATING_MODES = "operating_modes"
        const val DEFAULT_MINIMUM_ELEVATION_DEGREES = 10
    }
}
