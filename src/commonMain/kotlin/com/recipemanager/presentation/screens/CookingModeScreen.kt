package com.recipemanager.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recipemanager.domain.model.CookingStep
import com.recipemanager.domain.model.CookingTimer
import com.recipemanager.domain.model.TimerStatus
import com.recipemanager.presentation.viewmodel.CookingModeViewModel

/**
 * Cooking mode screen with large text display and timer controls.
 * Requirements: 5.4, 5.5, 7.2, 7.3
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingModeScreen(
    recipeId: String,
    viewModel: CookingModeViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(recipeId) {
        viewModel.startCookingSession(recipeId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = state.recipe?.title ?: "Cooking Mode",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.endCookingSession()
                        onBack()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Exit Cooking Mode")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.error != null -> {
                    ErrorMessage(
                        message = state.error!!,
                        onDismiss = { viewModel.clearError() }
                    )
                }
                state.recipe != null && state.isCookingSessionActive -> {
                    CookingContent(
                        viewModel = viewModel,
                        state = state
                    )
                }
                else -> {
                    Text(
                        text = "Recipe not found",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

/**
 * Main cooking content with step display and timer controls.
 * Requirement 7.2: Large, readable text suitable for kitchen use
 */
@Composable
private fun CookingContent(
    viewModel: CookingModeViewModel,
    state: com.recipemanager.presentation.viewmodel.CookingModeState
) {
    val recipe = state.recipe ?: return
    val sortedSteps = recipe.steps.sortedBy { it.stepNumber }
    val currentStep = sortedSteps.getOrNull(state.currentStepIndex)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Progress indicator
        StepProgressIndicator(
            currentStep = state.currentStepIndex + 1,
            totalSteps = sortedSteps.size
        )
        
        // Current step display with large text
        currentStep?.let { step ->
            CurrentStepDisplay(
                step = step,
                stepNumber = state.currentStepIndex + 1,
                onStartTimer = { viewModel.startStepTimer(step) }
            )
        }
        
        // Active timers display
        // Requirement 5.5: Display all running timers with remaining time
        if (state.activeTimers.isNotEmpty()) {
            ActiveTimersSection(
                timers = state.activeTimers.values.toList(),
                onPause = { viewModel.pauseTimer(it) },
                onResume = { viewModel.resumeTimer(it) },
                onCancel = { viewModel.cancelTimer(it) }
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Navigation controls
        // Requirement 7.3: Large, easily tappable controls
        NavigationControls(
            canGoPrevious = state.currentStepIndex > 0,
            canGoNext = state.currentStepIndex < sortedSteps.size - 1,
            onPrevious = { viewModel.previousStep() },
            onNext = { viewModel.nextStep() }
        )
    }
}

/**
 * Progress indicator showing current step.
 */
@Composable
private fun StepProgressIndicator(
    currentStep: Int,
    totalSteps: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Step $currentStep of $totalSteps",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LinearProgressIndicator(
            progress = currentStep.toFloat() / totalSteps.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
        )
    }
}

/**
 * Display current cooking step with large, readable text.
 * Requirement 7.2: Large text display suitable for kitchen use
 */
@Composable
private fun CurrentStepDisplay(
    step: CookingStep,
    stepNumber: Int,
    onStartTimer: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step number badge
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "Step $stepNumber",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Instruction with large text
            Text(
                text = step.instruction,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 24.sp,
                    lineHeight = 32.sp
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            // Step metadata
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                step.duration?.let { duration ->
                    Column {
                        Text(
                            text = "⏱ Duration",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$duration min",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                step.temperature?.let { temp ->
                    Column {
                        Text(
                            text = "🌡 Temperature",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$temp°",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Start timer button if step has duration
            // Requirement 5.1: Create timers for timed cooking steps
            step.duration?.let {
                Button(
                    onClick = onStartTimer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(
                        text = "⏱",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Timer",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

/**
 * Display all active timers with controls.
 * Requirement 5.5: Clearly display all running timers with remaining time
 * Requirement 7.3: Large, easily tappable controls
 */
@Composable
private fun ActiveTimersSection(
    timers: List<CookingTimer>,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Active Timers (${timers.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            timers.forEach { timer ->
                TimerCard(
                    timer = timer,
                    onPause = { onPause(timer.id) },
                    onResume = { onResume(timer.id) },
                    onCancel = { onCancel(timer.id) }
                )
            }
        }
    }
}

/**
 * Individual timer card with controls.
 * Requirement 5.4: Pause, resume, and modify active timers
 * Requirement 7.3: Large, easily tappable controls
 */
@Composable
private fun TimerCard(
    timer: CookingTimer,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timer display
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = formatTime(timer.remainingTime),
                    style = MaterialTheme.typography.displaySmall,
                    color = when (timer.status) {
                        TimerStatus.RUNNING -> MaterialTheme.colorScheme.primary
                        TimerStatus.PAUSED -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                
                Text(
                    text = when (timer.status) {
                        TimerStatus.RUNNING -> "Running"
                        TimerStatus.PAUSED -> "Paused"
                        TimerStatus.COMPLETED -> "Completed"
                        else -> "Ready"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Timer controls - large, tappable buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (timer.status) {
                    TimerStatus.RUNNING -> {
                        IconButton(
                            onClick = onPause,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Text(
                                text = "⏸",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                    TimerStatus.PAUSED -> {
                        IconButton(
                            onClick = onResume,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume Timer",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    else -> {}
                }
                
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel Timer",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Navigation controls for moving between steps.
 * Requirement 7.3: Large, easily tappable controls
 */
@Composable
private fun NavigationControls(
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Previous button
        Button(
            onClick = onPrevious,
            enabled = canGoPrevious,
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Previous",
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        // Next button
        Button(
            onClick = onNext,
            enabled = canGoNext,
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "Next",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Format seconds into MM:SS format.
 */
private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

@Composable
private fun ErrorMessage(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}
