package com.example.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.LlmSettings
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceStudioScreen(
    viewModel: VoiceViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showVoicesSheet by rememberSaveable { mutableStateOf(false) }
    var showApiSheet by rememberSaveable { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissError()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 14.dp, vertical = 16.dp)
                ) {
                    // Search & New Chat Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::updateSearchQuery,
                            placeholder = { Text("search chats") },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f)
                        )

                        FilledTonalIconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.startNewChat()
                                scope.launch { drawerState.close() }
                            }
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "new chat")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Chat Threads List
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = uiState.sessions,
                            key = { it.id },
                            contentType = { "session" }
                        ) { session ->
                            val selected = session.id == uiState.currentSessionId
                            NavigationDrawerItem(
                                icon = {
                                    Icon(
                                        Icons.Rounded.ChatBubbleOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = session.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                badge = {
                                    IconButton(
                                        onClick = { viewModel.deleteSession(session) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = "delete",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                selected = selected,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.selectSession(session.id)
                                    scope.launch { drawerState.close() }
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Hamburger Menu Footer: Custom Voices, API Setup, and Theme
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null) },
                        label = {
                            Column {
                                Text("custom voices & system tts", fontWeight = FontWeight.Medium)
                                Text(
                                    text = uiState.activeProfile?.name ?: "no .onnx model active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showVoicesSheet = true
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                        label = {
                            Column {
                                Text("api & model setup", fontWeight = FontWeight.Medium)
                                Text(
                                    text = uiState.llmSettings.modelName.ifBlank { "local tts mode" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showApiSheet = true
                        }
                    )

                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                if (uiState.isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = null
                            )
                        },
                        label = {
                            Text(if (uiState.isDarkTheme) "switch to cream light" else "switch to espresso dark")
                        },
                        selected = false,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleTheme()
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                scope.launch { drawerState.open() }
                            }
                        ) {
                            Icon(Icons.Rounded.Menu, contentDescription = "menu")
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ReactiveVoiceOrb(
                                audioEnergyFlow = viewModel.audioEnergy,
                                isSpeaking = uiState.isSpeaking
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = uiState.llmSettings.modelName.ifBlank { "auravoice" },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = uiState.activeProfile?.name ?: "tap menu to load .onnx voice",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        AnimatedVisibility(
                            visible = uiState.isSpeaking,
                            enter = scaleIn(spring(stiffness = Spring.StiffnessMedium)) + fadeIn(),
                            exit = scaleOut(spring(stiffness = Spring.StiffnessMedium)) + fadeOut()
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.stopSpeaking()
                                }
                            ) {
                                Icon(Icons.Rounded.Stop, contentDescription = "stop speaking")
                            }
                        }
                        IconButton(onClick = viewModel::startNewChat) {
                            Icon(Icons.Rounded.Add, contentDescription = "new chat")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                TextVoiceStudioPane(
                    uiState = uiState,
                    onSendPrompt = viewModel::sendPrompt,
                    onSpeakMessage = viewModel::speakTextDirect
                )
            }
        }
    }

    // Custom Voices & System TTS Sheet (opened from Hamburger Menu)
    if (showVoicesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showVoicesSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            VoicesLibraryPane(
                profiles = uiState.profiles,
                onImportVoice = viewModel::importOnnxVoice,
                onSelectVoice = viewModel::selectActiveVoice,
                onUpdateParameters = viewModel::updateVoiceParameters,
                onDeleteVoice = viewModel::deleteVoice,
                onOpenSystemTtsSettings = {
                    runCatching {
                        context.startActivity(Intent("com.android.settings.TTS_SETTINGS"))
                    }
                }
            )
        }
    }

    // Minimal 3-Box API & Model Sheet
    if (showApiSheet) {
        LlmSettingsBottomSheet(
            currentSettings = uiState.llmSettings,
            onDismiss = { showApiSheet = false },
            onSave = { updated ->
                viewModel.updateSettings(updated)
                showApiSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmSettingsBottomSheet(
    currentSettings: LlmSettings,
    onDismiss: () -> Unit,
    onSave: (LlmSettings) -> Unit
) {
    var apiUrl by rememberSaveable { mutableStateOf(currentSettings.apiUrl) }
    var apiKey by rememberSaveable { mutableStateOf(currentSettings.apiKey) }
    var modelName by rememberSaveable { mutableStateOf(currentSettings.modelName) }
    var systemPrompt by rememberSaveable { mutableStateOf(currentSettings.systemPrompt) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "api & model",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = apiUrl,
                onValueChange = { apiUrl = it },
                label = { Text("api link") },
                placeholder = { Text("http://192.168.1.50:8080 or https://openrouter.ai/api/v1") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("api key (optional)") },
                placeholder = { Text("leave blank for local llama.cpp") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = modelName,
                onValueChange = { modelName = it },
                label = { Text("model name") },
                placeholder = { Text("gemini-2.5-flash, gpt-4o-mini, local-model") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                label = { Text("system prompt") },
                maxLines = 3,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    onSave(
                        LlmSettings(
                            apiUrl = apiUrl.trim(),
                            apiKey = apiKey.trim(),
                            modelName = modelName.trim(),
                            systemPrompt = systemPrompt.trim()
                        )
                    )
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 24.dp)
            ) {
                Text("save")
            }
        }
    }
}

@Composable
fun ReactiveVoiceOrb(
    audioEnergyFlow: StateFlow<Float>,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    val rawEnergy by audioEnergyFlow.collectAsStateWithLifecycle()

    val smoothedEnergy by animateFloatAsState(
        targetValue = if (isSpeaking) rawEnergy else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "orb_energy"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "idle_breath")
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_scale"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier.size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val activeScale = if (isSpeaking) 1f + (smoothedEnergy * 0.55f) else idlePulse
                    scaleX = activeScale
                    scaleY = activeScale
                    alpha = if (isSpeaking) 0.95f else 0.5f
                }
        ) {
            val baseRadius = size.minDimension / 2f
            drawCircle(color = tertiaryColor.copy(alpha = 0.25f), radius = baseRadius)
            drawCircle(
                color = primaryColor.copy(alpha = 0.55f),
                radius = baseRadius * (0.66f + smoothedEnergy * 0.16f)
            )
            drawCircle(color = primaryColor, radius = baseRadius * 0.40f)
        }
    }
}