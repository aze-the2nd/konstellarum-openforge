package de.konstellarum.synesis.transcribe

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.konstellarum.synesis.core.domain.Transcript
import de.konstellarum.synesis.core.domain.TranscriptRepository
import de.konstellarum.synesis.core.transcribe.AiRefinementPrompt
import de.konstellarum.synesis.core.transcribe.TranscriptionText
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TranscribeModule(
    repository: TranscriptRepository,
    onOpenAiChat: () -> Boolean,
) {
    val transcripts by repository.transcripts.collectAsState()
    val clipboard = LocalClipboardManager.current
    var status by remember { mutableStateOf("Bereit für ein neues Diktat.") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val candidates = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                .orEmpty()
            val transcript = TranscriptionText.firstUsable(candidates)
            if (transcript == null) {
                status = "Keine Sprache erkannt."
            } else {
                repository.addTranscript(transcript)
                status = "Transkript gespeichert."
            }
        } else {
            status = "Transkription abgebrochen."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Transkribieren",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "Sprich über die Android-Spracherkennung; Synesis speichert das erkannte Ergebnis lokal. Mit KI präzisieren kopiert einen Auftrag für Thomas und öffnet den Chat.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = {
                val intent = speechIntent()
                try {
                    launcher.launch(intent)
                    status = "Spracherkennung läuft …"
                } catch (_: ActivityNotFoundException) {
                    status = "Auf diesem Gerät ist keine Spracherkennung installiert."
                }
            },
        ) {
            Text("Diktat starten")
        }
        Text(status, style = MaterialTheme.typography.labelMedium)

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
                        onAiRefine = {
                            val prompt = AiRefinementPrompt.fromTranscript(transcript.text)
                            if (prompt.isBlank()) {
                                status = "Leeres Transkript kann nicht präzisiert werden."
                            } else {
                                clipboard.setText(AnnotatedString(prompt))
                                status = if (onOpenAiChat()) {
                                    "KI-Auftrag kopiert; Chat geöffnet. Dort einfügen und senden."
                                } else {
                                    "KI-Auftrag kopiert; Chat konnte nicht automatisch geöffnet werden."
                                }
                            }
                        },
                        onDelete = { repository.removeTranscript(transcript.id) },
                    )
                }
            }
        }
    }
}

private fun speechIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
    putExtra(RecognizerIntent.EXTRA_PROMPT, "Synesis Transkription")
}

@Composable
private fun TranscriptCard(
    transcript: Transcript,
    onCopy: () -> Unit,
    onAiRefine: () -> Unit,
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCopy) {
                    Text("Kopieren")
                }
                TextButton(onClick = onAiRefine) {
                    Text("KI präzisieren")
                }
                TextButton(onClick = onDelete) {
                    Text("Löschen")
                }
            }
        }
    }
}
