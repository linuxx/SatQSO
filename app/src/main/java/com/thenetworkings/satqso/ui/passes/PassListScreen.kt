package com.thenetworkings.satqso.ui.passes

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thenetworkings.satqso.SatQsoDependencies
import com.thenetworkings.satqso.domain.OperatingMode
import com.thenetworkings.satqso.domain.PassSummary
import com.thenetworkings.satqso.domain.Satellite
import com.thenetworkings.satqso.ui.theme.SatQSOTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun PassListRoute(dependencies: SatQsoDependencies) {
    val viewModel: PassListViewModel = viewModel(
        factory = PassListViewModel.Factory(
            locationRepository = dependencies.locationRepository,
            passRepository = dependencies.passRepository,
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
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassListScreen(
    uiState: PassListUiState,
    onRequestLocation: () -> Unit,
    onRefresh: () -> Unit,
) {
    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = {
                    Column {
                        Text("SatQSO")
                        Text(
                            text = "Visible passes today",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            when {
                uiState.needsLocationPermission -> PermissionState(onRequestLocation)
                uiState.isLoading -> LoadingState()
                uiState.errorMessage != null -> ErrorState(
                    message = uiState.errorMessage,
                    onRefresh = onRefresh,
                )
                uiState.passes.isEmpty() -> EmptyState(onRefresh)
                else -> PassList(passes = uiState.passes, onRefresh = onRefresh)
            }
        }
    }
}

@Composable
private fun PassList(
    passes: List<PassSummary>,
    onRefresh: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${passes.size} passes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedButton(onClick = onRefresh) {
                    Text("Refresh")
                }
            }
        }
        items(passes) { pass ->
            PassCard(pass)
        }
    }
}

@Composable
private fun PassCard(pass: PassSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = pass.satellite.modes.joinToString { it.label },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AssistChip(
                    onClick = {},
                    label = { Text("${pass.maxElevationDegrees.roundToInt()} deg") },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                PassTime("AOS", pass.aos)
                PassTime("LOS", pass.los)
                Direction("Path", pass.aosAzimuthDegrees, pass.losAzimuthDegrees)
            }
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
private fun PermissionState(onRequestLocation: () -> Unit) {
    CenteredMessage(
        title = "Location required",
        body = "SatQSO needs your location to calculate visible satellite passes.",
        action = {
            Button(onClick = onRequestLocation) {
                Text("Allow location")
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
private fun ErrorState(message: String, onRefresh: () -> Unit) {
    CenteredMessage(
        title = "Pass calculation failed",
        body = message,
        action = {
            Button(onClick = onRefresh) {
                Text("Try again")
            }
        },
    )
}

@Composable
private fun EmptyState(onRefresh: () -> Unit) {
    CenteredMessage(
        title = "No visible passes today",
        body = "Try refreshing after the latest orbital data is available.",
        action = {
            OutlinedButton(onClick = onRefresh) {
                Text("Refresh")
            }
        },
    )
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

private fun compassPoint(degrees: Double): String {
    val points = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val index = ((degrees + 22.5) / 45.0).toInt() % points.size
    return points[index]
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
        )
    }
}
