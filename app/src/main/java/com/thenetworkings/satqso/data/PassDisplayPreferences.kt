package com.thenetworkings.satqso.data

import android.content.Context
import com.thenetworkings.satqso.domain.OperatingMode

interface PassDisplayPreferences {
    fun minimumElevationDegrees(): Int
    fun saveMinimumElevationDegrees(value: Int)
    fun lookAheadHours(): Int
    fun saveLookAheadHours(value: Int)
    fun operatingModes(): Set<OperatingMode>
    fun saveOperatingModes(values: Set<OperatingMode>)
    fun disableSleep(): Boolean = true
    fun saveDisableSleep(value: Boolean) = Unit
    fun disableRotation(): Boolean = true
    fun saveDisableRotation(value: Boolean) = Unit
    fun use24HourTime(): Boolean = true
    fun saveUse24HourTime(value: Boolean) = Unit
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

    override fun lookAheadHours(): Int =
        preferences.getInt(KEY_LOOK_AHEAD_HOURS, DEFAULT_LOOK_AHEAD_HOURS)

    override fun saveLookAheadHours(value: Int) {
        preferences.edit()
            .putInt(KEY_LOOK_AHEAD_HOURS, value)
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

    override fun disableSleep(): Boolean = preferences.getBoolean(KEY_DISABLE_SLEEP, true)

    override fun saveDisableSleep(value: Boolean) {
        preferences.edit().putBoolean(KEY_DISABLE_SLEEP, value).apply()
    }

    override fun disableRotation(): Boolean = preferences.getBoolean(KEY_DISABLE_ROTATION, true)

    override fun saveDisableRotation(value: Boolean) {
        preferences.edit().putBoolean(KEY_DISABLE_ROTATION, value).apply()
    }

    override fun use24HourTime(): Boolean = preferences.getBoolean(KEY_USE_24_HOUR_TIME, true)

    override fun saveUse24HourTime(value: Boolean) {
        preferences.edit().putBoolean(KEY_USE_24_HOUR_TIME, value).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "pass_display_preferences"
        const val KEY_MINIMUM_ELEVATION_DEGREES = "minimum_elevation_degrees"
        const val KEY_LOOK_AHEAD_HOURS = "look_ahead_hours"
        const val KEY_OPERATING_MODES = "operating_modes"
        const val KEY_DISABLE_SLEEP = "disable_sleep"
        const val KEY_DISABLE_ROTATION = "disable_rotation"
        const val KEY_USE_24_HOUR_TIME = "use_24_hour_time"
        const val DEFAULT_MINIMUM_ELEVATION_DEGREES = 10
        const val DEFAULT_LOOK_AHEAD_HOURS = 24
    }
}
