package com.thenetworkings.satqso.data

import com.thenetworkings.satqso.domain.Tle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class CelestrakTleDataSource(
    private val httpClient: OkHttpClient,
) {
    private val urls = listOf(
        "https://celestrak.org/NORAD/elements/gp.php?GROUP=amateur&FORMAT=tle",
        "https://celestrak.org/NORAD/elements/gp.php?GROUP=stations&FORMAT=tle",
    )

    suspend fun fetchTles(noradIds: Set<Int>): Map<Int, Tle> = withContext(Dispatchers.IO) {
        urls
            .flatMap { fetchUrl(it) }
            .mapNotNull { tle ->
                val id = tle.noradId()
                if (id != null && id in noradIds) id to tle else null
            }
            .toMap()
    }

    private fun fetchUrl(url: String): List<Tle> {
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("CelesTrak request failed: HTTP ${response.code}")
            }
            return parse(response.body.string())
        }
    }

    fun parse(rawTle: String): List<Tle> {
        val lines = rawTle
            .lineSequence()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
            .toList()

        val tles = mutableListOf<Tle>()
        var index = 0
        while (index < lines.size) {
            val maybeName = lines[index]
            val line1Index = if (maybeName.startsWith("1 ")) index else index + 1
            val line2Index = line1Index + 1
            if (line2Index < lines.size && lines[line1Index].startsWith("1 ") && lines[line2Index].startsWith("2 ")) {
                val name = if (line1Index == index) "NORAD ${lines[line1Index].substring(2, 7).trim()}" else maybeName.trim()
                tles += Tle(name = name, line1 = lines[line1Index], line2 = lines[line2Index])
                index = line2Index + 1
            } else {
                index += 1
            }
        }
        return tles
    }

    private fun Tle.noradId(): Int? = line1.substringOrNull(2, 7)?.trim()?.toIntOrNull()

    private fun String.substringOrNull(startIndex: Int, endIndex: Int): String? =
        if (length >= endIndex) substring(startIndex, endIndex) else null
}
