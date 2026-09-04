package com.thenetworkings.satqso.domain

import java.util.Locale

private val tonePattern = Regex("""(\d+(?:\.\d+)?)\s*Hz\s*tone""", RegexOption.IGNORE_CASE)
private const val chirpTuningStepHertz = 5_000L

private val chirpColumns = listOf(
    "Location",
    "Name",
    "Frequency",
    "Duplex",
    "Offset",
    "Tone",
    "rToneFreq",
    "cToneFreq",
    "DtcsCode",
    "DtcsPolarity",
    "RxDtcsCode",
    "CrossMode",
    "Mode",
    "TStep",
    "Skip",
    "Power",
    "Comment",
    "URCALL",
    "RPT1CALL",
    "RPT2CALL",
    "DVCODE",
)

/** Creates a CHIRP generic CSV for the Doppler-adjusted downlink tuning points in this pass. */
fun PassSummary.toChirpCsv(tuningPoints: List<DownlinkTuningPoint>): String? {
    val uplinkHertz = downlinkCenterFrequencyHertz(satellite.uplink) ?: return null
    if (tuningPoints.isEmpty()) return null

    val transmitTone = tonePattern.find(satellite.uplink)?.groupValues?.get(1)
    val toneMode = if (transmitTone == null) "" else "Tone"
    val toneFrequency = transmitTone ?: "88.5"
    val mode = if (satellite.modes.contains(OperatingMode.FmVoice)) "FM" else "FM"
    val comment = "${satellite.name} pass ${aos}"

    return buildString {
        appendLine(chirpColumns.joinToString(","))
        tuningPoints.forEachIndexed { index, point ->
            val channel = index + 1
            val row = listOf(
                channel.toString(),
                "${satellite.name}-$channel",
                point.frequencyHertz.chirpChannelFrequencyHertz().toChirpMegahertz(),
                "split",
                uplinkHertz.toChirpMegahertz(),
                toneMode,
                toneFrequency,
                toneFrequency,
                "023",
                "NN",
                "023",
                "Tone->Tone",
                mode,
                "5.00",
                "",
                "4.0W",
                comment,
                "",
                "",
                "",
                "",
            )
            appendLine(row.joinToString(",") { it.toCsvValue() })
        }
    }
}

/** Rounds a Doppler prediction to the 5 kHz tuning grid used for CHIRP UV-5R memories. */
fun Long.chirpChannelFrequencyHertz(): Long =
    ((this + chirpTuningStepHertz / 2) / chirpTuningStepHertz) * chirpTuningStepHertz

private fun Long.toChirpMegahertz(): String = String.format(Locale.US, "%.6f", this / 1_000_000.0)

private fun Double.toChirpMegahertz(): String = String.format(Locale.US, "%.6f", this / 1_000_000.0)

private fun String.toCsvValue(): String =
    if (contains(',') || contains('"') || contains('\n')) "\"${replace("\"", "\"\"")}\"" else this
