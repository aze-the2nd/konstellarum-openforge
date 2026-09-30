package de.konstellarum.synesis.transcribe

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.konstellarum.synesis.core.domain.Transcript
import de.konstellarum.synesis.core.domain.TranscriptRepository
import de.konstellarum.synesis.core.domain.WhisperTranscriptionRepository
import de.konstellarum.synesis.core.transcribe.WhisperTranscriptionResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
fun TranscribeModule(
    repository: TranscriptRepository,
    transcriptionRepository: WhisperTranscriptionRepository,
) {
    val transcripts by repository.transcripts.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf("Bereit für ein neues Diktat.") }
    var recording by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var elapsedSec by remember { mutableLongStateOf(0L) }
    var pendingRetry by remember { mutableStateOf<File?>(null) }

    val recorder = remember { AudioRecorder() }
    var recordingFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(recording) {
        while (recording) {
            delay(1000)
            elapsedSec += 1
        }
    }

    DisposableEffect(Unit) {
        onDispose { recorder.stop() }
    }

    fun startRecording() {
        if (recording || busy) return
        val output = File(context.cacheDir, RECORDING_FILE_NAME)
        try {
            recorder.start(output)
        } catch (_: Exception) {
            status = "Aufnahme konnte nicht gestartet werden (Mikrofon verfügbar?)."
            return
        }
        recordingFile = output
        elapsedSec = 0
        recording = true
        pendingRetry = null
        status = "Aufnahme läuft …"
    }

    fun stopAndTranscribe() {
        if (!recording) return
        recorder.stop()
        recording = false
        val audio = recordingFile ?: run {
            status = "Keine Aufnahme vorhanden."
            return
        }
        val packageDir = try {
            repository.newPackageDir()
        } catch (_: Exception) {
            status = "Transkript-Ordner konnte nicht angelegt werden."
            return
        }
        val storedAudio = File(packageDir, FileTranscriptRepository.AUDIO_FILE_NAME)
        if (!audio.copyTo(storedAudio, overwrite = true).exists()) {
            status = "Audio-Datei konnte nicht abgelegt werden."
            return
        }
        audio.delete()

        scope.launch {
            busy = true
            status = "Whisper transkribiert … (kann etwas dauern)"
            when (val result = transcriptionRepository.transcribe(storedAudio)) {
                is WhisperTranscriptionResult.Success -> {
                    repository.commitPackage(packageDir, result.text)
                    val modelLabel = result.model ?: "Whisper"
                    status = "Transkribiert mit $modelLabel; Paket gespeichert."
                    pendingRetry = null
                }

                is WhisperTranscriptionResult.Failure -> {
                    pendingRetry = storedAudio
                    status = "Transkription fehlgeschlagen: ${result.message} " +
                        "(Aufnahme bleibt unter ${packageDir.name} erhalten)."
                }
            }
            busy = false
        }
    }

    fun retryTranscription() {
        val audio = pendingRetry ?: return
        val packageDir = audio.parentFile ?: return
        scope.launch {
            busy = true
            status = "Whisper transkribiert erneut …"
            when (val result = transcriptionRepository.transcribe(audio)) {
                is WhisperTranscriptionResult.Success -> {
                    repository.commitPackage(packageDir, result.text)
                    status = "Transkribiert mit ${result.model ?: "Whisper"}; Paket gespeichert."
                    pendingRetry = null
                }

                is WhisperTranscriptionResult.Failure -> {
                    status = "Erneuter Versuch fehlgeschlagen: ${result.message}"
                }
            }
            busy = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) startRecording() else status = "Mikrofon-Berechtigung fehlt."
    }

    fun beginRecording() {
        val granted = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) startRecording() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Diktat & Transkription",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "Nimm ein Diktat auf; die private Whisper-Instanz auf der Bridge transkribiert es. " +
                "Audio und Text werden zusammen als Paket lokal abgelegt.",
            style = MaterialTheme.typography.bodyMedium,
        )

        when {
            recording -> {
                Button(onClick = ::stopAndTranscribe) {
                    Text("Stoppen & transkribieren")
                }
                Text(
                    text = "Aufnahme läuft: ${formatElapsed(elapsedSec)}",
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            busy -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                    Text(status, style = MaterialTheme.typography.labelMedium)
                }
            }

            else -> {
                Button(onClick = ::beginRecording) {
                    Text("Aufnahme starten")
                }
                if (pendingRetry != null) {
                    TextButton(onClick = ::retryTranscription) {
                        Text("Erneut versuchen")
                    }
                }
                Text(status, style = MaterialTheme.typography.labelMedium)
            }
        }

        if (transcripts.isEmpty()) {
            Text("Noch keine Transkripte.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(transcripts, key = { it.id }) { transcript ->
                    TranscriptCard(
                        transcript = transcript,
                        onCopy = {
                            clipboard.setText(AnnotatedString(transcript.text))
                            status = "Transkript kopiert."
                        },
                        onDelete = {
                            repository.removeTranscript(transcript.id)
                            if (pendingRetry?.parentFile?.name == transcript.id) pendingRetry = null
                        },
                    )
                }
            }
        }
    }
}

private fun formatElapsed(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun TranscriptCard(
    transcript: Transcript,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = transcript.text,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(Date(transcript.createdAtEpochMillis)),
                style = MaterialTheme.typography.labelMedium,
            )
            transcript.packageDir?.let {
                Text(
                    text = "Paket: $it (${FileTranscriptRepository.AUDIO_FILE_NAME} + " +
                        "${FileTranscriptRepository.TRANSCRIPT_FILE_NAME})",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCopy) {
                    Text("Kopieren")
                }
                TextButton(onClick = onDelete) {
                    Text("Löschen")
                }
            }
        }
    }
}

private const val RECORDING_FILE_NAME = "recording.m4a"
