package com.thenetworkings.satqso.data

import android.content.Context
import com.thenetworkings.satqso.domain.Tle
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

data class CachedTles(
    val fetchedAt: Instant,
    val tlesByNoradId: Map<Int, Tle>,
)

interface TleCache {
    fun read(): CachedTles?

    fun write(cachedTles: CachedTles)
}

class SharedPreferencesTleCache(context: Context) : TleCache {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun read(): CachedTles? = runCatching {
        val fetchedAtMillis = preferences.getLong(FETCHED_AT_MILLIS_KEY, 0L)
        val serializedTles = preferences.getString(TLES_KEY, null) ?: return null
        if (fetchedAtMillis <= 0L) return null

        val tles = JSONArray(serializedTles)
            .let { entries ->
                buildMap {
                    for (index in 0 until entries.length()) {
                        val entry = entries.getJSONObject(index)
                        val tle = Tle(
                            name = entry.getString(NAME_KEY),
                            line1 = entry.getString(LINE_1_KEY),
                            line2 = entry.getString(LINE_2_KEY),
                        )
                        put(entry.getInt(NORAD_ID_KEY), tle)
                    }
                }
            }
        CachedTles(Instant.ofEpochMilli(fetchedAtMillis), tles)
    }.getOrNull()

    override fun write(cachedTles: CachedTles) {
        val serializedTles = JSONArray().apply {
            cachedTles.tlesByNoradId.forEach { (noradId, tle) ->
                put(
                    JSONObject()
                        .put(NORAD_ID_KEY, noradId)
                        .put(NAME_KEY, tle.name)
                        .put(LINE_1_KEY, tle.line1)
                        .put(LINE_2_KEY, tle.line2),
                )
            }
        }
        preferences.edit()
            .putLong(FETCHED_AT_MILLIS_KEY, cachedTles.fetchedAt.toEpochMilli())
            .putString(TLES_KEY, serializedTles.toString())
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "tle_cache"
        const val FETCHED_AT_MILLIS_KEY = "fetched_at_millis"
        const val TLES_KEY = "tles"
        const val NORAD_ID_KEY = "norad_id"
        const val NAME_KEY = "name"
        const val LINE_1_KEY = "line_1"
        const val LINE_2_KEY = "line_2"
    }
}
