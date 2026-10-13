package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Main Container Screen hosting the Bottom Navigation Tabs:
 * 1. Voice Cloner (Reference audio capture, 24kHz transcoding, F5-TTS ODE Preview & 300-sample training)
 * 2. Text Studio (Natural language description-to-voice & iterative text fine-tuning)
 * 3. Voices Library (Management, inline audition, and fine-tuning drawer for saved voices)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceStudioScreen(viewModel: VoiceViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val allVoices by viewModel.allVoices.collectAsState()
    val userVoices = remember(allVoices) { allVoices.filter { !it.isPreset } }

    // Training Pipeline Telemetry & Overlay
    val trainingState by viewModel.trainingState.collectAsState()
    val datasetProgress by viewModel.datasetProgress.collectAsState()
    val reclaimedMemoryMb by viewModel.reclaimedMemoryMb.collectAsState()
    val piperProgress by viewModel.piperProgress.collectAsState()
    val lossHistory by viewModel.lossHistory.collectAsState()
    val finalTrainedResult by viewModel.finalTrainedResult.collectAsState()
    val hardwareTelemetry by viewModel.hardwareTelemetry.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFFF4081), Color(0xFF7C4DFF))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (selectedTab) {
                                    VoiceViewModel.StudioTab.CLONER -> Icons.Default.Mic
                                    VoiceViewModel.StudioTab.TEXT_STUDIO -> Icons.Default.AutoAwesome
                                    VoiceViewModel.StudioTab.LIBRARY -> Icons.Default.FolderShared
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Aura Voice Studio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (selectedTab) {
                                    VoiceViewModel.StudioTab.CLONER -> "F5-TTS & PIPER CLONING"
                                    VoiceViewModel.StudioTab.TEXT_STUDIO -> "DESCRIPTION-BASED VOICES"
                                    VoiceViewModel.StudioTab.LIBRARY -> "SAVED LOCAL PROFILES"
                                },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 9.sp,
                                color = Color(0xFFFF4081),
                                letterSpacing = 1.1.sp
                            )
                        }
                    }
                },
                actions = {
                    // Quick badge showing total local models
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (userVoices.isNotEmpty()) Color(0xFF00E676) else Color(0xFFFF9100),
                                        CircleShape
                                    )
                            )
                            Text(
                                text = "${userVoices.size} Voices",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == VoiceViewModel.StudioTab.CLONER,
                    onClick = { viewModel.selectTab(VoiceViewModel.StudioTab.CLONER) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Cloner"
                        )
                    },
                    label = { Text("Cloner") },
                    modifier = Modifier.testTag("nav_tab_cloner")
                )

                NavigationBarItem(
                    selected = selectedTab == VoiceViewModel.StudioTab.TEXT_STUDIO,
                    onClick = { viewModel.selectTab(VoiceViewModel.StudioTab.TEXT_STUDIO) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Text Studio"
                        )
                    },
                    label = { Text("Text Studio") },
                    modifier = Modifier.testTag("nav_tab_text_studio")
                )

                NavigationBarItem(
                    selected = selectedTab == VoiceViewModel.StudioTab.LIBRARY,
                    onClick = { viewModel.selectTab(VoiceViewModel.StudioTab.LIBRARY) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.FolderShared,
                            contentDescription = "Voices Library"
                        )
                    },
                    label = { Text("Library") },
                    modifier = Modifier.testTag("nav_tab_library")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Content
            when (selectedTab) {
                VoiceViewModel.StudioTab.CLONER -> {
                    VoiceClonerPane(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                VoiceViewModel.StudioTab.TEXT_STUDIO -> {
                    TextVoiceStudioPane(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                VoiceViewModel.StudioTab.LIBRARY -> {
                    VoicesLibraryPane(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // ==========================================
            // TRAINING OVERLAY ("Training, Do Not Exit")
            // ==========================================
            if (trainingState == VoiceViewModel.TrainingState.GENERATING_300_SAMPLES ||
                trainingState == VoiceViewModel.TrainingState.RECLAIMING_MEMORY ||
                trainingState == VoiceViewModel.TrainingState.PIPER_FINE_TUNING ||
                trainingState == VoiceViewModel.TrainingState.NAMING_VOICE ||
                trainingState == VoiceViewModel.TrainingState.COMPLETED
            ) {
                TrainingScreen(
                    state = trainingState,
                    datasetProgress = datasetProgress,
                    reclaimedMemoryMb = reclaimedMemoryMb,
                    piperProgress = piperProgress,
                    lossHistory = lossHistory,
                    finalResult = finalTrainedResult,
                    hardwareTelemetry = hardwareTelemetry,
                    onSaveVoice = { name, desc -> viewModel.saveClonedVoice(name, desc) },
                    onCancel = { viewModel.cancelTrainingPipeline() },
                    onDismissCompleted = { viewModel.dismissCompletedState() }
                )
            }
        }
    }
}
