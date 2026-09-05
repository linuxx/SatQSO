package com.thenetworkings.satqso.ui.passes

import com.thenetworkings.satqso.data.PassDataSource
import com.thenetworkings.satqso.data.PassDisplayPreferences
import com.thenetworkings.satqso.domain.ObserverLocation
import com.thenetworkings.satqso.domain.OperatingMode
import com.thenetworkings.satqso.domain.PassSummary
import com.thenetworkings.satqso.domain.Satellite
import com.thenetworkings.satqso.location.LocationDataSource
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PassListViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun manualLocationValidationRejectsInvalidValuesWithoutSaving() = runTest {
        val location = FakeLocationDataSource(hasPermission = false)
        val viewModel = createViewModel(location = location)

        assertEquals("Enter a latitude between -90 and 90.", viewModel.saveManualLocation("91", "-80", "0"))
        assertEquals("Enter a longitude between -180 and 180.", viewModel.saveManualLocation("35", "181", "0"))
        assertEquals("Enter a valid altitude in meters.", viewModel.saveManualLocation("35", "-80", "not-a-number"))
        assertNull(location.savedLocation)
    }

    @Test
    fun restoresSavedFiltersOnInitialization() = runTest {
        val preferences = FakePassDisplayPreferences(
            elevation = 15,
            modes = setOf(OperatingMode.Aprs, OperatingMode.Sstv),
        )
        val viewModel = createViewModel(preferences = preferences)
        runCurrent()

        assertEquals(15, viewModel.uiState.value.minimumElevationDegrees)
        assertEquals(setOf(OperatingMode.Aprs, OperatingMode.Sstv), viewModel.uiState.value.selectedOperatingModes)
    }

    @Test
    fun appliesElevationAndOperatingModeFiltersToLoadedPasses() = runTest {
        val passes = listOf(
            pass("Low FM", 5.0, OperatingMode.FmVoice),
            pass("High FM", 30.0, OperatingMode.FmVoice),
            pass("High APRS", 30.0, OperatingMode.Aprs),
        )
        val viewModel = createViewModel(passes = passes)
        advanceUntilIdle()

        assertEquals(listOf("High FM", "High APRS"), viewModel.uiState.value.passes.map { it.satellite.name })
        viewModel.setMinimumElevationDegrees(15)
        viewModel.toggleOperatingMode(OperatingMode.FmVoice)

        assertEquals(listOf("High APRS"), viewModel.uiState.value.passes.map { it.satellite.name })
        assertEquals(3, viewModel.uiState.value.unfilteredPassCount)
    }

    @Test
    fun exposesLoadingThenErrorWhenPassCalculationFails() = runTest {
        val gate = CompletableDeferred<Unit>()
        val viewModel = createViewModel(passSource = FakePassDataSource(gate = gate, failure = IllegalStateException("network unavailable")))
        runCurrent()

        assertTrue(viewModel.uiState.value.isLoading)
        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("network unavailable", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun loadsPassesReturnedByOfflineCacheSource() = runTest {
        val cachedPass = pass("Cached SO-50", 42.0, OperatingMode.FmVoice)
        val source = FakePassDataSource(passes = listOf(cachedPass))
        val viewModel = createViewModel(passSource = source)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertNotNull(viewModel.uiState.value.observerLocation)
        assertEquals(listOf("Cached SO-50"), viewModel.uiState.value.passes.map { it.satellite.name })
        assertEquals(1, source.requestCount)
    }

    private fun createViewModel(
        location: FakeLocationDataSource = FakeLocationDataSource(hasPermission = true),
        passSource: FakePassDataSource = FakePassDataSource(),
        preferences: FakePassDisplayPreferences = FakePassDisplayPreferences(),
        passes: List<PassSummary> = emptyList(),
    ): PassListViewModel {
        val actualPassSource = if (passes.isEmpty()) passSource else FakePassDataSource(passes = passes)
        return PassListViewModel(
            locationRepository = location,
            passRepository = actualPassSource,
            passDisplayPreferences = preferences,
            calculationDispatcher = dispatcher,
        )
    }

    private fun pass(name: String, elevation: Double, mode: OperatingMode) = PassSummary(
        satellite = Satellite(
            noradId = name.hashCode(),
            name = name,
            modes = listOf(mode),
            uplink = "145.850 MHz FM",
            downlink = "436.795 MHz FM",
            notes = "Test satellite.",
        ),
        aos = Instant.now().plusSeconds(60),
        los = Instant.now().plusSeconds(660),
        maxElevationDegrees = elevation,
        aosAzimuthDegrees = 180.0,
        losAzimuthDegrees = 45.0,
    )
}

private class FakeLocationDataSource(
    private val hasPermission: Boolean,
) : LocationDataSource {
    var savedLocation: ObserverLocation? = null

    override fun hasLocationPermission() = hasPermission

    override fun hasManualLocation() = savedLocation != null

    override fun saveManualLocation(location: ObserverLocation) {
        savedLocation = location
    }

    override fun clearManualLocation() {
        savedLocation = null
    }

    override suspend fun currentLocation() = savedLocation ?: ObserverLocation(35.0, -85.0, 200.0)
}

private class FakePassDataSource(
    private val passes: List<PassSummary> = emptyList(),
    private val gate: CompletableDeferred<Unit>? = null,
    private val failure: Throwable? = null,
) : PassDataSource {
    var requestCount = 0

    override suspend fun passes(
        observerLocation: ObserverLocation,
        start: java.time.Instant,
        end: java.time.Instant,
    ): List<PassSummary> {
        requestCount += 1
        gate?.await()
        failure?.let { throw it }
        return passes
    }
}

private class FakePassDisplayPreferences(
    private var elevation: Int = DefaultMinimumElevationDegrees,
    private var modes: Set<OperatingMode> = OperatingModeFilters.toSet(),
) : PassDisplayPreferences {
    override fun minimumElevationDegrees() = elevation

    private var lookAhead = 24

    override fun lookAheadHours() = lookAhead

    override fun saveLookAheadHours(value: Int) {
        lookAhead = value
    }

    override fun saveMinimumElevationDegrees(value: Int) {
        elevation = value
    }

    override fun operatingModes() = modes

    override fun saveOperatingModes(values: Set<OperatingMode>) {
        modes = values
    }
}
