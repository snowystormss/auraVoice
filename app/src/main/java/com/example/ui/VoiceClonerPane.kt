package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.HarvardSentences
import com.example.engine.LanguageCorpusRegistry
import java.io.File

/**
 * Dedicated Voice Cloner Studio tab.
 *
 * Provides:
 * 1. Reference audio recording and universal audio format import (MP3, FLAC, M4A, OGG, WAV).
 * 2. On-device 24kHz Mono WAV transcoding with auto-trimming and pure-voice recommendations.
 * 3. 1-sentence quick preview verification via F5-TTS Flow Matching ODE solver.
 * 4. Commitment button to launch the 300-sample F5-TTS -> Piper ONNX training pipeline.
 */
@Composable
fun VoiceClonerPane(
    viewModel: VoiceViewModel,
    modifier: Modifier = Modifier
) {
    val trainingState by viewModel.trainingState.collectAsState()
    val isRecordingMic by viewModel.isRecordingMic.collectAsState()
    val micAmplitude by viewModel.micAmplitude.collectAsState()
    val recordedDurationSec by viewModel.recordedDurationSec.collectAsState()
    val referenceAudioFile by viewModel.referenceAudioFile.collectAsState()
    val isTranscoding by viewModel.isTranscoding.collectAsState()
    val transcodeResult by viewModel.transcodeResult.collectAsState()
    val isReferencePlaying by viewModel.isReferencePlaying.collectAsState()
    val previewAudioFile by viewModel.previewAudioFile.collectAsState()
    val previewOdeStep by viewModel.previewOdeStep.collectAsState()
    val isPreviewPlaying by viewModel.isPreviewPlaying.collectAsState()
    val currentPreviewSentence by viewModel.previewSentenceText.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val cloningLanguage by viewModel.cloningLanguage.collectAsState()

    var sentenceIndex by remember { mutableIntStateOf(0) }

    // File picker for universal audio files (WAV, MP3, FLAC, M4A, OGG)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importAudioUri(uri)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("voice_cloner_screen")
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Error Alert Banner
        if (errorMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearErrorMessage() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Hero Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFFFF4081), Color(0xFF7C4DFF))),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "F5-TTS Voice Cloning Studio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Flow Matching (32 ODE Steps) → Lightweight Piper ONNX",
                                fontSize = 11.sp,
                                color = Color(0xFFFF4081),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = "Provide a single voice sample (15–30s). The app will test acoustic similarity on a 1-sentence preview, generate 300 Harvard sentences, terminate the heavy model to free RAM, and train a compact Piper ONNX model for zero-latency speech.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // Step 1: Reference Audio Ingestion & Recommendations
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFFFF4081), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "1",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = "Audio Ingestion & Reference",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFF4081).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "15–30s Pure Voice",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF4081)
                            )
                        }
                    }

                    // Pure vocal speech recommendation banner
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFFFF4081).copy(alpha = 0.4f), Color(0xFF7C4DFF).copy(alpha = 0.4f))
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("💡", fontSize = 14.sp)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Best Results: Pure Vocal Sample (15–30s)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Use clean spoken audio without background music, reverb, or noise. Any audio file (MP3, FLAC, M4A, OGG, WAV) is decoded and standardized to 24kHz Mono WAV.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    if (isTranscoding) {
                        // Transcoding in progress
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp,
                                    color = Color(0xFFFF4081)
                                )
                                Column {
                                    Text(
                                        text = "Decoding & Transcoding Audio...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Converting to 24,000 Hz 16-bit Mono WAV for F5-TTS",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else if (referenceAudioFile == null) {
                        // Action Buttons: Record or Import
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (isRecordingMic) viewModel.stopRecordingMic() else viewModel.startRecordingMic()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("record_mic_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRecordingMic) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (isRecordingMic) Icons.Default.Close else Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isRecordingMic) "Stop ($recordedDurationSec s)" else "Record Voice")
                            }

                            OutlinedButton(
                                onClick = {
                                    filePickerLauncher.launch(
                                        arrayOf(
                                            "audio/*",
                                            "application/ogg",
                                            "audio/x-wav",
                                            "audio/wav",
                                            "audio/mpeg",
                                            "audio/mp3",
                                            "audio/flac",
                                            "audio/aac",
                                            "audio/m4a",
                                            "audio/x-m4a"
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_audio_btn")
                                    .testTag("import_wav_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("Import Audio", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("MP3•FLAC•WAV", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        if (isRecordingMic) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .background(Color(0xFF131722), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                LiveMicWaveform(amplitude = micAmplitude)
                            }
                        }
                    } else {
                        // Reference Clip Ready Card
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF00E676).copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = referenceAudioFile?.name ?: "reference_voice.wav",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Surface(
                                                color = Color(0xFF7C4DFF).copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = transcodeResult?.originalFormat ?: "AUDIO",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF7C4DFF),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "24.0 kHz Mono WAV • ${String.format("%.1f", transcodeResult?.finalDurationSec ?: 0f)}s duration",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.clearReferenceAudio() },
                                    modifier = Modifier.testTag("clear_ref_audio_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove audio",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            // Auto-trim notification banner if original was > 30s
                            if (transcodeResult?.wasTrimmed == true) {
                                Surface(
                                    color = Color(0xFFFF9100).copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("✂️", fontSize = 12.sp)
                                        Text(
                                            text = "Audio was auto-trimmed to 30.0s (source was ${String.format("%.1f", transcodeResult?.originalDurationSec ?: 0f)}s). Pure voice clips of 15–30s give the highest cloning resemblance!",
                                            fontSize = 11.sp,
                                            color = Color(0xFFFF9100),
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }

                            // Listen to Reference Audio Player
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isReferencePlaying) "Playing Reference Audio..." else "Reference Sample Ready",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (isReferencePlaying) viewModel.stopReferenceAudio() else viewModel.playReferenceAudio()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isReferencePlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                        contentColor = if (isReferencePlaying) Color.White else MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.testTag("play_ref_audio_btn")
                                ) {
                                    Icon(
                                        imageVector = if (isReferencePlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (isReferencePlaying) "Stop" else "Listen to Reference Sample",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Step 2: 1-Sentence Quick Preview (Harvard Sentence)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFF7C4DFF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "2",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = "1-Sentence Acoustic Preview",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF7C4DFF).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "F5-TTS Instant Test",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C4DFF)
                            )
                        }
                    }

                    Text(
                        text = "Listen to an instant preview synthesized with F5-TTS on a phonetically-balanced sentence from the chosen language corpus before running the 300-sample training batch.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Target Language Selector (Strictly no emojis)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Training Language & Phoneme Corpus",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Translate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "${cloningLanguage.shortCode} • ${cloningLanguage.displayName}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(LanguageCorpusRegistry.supportedLanguages) { lang ->
                                FilterChip(
                                    selected = cloningLanguage.code == lang.code,
                                    onClick = {
                                        viewModel.setCloningLanguage(lang)
                                        viewModel.setPreviewSentence(lang.previewSentence)
                                    },
                                    label = {
                                        Text(
                                            text = "${lang.shortCode} ${lang.displayName}",
                                            fontSize = 11.sp
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Target Sentence selection Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${cloningLanguage.displayName} Corpus Sentence (${cloningLanguage.corpusName})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = {
                                    val list = cloningLanguage.trainingSentences
                                    if (list.isNotEmpty()) {
                                        sentenceIndex = (sentenceIndex + 1) % list.size
                                        viewModel.setPreviewSentence(list[sentenceIndex])
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Cycle sentence",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = "\"$currentPreviewSentence\"",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Action: Generate Quick Preview Button
                    if (previewAudioFile == null) {
                        Button(
                            onClick = { viewModel.generateQuickPreview() },
                            enabled = referenceAudioFile != null && trainingState != VoiceViewModel.TrainingState.PREVIEW_GENERATING,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("preview_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (trainingState == VoiceViewModel.TrainingState.PREVIEW_GENERATING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Synthesizing Preview (ODE Step $previewOdeStep/32)...", color = Color.White)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Synthesize 1-Sentence Preview", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Preview audio is ready!
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF7C4DFF).copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF7C4DFF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Preview Generated Successfully",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Flow Matching ODE synthesized preview",
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = {
                                            if (isPreviewPlaying) viewModel.stopPreviewAudio() else viewModel.playPreviewAudio()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isPreviewPlaying) MaterialTheme.colorScheme.error else Color(0xFF7C4DFF)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("play_preview_audio_btn")
                                    ) {
                                        Icon(
                                            imageVector = if (isPreviewPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isPreviewPlaying) "Stop" else "Listen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.generateQuickPreview() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            // Commit to 300 samples
                            Button(
                                onClick = { viewModel.startFullTrainingPipeline() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("start_full_training_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Done, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Proceed to Full 300-Sample Training", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Technical Specs Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Voice Cloning Architecture Specs",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SpecRow(label = "Heavy Synthetic Generator", value = "F5-TTS Flow Matching (32 ODE Steps)")
                    SpecRow(label = "Corpus Dataset", value = "300 Samples • ${cloningLanguage.corpusName} (${cloningLanguage.shortCode})")
                    SpecRow(label = "Batch Processing", value = "20 Micro-Batches (15 Sentences / Batch)")
                    SpecRow(label = "RAM Reclaim", value = "Model Termination + GC Session Freeing")
                    SpecRow(label = "Inference Engine", value = "Piper ONNX (<25MB RAM, <30ms Latency)")
                }
            }
        }
    }
}

@Composable
fun LiveMicWaveform(amplitude: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val bars = 32
        val barWidth = (width / bars) * 0.7f
        val spacing = (width / bars) * 0.3f

        for (i in 0 until bars) {
            val progress = i.toFloat() / bars
            val wave = kotlin.math.sin(progress * 6.28f * 2f + phase)
            val barHeight = ((amplitude * 0.8f + 0.2f * kotlin.math.abs(wave.toFloat())) * height).coerceIn(4f, height)
            val x = i * (barWidth + spacing)
            val y = centerY - (barHeight / 2f)

            drawRoundRect(
                color = Color(0xFFFF4081),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
