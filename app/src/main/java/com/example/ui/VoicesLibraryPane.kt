package com.example.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VoiceProfile
import com.example.engine.LanguageCorpusRegistry
import com.example.engine.LanguageInfo
import com.example.engine.VoiceModelExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoicesLibraryPane(
    viewModel: VoiceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allVoices by viewModel.allVoices.collectAsState()
    val userVoices = remember(allVoices) { allVoices.filter { !it.isPreset } }
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isExporting by viewModel.isExportingVoice.collectAsState()
    val exportMessage by viewModel.exportMessage.collectAsState()
    val exportedResult by viewModel.exportedResult.collectAsState()

    var filterCategory by remember { mutableStateOf("ALL") } // "ALL", "CLONED", "TEXT"
    var voiceToDelete by remember { mutableStateOf<VoiceProfile?>(null) }
    var voiceToExport by remember { mutableStateOf<VoiceProfile?>(null) }
    var voiceToChangeLanguage by remember { mutableStateOf<VoiceProfile?>(null) }
    var showExportSuccessDialog by remember { mutableStateOf(false) }
    var playingVoiceId by remember { mutableStateOf<Int?>(null) }

    // System Documents "Create Document" SAF Launcher for backing up .zip file to local storage
    val createZipFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            viewModel.saveExportedZipToUri(uri) {
                // Done saving
            }
        }
    }

    val filteredVoices = remember(userVoices, filterCategory) {
        when (filterCategory) {
            "CLONED" -> userVoices.filter { it.modelType == "PIPER_ONNX" || it.isTrained }
            "TEXT" -> userVoices.filter { it.modelType != "PIPER_ONNX" && !it.isTrained }
            else -> userVoices
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("voices_library_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Library Hero Header
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
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
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FolderShared,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Voices Library",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${userVoices.size} personal voice models ready for on-device synthesis",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Export Banner if notification is active
        exportMessage?.let { msg ->
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_export_banner")
                ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.dismissExportNotification() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Filter Chips Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                FilterChip(
                    selected = filterCategory == "ALL",
                    onClick = { filterCategory = "ALL" },
                    label = { Text("All (${userVoices.size})") }
                )
                FilterChip(
                    selected = filterCategory == "CLONED",
                    onClick = { filterCategory = "CLONED" },
                    label = { Text("Audio Clones (${userVoices.count { it.modelType == "PIPER_ONNX" || it.isTrained }})") },
                    leadingIcon = {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                )
                FilterChip(
                    selected = filterCategory == "TEXT",
                    onClick = { filterCategory = "TEXT" },
                    label = { Text("Text Crafted (${userVoices.count { it.modelType != "PIPER_ONNX" && !it.isTrained }})") },
                    leadingIcon = {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                )
            }
        }

        // Empty state
        if (filteredVoices.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (filterCategory == "ALL") "No Voices Found" else "No $filterCategory Voices Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Record reference audio in Voice Cloner or describe a new voice in Text Studio to create one.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilledTonalButton(
                                onClick = { viewModel.selectTab(VoiceViewModel.StudioTab.CLONER) }
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Go to Cloner")
                            }
                            Button(
                                onClick = { viewModel.selectTab(VoiceViewModel.StudioTab.TEXT_STUDIO) }
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Go to Text Studio")
                            }
                        }
                    }
                }
            }
        } else {
            // Voices Cards
            items(filteredVoices, key = { it.id }) { voice ->
                VoiceLibraryCard(
                    voice = voice,
                    isPlaying = isSpeaking && playingVoiceId == voice.id,
                    onPlay = { text ->
                        playingVoiceId = voice.id
                        viewModel.speakText(text, voice)
                    },
                    onStop = {
                        viewModel.stopSpeaking()
                        playingVoiceId = null
                    },
                    onDelete = { voiceToDelete = voice },
                    onEditInStudio = { viewModel.editVoiceInStudio(voice) },
                    onExportZip = {
                        voiceToExport = voice
                        viewModel.exportVoiceModel(voice) { result ->
                            showExportSuccessDialog = true
                        }
                    },
                    onChangeLanguage = { voiceToChangeLanguage = voice },
                    onRefine = { refinementText ->
                        viewModel.refineSavedVoiceWithText(voice, refinementText)
                    }
                )
            }
        }
    }

    // Exporting progress indicator overlay
    if (isExporting) {
        Surface(
            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Column {
                            Text(
                                text = "Compressing Voice Model...",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Packaging ONNX weights, configs, and manifest into ZIP",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Export Confirmation & Share Dialog
    if (showExportSuccessDialog && exportedResult != null && voiceToExport != null) {
        val result = exportedResult!!
        val voice = voiceToExport!!

        AlertDialog(
            onDismissRequest = {
                showExportSuccessDialog = false
                voiceToExport = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Export Voice Model Package",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "\"${voice.name}\" was successfully compressed into a standardized ZIP archive ready for backup or sharing.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "File: ${result.fileName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Archive Size: ${VoiceModelExporter.formatFileSize(result.zipSizeBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Contents: ${result.entryCount} files (model.onnx, configs, manifest)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Choose an action below to share via Android share sheet or save directly to your Files app:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Save to Disk via Storage Access Framework
                    OutlinedButton(
                        onClick = {
                            createZipFileLauncher.launch(result.fileName)
                            showExportSuccessDialog = false
                            voiceToExport = null
                        },
                        modifier = Modifier.testTag("export_save_to_disk_btn")
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save to Files")
                    }

                    // Share via System Intent
                    Button(
                        onClick = {
                            val shareIntent = VoiceModelExporter.createShareIntent(
                                context = context,
                                result = result,
                                voiceName = voice.name
                            )
                            context.startActivity(Intent.createChooser(shareIntent, "Share Voice Model ZIP"))
                            showExportSuccessDialog = false
                            voiceToExport = null
                        },
                        modifier = Modifier.testTag("export_share_sheet_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExportSuccessDialog = false
                        voiceToExport = null
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Delete Voice Confirmation Dialog
    voiceToDelete?.let { voice ->
        AlertDialog(
            onDismissRequest = { voiceToDelete = null },
            icon = {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("Delete Voice Model?") },
            text = {
                Text("Are you sure you want to delete \"${voice.name}\"? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteVoice(voice)
                        voiceToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_voice_btn")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { voiceToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Change Language & Corpus Dialog
    voiceToChangeLanguage?.let { voice ->
        ChangeLanguageDialog(
            voice = voice,
            onDismiss = { voiceToChangeLanguage = null },
            onQuickSwitch = { selectedLang ->
                viewModel.updateVoiceLanguage(voice, selectedLang)
                voiceToChangeLanguage = null
            },
            onRetrainCorpus = { selectedLang ->
                viewModel.startLanguageRetraining(voice, selectedLang)
                voiceToChangeLanguage = null
            }
        )
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceLibraryCard(
    voice: VoiceProfile,
    isPlaying: Boolean,
    onPlay: (String) -> Unit,
    onStop: () -> Unit,
    onDelete: () -> Unit,
    onEditInStudio: () -> Unit,
    onExportZip: () -> Unit,
    onChangeLanguage: () -> Unit,
    onRefine: (String) -> Unit
) {
    val isClonedModel = voice.modelType == "PIPER_ONNX" || voice.isTrained
    val currentLang = remember(voice.languageCode) {
        LanguageCorpusRegistry.getByCode(voice.languageCode)
    }
    var testPromptText by remember(voice.id, voice.languageCode) {
        mutableStateOf(currentLang.samplePhrases.firstOrNull() ?: "Hello! This is my on-device voice.")
    }
    var isRefineExpanded by remember { mutableStateOf(false) }
    var refineInstructionInput by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("library_voice_card_${voice.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Avatar, Name, Badges, 3-Dots Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isClonedModel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isClonedModel) Icons.Default.GraphicEq else Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = voice.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isClonedModel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = if (isClonedModel) "PIPER ONNX CLONE" else "TEXT CRAFTED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            // Language Badge (Strictly no emojis)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Translate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "${currentLang.shortCode} • ${currentLang.displayName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }

                            Text(
                                text = "Pitch: ${String.format("%.2f", voice.pitch)}x • Rate: ${String.format("%.2f", voice.speechRate)}x",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 3-Dots Overflow Menu
                Box {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("voice_menu_btn_${voice.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Actions for ${voice.name}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.testTag("voice_menu_dropdown_${voice.id}")
                    ) {
                        // Change / Train Language
                        DropdownMenuItem(
                            text = { Text("Change / Train Language") },
                            onClick = {
                                menuExpanded = false
                                onChangeLanguage()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.testTag("menu_change_language_${voice.id}")
                        )

                        // Edit in Studio
                        DropdownMenuItem(
                            text = { Text("Edit in Studio") },
                            onClick = {
                                menuExpanded = false
                                onEditInStudio()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            modifier = Modifier.testTag("menu_edit_studio_${voice.id}")
                        )

                        // Export ZIP
                        DropdownMenuItem(
                            text = { Text("Export ZIP") },
                            onClick = {
                                menuExpanded = false
                                onExportZip()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.testTag("menu_export_zip_${voice.id}")
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Delete Model
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Delete Model",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            modifier = Modifier.testTag("menu_delete_voice_${voice.id}")
                        )
                    }
                }
            }

            // Description
            if (voice.description.isNotBlank()) {
                Text(
                    text = voice.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Inline Test Speech Bar with Multilingual Sample Phrases
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Speech Test (${currentLang.displayName})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap phrase below to load",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Sample phrase quick selection chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(currentLang.samplePhrases) { phrase ->
                        SuggestionChip(
                            onClick = { testPromptText = phrase },
                            label = {
                                Text(
                                    text = phrase,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = testPromptText,
                        onValueChange = { testPromptText = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("library_test_prompt_input_${voice.id}"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    FilledIconButton(
                        onClick = {
                            if (isPlaying) onStop() else onPlay(testPromptText)
                        },
                        modifier = Modifier.testTag("library_play_btn_${voice.id}")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Stop" else "Speak"
                        )
                    }
                }
            }

            // Expandable Text Fine-Tuning Drawer
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { isRefineExpanded = !isRefineExpanded },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = if (isRefineExpanded) Icons.Default.ExpandLess else Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isRefineExpanded) "Hide Text Fine-Tuner" else "Fine-Tune Voice via Text",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isRefineExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Apply Text Instructions to Adjust this Model:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Quick adjustment pills
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(listOf("Make deeper", "Higher pitch", "Speak slower", "Speak faster", "More cheerful", "More formal")) { pill ->
                                    SuggestionChip(
                                        onClick = {
                                            refineInstructionInput = pill
                                            onRefine(pill)
                                        },
                                        label = { Text(pill, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = refineInstructionInput,
                                    onValueChange = { refineInstructionInput = it },
                                    placeholder = { Text("e.g. Lower pitch, sound calmer") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("refine_input_${voice.id}"),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )

                                FilledTonalButton(
                                    onClick = {
                                        if (refineInstructionInput.isNotBlank()) {
                                            onRefine(refineInstructionInput)
                                            refineInstructionInput = ""
                                        }
                                    },
                                    modifier = Modifier.testTag("library_apply_refine_btn_${voice.id}"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Apply")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangeLanguageDialog(
    voice: VoiceProfile,
    onDismiss: () -> Unit,
    onQuickSwitch: (LanguageInfo) -> Unit,
    onRetrainCorpus: (LanguageInfo) -> Unit
) {
    var selectedLanguage by remember {
        mutableStateOf(LanguageCorpusRegistry.getByCode(voice.languageCode))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Column {
                Text(
                    text = "Change Voice Language",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Model: ${voice.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select a target language to adapt phoneme alignment and acoustic prosody:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(LanguageCorpusRegistry.supportedLanguages, key = { it.code }) { lang ->
                        val isSelected = selectedLanguage.code == lang.code
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedLanguage = lang }
                                .testTag("select_lang_${lang.shortCode.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = lang.shortCode,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = lang.displayName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "(${lang.nativeName})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = lang.corpusName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Sample phrase preview card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Sample Sentence (${selectedLanguage.displayName}):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = selectedLanguage.previewSentence,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Switch Button
                OutlinedButton(
                    onClick = { onQuickSwitch(selectedLanguage) },
                    modifier = Modifier.testTag("quick_switch_lang_btn")
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Quick Switch")
                }

                // Retrain on Corpus Button
                Button(
                    onClick = { onRetrainCorpus(selectedLanguage) },
                    modifier = Modifier.testTag("retrain_corpus_btn")
                ) {
                    Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Train Corpus")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
