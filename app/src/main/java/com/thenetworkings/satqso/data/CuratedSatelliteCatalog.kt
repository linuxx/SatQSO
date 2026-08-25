package com.thenetworkings.satqso.data

import com.thenetworkings.satqso.domain.OperatingMode
import com.thenetworkings.satqso.domain.Satellite

object CuratedSatelliteCatalog {
    val satellites = listOf(
        Satellite(
            noradId = 25544,
            name = "ISS",
            modes = listOf(OperatingMode.Aprs, OperatingMode.Sstv, OperatingMode.FmVoice),
            uplink = "145.990 MHz FM",
            downlink = "145.800 MHz FM",
            notes = "ARISS packet, SSTV, and crew voice activity when scheduled.",
        ),
        Satellite(
            noradId = 27607,
            name = "SO-50",
            modes = listOf(OperatingMode.FmVoice),
            uplink = "145.850 MHz FM, 67.0 Hz tone",
            downlink = "436.795 MHz FM",
            notes = "FM repeater satellite.",
        ),
        Satellite(
            noradId = 42758,
            name = "CAS-4A",
            modes = listOf(OperatingMode.SsbCw, OperatingMode.Telemetry),
            uplink = "435.220-435.280 MHz LSB/CW",
            downlink = "145.870-145.930 MHz USB/CW",
            notes = "Linear transponder satellite.",
        ),
        Satellite(
            noradId = 42759,
            name = "CAS-4B",
            modes = listOf(OperatingMode.SsbCw, OperatingMode.Telemetry),
            uplink = "435.280-435.340 MHz LSB/CW",
            downlink = "145.910-145.970 MHz USB/CW",
            notes = "Linear transponder satellite.",
        ),
        Satellite(
            noradId = 43678,
            name = "PO-101",
            modes = listOf(OperatingMode.FmVoice, OperatingMode.Telemetry),
            uplink = "145.900 MHz FM, 141.3 Hz tone",
            downlink = "437.500 MHz FM",
            notes = "FM repeater availability varies by schedule.",
        ),
        Satellite(
            noradId = 43803,
            name = "JO-97",
            modes = listOf(OperatingMode.SsbCw, OperatingMode.Telemetry),
            uplink = "435.100-435.120 MHz LSB/CW",
            downlink = "145.855-145.875 MHz USB/CW",
            notes = "Linear transponder satellite.",
        ),
        Satellite(
            noradId = 53106,
            name = "IO-117",
            modes = listOf(OperatingMode.DigitalData),
            uplink = "435.310 MHz MFSK",
            downlink = "435.310 MHz MFSK",
            notes = "Store-and-forward digital satellite.",
        ),
    )
}
