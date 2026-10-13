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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FilePresent
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
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

@Composable
fun VoicesLibraryPane(
    profiles: List<VoiceProfile>,
    onImportVoice: (Context, String, Uri, Uri) -> Unit,
    onSelectVoice: (VoiceProfile) -> Unit,
    onUpdateParameters: (VoiceProfile, Float, Float, Int) -> Unit,
    onDeleteVoice: (VoiceProfile) -> Unit,
    onOpenSystemTtsSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var showImporter by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "custom .onnx voices",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            FilledTonalButton(
                onClick = { showImporter = !showImporter },
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (showImporter) "cancel" else "add voice")
            }
        }

        // One-tap shortcut to set AuraVoice as the Android system-wide voice
        OutlinedButton(
            onClick = onOpenSystemTtsSettings,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.PhoneAndroid, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("set as android system voice")
        }

        AnimatedVisibility(visible = showImporter) {
            InlineOnnxImporter(
                onConfirmImport = { name, onnxUri, jsonUri ->
                    onImportVoice(context, name, onnxUri, jsonUri)
                    showImporter = false
                }
            )
        }

        if (profiles.isEmpty() && !showImporter) {
            Text(
                text = "no voices loaded yet. tap 'add voice' to load a piper .onnx file and its .onnx.json config.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items = profiles, key = { it.id }) { profile ->
                    OnnxVoiceCard(
                        profile = profile,
                        onSelect = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSelectVoice(profile)
                        },
                        onUpdateParams = { length, noise, speaker ->
                            onUpdateParameters(profile, length, noise, speaker)
                        },
                        onDelete = { onDeleteVoice(profile) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineOnnxImporter(
    onConfirmImport: (String, Uri, Uri) -> Unit
) {
    var voiceName by rememberSaveable { mutableStateOf("") }
    var onnxUri by remember { mutableStateOf<Uri?>(null) }
    var jsonUri by remember { mutableStateOf<Uri?>(null) }

    val onnxPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { onnxUri = it }
    val jsonPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { jsonUri = it }

    OutlinedCard(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = voiceName,
                onValueChange = { voiceName = it },
                label = { Text("voice name") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onnxPicker.launch(arrayOf("*/*")) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (onnxUri != null) Icons.Rounded.CheckCircle else Icons.Rounded.FilePresent,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (onnxUri != null) ".onnx ready" else "1. .onnx file")
                }
                OutlinedButton(
                    onClick = { jsonPicker.launch(arrayOf("*/*")) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (jsonUri != null) Icons.Rounded.CheckCircle else Icons.Rounded.FilePresent,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (jsonUri != null) ".json ready" else "2. .json file")
                }
            }
            Button(
                onClick = {
                    val m = onnxUri
                    val c = jsonUri
                    if (m != null && c != null) {
                        onConfirmImport(voiceName.ifBlank { "custom voice" }, m, c)
                    }
                },
                enabled = onnxUri != null && jsonUri != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("import voice")
            }
        }
    }
}

@Composable
private fun OnnxVoiceCard(
    profile: VoiceProfile,
    onSelect: () -> Unit,
    onUpdateParams: (Float, Float, Int) -> Unit,
    onDelete: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    var lengthScale by remember(profile.lengthScale) { mutableFloatStateOf(profile.lengthScale) }
    var noiseScale by remember(profile.noiseScale) { mutableFloatStateOf(profile.noiseScale) }
    val speakerId by remember(profile.speakerId) { mutableIntStateOf(profile.speakerId) }

    val borderColor by animateColorAsState(
        targetValue = if (profile.isActiveSystemVoice) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        label = "border"
    )

    OutlinedCard(
        onClick = onSelect,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
        border = BorderStroke(if (profile.isActiveSystemVoice) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (profile.isActiveSystemVoice) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(profile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (profile.isActiveSystemVoice) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = "active",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                    Text(
                        text = "${profile.sampleRate} hz • active for chat & system tts",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(Icons.Rounded.Tune, contentDescription = "tune")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(String.format(Locale.US, "speed scale: %.2fx", lengthScale), style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = lengthScale,
                        onValueChange = { lengthScale = it },
                        onValueChangeFinished = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onUpdateParams(lengthScale, noiseScale, speakerId)
                        },
                        valueRange = 0.5f..2.0f
                    )
                    Text(String.format(Locale.US, "expressiveness: %.2f", noiseScale), style = MaterialTheme.typography.labelSmall)
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