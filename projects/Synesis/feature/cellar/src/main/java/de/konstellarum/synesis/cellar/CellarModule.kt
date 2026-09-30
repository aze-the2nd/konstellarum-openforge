package de.konstellarum.synesis.cellar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.konstellarum.synesis.core.sensor.ChartWindow
import de.konstellarum.synesis.core.sensor.ChartWindowLogic
import de.konstellarum.synesis.core.sensor.FetchStatus
import de.konstellarum.synesis.core.sensor.TempFormat
import de.konstellarum.synesis.core.sensor.TempRepository
import de.konstellarum.synesis.core.sensor.TempSample
import de.konstellarum.synesis.core.sensor.TempStats
import de.konstellarum.synesis.core.sensor.TimeRangePreset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CellarModule(repository: TempRepository) {
    val samples by repository.samples.collectAsState()
    val status by repository.status.collectAsState()
    val scope = rememberCoroutineScope()
    var autoRefresh by remember { mutableStateOf(true) }
    var preset by rememberSaveable { mutableStateOf(TimeRangePreset.LAST_24_HOURS) }
    var zoomWindow by remember(preset) { mutableStateOf<ChartWindow?>(null) }

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
            samples.size >= 2 -> {
                val dataMin = samples.minOf { it.t }
                val dataMax = samples.maxOf { it.t }
                val window = zoomWindow ?: preset.window(dataMax, dataMin, dataMax)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TimeRangePreset.entries.forEach { option ->
                        FilterChip(
                            selected = preset == option,
                            onClick = { preset = option },
                            label = { Text(option.label) },
                        )
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TempChart(
                            samples = samples,
                            window = window,
                            dataMin = dataMin,
                            dataMax = dataMax,
                            onZoomChange = { zoomWindow = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = TempFormat.rangeLabel(window.startSec, window.endSec),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.weight(1f),
                            )
                            if (zoomWindow != null) {
                                TextButton(onClick = { zoomWindow = null }) {
                                    Text("Zoom zurücksetzen")
                                }
                            }
                        }
                    }
                }
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

/**
 * Temperature line chart with labeled axes: the Y scale shows round degree values, the
 * X axis shows absolute clock times (HH:mm) or dates (dd.MM.) depending on the visible
 * span. Pinch to zoom, drag to pan; the visible window is clamped to the data extent.
 */
@Composable
private fun TempChart(
    samples: List<TempSample>,
    window: ChartWindow,
    dataMin: Long,
    dataMax: Long,
    onZoomChange: (ChartWindow?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = remember(labelColor) { TextStyle(fontSize = 10.sp, color = labelColor) }

    val currentWindow = rememberUpdatedState(window)
    val extent = remember(dataMin, dataMax) { ChartWindow(dataMin, dataMax) }
    val extentState = rememberUpdatedState(extent)

    Canvas(
        modifier = modifier.pointerInput(samples.size, extent) {
            detectTransformGestures { centroid, pan, zoom, _ ->
                val current = currentWindow.value
                val width = size.width.toFloat()
                if (width <= 0f || zoom <= 0f) return@detectTransformGestures

                val centroidSec = current.startSec + (centroid.x / width * current.spanSec)
                val fraction = ((centroidSec - current.startSec).toDouble() / current.spanSec)
                    .coerceIn(0.0, 1.0)
                val span = (current.spanSec / zoom).toLong()
                val start = (centroidSec - fraction * span).toLong()
                val clamped = ChartWindowLogic.clampWindow(
                    ChartWindow(startSec = start, endSec = start + span),
                    dataMinSec = extentState.value.startSec,
                    dataMaxSec = extentState.value.endSec,
                )
                onZoomChange(clamped)
            }
        },
    ) {
        val padL = 48.dp.toPx()
        val padR = 14.dp.toPx()
        val padT = 18.dp.toPx()
        val padB = 28.dp.toPx()
        val innerW = size.width - padL - padR
        val innerH = size.height - padT - padB
        if (innerW <= 0f || innerH <= 0f) return@Canvas

        val visible = samples.filter { it.t in window.startSec..window.endSec }
        if (visible.size < 2) {
            drawText(
                textMeasurer.measure(
                    AnnotatedString("keine Messwerte im Fenster"),
                    labelStyle,
                ),
                topLeft = Offset(padL, padT + innerH / 2),
            )
            return@Canvas
        }

        val minTemp = visible.minOf { it.tempC }
        val maxTemp = visible.maxOf { it.tempC }
        val bounds = ChartWindowLogic.temperatureBounds(minTemp, maxTemp)

        fun xOf(t: Long): Float =
            padL + (t - window.startSec).toFloat() / window.spanSec * innerW

        fun yOf(temp: Double): Float =
            padT + ((bounds.hi - temp) / (bounds.hi - bounds.lo)).toFloat() * innerH

        // Y grid with labeled scale
        var value = bounds.lo
        while (value <= bounds.hi + bounds.step * 0.01) {
            val y = yOf(value)
            drawLine(gridColor, Offset(padL, y), Offset(padL + innerW, y), strokeWidth = 1f)
            val label = textMeasurer.measure(AnnotatedString(TempFormat.axisCelsius(value)), labelStyle)
            drawText(label, topLeft = Offset(padL - label.size.width - 6f, y - label.size.height / 2f))
            value += bounds.step
        }

        // X grid with absolute time labels
        val spanSec = window.spanSec
        ChartWindowLogic.timeTicks(window.startSec, window.endSec).forEach { tick ->
            val x = xOf(tick)
            drawLine(gridColor, Offset(x, padT), Offset(x, padT + innerH), strokeWidth = 1f)
            val label = textMeasurer.measure(AnnotatedString(TempFormat.axisTime(tick, spanSec)), labelStyle)
            val labelX = (x - label.size.width / 2f)
                .coerceIn(padL, size.width - padR - label.size.width)
            drawText(label, topLeft = Offset(labelX, padT + innerH + 6f))
        }

        // Axis titles
        drawText(
            textMeasurer.measure(AnnotatedString("°C"), labelStyle),
            topLeft = Offset(2f, 0f),
        )
        val timeTitle = textMeasurer.measure(AnnotatedString("Uhrzeit"), labelStyle)
        drawText(
            timeTitle,
            topLeft = Offset(size.width - padR - timeTitle.size.width, size.height - timeTitle.size.height),
        )

        // Area under the line
        val path = Path()
        visible.forEachIndexed { index, point ->
            if (index == 0) path.moveTo(xOf(point.t), yOf(point.tempC))
            else path.lineTo(xOf(point.t), yOf(point.tempC))
        }
        val areaPath = Path().apply {
            addPath(path)
            lineTo(xOf(visible.last().t), padT + innerH)
            lineTo(xOf(visible.first().t), padT + innerH)
            close()
        }
        drawPath(path = areaPath, color = lineColor.copy(alpha = 0.12f))
        drawPath(path = path, color = lineColor, style = Stroke(width = 2.dp.toPx()))

        // Latest visible point
        val last = visible.last()
        drawCircle(
            color = lineColor,
            radius = 3.dp.toPx(),
            center = Offset(xOf(last.t), yOf(last.tempC)),
        )
    }
}

private const val AUTO_REFRESH_MILLIS = 60_000L
