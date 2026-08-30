package com.thenetworkings.satqso.ui.passes

import android.Manifest
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
        onShowFilters = viewModel::showFilters,
        onDismissFilters = viewModel::dismissFilters,
        onMinimumElevationSelected = viewModel::setMinimumElevationDegrees,
        onOperatingModeToggled = viewModel::toggleOperatingMode,
        onPassSelected = viewModel::selectPass,
        onClosePassDetails = viewModel::closePassDetails,
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
    onShowFilters: () -> Unit,
    onDismissFilters: () -> Unit,
    onMinimumElevationSelected: (Int) -> Unit,
    onOperatingModeToggled: (OperatingMode) -> Unit,
    onPassSelected: (PassSummary) -> Unit,
    onClosePassDetails: () -> Unit,
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
                                "Visible passes today"
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
                    PassDetail(pass = selectedPass)
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
                            onPassSelected = onPassSelected,
                        )
                    }
                }
                if (uiState.showManualLocationEditor) {
                    ManualLocationDialog(
                        onDismiss = onDismissManualLocation,
                        onSave = onSaveManualLocation,
                    )
                }
                if (uiState.showFilters) {
                    FilterDialog(
                        minimumElevationDegrees = uiState.minimumElevationDegrees,
                        selectedOperatingModes = uiState.selectedOperatingModes,
                        onMinimumElevationSelected = onMinimumElevationSelected,
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
    onPassSelected: (PassSummary) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            LocationSummaryCard(observerLocation = observerLocation)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "TODAY",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyanPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        text = "${dateFormatter.format(LocalDate.now())} - " +
                            passCountLabel(passes.size, unfilteredPassCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(passes) { pass ->
            PassCard(pass = pass, onClick = { onPassSelected(pass) })
        }
    }
}

@Composable
private fun PassCard(
    pass: PassSummary,
    onClick: () -> Unit,
) {
    val accent = passAccent(pass)
    val isActive = Instant.now().let { it >= pass.aos && it <= pass.los }
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
                        text = if (isActive) "ACTIVE" else "UPCOMING",
                        accent = if (isActive) SignalGreen else accent,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    PassDatum(
                        label = if (isActive) "PASS" else "AOS",
                        value = if (isActive) {
                            "${timeFormatter.format(pass.aos)} - ${timeFormatter.format(pass.los)}"
                        } else {
                            timeFormatter.format(pass.aos)
                        },
                    )
                    PassDatum("MAX ELEV.", "${pass.maxElevationDegrees.roundToInt()} deg", accent)
                    PassDatum("DIRECTION", passDirection(pass), accent)
                }
            }
        }
    }
}

@Composable
private fun LocationSummaryCard(observerLocation: ObserverLocation?) {
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
private fun PassDetail(pass: PassSummary) {
    val accent = passAccent(pass)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            OrbitPreviewCard(pass = pass, accent = accent)
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
private fun OrbitPreviewCard(
    pass: PassSummary,
    accent: Color,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.05f)
            .border(1.dp, SpaceBorder, MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = SpaceSurfaceHigh.copy(alpha = 0.82f)),
        shape = MaterialTheme.shapes.medium,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
            SatelliteAvatar(
                pass = pass,
                accent = accent,
                sizeDp = 92,
                modifier = Modifier.align(Alignment.Center),
            )
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
    selectedOperatingModes: Set<OperatingMode>,
    onMinimumElevationSelected: (Int) -> Unit,
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

private val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d")

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

private fun formatDuration(aos: Instant, los: Instant): String {
    val duration = Duration.between(aos, los)
    val minutes = duration.toMinutes()
    val seconds = duration.minusMinutes(minutes).seconds

    return "${minutes}m ${seconds}s"
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
