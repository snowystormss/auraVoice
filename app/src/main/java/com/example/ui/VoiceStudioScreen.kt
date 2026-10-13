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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceStudioScreen(
    viewModel: VoiceViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissError()
        }
    }

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
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ReactiveVoiceOrb(
                            audioEnergyFlow = viewModel.audioEnergy,
                            isSpeaking = uiState.isSpeaking
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "auravoice",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = uiState.activeProfile?.let {
                                    "${it.name} (${it.sampleRate} hz)"
                                } ?: "no onnx model loaded",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Stop,
                                contentDescription = "stop voice playback"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        if (selectedTab != 0) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedTab = 0
                        }
                    },
                    icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                    label = { Text("studio chat") }
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

@Composable
private fun ReactiveVoiceOrb(
    audioEnergyFlow: StateFlow<Float>,
    isSpeaking: Boolean
) {
    // Isolated collection: only this tiny leaf node observes the 60Hz audio flow
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
        modifier = Modifier.size(38.dp),
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
            // Outer warm crema halo
            drawCircle(
                color = tertiaryColor.copy(alpha = 0.28f),
                radius = baseRadius
            )
            // Middle caramel ring
            drawCircle(
                color = primaryColor.copy(alpha = 0.55f),
                radius = baseRadius * (0.68f + smoothedEnergy * 0.15f)
            )
            // Core espresso/caramel nucleus
            drawCircle(
                color = primaryColor,
                radius = baseRadius * 0.42f
            )
        }
    }
}