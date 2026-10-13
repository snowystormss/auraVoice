package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.LlmProvider
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
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current

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
                modifier = Modifier.width(310.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "conversations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FilledTonalButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.startNewChat()
                                selectedTab = 0
                                scope.launch { drawerState.close() }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("new chat")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::updateSearchQuery,
                        placeholder = { Text("search chats & messages...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = uiState.sessions,
                            key = { it.id },
                            contentType = { "chat_session_item" }
                        ) { session ->
                            val isSelected = session.id == uiState.currentSessionId
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
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = "delete chat",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                selected = isSelected,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.selectSession(session.id)
                                    selectedTab = 0
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
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                scope.launch { drawerState.open() }
                            }
                        ) {
                            Icon(Icons.Rounded.Menu, contentDescription = "open chat drawer")
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
                                    text = "auravoice",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${uiState.llmSettings.modelName.ifBlank { "local tts" }} • ${uiState.activeProfile?.name ?: "no voice"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
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
                                Icon(Icons.Rounded.Stop, contentDescription = "stop voice playback")
                            }
                        }
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Rounded.Settings, contentDescription = "server & model settings")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = {
                            if (selectedTab != 0) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTab = 0
                            }
                        },
                        icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                        label = { Text("chat") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = {
                            if (selectedTab != 1) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTab = 1
                            }
                        },
                        icon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null) },
                        label = { Text("onnx voices") }
                    )
                }
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) { fullWidth -> direction * (fullWidth / 5) } + fadeIn())
                        .togetherWith(
                            slideOutHorizontally(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) { fullWidth -> -direction * (fullWidth / 5) } + fadeOut()
                        ).using(SizeTransform(clip = false))
                },
                label = "pane_transition"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> TextVoiceStudioPane(
                        uiState = uiState,
                        onSendPrompt = viewModel::sendPrompt,
                        onSpeakMessage = viewModel::speakTextDirect
                    )
                    1 -> VoicesLibraryPane(
                        profiles = uiState.profiles,
                        onImportVoice = viewModel::importOnnxVoice,
                        onSelectVoice = viewModel::selectActiveVoice,
                        onUpdateParameters = viewModel::updateVoiceParameters,
                        onDeleteVoice = viewModel::deleteVoice
                    )
                }
            }
        }
    }

    if (showSettingsSheet) {
        LlmSettingsBottomSheet(
            currentSettings = uiState.llmSettings,
            onDismiss = { showSettingsSheet = false },
            onSave = { updated ->
                viewModel.updateSettings(updated)
                showSettingsSheet = false
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
                text = "api & model setup",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = apiUrl,
                onValueChange = { apiUrl = it },
                label = { Text("api link") },
                placeholder = { Text("e.g. http://192.168.1.50:8080/v1 or https://openrouter.ai/api/v1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("api key (optional for local servers)") },
                placeholder = { Text("paste key or leave blank") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = modelName,
                onValueChange = { modelName = it },
                label = { Text("exact model name") },
                placeholder = { Text("e.g. gemini-2.5-flash, gpt-4o-mini, or local-model") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                label = { Text("system prompt (optional)") },
                maxLines = 3,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 24.dp)
            ) {
                Text("save")
            }
        }
    }
}

@Composable
private fun ReactiveVoiceOrb(
    audioEnergyFlow: StateFlow<Float>,
    isSpeaking: Boolean
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
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_scale"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier.size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val activeScale = if (isSpeaking) 1f + (smoothedEnergy * 0.55f) else idlePulse
                    scaleX = activeScale
                    scaleY = activeScale
                    alpha = if (isSpeaking) 0.95f else 0.45f
                }
        ) {
            val baseRadius = size.minDimension / 2f
            drawCircle(color = tertiaryColor.copy(alpha = 0.28f), radius = baseRadius)
            drawCircle(
                color = primaryColor.copy(alpha = 0.55f),
                radius = baseRadius * (0.68f + smoothedEnergy * 0.15f)
            )
            drawCircle(color = primaryColor, radius = baseRadius * 0.42f)
        }
    }
}