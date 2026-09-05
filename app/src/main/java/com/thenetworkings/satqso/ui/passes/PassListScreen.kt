package com.thenetworkings.satqso.ui.passes

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thenetworkings.satqso.R
import com.thenetworkings.satqso.SatQsoDependencies
import com.thenetworkings.satqso.domain.DownlinkTuningPoint
import com.thenetworkings.satqso.domain.ObserverLocation
import com.thenetworkings.satqso.domain.OperatingMode
import com.thenetworkings.satqso.domain.PassSummary
import com.thenetworkings.satqso.domain.PassTrackPoint
import com.thenetworkings.satqso.domain.Satellite
import com.thenetworkings.satqso.domain.downlinkCenterFrequencyHertz
import com.thenetworkings.satqso.domain.downlinkTuningPoints
import com.thenetworkings.satqso.domain.chirpChannelFrequencyHertz
import com.thenetworkings.satqso.domain.toChirpCsv
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
import com.thenetworkings.satqso.ui.theme.TextPrimary
import com.thenetworkings.satqso.ui.theme.TextSecondary
import com.thenetworkings.satqso.location.OrientationRepository
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.io.File
import androidx.core.content.FileProvider
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
fun PassListRoute(
    dependencies: SatQsoDependencies,
    onDisableSleepChanged: (Boolean) -> Unit = {},
    onDisableRotationChanged: (Boolean) -> Unit = {},
) {
    val viewModel: PassListViewModel = viewModel(
        factory = PassListViewModel.Factory(
            locationRepository = dependencies.locationRepository,
            passRepository = dependencies.passRepository,
            passDisplayPreferences = dependencies.passDisplayPreferences,
            passCache = dependencies.passCache,
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
        onShowSettings = viewModel::showSettings,
        onDismissSettings = viewModel::dismissSettings,
        onDisableSleepChanged = {
            viewModel.setDisableSleep(it)
            onDisableSleepChanged(it)
        },
        onDisableRotationChanged = {
            viewModel.setDisableRotation(it)
            onDisableRotationChanged(it)
        },
        onUse24HourTimeChanged = viewModel::setUse24HourTime,
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
    onShowSettings: () -> Unit = {},
    onDismissSettings: () -> Unit = {},
    onDisableSleepChanged: (Boolean) -> Unit = {},
    onDisableRotationChanged: (Boolean) -> Unit = {},
    onUse24HourTimeChanged: (Boolean) -> Unit = {},
    onMinimumElevationSelected: (Int) -> Unit,
    onLookAheadHoursSelected: (Int) -> Unit = {},
    onOperatingModeToggled: (OperatingMode) -> Unit,
    onPassSelected: (PassSummary) -> Unit,
    onClosePassDetails: () -> Unit,
    orientationRepository: OrientationRepository? = null,
) {
    BackHandler(enabled = uiState.selectedPass != null || uiState.showSettings) {
        if (uiState.selectedPass != null) onClosePassDetails() else onDismissSettings()
    }

    CompositionLocalProvider(LocalTimeFormatter provides timeFormatterFor(uiState.use24HourTime)) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (uiState.selectedPass != null) {
                TopAppBar(
                    navigationIcon = {
                        TextButton(onClick = onClosePassDetails) {
                            Text("Back")
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = uiState.selectedPass?.satellite?.name.orEmpty(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Pass details",
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
                        Text(
                            text = uiState.observerLocation?.maidenheadGrid() ?: "----",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary,
                        )
                        Spacer(Modifier.width(16.dp))
                    },
                )
            }
        },
        bottomBar = {
            if (uiState.selectedPass == null) {
                PassListBottomBar(
                    onShowFilters = onShowFilters,
                    onRefresh = onRefresh,
                    isRefreshing = uiState.isLoading,
                    showSettings = uiState.showSettings,
                    onShowSettings = onShowSettings,
                )
            }
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
                val selectedPass = uiState.selectedPass
                if (selectedPass != null) {
                    PassDetail(
                        pass = selectedPass,
                        orientationRepository = orientationRepository,
                    )
                } else if (uiState.showSettings) {
                    SettingsPage(
                        disableSleep = uiState.disableSleep,
                        disableRotation = uiState.disableRotation,
                        use24HourTime = uiState.use24HourTime,
                        onDisableSleepChanged = onDisableSleepChanged,
                        onDisableRotationChanged = onDisableRotationChanged,
                        onUse24HourTimeChanged = onUse24HourTimeChanged,
                    )
                } else {
                    when {
                        uiState.needsLocationPermission -> PermissionState(onRequestLocation, onManualLocation)
                        uiState.isLoading && uiState.passes.isEmpty() -> LoadingState()
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
                            isManualLocation = uiState.isManualLocation,
                            unfilteredPassCount = uiState.unfilteredPassCount,
                            lookAheadHours = uiState.lookAheadHours,
                            onPassSelected = onPassSelected,
                            onLocationClick = onLocationClick,
                            isRefreshing = uiState.isLoading,
                            onRefresh = onRefresh,
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
}

@Composable
private fun PassListBottomBar(
    onShowFilters: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    showSettings: Boolean,
    onShowSettings: () -> Unit,
) {
    NavigationBar(
        containerColor = SpaceSurfaceHigh.copy(alpha = 0.98f),
        tonalElevation = 0.dp,
    ) {
        NavigationBarItem(
            selected = false,
            onClick = {},
            enabled = false,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_recordings),
                    contentDescription = "Recordings",
                )
            },
            label = { Text("Recordings") },
        )
        NavigationBarItem(
            selected = false,
            onClick = onShowFilters,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_filter),
                    contentDescription = "Filter",
                )
            },
            label = { Text("Filter") },
        )
        NavigationBarItem(
            selected = false,
            onClick = onRefresh,
            enabled = !isRefreshing,
            icon = {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = CyanPrimary,
                        strokeWidth = 3.dp,
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_refresh),
                        contentDescription = "Refresh",
                    )
                }
            },
            label = {
                Text(
                    text = if (isRefreshing) "Refreshing…" else "Refresh",
                    color = if (isRefreshing) CyanPrimary else TextSecondary,
                )
            },
        )
        NavigationBarItem(
            selected = showSettings,
            onClick = onShowSettings,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = "Settings",
                )
            },
            label = { Text("Settings") },
        )
    }
}

@Composable
private fun SettingsPage(
    disableSleep: Boolean,
    disableRotation: Boolean,
    use24HourTime: Boolean,
    onDisableSleepChanged: (Boolean) -> Unit,
    onDisableRotationChanged: (Boolean) -> Unit,
    onUse24HourTimeChanged: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = CyanPrimary,
        )
        Text(
            text = "Display & device",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        SettingSwitchRow(
            title = "Disable sleep",
            description = "Keep the display on while using SatQSO.",
            checked = disableSleep,
            onCheckedChange = onDisableSleepChanged,
        )
        SettingSwitchRow(
            title = "Disable rotation",
            description = "Keep SatQSO in portrait orientation.",
            checked = disableRotation,
            onCheckedChange = onDisableRotationChanged,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Time format",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = use24HourTime,
                    onClick = { onUse24HourTimeChanged(true) },
                    label = { Text("24-hour") },
                    colors = filterChipColors(CyanPrimary),
                )
                FilterChip(
                    selected = !use24HourTime,
                    onClick = { onUse24HourTimeChanged(false) },
                    label = { Text("12-hour") },
                    colors = filterChipColors(CyanPrimary),
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PassList(
    passes: List<PassSummary>,
    observerLocation: ObserverLocation?,
    isManualLocation: Boolean,
    unfilteredPassCount: Int,
    lookAheadHours: Int,
    onPassSelected: (PassSummary) -> Unit,
    onLocationClick: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    val refreshFade by animateFloatAsState(
        targetValue = if (isRefreshing) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "pass-list-refresh-fade",
    )
    val refreshPaint = remember { Paint() }
    var currentTime by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Instant.now()
            delay(1_000)
        }
    }
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { if (!isRefreshing) onRefresh() },
        indicator = {},
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().drawWithContent {
                if (refreshFade > 0f) {
                    refreshPaint.colorFilter = ColorFilter.colorMatrix(
                        ColorMatrix().apply { setToSaturation(1f - refreshFade) },
                    )
                    refreshPaint.alpha = 1f - 0.45f * refreshFade
                    drawContext.canvas.saveLayer(Rect(Offset.Zero, size), refreshPaint)
                    drawContent()
                    drawContext.canvas.restore()
                } else {
                    drawContent()
                }
            },
            userScrollEnabled = !isRefreshing,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        item {
            LocationSummaryCard(
                observerLocation = observerLocation,
                isManualLocation = isManualLocation,
                onClick = onLocationClick,
                enabled = !isRefreshing,
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
                        text = "PASSES",
                        style = MaterialTheme.typography.headlineSmall,
                        color = CyanPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        text = "Next ${lookAheadLabel(lookAheadHours)} - " +
                            passCountLabel(passes.size, unfilteredPassCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (passes.isEmpty()) {
            item {
                EmptyUpcomingState()
            }
        } else {
            items(passes) { pass ->
                PassCard(
                    pass = pass,
                    currentTime = currentTime,
                    onClick = { onPassSelected(pass) },
                    enabled = !isRefreshing,
                )
            }
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
            text = "No passes in this window",
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
    enabled: Boolean = true,
) {
    val accent = passAccent(pass)
    val status = passStatusLabel(pass, currentTime)
    val targetInstant = if (status == "Active") pass.los else pass.aos
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
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
                    Column(
                        modifier = Modifier.width(100.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        StatusPill(
                            text = status,
                            accent = statusAccent(status, accent),
                            modifier = Modifier.fillMaxWidth().height(30.dp),
                            progress = passProgress(pass, currentTime).takeIf { status == "Active" },
                            compact = true,
                        )
                        if (status != "Passed") {
                            Text(
                                text = formatCountdown(Duration.between(currentTime, targetInstant)),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (status != "Passed") {
                        PassDatum(
                            label = "${if (status == "Active") "ENDS" else "STARTS"} ${passDateFormatter.format(targetInstant).uppercase()}",
                            value = formatAppTime(targetInstant),
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    PassDatum(
                        label = "MAX EL.", value = "${pass.maxElevationDegrees.roundToInt()}°", color = accent,
                        modifier = Modifier.width(52.dp),
                        alignment = TextAlign.Center,
                    )
                    Spacer(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .width(1.dp)
                            .height(32.dp)
                            .background(SpaceBorder.copy(alpha = 0.65f)),
                    )
                    PassDatum(
                        label = "DIRECTION", value = passDirection(pass), color = accent,
                        modifier = Modifier.width(76.dp),
                        alignment = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationSummaryCard(
    observerLocation: ObserverLocation?,
    isManualLocation: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
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
            .clickable(enabled = enabled && observerLocation != null, onClick = onClick)
            .border(1.dp, SpaceBorder, MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(
            containerColor = SpaceSurfaceHigh.copy(alpha = 0.72f),
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SummaryBlock(
                    label = if (isManualLocation) "GPS • MODIFIED" else "GPS",
                    value = observerLocation?.formattedCoordinates() ?: "Calculating",
                    hint = if (isManualLocation) "" else "Tap To Change",
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                    color = if (isManualLocation) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface,
                )
                SummaryBlock(
                    label = "LOCAL TIME",
                    value = formatAppTime(currentTime),
                    detail = dateFormatter.format(currentTime),
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                    prominent = true,
                )
            }
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(SpaceBorder.copy(alpha = 0.65f)))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LocatorSummary(
                    label = "GRID",
                    value = observerLocation?.maidenheadGrid() ?: "--",
                    modifier = Modifier.weight(1f),
                )
                LocatorSummary(
                    label = "SUBSQUARE",
                    value = observerLocation?.maidenheadLocator() ?: "--",
                    modifier = Modifier.weight(1f),
                    arrangement = Arrangement.End,
                )
            }
        }
    }
}

@Composable
private fun SummaryBlock(
    label: String,
    value: String,
    detail: String = "",
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    color: Color = MaterialTheme.colorScheme.onSurface,
    prominent: Boolean = false,
    hint: String = "",
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (color == MaterialTheme.colorScheme.onSurface) TextSecondary else color,
                fontWeight = FontWeight.SemiBold,
            )
            if (hint.isNotBlank()) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanPrimary,
                )
            }
        }
        Text(
            text = value,
            style = if (prominent) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
        if (detail.isNotBlank()) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun LocatorSummary(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    arrangement: Arrangement.Horizontal = Arrangement.Start,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = arrangement,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.width(8.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = CyanSecondary,
        )
    }
}

@Composable
private fun PassDatum(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
    alignment: TextAlign = TextAlign.Start,
    labelModifier: Modifier = Modifier,
    valueStyle: TextStyle? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().then(labelModifier),
            textAlign = alignment,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = valueStyle ?: MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = alignment,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
            delay(16)
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
        item { PassSummaryCard(pass = pass, accent = accent, currentTime = currentTime) }
        item { FrequenciesAndOperatingInfoCard(pass = pass, accent = accent, currentTime = currentTime) }
        item {
            PassTimeline(pass = pass, accent = accent, currentTime = currentTime)
        }
        item {
            DetailSection(
                title = "OPERATING NOTES",
                rows = listOf(
                    "Modes" to pass.satellite.modes.joinToString { it.label },
                    "NORAD ID" to pass.satellite.noradId.toString(),
                    "Altitude" to pass.satellite.altitudeKm?.let { "$it km" }.orEmpty(),
                    "Launch" to pass.satellite.launchDate.orEmpty(),
                    "Owner" to pass.satellite.owner.orEmpty(),
                    "Website" to pass.satellite.website.orEmpty(),
                    "Notes" to pass.satellite.notes,
                ).filter { it.second.isNotBlank() },
            )
        }
    }
}

@Composable
private fun PassSummaryCard(
    pass: PassSummary,
    accent: Color,
    currentTime: Instant,
) {
    val livePoint = pass.track.positionAt(currentTime)
    val currentAzimuth = livePoint?.azimuthDegrees ?: pass.aosAzimuthDegrees
    val currentElevation = livePoint?.elevationDegrees ?: 0.0
    TelemetryCard(title = "PASS SUMMARY", accent = accent) {
        TelemetryValue("AOS", formatAppTime(pass.aos), "${formatAzimuth(pass.aosAzimuthDegrees)}")
        TelemetryDivider()
        TelemetryValue(
            "MAX ELEVATION",
            "${pass.maxElevationDegrees.roundToInt()} deg",
            formatAppTime(pass.aos.plusSeconds(Duration.between(pass.aos, pass.los).seconds / 2)),
        )
        TelemetryDivider()
        TelemetryValue("LOS", formatAppTime(pass.los), "${formatAzimuth(pass.losAzimuthDegrees)}")
        TelemetryDivider()
        TelemetryValue(
            "CURRENT",
            "${currentElevation.roundToInt()} deg",
            "${formatAzimuth(currentAzimuth)}",
        )
    }
}

@Composable
private fun FrequenciesAndOperatingInfoCard(
    pass: PassSummary,
    accent: Color,
    currentTime: Instant,
) {
    val context = LocalContext.current
    val tuningPoints = remember(pass) { pass.downlinkTuningPoints() }
    val nominalFrequencyHertz = remember(pass) { downlinkCenterFrequencyHertz(pass.satellite.downlink) }
    val chirpCsv = remember(pass, tuningPoints) { pass.toChirpCsv(tuningPoints) }
    val (uplinkFrequency, uplinkTone) = remember(pass) { splitFrequencyTone(pass.satellite.uplink) }
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
                text = "FREQUENCIES AND OPERATING INFO",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = CyanPrimary,
            )
            FrequencyValue(
                label = "MODE",
                value = pass.satellite.modes.joinToString { it.label },
                color = accent,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FrequencyValue(
                    label = "UPLINK (TX)",
                    value = uplinkFrequency,
                    detail = uplinkTone,
                    modifier = Modifier.weight(1f),
                    color = OrbitBlue,
                )
                FrequencyValue(
                    label = "DOWNLINK (RX)",
                    value = pass.satellite.downlink,
                    modifier = Modifier.weight(1f),
                    color = CyanSecondary,
                )
            }
            Text(
                text = "DOPPLER TUNING",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = CyanPrimary,
            )
            if (tuningPoints.size < 2 || nominalFrequencyHertz == null) {
                Text(
                    text = "A numeric downlink frequency and pass track are required for Doppler tuning.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                val markerTime = when {
                    currentTime.isBefore(pass.aos) -> pass.aos
                    currentTime.isAfter(pass.los) -> pass.los
                    else -> currentTime
                }
                val selectedPoint = tuningPoints.lastOrNull { !it.instant.isAfter(markerTime) }
                    ?: tuningPoints.first()
                val label = when {
                    currentTime.isBefore(pass.aos) -> "STARTING TUNE"
                    currentTime.isAfter(pass.los) -> "FINAL TUNE"
                    else -> "TUNE NOW"
                }
                Surface(
                    color = CyanPrimary.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(label, style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                            Text(
                                text = formatFrequencyHertz(selectedPoint.frequencyHertz),
                                style = MaterialTheme.typography.headlineSmall,
                                color = CyanPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = formatAppTime(selectedPoint.instant),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                        )
                    }
                }
                DopplerFrequencyChart(
                    points = tuningPoints,
                    markerTime = markerTime,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TuningEndpoint(
                        label = "AOS",
                        frequencyHertz = tuningPoints.first().frequencyHertz,
                        color = SignalGreen,
                    )
                    TuningEndpoint(
                        label = "LOS",
                        frequencyHertz = tuningPoints.last().frequencyHertz,
                        color = Color(0xFFFF4D5A),
                        alignment = Alignment.End,
                    )
                }
                val intervalMinutes = (Duration.between(pass.aos, pass.los).seconds /
                    (tuningPoints.size - 1) / 60.0).roundToInt().coerceAtLeast(1)
                Text(
                    text = "${tuningPoints.size} tuning points · updates about every $intervalMinutes min",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                )
                DopplerFrequencyTable(
                    points = tuningPoints,
                    selectedPoint = selectedPoint,
                    onExportToChirp = chirpCsv?.let { csv ->
                        { exportPassToChirp(context, pass, csv) }
                    },
                )
            }
        }
    }
}

@Composable
private fun TuningEndpoint(
    label: String,
    frequencyHertz: Long,
    color: Color,
    alignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(horizontalAlignment = alignment) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
        Text(
            text = formatFrequencyHertz(frequencyHertz),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun DopplerFrequencyTable(
    points: List<DownlinkTuningPoint>,
    selectedPoint: DownlinkTuningPoint,
    onExportToChirp: (() -> Unit)?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            color = SpaceSurfaceHigh.copy(alpha = 0.72f),
            shape = MaterialTheme.shapes.small,
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "CH.",
                        modifier = Modifier.weight(0.16f),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                    )
                    Text(
                        text = "TIME",
                        modifier = Modifier.weight(0.32f),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                    )
                    Text(
                        text = "DOWNLINK (RX)",
                        modifier = Modifier.weight(0.52f),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                    )
                }
                points.forEachIndexed { index, point ->
                    val isSelected = point.instant == selectedPoint.instant
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) CyanPrimary.copy(alpha = 0.12f) else Color.Transparent)
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            modifier = Modifier.weight(0.16f),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) SignalGreen else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                        Text(
                            text = formatAppTime(point.instant),
                            modifier = Modifier.weight(0.32f),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) SignalGreen else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                        Text(
                            text = formatFrequencyHertz(point.frequencyHertz.chirpChannelFrequencyHertz()),
                            modifier = Modifier.weight(0.52f),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) CyanPrimary else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
        Button(
            onClick = { onExportToChirp?.invoke() },
            modifier = Modifier.fillMaxWidth(),
            enabled = onExportToChirp != null,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_open_in_new),
                contentDescription = "Open in another app",
            )
            Spacer(Modifier.width(8.dp))
            Text("Export this pass to CHIRP")
        }
    }
}

private fun exportPassToChirp(context: Context, pass: PassSummary, csv: String) {
    val exportDirectory = File(context.cacheDir, "chirp_exports").apply { mkdirs() }
    val safeSatelliteName = pass.satellite.name.replace(Regex("[^A-Za-z0-9]+"), "-").trim('-')
    val outputFile = File(exportDirectory, "${safeSatelliteName}-${pass.aos.epochSecond}.csv")
    outputFile.writeText(csv)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outputFile)
    val openIntent = Intent(Intent.ACTION_SEND)
        .setType("text/csv")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_TITLE, outputFile.name)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    openIntent.clipData = ClipData.newRawUri("CHIRP pass", uri)
    try {
        context.startActivity(Intent.createChooser(openIntent, "Send CHIRP pass export"))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app is available to open CSV files.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun DopplerFrequencyChart(
    points: List<DownlinkTuningPoint>,
    markerTime: Instant,
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp),
    ) {
        val left = 8.dp.toPx()
        val right = 8.dp.toPx()
        val top = 12.dp.toPx()
        val bottom = 12.dp.toPx()
        val width = (size.width - left - right).coerceAtLeast(1f)
        val height = (size.height - top - bottom).coerceAtLeast(1f)
        val start = points.first().instant.toEpochMilli()
        val end = points.last().instant.toEpochMilli()
        val duration = (end - start).coerceAtLeast(1L)
        val low = points.minOf { it.frequencyHertz }.toDouble()
        val high = points.maxOf { it.frequencyHertz }.toDouble()
        val frequencyPadding = maxOf(1_000.0, (high - low) * 0.15)
        val minFrequency = low - frequencyPadding
        val frequencySpan = (high - low + frequencyPadding * 2.0).coerceAtLeast(1.0)
        fun x(instant: Instant) = left +
            ((instant.toEpochMilli() - start).toFloat() / duration.toFloat()) * width
        fun y(frequencyHertz: Long) = top +
            (1f - ((frequencyHertz - minFrequency) / frequencySpan).toFloat()) * height

        listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
            val gridY = top + height * fraction
            drawLine(
                color = SpaceBorder.copy(alpha = 0.45f),
                start = Offset(left, gridY),
                end = Offset(left + width, gridY),
                strokeWidth = 1.dp.toPx(),
            )
        }
        val path = Path()
        points.forEachIndexed { index, point ->
            val position = Offset(x(point.instant), y(point.frequencyHertz))
            if (index == 0) path.moveTo(position.x, position.y) else path.lineTo(position.x, position.y)
        }
        drawPath(path, color = CyanPrimary, style = Stroke(width = 3.dp.toPx()))
        points.forEach { point ->
            drawCircle(
                color = CyanSecondary,
                radius = 4.dp.toPx(),
                center = Offset(x(point.instant), y(point.frequencyHertz)),
            )
        }
        val markerX = x(markerTime)
        drawLine(
            color = SignalGreen.copy(alpha = 0.85f),
            start = Offset(markerX, top),
            end = Offset(markerX, top + height),
            strokeWidth = 2.dp.toPx(),
        )
    }
}

@Composable
private fun FrequencyValue(
    label: String,
    value: String,
    detail: String = "",
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = color, fontWeight = FontWeight.Medium)
        if (detail.isNotBlank()) {
            Text(detail, style = MaterialTheme.typography.labelLarge, color = SignalGreen, fontWeight = FontWeight.Medium)
        }
    }
}

private fun splitFrequencyTone(value: String): Pair<String, String> {
    val parts = value.split(",", limit = 2)
    if (parts.size < 2 || !parts[1].contains("tone", ignoreCase = true)) {
        return value to ""
    }
    return parts[0].trim() to parts[1].trim()
}

private fun formatFrequencyHertz(frequencyHertz: Long): String =
    String.format(Locale.US, "%.3f MHz", frequencyHertz / 1_000_000.0)

@Composable
private fun TelemetryCard(
    title: String,
    accent: Color,
    content: @Composable RowScope.() -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SpaceBorder, MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = SpaceSurface.copy(alpha = 0.9f)),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = CyanPrimary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

@Composable
private fun RowScope.TelemetryValue(label: String, value: String, detail: String) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary, textAlign = TextAlign.Center)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        if (detail.isNotBlank()) {
            Text(detail, style = MaterialTheme.typography.labelMedium, color = CyanSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TelemetryDivider() {
    Box(modifier = Modifier.height(58.dp).width(1.dp).background(SpaceBorder))
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
                text = "PASS TIMELINE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = CyanPrimary,
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
                val markerTime = when {
                    currentTime.isBefore(pass.aos) -> pass.aos
                    currentTime.isAfter(pass.los) -> pass.los
                    else -> currentTime
                }
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
                    val start = points.first().instant.toEpochMilli()
                    val end = points.last().instant.toEpochMilli()
                    val span = (end - start).coerceAtLeast(1L)
                    fun x(instant: Instant) = left +
                        ((instant.toEpochMilli() - start).toFloat() / span.toFloat()) * plotWidth
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
                    val elevationPoints = mutableListOf<Offset>()
                    val azimuthPoints = mutableListOf<Offset>()
                    points.forEach { point ->
                        val pointX = x(point.instant)
                        val elevationPoint = Offset(pointX, elevationY(point.elevationDegrees))
                        val azimuthPoint = Offset(pointX, azimuthY(point.azimuthDegrees))
                        elevationPoints += elevationPoint
                        azimuthPoints += azimuthPoint
                    }
                    drawPath(smoothLinePath(elevationPoints), color = OrbitOrange, style = Stroke(width = 3.dp.toPx()))
                    drawPath(smoothLinePath(azimuthPoints), color = CyanPrimary, style = Stroke(width = 2.dp.toPx()))
                    points.positionAt(markerTime)?.let { currentPoint ->
                        val currentX = x(markerTime)
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
                    Text(
                        formatAppTime(pass.aos),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                    Text(
                        formatAppTime(pass.los),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

private fun smoothLinePath(points: List<Offset>): Path = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points.first().x, points.first().y)
    if (points.size == 1) return@apply

    for (index in 0 until points.lastIndex) {
        val previous = points.getOrElse(index - 1) { points[index] }
        val current = points[index]
        val next = points[index + 1]
        val following = points.getOrElse(index + 2) { next }
        cubicTo(
            current.x + (next.x - previous.x) / 6f,
            current.y + (next.y - previous.y) / 6f,
            next.x - (following.x - current.x) / 6f,
            next.y - (following.y - current.y) / 6f,
            next.x,
            next.y,
        )
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
    val status = passStatusLabel(pass, currentTime)
    val tuningPoints = remember(pass) { pass.downlinkTuningPoints() }
    val tuningMarkerTime = when {
        currentTime.isBefore(pass.aos) -> pass.aos
        currentTime.isAfter(pass.los) -> pass.los
        else -> currentTime
    }
    val currentTuningPoint = tuningPoints.lastOrNull { !it.instant.isAfter(tuningMarkerTime) }
    val countdown = when {
        currentTime.isBefore(pass.aos) -> Duration.between(currentTime, pass.aos)
        currentTime.isBefore(pass.los) -> Duration.between(currentTime, pass.los)
        else -> null
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.84f)
            .border(1.dp, SpaceBorder, MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = SpaceSurfaceHigh.copy(alpha = 0.82f)),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    text = status,
                    accent = statusAccent(status, accent),
                    progress = passProgress(pass, currentTime).takeIf { status == "Active" },
                )
                if (countdown != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            if (status == "Active") "ENDS IN" else "STARTS IN",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                        Text(formatCountdown(countdown), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f).offset(y = (-6).dp)) {
            val radarRadius = minOf(maxWidth, maxHeight) * 0.41f
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
            val iconSize = 26.dp
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension * 0.41f
                val heading = headingDegrees?.toDouble() ?: 0.0
                for (azimuth in 0 until 360 step 5) {
                    val tickRadians = Math.toRadians(azimuth.toDouble() - heading)
                    val innerRadius = when {
                        azimuth % 30 == 0 -> radius * 0.93f
                        azimuth % 10 == 0 -> radius * 0.96f
                        else -> radius * 0.985f
                    }
                    val outerRadius = radius * 1.04f
                    drawLine(
                        color = when {
                            azimuth % 30 == 0 -> CyanSecondary.copy(alpha = 0.5f)
                            azimuth % 10 == 0 -> SpaceBorder.copy(alpha = 0.9f)
                            else -> SpaceBorder.copy(alpha = 0.55f)
                        },
                        start = Offset(
                            center.x + innerRadius * kotlin.math.sin(tickRadians).toFloat(),
                            center.y - innerRadius * kotlin.math.cos(tickRadians).toFloat(),
                        ),
                        end = Offset(
                            center.x + outerRadius * kotlin.math.sin(tickRadians).toFloat(),
                            center.y - outerRadius * kotlin.math.cos(tickRadians).toFloat(),
                        ),
                        strokeWidth = when {
                            azimuth % 30 == 0 -> 1.5.dp.toPx()
                            azimuth % 10 == 0 -> 1.dp.toPx()
                            else -> 0.75.dp.toPx()
                        },
                    )
                }
                drawCircle(
                    color = SpaceBorder,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
                drawCircle(
                    color = SpaceBorder.copy(alpha = 0.75f),
                    radius = radius * (2f / 3f),
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
                drawCircle(
                    color = SpaceBorder.copy(alpha = 0.45f),
                    radius = radius / 3f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
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
                    x = center.x + radius * 0.95f * kotlin.math.sin(northRadians).toFloat(),
                    y = center.y - radius * 0.95f * kotlin.math.cos(northRadians).toFloat(),
                )
                val northBaseCenter = Offset(
                    x = center.x + radius * 1.02f * kotlin.math.sin(northRadians).toFloat(),
                    y = center.y - radius * 1.02f * kotlin.math.cos(northRadians).toFloat(),
                )
                val northBaseHalfWidth = 4.dp.toPx()
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
                    drawPath(path, color = accent, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                    val startPoint = track.first()
                    val endPoint = track.last()
                    drawCircle(color = SignalGreen, radius = 5.dp.toPx(), center = skyPoint(startPoint.elevationDegrees, startPoint.azimuthDegrees))
                    drawCircle(color = Color(0xFFFF4D5A), radius = 5.dp.toPx(), center = skyPoint(endPoint.elevationDegrees, endPoint.azimuthDegrees))
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
                    drawCircle(color = Color(0xFFFF4D5A), radius = 9.dp.toPx(), center = end)
                }
            }
            if (status == "Active") {
                SatelliteAvatar(
                    pass = pass,
                    accent = accent,
                    sizeDp = 26,
                    modifier = Modifier.offset(
                        x = iconCenterX - iconSize / 2f,
                        y = iconCenterY - iconSize / 2f,
                    ),
                )
            }
            val compassHeading = headingDegrees?.toDouble() ?: 0.0
            CompassLabel("N", 0.0, compassHeading, radarRadius * 1.14f)
            CompassLabel("E", 90.0, compassHeading, radarRadius * 1.14f)
            CompassLabel("S", 180.0, compassHeading, radarRadius * 1.14f)
            CompassLabel("W", 270.0, compassHeading, radarRadius * 1.14f)
        }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PassDatum("AOS", formatAppTime(pass.aos), color = SignalGreen, modifier = Modifier.weight(1f))
                PassDatum("DURATION", formatDuration(pass.aos, pass.los), modifier = Modifier.weight(1f), alignment = TextAlign.Center)
                PassDatum("LOS", formatAppTime(pass.los), color = Color(0xFFFF4D5A), modifier = Modifier.weight(1f), alignment = TextAlign.End)
            }
            currentTuningPoint?.let { tuningPoint ->
                val channel = tuningPoints.indexOf(tuningPoint) + 1
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(SpaceBorder))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("TUNE", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Text("CH $channel", style = MaterialTheme.typography.labelLarge, color = SignalGreen)
                    }
                    Text(
                        formatFrequencyHertz(tuningPoint.frequencyHertz.chirpChannelFrequencyHertz()),
                        style = MaterialTheme.typography.titleLarge,
                        color = CyanPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
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
private fun CompassLabel(
    label: String,
    azimuthDegrees: Double,
    headingDegrees: Double,
    radius: androidx.compose.ui.unit.Dp,
) {
    val radians = Math.toRadians(azimuthDegrees - headingDegrees)
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = label,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(
                    x = (radius.value * kotlin.math.sin(radians)).dp,
                    y = (-radius.value * kotlin.math.cos(radians)).dp,
                ),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (label == "N") Color(0xFFFF4D5A) else TextSecondary,
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
                color = CyanPrimary,
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
            color = TextSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
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
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = accent.copy(alpha = 0.16f),
                radius = size.minDimension * 0.37f,
                style = Stroke(width = 1.dp.toPx()),
            )
            drawArc(
                color = accent.copy(alpha = 0.42f),
                startAngle = 22f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(size.width * 0.08f, size.height * 0.08f),
                size = Size(size.width * 0.84f, size.height * 0.84f),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        Image(
            painter = painterResource(id = satelliteArtwork(pass)),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun StatusPill(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    compact: Boolean = false,
) {
    Surface(
        modifier = modifier.border(1.dp, accent.copy(alpha = 0.8f), CircleShape),
        shape = CircleShape,
        color = accent.copy(alpha = 0.13f),
        contentColor = accent,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = if (compact) 2.dp else 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (progress == null) Arrangement.Center else Arrangement.spacedBy(3.dp),
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.width(64.dp).height(3.dp),
                    color = accent,
                    trackColor = accent.copy(alpha = 0.2f),
                )
            }
        }
    }
}

@Composable
private fun StarField(
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    Canvas(modifier = modifier) {
        val stars = listOf(
            Offset(0.04f, 0.12f), Offset(0.09f, 0.61f), Offset(0.14f, 0.29f),
            Offset(0.19f, 0.84f), Offset(0.25f, 0.17f), Offset(0.28f, 0.48f),
            Offset(0.34f, 0.72f), Offset(0.39f, 0.31f), Offset(0.44f, 0.9f),
            Offset(0.49f, 0.11f), Offset(0.53f, 0.58f), Offset(0.58f, 0.27f),
            Offset(0.63f, 0.78f), Offset(0.68f, 0.42f), Offset(0.73f, 0.08f),
            Offset(0.77f, 0.64f), Offset(0.82f, 0.23f), Offset(0.87f, 0.87f),
            Offset(0.92f, 0.51f), Offset(0.97f, 0.18f), Offset(0.11f, 0.95f),
            Offset(0.31f, 0.05f), Offset(0.57f, 0.96f), Offset(0.91f, 0.75f),
        )
        stars.forEachIndexed { index, star ->
            drawCircle(
                color = CyanSecondary.copy(alpha = alpha * if (index % 4 == 0) 0.9f else 0.45f),
                radius = if (index % 4 == 0) 2.3.dp.toPx() else 1.2.dp.toPx(),
                center = Offset(size.width * star.x, size.height * star.y),
            )
        }
    }
}

@Composable
private fun ConstellationField(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val points = listOf(
            Offset(0.34f, 0.37f), Offset(0.39f, 0.32f), Offset(0.45f, 0.37f), Offset(0.49f, 0.30f),
            Offset(0.58f, 0.60f), Offset(0.63f, 0.55f), Offset(0.69f, 0.60f), Offset(0.72f, 0.52f),
            Offset(0.35f, 0.63f), Offset(0.40f, 0.69f), Offset(0.46f, 0.65f), Offset(0.50f, 0.73f),
        )
        val links = listOf(
            0 to 1, 1 to 2, 2 to 3,
            4 to 5, 5 to 6, 6 to 7,
            8 to 9, 9 to 10, 10 to 11,
        )
        links.forEach { (from, to) ->
            drawLine(
                color = TextSecondary.copy(alpha = 0.16f),
                start = Offset(size.width * points[from].x, size.height * points[from].y),
                end = Offset(size.width * points[to].x, size.height * points[to].y),
                strokeWidth = 0.7.dp.toPx(),
            )
        }
        points.forEachIndexed { index, point ->
            drawCircle(
                color = if (index % 4 == 0) TextPrimary.copy(alpha = 0.42f) else CyanSecondary.copy(alpha = 0.28f),
                radius = if (index % 4 == 0) 1.4.dp.toPx() else 0.8.dp.toPx(),
                center = Offset(size.width * point.x, size.height * point.y),
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
            text = formatAppTime(instant),
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
                "No visible passes in this window"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (unfilteredPassCount > 0) {
                "$unfilteredPassCount passes are hidden by the current elevation or mode filters."
            } else {
                "Try a longer look-ahead period or refresh the orbital data."
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

private val timeFormatter24 = DateTimeFormatter
    .ofPattern("HH:mm")
    .withZone(ZoneId.systemDefault())

private val timeFormatter12 = DateTimeFormatter
    .ofPattern("h:mm a")
    .withZone(ZoneId.systemDefault())

private val LocalTimeFormatter = staticCompositionLocalOf { timeFormatter24 }

private fun timeFormatterFor(use24HourTime: Boolean): DateTimeFormatter =
    if (use24HourTime) timeFormatter24 else timeFormatter12

@Composable
private fun formatAppTime(instant: Instant): String = LocalTimeFormatter.current.format(instant)

private val dateFormatter = DateTimeFormatter
    .ofPattern("MMM d, yyyy")
    .withZone(ZoneId.systemDefault())

private val passDateFormatter = DateTimeFormatter
    .ofPattern("MMM d")
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
    "${compassPoint(pass.aosAzimuthDegrees)} → ${compassPoint(pass.losAzimuthDegrees)}"

private fun passStatusLabel(pass: PassSummary, currentTime: Instant): String = when {
    currentTime.isBefore(pass.aos) -> "Upcoming"
    currentTime.isBefore(pass.los) -> "Active"
    else -> "Passed"
}

private fun statusAccent(status: String, fallback: Color): Color = when (status) {
    "Active" -> SignalGreen
    "Passed" -> Color(0xFFFF4D5A)
    else -> fallback
}

private fun passProgress(pass: PassSummary, currentTime: Instant): Float {
    val duration = Duration.between(pass.aos, pass.los).toMillis().coerceAtLeast(1L)
    val elapsed = Duration.between(pass.aos, currentTime).toMillis()
    return (elapsed.toDouble() / duration).coerceIn(0.0, 1.0).toFloat()
}

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
    pass.satellite.name.contains("ISS", ignoreCase = true) -> R.drawable.tb_iss
    pass.satellite.modes.any { it == OperatingMode.SsbCw } -> R.drawable.tb_cubesat
    pass.satellite.name.contains("RS", ignoreCase = true) -> R.drawable.tb_cubesat
    else -> R.drawable.tb_sat
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

private fun formatCountdown(duration: Duration): String {
    val seconds = duration.seconds.coerceAtLeast(0L)
    if (seconds < 60L) return "%02d seconds".format(seconds)
    val hours = seconds / 3_600
    val minutes = (seconds % 3_600) / 60
    return if (hours > 0) {
        "%d hr %02d min".format(hours, minutes)
    } else {
        "%d min".format(minutes)
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
    var locationMap by remember { mutableStateOf<MapView?>(null) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxSize(0.9f),
            shape = MaterialTheme.shapes.large,
            color = SpaceSurfaceHigh,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Choose location", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Tap to move the pin. Pinch or use + / − to zoom.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f)
                        .clip(MaterialTheme.shapes.medium).clipToBounds(),
                ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize().clipToBounds(),
                    factory = { context ->
                        createLocationMap(context, selectedLocation) { latitude, longitude ->
                            selectedLocation = ObserverLocation(
                                latitudeDegrees = latitude,
                                longitudeDegrees = longitude,
                                altitudeMeters = selectedLocation.altitudeMeters,
                            )
                        }.also { locationMap = it }
                    },
                    onRelease = { mapView ->
                        locationMap = null
                        mapView.onPause()
                        mapView.onDetach()
                    },
                    update = { mapView ->
                        val marker = mapView.tag as Marker
                        marker.position = GeoPoint(selectedLocation.latitudeDegrees, selectedLocation.longitudeDegrees)
                        mapView.invalidate()
                    },
                )
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                        shape = MaterialTheme.shapes.small,
                        color = SpaceSurfaceHigh,
                        shadowElevation = 4.dp,
                    ) {
                        Column {
                            TextButton(
                                onClick = { locationMap?.controller?.zoomIn() },
                                modifier = Modifier.size(48.dp).semantics { contentDescription = "Zoom in" },
                            ) { Text("+", style = MaterialTheme.typography.headlineSmall) }
                            TextButton(
                                onClick = { locationMap?.controller?.zoomOut() },
                                modifier = Modifier.size(48.dp).semantics { contentDescription = "Zoom out" },
                            ) { Text("−", style = MaterialTheme.typography.headlineSmall) }
                        }
                    }
                }
                Text(
                    text = "${selectedLocation.latitudeDegrees.formatMapCoordinate()}, " +
                        selectedLocation.longitudeDegrees.formatMapCoordinate(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = { onSave(selectedLocation) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Use this location")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onReset) { Text("Reset to GPS") }
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            }
        }
    }
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
        zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
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
