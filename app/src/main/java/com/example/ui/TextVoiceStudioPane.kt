package com.example.ui

import androidx.compose.animation.*
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.HarvardSentences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextVoiceStudioPane(
    viewModel: VoiceViewModel,
    modifier: Modifier = Modifier
) {
    val description by viewModel.textVoiceDescription.collectAsState()
    val voiceName by viewModel.textVoiceName.collectAsState()
    val parsedTraits by viewModel.parsedVoiceTraits.collectAsState()
    val refinementHistory by viewModel.textRefinementHistory.collectAsState()
    val auditionSample by viewModel.textAuditionSample.collectAsState()
    val lastRefinementSummary by viewModel.lastRefinementSummary.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()

    var refinementInput by remember { mutableStateOf("") }
    var sentenceIndex by remember { mutableIntStateOf(0) }
    var showSaveDialog by remember { mutableStateOf(false) }

    val inspirationPrompts = listOf(
        "Deep Baritone Narrator" to "Deep baritone narrator with warm resonance and measured calm cadence",
        "Bubbly Anime Companion" to "High pitch energetic anime companion, rapid speech, bubbly and playful",
        "Calm Meditation Guide" to "Ultra calm meditation guide, gentle soft whispering voice, very slow cadence",
        "Authoritative Tech Anchor" to "Formal professional news anchor, crisp clear articulation, steady pace",
        "Gruff Fantasy Hero" to "Gravelly gruff fantasy hero, deep raspy tone, deliberate earthy pace"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("text_voice_studio_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Text-to-Voice Studio",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Craft & fine-tune acoustic voices purely through text descriptions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Quick Inspiration Pills
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Inspiration Presets",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(inspirationPrompts) { (label, promptText) ->
                        FilterChip(
                            selected = description == promptText,
                            onClick = {
                                viewModel.updateTextVoiceName(label)
                                viewModel.updateTextVoiceDescription(promptText)
                            },
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        // Voice Description Input Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Voice Description",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Describe timbre, pitch, tempo, mood, and vocal weight in natural language:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { viewModel.updateTextVoiceDescription(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_description_input"),
                        minLines = 3,
                        maxLines = 5,
                        placeholder = { Text("e.g. Deep baritone narrator, warm low pitch, slow deliberate cadence, formal tone") },
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Acoustic Gauges Card
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Live Acoustic Parameters (Extracted On-Device)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Pitch Badge
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Pitch",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${String.format("%.2f", parsedTraits.pitch)}x",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    LinearProgressIndicator(
                                        progress = { (parsedTraits.pitch / 2.0f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .width(80.dp)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                    )
                                }

                                // Speed / Rate Badge
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Speed / Rate",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${String.format("%.2f", parsedTraits.speechRate)}x",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    LinearProgressIndicator(
                                        progress = { (parsedTraits.speechRate / 2.0f).coerceIn(0f, 1f) },
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier
                                            .width(80.dp)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                    )
                                }

                                // Emotion Badge
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Emotion",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = parsedTraits.emotion.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    SuggestionChip(
                                        onClick = {},
                                        label = {
                                            Text(
                                                parsedTraits.emotion.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp
                                            )
                                        },
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }

                            // Acoustic tags row
                            if (parsedTraits.detectedTags.isNotEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Traits:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    parsedTraits.detectedTags.forEach { tag ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = tag,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Iterative Text Fine-Tuning Box
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                        )
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "2. Fine-Tune Purely Through Text",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Instruct modifications using plain conversational phrases",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Quick refinement helper pills
                    val quickTuningPhrases = listOf(
                        "Make it deeper",
                        "Higher pitch",
                        "Speak slower",
                        "Speak faster",
                        "More enthusiastic",
                        "Calm down"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(quickTuningPhrases) { phrase ->
                            SuggestionChip(
                                onClick = {
                                    refinementInput = phrase
                                    viewModel.applyTextRefinement(phrase)
                                },
                                label = { Text(phrase, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    // Refinement text input row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = refinementInput,
                            onValueChange = { refinementInput = it },
                            placeholder = { Text("e.g. Lower pitch a bit, speak more briskly") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("refinement_instruction_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        FilledTonalButton(
                            onClick = {
                                if (refinementInput.isNotBlank()) {
                                    viewModel.applyTextRefinement(refinementInput)
                                    refinementInput = ""
                                }
                            },
                            modifier = Modifier.testTag("apply_refinement_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Tune")
                        }
                    }

                    // Last refinement feedback banner
                    lastRefinementSummary?.let { summary ->
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = summary,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Refinement history
                    if (refinementHistory.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Recent Refinements:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            refinementHistory.takeLast(3).forEach { hist ->
                                Text(
                                    text = "• $hist",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Audio Audition Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3. Audition Voice Sample",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = {
                                val list = HarvardSentences.SENTENCES
                                sentenceIndex = (sentenceIndex + 1) % list.size
                                viewModel.updateTextAuditionSample(list[sentenceIndex])
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Next sample sentence",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = auditionSample,
                        onValueChange = { viewModel.updateTextAuditionSample(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("audition_sample_text_input"),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        maxLines = 3,
                        label = { Text("Sample Sentence to Speak") }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isSpeaking) {
                                    viewModel.stopSpeaking()
                                } else {
                                    viewModel.auditionTextVoice()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("audition_voice_speak_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (isSpeaking) "Stop Speaking" else "Audition Voice")
                        }

                        OutlinedButton(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier.testTag("save_text_voice_open_dialog_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Save Voice")
                        }
                    }
                }
            }
        }
    }

    // Save Voice Profile Dialog
    if (showSaveDialog) {
        var nameInput by remember { mutableStateOf(voiceName) }

        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text("Save Text-Crafted Voice Profile", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter a name to save this tuned voice into your personal Voices Library:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Voice Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_text_voice_name_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Pitch: ${String.format("%.2f", parsedTraits.pitch)}x | Speed: ${String.format("%.2f", parsedTraits.speechRate)}x | Emotion: ${parsedTraits.emotion}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateTextVoiceName(nameInput)
                        viewModel.saveTextCraftedVoice(nameInput)
                        showSaveDialog = false
                    },
                    modifier = Modifier.testTag("confirm_save_text_voice_btn")
                ) {
                    Text("Save to Library")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
