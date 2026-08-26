package com.thenetworkings.satqso.data

import com.thenetworkings.satqso.domain.Tle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Clock
import java.time.Duration

class CelestrakTleDataSource(
    private val httpClient: OkHttpClient,
    private val cache: TleCache? = null,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val urls = listOf(
        "https://www.amsat.org/tle/current/nasabare.txt",
        "https://celestrak.org/NORAD/elements/gp.php?GROUP=amateur&FORMAT=tle",
        "https://celestrak.org/NORAD/elements/gp.php?GROUP=stations&FORMAT=tle",
    )

    suspend fun fetchTles(noradIds: Set<Int>): Map<Int, Tle> = withContext(Dispatchers.IO) {
        val cachedTles = cache?.read()
        val matchingCachedTles = cachedTles?.tlesByNoradId.orEmpty().filterKeys { it in noradIds }
        if (cachedTles != null &&
            matchingCachedTles.keys.containsAll(noradIds) &&
            Duration.between(cachedTles.fetchedAt, clock.instant()) < CACHE_TTL
        ) {
            return@withContext matchingCachedTles
        }

        val failures = mutableListOf<String>()
        val tles = urls
            .flatMap { url ->
                runCatching { fetchUrl(url) }
                    .onFailure { failures += "${url.hostLabel()}: ${it.message ?: it.javaClass.simpleName}" }
                    .getOrDefault(emptyList())
            }
            .mapNotNull { tle ->
                val id = tle.noradId()
                if (id != null && id in noradIds) id to tle else null
            }
            .toMap()

        if (tles.isEmpty()) {
            if (matchingCachedTles.isNotEmpty()) {
                return@withContext matchingCachedTles
            }
            val detail = failures.joinToString("; ").ifBlank { "no matching TLEs found" }
            error("Unable to load orbital elements from AMSAT or CelesTrak. $detail")
        }

        cache?.write(
            CachedTles(
                fetchedAt = clock.instant(),
                tlesByNoradId = cachedTles?.tlesByNoradId.orEmpty() + tles,
            ),
        )
        tles
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

    private fun String.hostLabel(): String = substringAfter("://").substringBefore("/")

    private companion object {
        val CACHE_TTL: Duration = Duration.ofHours(12)
    }
}
