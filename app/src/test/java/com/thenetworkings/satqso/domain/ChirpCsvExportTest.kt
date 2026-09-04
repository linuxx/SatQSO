package com.thenetworkings.satqso.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChirpCsvExportTest {
    @Test
    fun exportsDopplerChannelsAsChirpSplitMemoriesWithTransmitTone() {
        val start = Instant.parse("2026-01-01T00:00:00Z")
        val pass = PassSummary(
            satellite = Satellite(
                noradId = 27607,
                name = "SO-50",
                modes = listOf(OperatingMode.FmVoice),
                uplink = "145.850 MHz FM, 67.0 Hz tone",
                downlink = "436.795 MHz FM",
                notes = "",
            ),
            aos = start,
            los = start.plusSeconds(600),
            maxElevationDegrees = 45.0,
            aosAzimuthDegrees = 0.0,
            losAzimuthDegrees = 180.0,
        )
        val csv = pass.toChirpCsv(
            listOf(
                DownlinkTuningPoint(start, 436_805_199L),
                DownlinkTuningPoint(start.plusSeconds(120), 436_795_000L),
            ),
        )!!
        val rows = csv.lineSequence().filter { it.isNotBlank() }.toList()

        assertTrue(rows.first().startsWith("Location,Name,Frequency,Duplex,Offset,Tone,rToneFreq"))
        assertTrue(rows[1].startsWith("1,SO-50-1,436.805000,split,145.850000,Tone,67.0,67.0"))
        assertTrue(rows[2].startsWith("2,SO-50-2,436.795000,split,145.850000,Tone,67.0,67.0"))
        assertTrue(rows[1].contains(",023,NN,023,Tone->Tone,FM,"))
        assertTrue(rows[1].contains(",FM,5.00,,4.0W,"))
        assertEquals(3, rows.size)
    }

    @Test
    fun roundsExportedChannelsToTheFiveKilohertzTuningGrid() {
        assertEquals(436_805_000L, 436_804_610L.chirpChannelFrequencyHertz())
        assertEquals(436_785_000L, 436_784_543L.chirpChannelFrequencyHertz())
    }
}
