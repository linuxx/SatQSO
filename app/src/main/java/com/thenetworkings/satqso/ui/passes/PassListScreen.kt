package com.thenetworkings.satqso.ui.passes

import android.Manifest
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thenetworkings.satqso.R
import com.thenetworkings.satqso.SatQsoDependencies
import com.thenetworkings.satqso.domain.ObserverLocation
import com.thenetworkings.satqso.domain.OperatingMode
import com.thenetworkings.satqso.domain.PassSummary
import com.thenetworkings.satqso.domain.PassTrackPoint
import com.thenetworkings.satqso.domain.Satellite
import com.thenetworkings.satqso.ui.theme.CyanPrimary
import com.thenetworkings.satqso.ui.theme.CyanSecondary
import com.thenetworkings.satqso.ui.theme.OrbitBlue
import com.thenetworkings.satqso.ui.theme.OrbitOrange
import com.thenetworkings.satqso.ui.theme.OrbitPurple
import com.thenetworkings.satqso.ui.theme.SignalGreen
import com.thenetworkings.satqso.ui.theme.SpaceBorder
import com.thenetworkings.satqso.ui.theme.SpaceSurface
import com.thenetworkings.satqso.ui.theme.SpaceSurfaceHigh
import com.thenetworkings.satqso.ui.theme.SatQSOTheme
import com.thenetworkings.satqso.ui.theme.TextSecondary
import com.thenetworkings.satqso.location.OrientationRepository
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun PassListRoute(dependencies: SatQsoDependencies) {
    val viewModel: PassListViewModel = viewModel(
        factory = PassListViewModel.Factory(
            locationRepository = dependencies.locationRepository,
            passRepository = dependencies.passRepository,
            passDisplayPreferences = dependencies.passDisplayPreferences,
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.refresh()
    }

    LaunchedEffect(uiState.needsLocationPermission) {
        if (uiState.needsLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    PassListScreen(
        uiState = uiState,
        onRequestLocation = {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        },
        onRefresh = viewModel::refresh,
        onManualLocation = viewModel::showManualLocationEditor,
        onDismissManualLocation = viewModel::dismissManualLocationEditor,
        onSaveManualLocation = viewModel::saveManualLocation,
        onLocationClick = { viewModel.showMapLocationPicker() },
        onDismissMapLocation = viewModel::dismissMapLocationPicker,
        onSaveMapLocation = viewModel::saveMapLocation,
        onResetLocation = viewModel::resetToGpsLocation,
        onShowFilters = viewModel::showFilters,
        onDismissFilters = viewModel::dismissFilters,
        onMinimumElevationSelected = viewModel::setMinimumElevationDegrees,
        onLookAheadHoursSelected = viewModel::setLookAheadHours,
        onOperatingModeToggled = viewModel::toggleOperatingMode,
        onPassSelected = viewModel::selectPass,
        onClosePassDetails = viewModel::closePassDetails,
        orientationRepository = dependencies.orientationRepository,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassListScreen(
    uiState: PassListUiState,
    onRequestLocation: () -> Unit,
    onRefresh: () -> Unit,
    onManualLocation: () -> Unit,
    onDismissManualLocation: () -> Unit,
    onSaveManualLocation: (String, String, String) -> String?,
    onLocationClick: () -> Unit = {},
    onDismissMapLocation: () -> Unit = {},
    onSaveMapLocation: (ObserverLocation) -> Unit = {},
    onResetLocation: () -> Unit = {},
    onShowFilters: () -> Unit,
    onDismissFilters: () -> Unit,
    onMinimumElevationSelected: (Int) -> Unit,
    onLookAheadHoursSelected: (Int) -> Unit = {},
    onOperatingModeToggled: (OperatingMode) -> Unit,
    onPassSelected: (PassSummary) -> Unit,
    onClosePassDetails: () -> Unit,
    orientationRepository: OrientationRepository? = null,
) {
    BackHandler(enabled = uiState.selectedPass != null) {
        onClosePassDetails()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (uiState.selectedPass != null) {
                        TextButton(onClick = onClosePassDetails) {
                            Text("Back")
                        }
                    }
                },
                title = {
                    Column {
                        val title = uiState.selectedPass?.satellite?.name ?: "SatQSO"
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (uiState.selectedPass != null) {
                                "Pass details"
                            } else {
                                "Upcoming satellite passes"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                actions = {
                    if (uiState.selectedPass == null) {
                        HeaderActionButton(text = "Filter", onClick = onShowFilters)
                        Spacer(Modifier.width(8.dp))
                        HeaderActionButton(text = "Refresh", onClick = onRefresh)
                    }
                },
            )
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(spaceGradient()),
            ) {
                StarField()
                val selectedPass = uiState.selectedPass
                if (selectedPass != null) {
                    PassDetail(
                        pass = selectedPass,
                        orientationRepository = orientationRepository,
                    )
                } else {
                    when {
                        uiState.needsLocationPermission -> PermissionState(onRequestLocation, onManualLocation)
                        uiState.isLoading -> LoadingState()
                        uiState.errorMessage != null -> ErrorState(
                            message = uiState.errorMessage,
                            onRefresh = onRefresh,
                            onManualLocation = onManualLocation,
                        )
                        uiState.passes.isEmpty() -> EmptyState(
                            unfilteredPassCount = uiState.unfilteredPassCount,
                        )
                        else -> PassList(
                            passes = uiState.passes,
                            observerLocation = uiState.observerLocation,
                            unfilteredPassCount = uiState.unfilteredPassCount,
                            lookAheadHours = uiState.lookAheadHours,
                            onPassSelected = onPassSelected,
                            onLocationClick = onLocationClick,
                        )
                    }
                }
                if (uiState.showManualLocationEditor) {
                    ManualLocationDialog(
                        onDismiss = onDismissManualLocation,
                        onSave = onSaveManualLocation,
                    )
                }
                if (uiState.showMapLocationPicker && uiState.observerLocation != null) {
                    MapLocationDialog(
                        initialLocation = uiState.observerLocation,
                        onDismiss = onDismissMapLocation,
                        onSave = onSaveMapLocation,
                        onReset = {
                            onResetLocation()
                            onDismissMapLocation()
                        },
                    )
                }
                if (uiState.showFilters) {
                    FilterDialog(
                        minimumElevationDegrees = uiState.minimumElevationDegrees,
                        lookAheadHours = uiState.lookAheadHours,
                        selectedOperatingModes = uiState.selectedOperatingModes,
                        onMinimumElevationSelected = onMinimumElevationSelected,
                        onLookAheadHoursSelected = onLookAheadHoursSelected,
                        onOperatingModeToggled = onOperatingModeToggled,
                        onDismiss = onDismissFilters,
                    )
                }
            }
        }
    }
}

@Composable
private fun PassList(
    passes: List<PassSummary>,
    observerLocation: ObserverLocation?,
    unfilteredPassCount: Int,
    lookAheadHours: Int,
    onPassSelected: (PassSummary) -> Unit,
    onLocationClick: () -> Unit,
) {
    var currentTime by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Instant.now()
            delay(1_000)
        }
    }
    val upcomingPasses = passes.filter { it.aos.isAfter(currentTime) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            LocationSummaryCard(
                observerLocation = observerLocation,
                onClick = onLocationClick,
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "UPCOMING",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyanPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        text = "Next ${lookAheadLabel(lookAheadHours)} - " +
                            passCountLabel(upcomingPasses.size, unfilteredPassCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (upcomingPasses.isEmpty()) {
            item {
                EmptyUpcomingState()
            }
        } else {
            items(upcomingPasses) { pass ->
                PassCard(
                    pass = pass,
                    currentTime = currentTime,
                    onClick = { onPassSelected(pass) },
                )
            }
        }
    }
}

@Composable
private fun EmptyUpcomingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No upcoming passes in this window",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Try a longer look-ahead period or refresh the orbital data.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PassCard(
    pass: PassSummary,
    currentTime: Instant,
    onClick: () -> Unit,
) {
    val accent = passAccent(pass)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, accent.copy(alpha = 0.6f), MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(
            containerColor = SpaceSurface.copy(alpha = 0.88f),
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SatelliteAvatar(pass = pass, accent = accent, sizeDp = 82)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pass.satellite.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = pass.satellite.modes.joinToString { it.label },
                            style = MaterialTheme.typography.titleMedium,
                            color = accent,
                        )
                    }
                    StatusPill(
                        text = "UPCOMING",
                        accent = accent,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    PassDatum(
                        label = "STARTS AT",
                        value = "${timeFormatter.format(pass.aos)} (${formatStartCountdown(currentTime, pass.aos)})",
                    )
                    PassDatum("MAX ELEV.", "${pass.maxElevationDegrees.roundToInt()} deg", accent)
                    PassDatum("DIRECTION", passDirection(pass), accent)
                }
            }
        }
    }
}

@Composable
private fun LocationSummaryCard(
    observerLocation: ObserverLocation?,
    onClick: () -> Unit,
) {
    var currentTime by remember { mutableStateOf(Instant.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Instant.now()
            delay(1_000)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = observerLocation != null, onClick = onClick)
            .border(1.dp, SpaceBorder, MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(
            containerColor = SpaceSurfaceHigh.copy(alpha = 0.72f),
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            StarField(modifier = Modifier.matchParentSize(), alpha = 0.55f)
            Row(
                modifier = Modifier.padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SummaryBlock(
                    label = "GPS",
                    value = observerLocation?.formattedCoordinates() ?: "Calculating",
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                )
                SummaryDivider()
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SummaryBlock(
                        label = "GRID",
                        value = observerLocation?.maidenheadGrid() ?: "--",
                        horizontalAlignment = Alignment.CenterHorizontally,
                    )
                    SummaryBlock(
                        label = "SUBSQUARE",
                        value = observerLocation?.maidenheadLocator() ?: "--",
                        horizontalAlignment = Alignment.CenterHorizontally,
                    )
                }
                SummaryDivider()
                SummaryBlock(
                    label = "LOCAL TIME",
                    value = clockFormatter.format(currentTime),
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                )
            }
        }
    }
}

@Composable
private fun SummaryBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = CyanSecondary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SummaryDivider() {
    Box(
        modifier = Modifier
            .height(62.dp)
            .width(1.dp)
            .background(SpaceBorder),
    )
}

@Composable
private fun PassDatum(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PassDetail(
    pass: PassSummary,
    orientationRepository: OrientationRepository?,
) {
    val headingDegrees by orientationRepository?.headingDegrees?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf<Int?>(null) }
    var currentTime by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Instant.now()
            delay(1_000)
        }
    }
    DisposableEffect(orientationRepository) {
        orientationRepository?.start()
        onDispose { orientationRepository?.stop() }
    }
    val accent = passAccent(pass)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            OrbitPreviewCard(
                pass = pass,
                accent = accent,
                headingDegrees = headingDegrees,
                currentTime = currentTime,
            )
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SpaceBorder, MaterialTheme.shapes.medium),
                colors = CardDefaults.cardColors(
                    containerColor = SpaceSurface.copy(alpha = 0.9f),
                ),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = pass.satellite.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = pass.satellite.modes.joinToString { it.label },
                            style = MaterialTheme.typography.bodyLarge,
                            color = accent,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        DetailMetric("AOS", timeFormatter.format(pass.aos))
                        DetailMetric("Max", "${pass.maxElevationDegrees.roundToInt()} deg")
                        DetailMetric("LOS", timeFormatter.format(pass.los))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        DetailMetric("Duration", formatDuration(pass.aos, pass.los))
                        DetailMetric("AOS az", formatAzimuth(pass.aosAzimuthDegrees))
                        DetailMetric("LOS az", formatAzimuth(pass.losAzimuthDegrees))
                    }
                }
            }
        }
        item {
            PassTimeline(pass = pass, accent = accent, currentTime = currentTime)
        }
        item {
            DetailSection(
                title = "Frequencies",
                rows = listOf(
                    "Uplink" to pass.satellite.uplink,
                    "Downlink" to pass.satellite.downlink,
                ),
            )
        }
        item {
            DetailSection(
                title = "Operating Notes",
                rows = listOf(
                    "Modes" to pass.satellite.modes.joinToString { it.label },
                    "Notes" to pass.satellite.notes,
                ),
            )
        }
    }
}

@Composable
private fun PassTimeline(
    pass: PassSummary,
    accent: Color,
    currentTime: Instant,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SpaceBorder, MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = SpaceSurface.copy(alpha = 0.9f)),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Pass timeline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TimelineLegend(label = "Elevation", color = OrbitOrange)
                TimelineLegend(label = "Azimuth", color = CyanPrimary)
            }
            if (pass.track.size < 2) {
                Text(
                    text = "Track data unavailable for this pass.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                ) {
                    val points = pass.track.sortedBy { it.instant }
                    val left = 42.dp.toPx()
                    val right = 12.dp.toPx()
                    val top = 12.dp.toPx()
                    val bottom = 24.dp.toPx()
                    val plotWidth = (size.width - left - right).coerceAtLeast(1f)
                    val plotHeight = (size.height - top - bottom).coerceAtLeast(1f)
                    val start = points.first().instant.toEpochMilli().toFloat()
                    val end = points.last().instant.toEpochMilli().toFloat()
                    val span = (end - start).coerceAtLeast(1f)
                    fun x(instant: Instant) = left +
                        ((instant.toEpochMilli().toFloat() - start) / span) * plotWidth
                    fun elevationY(value: Double) = top +
                        (1f - (value / 90.0).coerceIn(0.0, 1.0).toFloat()) * plotHeight
                    fun azimuthY(value: Double) = top +
                        (1f - (value / 360.0).coerceIn(0.0, 1.0).toFloat()) * plotHeight

                    drawLine(
                        color = SpaceBorder,
                        start = Offset(left, top),
                        end = Offset(left, top + plotHeight),
                        strokeWidth = 1.dp.toPx(),
                    )
                    drawLine(
                        color = SpaceBorder,
                        start = Offset(left, top + plotHeight),
                        end = Offset(left + plotWidth, top + plotHeight),
                        strokeWidth = 1.dp.toPx(),
                    )
                    listOf(0, 45, 90).forEach { value ->
                        val y = elevationY(value.toDouble())
                        drawLine(
                            color = SpaceBorder.copy(alpha = 0.45f),
                            start = Offset(left, y),
                            end = Offset(left + plotWidth, y),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    val elevationPath = Path()
                    val azimuthPath = Path()
                    points.forEachIndexed { index, point ->
                        val pointX = x(point.instant)
                        val elevationPoint = Offset(pointX, elevationY(point.elevationDegrees))
                        val azimuthPoint = Offset(pointX, azimuthY(point.azimuthDegrees))
                        if (index == 0) {
                            elevationPath.moveTo(elevationPoint.x, elevationPoint.y)
                            azimuthPath.moveTo(azimuthPoint.x, azimuthPoint.y)
                        } else {
                            elevationPath.lineTo(elevationPoint.x, elevationPoint.y)
                            azimuthPath.lineTo(azimuthPoint.x, azimuthPoint.y)
                        }
                    }
                    drawPath(elevationPath, color = OrbitOrange, style = Stroke(width = 3.dp.toPx()))
                    drawPath(azimuthPath, color = CyanPrimary, style = Stroke(width = 2.dp.toPx()))
                    points.positionAt(currentTime)?.let { currentPoint ->
                        val currentX = x(currentPoint.instant)
                        drawLine(
                            color = SignalGreen.copy(alpha = 0.8f),
                            start = Offset(currentX, top),
                            end = Offset(currentX, top + plotHeight),
                            strokeWidth = 2.dp.toPx(),
                        )
                        drawCircle(
                            color = SignalGreen,
                            radius = 5.dp.toPx(),
                            center = Offset(currentX, elevationY(currentPoint.elevationDegrees)),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(timeFormatter.format(pass.aos), style = MaterialTheme.typography.labelSmall)
                    Text(timeFormatter.format(pass.los), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun TimelineLegend(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}

@Composable
private fun OrbitPreviewCard(
    pass: PassSummary,
    accent: Color,
    headingDegrees: Int?,
    currentTime: Instant,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.05f)
            .border(1.dp, SpaceBorder, MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = SpaceSurfaceHigh.copy(alpha = 0.82f)),
        shape = MaterialTheme.shapes.medium,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val radarRadius = minOf(maxWidth, maxHeight) * 0.39f
            val radarCenterX = maxWidth / 2f
            val radarCenterY = maxHeight / 2f
            val livePoint = pass.track.positionAt(currentTime)
            val currentAzimuth = livePoint?.azimuthDegrees ?: pass.aosAzimuthDegrees
            val currentElevation = livePoint?.elevationDegrees ?: 0.0
            val relativeAzimuthRadians = Math.toRadians(currentAzimuth - (headingDegrees ?: 0))
            val currentRadius = radarRadius *
                (1.0 - (currentElevation / 90.0).coerceIn(0.0, 1.0)).toFloat()
            val iconCenterX = radarCenterX + currentRadius * kotlin.math.sin(relativeAzimuthRadians).toFloat()
            val iconCenterY = radarCenterY - currentRadius * kotlin.math.cos(relativeAzimuthRadians).toFloat()
            val iconSize = 30.dp
            StarField(modifier = Modifier.matchParentSize(), alpha = 0.8f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension * 0.39f
                drawCircle(
                    color = OrbitBlue.copy(alpha = 0.52f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx()),
                )
                drawCircle(
                    color = SpaceBorder.copy(alpha = 0.75f),
                    radius = radius * 0.68f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
                drawCircle(
                    color = SpaceBorder.copy(alpha = 0.45f),
                    radius = radius * 0.38f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
                val heading = headingDegrees?.toDouble() ?: 0.0
                fun skyPoint(elevationDegrees: Double, azimuthDegrees: Double): Offset {
                    val trackRadius = radius *
                        (1.0 - (elevationDegrees / 90.0).coerceIn(0.0, 1.0)).toFloat()
                    val relativeAzimuthRadians = Math.toRadians(azimuthDegrees - heading)
                    return Offset(
                        x = center.x + trackRadius * kotlin.math.sin(relativeAzimuthRadians).toFloat(),
                        y = center.y - trackRadius * kotlin.math.cos(relativeAzimuthRadians).toFloat(),
                    )
                }
                val northRadians = Math.toRadians(-heading)
                val northTip = Offset(
                    x = center.x + radius * 0.92f * kotlin.math.sin(northRadians).toFloat(),
                    y = center.y - radius * 0.92f * kotlin.math.cos(northRadians).toFloat(),
                )
                val northBaseCenter = Offset(
                    x = center.x + radius * 1.08f * kotlin.math.sin(northRadians).toFloat(),
                    y = center.y - radius * 1.08f * kotlin.math.cos(northRadians).toFloat(),
                )
                val northBaseHalfWidth = 10.dp.toPx()
                val northPerpendicular = Offset(
                    x = kotlin.math.cos(northRadians).toFloat() * northBaseHalfWidth,
                    y = kotlin.math.sin(northRadians).toFloat() * northBaseHalfWidth,
                )
                val northMarker = Path().apply {
                    moveTo(northTip.x, northTip.y)
                    lineTo(northBaseCenter.x + northPerpendicular.x, northBaseCenter.y + northPerpendicular.y)
                    lineTo(northBaseCenter.x - northPerpendicular.x, northBaseCenter.y - northPerpendicular.y)
                    close()
                }
                drawPath(northMarker, color = Color(0xFFFF4D5A))
                val track = pass.track.sortedBy { it.instant }
                if (track.size >= 2) {
                    val path = Path()
                    track.forEachIndexed { index, point ->
                        val pointOffset = skyPoint(point.elevationDegrees, point.azimuthDegrees)
                        if (index == 0) {
                            path.moveTo(pointOffset.x, pointOffset.y)
                        } else {
                            path.lineTo(pointOffset.x, pointOffset.y)
                        }
                    }
                    drawPath(path, color = accent, style = Stroke(width = 4.dp.toPx()))
                    val startPoint = track.first()
                    val endPoint = track.last()
                    drawCircle(color = SignalGreen, radius = 9.dp.toPx(), center = skyPoint(startPoint.elevationDegrees, startPoint.azimuthDegrees))
                    drawCircle(color = TextSecondary, radius = 9.dp.toPx(), center = skyPoint(endPoint.elevationDegrees, endPoint.azimuthDegrees))
                } else {
                    val start = Offset(center.x - radius * 0.72f, center.y + radius * 0.55f)
                    val end = Offset(center.x + radius * 0.62f, center.y - radius * 0.48f)
                    drawLine(
                        color = accent,
                        start = start,
                        end = end,
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawCircle(color = SignalGreen, radius = 9.dp.toPx(), center = start)
                    drawCircle(color = TextSecondary, radius = 9.dp.toPx(), center = end)
                }
            }
            SatelliteAvatar(
                pass = pass,
                accent = accent,
                sizeDp = 30,
                modifier = Modifier.offset(
                    x = iconCenterX - iconSize / 2f,
                    y = iconCenterY - iconSize / 2f,
                ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("HEADING", color = CyanSecondary, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = headingDegrees?.let { "$it deg" } ?: "Unavailable",
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp),
            ) {
                Text("AOS", color = SignalGreen, style = MaterialTheme.typography.labelLarge)
                Text(timeFormatter.format(pass.aos), fontWeight = FontWeight.Bold)
            }
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(18.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text("LOS", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                Text(timeFormatter.format(pass.los), fontWeight = FontWeight.Bold)
            }
            val passState = passStateLabel(pass, currentTime)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(18.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = passState.first,
                    color = passState.second,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = passState.third,
                    fontWeight = FontWeight.Bold,
                )
            }
            StatusPill(
                text = "${pass.maxElevationDegrees.roundToInt()} deg max",
                accent = accent,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(18.dp),
            )
        }
    }
}

@Composable
private fun DetailMetric(label: String, value: String) {
    Column(modifier = Modifier.width(92.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun DetailSection(
    title: String,
    rows: List<Pair<String, String>>,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SpaceBorder, MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(
            containerColor = SpaceSurface.copy(alpha = 0.9f),
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            rows.forEach { (label, value) ->
                DetailRow(label = label, value = value)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun SatelliteAvatar(
    pass: PassSummary,
    accent: Color,
    sizeDp: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(SpaceSurfaceHigh)
            .border(1.dp, accent.copy(alpha = 0.75f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = satelliteArtwork(pass)),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun StatusPill(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.border(1.dp, accent.copy(alpha = 0.8f), CircleShape),
        shape = CircleShape,
        color = accent.copy(alpha = 0.13f),
        contentColor = accent,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HeaderActionButton(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .border(1.dp, CyanPrimary.copy(alpha = 0.65f), CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = CyanPrimary.copy(alpha = 0.1f),
        contentColor = CyanPrimary,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StarField(
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    Canvas(modifier = modifier) {
        val stars = listOf(
            Offset(0.09f, 0.18f),
            Offset(0.18f, 0.72f),
            Offset(0.28f, 0.31f),
            Offset(0.39f, 0.83f),
            Offset(0.55f, 0.16f),
            Offset(0.66f, 0.57f),
            Offset(0.78f, 0.26f),
            Offset(0.88f, 0.68f),
            Offset(0.95f, 0.42f),
        )
        stars.forEachIndexed { index, star ->
            drawCircle(
                color = CyanSecondary.copy(alpha = alpha * if (index % 3 == 0) 0.85f else 0.45f),
                radius = if (index % 3 == 0) 2.1.dp.toPx() else 1.2.dp.toPx(),
                center = Offset(size.width * star.x, size.height * star.y),
            )
        }
    }
}

@Composable
private fun PassTime(label: String, instant: Instant) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = timeFormatter.format(instant),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun Direction(label: String, aosAzimuth: Double, losAzimuth: Double) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${compassPoint(aosAzimuth)} to ${compassPoint(losAzimuth)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun PermissionState(
    onRequestLocation: () -> Unit,
    onManualLocation: () -> Unit,
) {
    CenteredMessage(
        title = "Location required",
        body = "SatQSO needs your location to calculate visible satellite passes.",
        action = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = onRequestLocation) {
                    Text("Allow location")
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onManualLocation) {
                    Text("Enter location manually")
                }
            }
        },
    )
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Calculating passes")
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRefresh: () -> Unit,
    onManualLocation: () -> Unit,
) {
    CenteredMessage(
        title = "Pass calculation failed",
        body = message,
        action = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = onRefresh) {
                    Text("Try again")
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onManualLocation) {
                    Text("Enter location manually")
                }
            }
        },
    )
}

@Composable
private fun ManualLocationDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> String?,
) {
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var altitude by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual location") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Use decimal degrees. Altitude is optional and defaults to sea level.")
                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text("Latitude") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text("Longitude") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = altitude,
                    onValueChange = { altitude = it },
                    label = { Text("Altitude (meters)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                validationError?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    validationError = onSave(latitude, longitude, altitude)
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun EmptyState(
    unfilteredPassCount: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (unfilteredPassCount > 0) {
                "No passes match filters"
            } else {
                "No visible passes today"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (unfilteredPassCount > 0) {
                "$unfilteredPassCount passes are hidden by the current elevation or mode filters."
            } else {
                "Try refreshing after the latest orbital data is available."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FilterDialog(
    minimumElevationDegrees: Int,
    lookAheadHours: Int,
    selectedOperatingModes: Set<OperatingMode>,
    onMinimumElevationSelected: (Int) -> Unit,
    onLookAheadHoursSelected: (Int) -> Unit,
    onOperatingModeToggled: (OperatingMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filters") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LookAheadSelector(
                    selectedLookAheadHours = lookAheadHours,
                    onSelected = onLookAheadHoursSelected,
                )
                MinimumElevationSelector(
                    selectedMinimumElevationDegrees = minimumElevationDegrees,
                    onSelected = onMinimumElevationSelected,
                )
                OperatingModeSelector(
                    selectedOperatingModes = selectedOperatingModes,
                    onToggled = onOperatingModeToggled,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}

@Composable
private fun LookAheadSelector(
    selectedLookAheadHours: Int,
    onSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Look ahead",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LookAheadHourOptions.forEach { hours ->
                FilterChip(
                    selected = selectedLookAheadHours == hours,
                    onClick = { onSelected(hours) },
                    label = { Text(lookAheadLabel(hours)) },
                    colors = filterChipColors(CyanPrimary),
                )
            }
        }
    }
}

@Composable
private fun MinimumElevationSelector(
    selectedMinimumElevationDegrees: Int,
    onSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Minimum elevation",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MinimumElevationCutoffs.forEach { cutoff ->
                FilterChip(
                    selected = selectedMinimumElevationDegrees == cutoff,
                    onClick = { onSelected(cutoff) },
                    label = { Text("$cutoff deg") },
                    colors = filterChipColors(CyanPrimary),
                )
            }
        }
    }
}

@Composable
private fun OperatingModeSelector(
    selectedOperatingModes: Set<OperatingMode>,
    onToggled: (OperatingMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Modes",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OperatingModeFilters.forEach { mode ->
                FilterChip(
                    selected = mode in selectedOperatingModes,
                    onClick = { onToggled(mode) },
                    label = { Text(mode.label) },
                    colors = filterChipColors(modeAccent(mode)),
                )
            }
        }
    }
}

@Composable
private fun CenteredMessage(
    title: String,
    body: String,
    action: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        action()
    }
}

private val timeFormatter = DateTimeFormatter
    .ofPattern("HH:mm")
    .withZone(ZoneId.systemDefault())

private val clockFormatter = DateTimeFormatter
    .ofPattern("h:mm a")
    .withZone(ZoneId.systemDefault())

private fun spaceGradient(): Brush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF020711),
        Color(0xFF041322),
        Color(0xFF020812),
    ),
)

private fun compassPoint(degrees: Double): String {
    val points = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val index = ((degrees + 22.5) / 45.0).toInt() % points.size
    return points[index]
}

private fun passDirection(pass: PassSummary): String =
    "${compassPoint(pass.aosAzimuthDegrees)} -> ${compassPoint(pass.losAzimuthDegrees)}"

private fun passAccent(pass: PassSummary): Color = when {
    pass.satellite.modes.any { it == OperatingMode.FmVoice } && pass.maxElevationDegrees >= 70.0 -> SignalGreen
    pass.satellite.modes.any { it == OperatingMode.SsbCw } -> OrbitOrange
    pass.satellite.modes.any { it == OperatingMode.Aprs } -> CyanPrimary
    pass.satellite.name.contains("ISS", ignoreCase = true) -> OrbitPurple
    else -> OrbitBlue
}

private fun modeAccent(mode: OperatingMode): Color = when (mode) {
    OperatingMode.FmVoice -> SignalGreen
    OperatingMode.SsbCw -> OrbitOrange
    OperatingMode.Aprs -> CyanPrimary
    OperatingMode.Sstv -> OrbitPurple
    OperatingMode.DigitalData -> OrbitBlue
    OperatingMode.Telemetry -> CyanSecondary
}

private fun satelliteArtwork(pass: PassSummary): Int = when {
    pass.satellite.name.contains("ISS", ignoreCase = true) -> R.drawable.satellite_iss
    pass.satellite.modes.any { it == OperatingMode.SsbCw } -> R.drawable.satellite_cubesat
    pass.satellite.name.contains("RS", ignoreCase = true) -> R.drawable.satellite_cubesat
    else -> R.drawable.satellite_sat
}

private fun formatAzimuth(degrees: Double): String =
    "${degrees.roundToInt()} deg ${compassPoint(degrees)}"

private fun List<PassTrackPoint>.positionAt(instant: Instant): PassTrackPoint? {
    if (isEmpty()) return null
    val points = sortedBy { it.instant }
    if (instant.isBefore(points.first().instant)) return points.first()
    if (instant.isAfter(points.last().instant)) return points.last()

    points.zipWithNext().firstOrNull { (before, after) ->
        !instant.isBefore(before.instant) && !instant.isAfter(after.instant)
    }?.let { (before, after) ->
        val span = Duration.between(before.instant, after.instant).toMillis()
        val elapsed = Duration.between(before.instant, instant).toMillis()
        val fraction = if (span <= 0L) 0.0 else elapsed.toDouble() / span
        val azimuthDelta = ((after.azimuthDegrees - before.azimuthDegrees + 540.0) % 360.0) - 180.0
        return PassTrackPoint(
            instant = instant,
            elevationDegrees = before.elevationDegrees +
                (after.elevationDegrees - before.elevationDegrees) * fraction,
            azimuthDegrees = (before.azimuthDegrees + azimuthDelta * fraction + 360.0) % 360.0,
        )
    }
    return points.last()
}

private fun formatDuration(aos: Instant, los: Instant): String {
    val duration = Duration.between(aos, los)
    val minutes = duration.toMinutes()
    val seconds = duration.minusMinutes(minutes).seconds

    return "${minutes}m ${seconds}s"
}

private fun passStateLabel(pass: PassSummary, currentTime: Instant): Triple<String, Color, String> = when {
    currentTime.isBefore(pass.aos) -> Triple(
        "STARTS IN",
        SignalGreen,
        formatCountdown(Duration.between(currentTime, pass.aos)),
    )
    currentTime.isBefore(pass.los) -> Triple(
        "ENDS IN",
        OrbitOrange,
        formatCountdown(Duration.between(currentTime, pass.los)),
    )
    else -> Triple("PASS COMPLETE", TextSecondary, "")
}

private fun formatCountdown(duration: Duration): String {
    val seconds = duration.seconds.coerceAtLeast(0L)
    val hours = seconds / 3_600
    val minutes = (seconds % 3_600) / 60
    val remainingSeconds = seconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, remainingSeconds)
    } else {
        "%02d:%02d".format(minutes, remainingSeconds)
    }
}

@Composable
private fun MapLocationDialog(
    initialLocation: ObserverLocation,
    onDismiss: () -> Unit,
    onSave: (ObserverLocation) -> Unit,
    onReset: () -> Unit,
) {
    var selectedLocation by remember(initialLocation) { mutableStateOf(initialLocation) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose location") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tap the map to move the pin.")
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    factory = { context ->
                        createLocationMap(context, selectedLocation) { latitude, longitude ->
                            selectedLocation = ObserverLocation(
                                latitudeDegrees = latitude,
                                longitudeDegrees = longitude,
                                altitudeMeters = selectedLocation.altitudeMeters,
                            )
                        }
                    },
                    update = { mapView ->
                        val marker = mapView.tag as Marker
                        marker.position = GeoPoint(selectedLocation.latitudeDegrees, selectedLocation.longitudeDegrees)
                        mapView.invalidate()
                    },
                )
                Text(
                    text = "${selectedLocation.latitudeDegrees.formatMapCoordinate()}, " +
                        selectedLocation.longitudeDegrees.formatMapCoordinate(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selectedLocation) }) {
                Text("Use this location")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onReset) { Text("Reset to GPS") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )

}

private fun createLocationMap(
    context: Context,
    location: ObserverLocation,
    onLocationSelected: (Double, Double) -> Unit,
): MapView {
    Configuration.getInstance().userAgentValue = context.packageName
    return MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        controller.setZoom(11.0)
        controller.setCenter(GeoPoint(location.latitudeDegrees, location.longitudeDegrees))

        val marker = Marker(this).apply {
            position = GeoPoint(location.latitudeDegrees, location.longitudeDegrees)
            title = "Selected location"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        overlays.add(marker)
        tag = marker
        overlays.add(
            MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(point: GeoPoint): Boolean {
                    onLocationSelected(point.latitude, point.longitude)
                    marker.position = point
                    invalidate()
                    return true
                }

                override fun longPressHelper(point: GeoPoint): Boolean = false
            }),
        )
        onResume()
    }
}

private fun Double.formatMapCoordinate(): String = "%.5f".format(this)

private fun formatStartCountdown(currentTime: Instant, start: Instant): String {
    val seconds = Duration.between(currentTime, start).seconds.coerceAtLeast(0L)
    val minutes = (seconds + 59L) / 60L
    if (minutes < 1L) return "less than a minute"
    val hours = minutes / 60L
    val remainingMinutes = minutes % 60L
    return when {
        hours > 0L && remainingMinutes > 0L ->
            "$hours hr $remainingMinutes min"
        hours > 0L ->
            "$hours hr"
        else ->
            "$minutes min"
    }
}

@Composable
private fun filterChipColors(accent: Color) = androidx.compose.material3.FilterChipDefaults.filterChipColors(
    containerColor = Color.Transparent,
    labelColor = TextSecondary,
    selectedContainerColor = accent.copy(alpha = 0.18f),
    selectedLabelColor = accent,
)

private fun passCountLabel(
    visiblePassCount: Int,
    unfilteredPassCount: Int,
): String = if (visiblePassCount == unfilteredPassCount) {
    "$visiblePassCount passes"
} else {
    "$visiblePassCount/$unfilteredPassCount passes"
}

@Preview(showBackground = true)
@Composable
private fun PassListScreenPreview() {
    SatQSOTheme {
        PassListScreen(
            uiState = PassListUiState(
                passes = listOf(
                    PassSummary(
                        satellite = Satellite(
                            noradId = 27607,
                            name = "SO-50",
                            modes = listOf(OperatingMode.FmVoice),
                            uplink = "145.850 MHz FM",
                            downlink = "436.795 MHz FM",
                            notes = "FM repeater satellite.",
                        ),
                        aos = Instant.now(),
                        los = Instant.now().plusSeconds(610),
                        maxElevationDegrees = 42.0,
                        aosAzimuthDegrees = 214.0,
                        losAzimuthDegrees = 28.0,
                    ),
                ),
            ),
            onRequestLocation = {},
            onRefresh = {},
            onManualLocation = {},
            onDismissManualLocation = {},
            onSaveManualLocation = { _, _, _ -> null },
            onShowFilters = {},
            onDismissFilters = {},
            onMinimumElevationSelected = {},
            onOperatingModeToggled = {},
            onPassSelected = {},
            onClosePassDetails = {},
        )
    }
}
