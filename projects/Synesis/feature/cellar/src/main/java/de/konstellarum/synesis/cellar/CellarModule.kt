package de.konstellarum.synesis.cellar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import de.konstellarum.synesis.core.sensor.ChartScaling
import de.konstellarum.synesis.core.sensor.FetchStatus
import de.konstellarum.synesis.core.sensor.TempFormat
import de.konstellarum.synesis.core.sensor.TempRepository
import de.konstellarum.synesis.core.sensor.TempSample
import de.konstellarum.synesis.core.sensor.TempStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CellarModule(repository: TempRepository) {
    val samples by repository.samples.collectAsState()
    val status by repository.status.collectAsState()
    val scope = rememberCoroutineScope()
    var autoRefresh by remember { mutableStateOf(true) }

    LaunchedEffect(autoRefresh) {
        while (autoRefresh) {
            repository.fetch()
            delay(AUTO_REFRESH_MILLIS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Keller", style = MaterialTheme.typography.titleLarge)
                StatusLine(status)
            }
            IconButton(onClick = { scope.launch { repository.fetch() } }) {
                Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
            }
        }

        val current = samples.lastOrNull()
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Aktuell", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = current?.let { TempFormat.celsius(it.tempC) } ?: "–",
                    style = MaterialTheme.typography.displayMedium,
                )
                current?.let {
                    Text(
                        text = "Stand: ${TempFormat.timestamp(it.t)}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        TempStats.compute(samples)?.let { stats ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatColumn(label = "Min", value = TempFormat.celsius(stats.min))
                StatColumn(label = "Ø", value = TempFormat.celsius(stats.avg))
                StatColumn(label = "Max", value = TempFormat.celsius(stats.max))
            }
        }

        when {
            samples.size >= 2 -> Card(modifier = Modifier.fillMaxWidth()) {
                TempChart(
                    samples = samples,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(8.dp),
                )
            }

            samples.isEmpty() && status !is FetchStatus.Loading -> Text(
                "Noch keine Messwerte. Verbindung zum Sensor prüfen.",
            )
        }
    }
}

@Composable
private fun StatusLine(status: FetchStatus) {
    when (status) {
        FetchStatus.Idle -> Text("Bereit", style = MaterialTheme.typography.labelMedium)

        FetchStatus.Loading -> Text("Lade …", style = MaterialTheme.typography.labelMedium)

        is FetchStatus.Success -> Text(
            text = "Zuletzt aktualisiert: ${TempFormat.timestamp(status.fetchedAtEpochMillis / 1000)}",
            style = MaterialTheme.typography.labelMedium,
        )

        is FetchStatus.Error -> Text(
            text = "Fehler: ${status.message}",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun TempChart(samples: List<TempSample>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val points = ChartScaling.fit(
            samples = samples,
            width = size.width,
            height = size.height,
            padding = 8.dp.toPx(),
        )
        if (points.size < 2) return@Canvas
        val path = Path()
        points.forEachIndexed { index, point ->
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path = path, color = lineColor, style = Stroke(width = 2.dp.toPx()))
    }
}

private const val AUTO_REFRESH_MILLIS = 60_000L
