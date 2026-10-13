package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FilePresent
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.VoiceProfile
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoicesLibraryPane(
    profiles: List<VoiceProfile>,
    onImportVoice: (Context, String, Uri, Uri) -> Unit,
    onSelectVoice: (VoiceProfile) -> Unit,
    onUpdateParameters: (VoiceProfile, Float, Float, Int) -> Unit,
    onDeleteVoice: (VoiceProfile) -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var showImportSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showImportSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("load .onnx voice") }
            )
        }
    ) { padding ->
        if (profiles.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "no piper .onnx models loaded",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "tap 'load .onnx voice' to import a piper .onnx model file and its matching .onnx.json config from your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = profiles,
                    key = { it.id },
                    contentType = { "voice_card" }
                ) { profile ->
                    OnnxVoiceCard(
                        profile = profile,
                        onSelect = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSelectVoice(profile)
                        },
                        onUpdateParams = { length, noise, speaker ->
                            onUpdateParameters(profile, length, noise, speaker)
                        },
                        onDelete = { onDeleteVoice(profile) },
                        modifier = Modifier.animateItem(
                            fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    )
                }
            }
        }
    }

    if (showImportSheet) {
        ImportOnnxBottomSheet(
            onDismiss = { showImportSheet = false },
            onConfirmImport = { name, onnxUri, jsonUri ->
                onImportVoice(context, name, onnxUri, jsonUri)
                showImportSheet = false
            }
        )
    }
}

@Composable
private fun OnnxVoiceCard(
    profile: VoiceProfile,
    onSelect: () -> Unit,
    onUpdateParams: (Float, Float, Int) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    var lengthScale by remember(profile.lengthScale) { mutableFloatStateOf(profile.lengthScale) }
    var noiseScale by remember(profile.noiseScale) { mutableFloatStateOf(profile.noiseScale) }
    var speakerId by remember(profile.speakerId) { mutableIntStateOf(profile.speakerId) }

    val borderColor by animateColorAsState(
        targetValue = if (profile.isActiveSystemVoice) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        label = "card_border"
    )

    OutlinedCard(
        onClick = onSelect,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        border = BorderStroke(if (profile.isActiveSystemVoice) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (profile.isActiveSystemVoice) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (profile.isActiveSystemVoice) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "active voice",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    Text(
                        text = "${profile.sampleRate} hz - speaker #${profile.speakerId}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        expanded = !expanded
                    }) {
                        Icon(Icons.Rounded.Tune, contentDescription = "tune onnx parameters")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "delete voice",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "pace (length scale): %.2fx", lengthScale),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = lengthScale,
                        onValueChange = { lengthScale = it },
                        onValueChangeFinished = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onUpdateParams(lengthScale, noiseScale, speakerId)
                        },
                        valueRange = 0.5f..2.0f
                    )

                    Text(
                        text = String.format(Locale.US, "expressiveness (noise scale): %.2f", noiseScale),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = noiseScale,
                        onValueChange = { noiseScale = it },
                        onValueChangeFinished = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onUpdateParams(lengthScale, noiseScale, speakerId)
                        },
                        valueRange = 0.1f..1.2f
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportOnnxBottomSheet(
    onDismiss: () -> Unit,
    onConfirmImport: (String, Uri, Uri) -> Unit
) {
    var voiceName by rememberSaveable { mutableStateOf("") }
    var onnxUri by remember { mutableStateOf<Uri?>(null) }
    var jsonUri by remember { mutableStateOf<Uri?>(null) }

    val onnxPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> onnxUri = uri }

    val jsonPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> jsonUri = uri }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "import piper .onnx voice",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = voiceName,
                onValueChange = { voiceName = it },
                label = { Text("voice display name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = { onnxPicker.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (onnxUri != null) Icons.Rounded.CheckCircle else Icons.Rounded.FilePresent,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (onnxUri != null) ".onnx model selected" else "1. select .onnx model file")
            }

            OutlinedButton(
                onClick = { jsonPicker.launch(arrayOf("application/json", "*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (jsonUri != null) Icons.Rounded.CheckCircle else Icons.Rounded.FilePresent,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (jsonUri != null) ".onnx.json config selected" else "2. select .onnx.json config file")
            }

            Button(
                onClick = {
                    val mUri = onnxUri
                    val cUri = jsonUri
                    if (mUri != null && cUri != null) {
                        onConfirmImport(voiceName.ifBlank { "custom piper voice" }, mUri, cUri)
                    }
                },
                enabled = onnxUri != null && jsonUri != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 24.dp)
            ) {
                Text("import into engine")
            }
        }
    }
}