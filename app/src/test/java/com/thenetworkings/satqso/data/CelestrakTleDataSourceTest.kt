package com.thenetworkings.satqso.data

import com.thenetworkings.satqso.domain.Tle
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CelestrakTleDataSourceTest {
    private val dataSource = CelestrakTleDataSource(OkHttpClient())

    @Test
    fun parseReadsThreeLineTleBlocks() {
        val tles = dataSource.parse(
            """
            ISS (ZARYA)
            1 25544U 98067A   24234.51782528  .00012345  00000+0  12345-3 0  9991
            2 25544  51.6416 247.4627 0006703 130.5360 325.0288 15.72125391563537
            SO-50
            1 27607U 02058C   24234.51782528  .00001234  00000+0  12345-3 0  9992
            2 27607  64.5550 101.1200 0084000 120.0000 240.0000 14.70000000123456
            """.trimIndent(),
        )

        assertEquals(2, tles.size)
        assertEquals("ISS (ZARYA)", tles[0].name)
        assertEquals("1 25544U", tles[0].line1.take(8))
        assertEquals("SO-50", tles[1].name)
    }

    @Test
    fun parseReadsTwoLineTleBlocks() {
        val tles = dataSource.parse(
            """
            1 27607U 02058C   24234.51782528  .00001234  00000+0  12345-3 0  9992
            2 27607  64.5550 101.1200 0084000 120.0000 240.0000 14.70000000123456
            """.trimIndent(),
        )

        assertEquals(1, tles.size)
        assertEquals("NORAD 27607", tles.single().name)
    }

    @Test
    fun fetchUsesFreshCacheWithoutMakingNetworkRequest() = runBlocking {
        val tle = Tle(
            name = "SO-50",
            line1 = "1 27607U 02058C   24234.51782528  .00001234  00000+0  12345-3 0  9992",
            line2 = "2 27607  64.5550 101.1200 0084000 120.0000 240.0000 14.70000000123456",
        )
        val cache = InMemoryTleCache(
            CachedTles(
                fetchedAt = Instant.parse("2026-08-25T12:00:00Z"),
                tlesByNoradId = mapOf(27607 to tle),
            ),
        )
        val dataSource = CelestrakTleDataSource(
            httpClient = OkHttpClient(),
            cache = cache,
            clock = Clock.fixed(Instant.parse("2026-08-25T23:59:59Z"), ZoneOffset.UTC),
        )

        assertEquals(mapOf(27607 to tle), dataSource.fetchTles(setOf(27607)))
    }

    private class InMemoryTleCache(
        private var cachedTles: CachedTles?,
    ) : TleCache {
        override fun read(): CachedTles? = cachedTles

        override fun write(cachedTles: CachedTles) {
            this.cachedTles = cachedTles
        }
    }
}
