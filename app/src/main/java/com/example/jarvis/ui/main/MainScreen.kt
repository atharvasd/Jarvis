package com.example.jarvis.ui.main

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.jarvis.data.ChatHistoryEntry
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// Theme Colors
val JarvisBackground = Color(0xFF070B12)
val JarvisSurface = Color(0xFF0F172A)
val JarvisPrimary = Color(0xFF00E5FF)
val JarvisSecondary = Color(0xFF00A2FF)
val JarvisAccent = Color(0xFFFFB300)
val JarvisGlow = Color(0x3300E5FF)

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // States from viewmodel
    val assistantState by viewModel.assistantState.collectAsState()
    val speechText by viewModel.speechText.collectAsState()
    val jarvisReplyText by viewModel.jarvisReplyText.collectAsState()
    val volumeDb by viewModel.volumeDb.collectAsState()
    val isTtsInitialized by viewModel.isTtsInitialized.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val showSettings by viewModel.showSettings.collectAsState()
    val pitch by viewModel.voicePitch.collectAsState()
    val rate by viewModel.voiceRate.collectAsState()
    val history by viewModel.history.collectAsState()

    // Setup permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.toggleListening(isGranted)
    }

    val requestPermission = {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(viewModel.requestPermissionEvent) {
        viewModel.requestPermissionEvent.collect {
            requestPermission()
        }
    }

    // Main layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(16.dp)
    ) {
        // Futuristic background grids/effects
        BackgroundGridEffects()

        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            HeaderBar(
                assistantState = assistantState,
                onSettingsClick = { viewModel.setShowSettings(true) },
                onClearClick = { viewModel.clearHistory() }
            )

            Spacer(modifier = Modifier.weight(0.1f))

            // The Glowing Arc Reactor Centerpiece
            ArcReactorCenterpiece(
                assistantState = assistantState,
                volume = volumeDb,
                onClick = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    viewModel.toggleListening(hasPermission)
                }
            )

            Spacer(modifier = Modifier.weight(0.1f))

            // Subtitle Display for user voice input
            UserVoiceSubtitles(text = speechText)

            Spacer(modifier = Modifier.height(16.dp))

            // Console output from J.A.R.V.I.S. (Typewriter style)
            JarvisConsoleOutput(text = jarvisReplyText)

            Spacer(modifier = Modifier.weight(0.1f))

            // View Logs & Manual Input section
            LogAndManualInputArea(
                history = history,
                onSendCommand = { viewModel.processTextCommand(it) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Settings Modal
        if (showSettings) {
            SettingsDialog(
                apiKey = apiKey,
                pitch = pitch,
                rate = rate,
                onDismiss = { viewModel.setShowSettings(false) },
                onSave = { key, newPitch, newRate ->
                    viewModel.setApiKey(key)
                    viewModel.setVoiceSettings(newPitch, newRate)
                    viewModel.setShowSettings(false)
                }
            )
        }
    }
}

@Composable
fun BackgroundGridEffects() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Soft grid lines
        val gridSpacing = 60.dp.toPx()
        val paintColor = Color(0xFF00E5FF).copy(alpha = 0.03f)
        
        var x = 0f
        while (x < width) {
            drawLine(
                color = paintColor,
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1.dp.toPx()
            )
            x += gridSpacing
        }

        var y = 0f
        while (y < height) {
            drawLine(
                color = paintColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
            y += gridSpacing
        }

        // Cybernetic circular background accents
        drawCircle(
            color = Color(0xFF00A2FF).copy(alpha = 0.02f),
            radius = width * 0.4f,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun HeaderBar(
    assistantState: AssistantState,
    onSettingsClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title & status indicator
        Column {
            Text(
                text = "J.A.R.V.I.S.",
                color = JarvisPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pulsing dot
                val infiniteTransition = rememberInfiniteTransition(label = "status_dot")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = EaseInOutQuad),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when (assistantState) {
                                AssistantState.IDLE -> JarvisPrimary.copy(alpha = alpha)
                                AssistantState.LISTENING -> Color.Red.copy(alpha = alpha)
                                AssistantState.PROCESSING -> JarvisAccent.copy(alpha = alpha)
                                AssistantState.SPEAKING -> JarvisSecondary.copy(alpha = alpha)
                            }
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when (assistantState) {
                        AssistantState.IDLE -> "SYSTEM ACTIVE"
                        AssistantState.LISTENING -> "LISTENING..."
                        AssistantState.PROCESSING -> "PROCESSING..."
                        AssistantState.SPEAKING -> "TRANSMITTING..."
                    },
                    color = Color.LightGray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        // Action Buttons
        Row {
            IconButton(
                onClick = onClearClick,
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.LightGray)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear logs",
                    tint = Color.LightGray.copy(alpha = 0.7f)
                )
            }
            IconButton(
                onClick = onSettingsClick,
                colors = IconButtonDefaults.iconButtonColors(contentColor = JarvisPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = JarvisPrimary
                )
            }
        }
    }
}

@Composable
fun ArcReactorCenterpiece(
    assistantState: AssistantState,
    volume: Float,
    onClick: () -> Unit
) {
    // Rotation animations
    val infiniteTransition = rememberInfiniteTransition(label = "reactor_rotation")
    val angle1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle1"
    )
    val angle2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle2"
    )

    // Pulsing size based on state
    val basePulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (assistantState) {
                    AssistantState.PROCESSING -> 400
                    AssistantState.LISTENING -> 800
                    else -> 2000
                },
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "basePulse"
    )

    // Speaking wave animations
    val waveAnim = remember { Animatable(0f) }
    LaunchedEffect(assistantState) {
        if (assistantState == AssistantState.SPEAKING) {
            while (true) {
                waveAnim.animateTo(1f, animationSpec = tween(600, easing = EaseInOutQuad))
                waveAnim.animateTo(0f, animationSpec = tween(600, easing = EaseInOutQuad))
            }
        } else {
            waveAnim.snapTo(0f)
        }
    }

    val reactorSize = 220.dp
    
    Box(
        modifier = Modifier
            .size(reactorSize)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .drawBehind {
                // Soft neon glow underlay
                val glowRadius = size.width * 0.5f * basePulse
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            when (assistantState) {
                                AssistantState.LISTENING -> Color.Red.copy(alpha = 0.2f)
                                AssistantState.PROCESSING -> JarvisAccent.copy(alpha = 0.2f)
                                AssistantState.SPEAKING -> JarvisSecondary.copy(alpha = 0.25f)
                                AssistantState.IDLE -> JarvisPrimary.copy(alpha = 0.15f)
                            },
                            Color.Transparent
                        ),
                        center = center,
                        radius = glowRadius
                    ),
                    radius = glowRadius
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize(0.85f * basePulse)) {
            val centerOffset = center
            val maxRadius = size.width / 2f
            
            // 1. Draw central glowing core
            val coreColor = when (assistantState) {
                AssistantState.LISTENING -> Color.Red
                AssistantState.PROCESSING -> JarvisAccent
                AssistantState.SPEAKING -> JarvisSecondary
                AssistantState.IDLE -> JarvisPrimary
            }
            
            drawCircle(
                color = coreColor.copy(alpha = 0.15f),
                radius = maxRadius * 0.28f,
                center = centerOffset
            )
            drawCircle(
                color = coreColor.copy(alpha = 0.4f),
                radius = maxRadius * 0.22f,
                center = centerOffset
            )
            drawCircle(
                color = Color.White,
                radius = maxRadius * 0.15f,
                center = centerOffset
            )

            // 2. Draw Ring 1 (dashed outer ring)
            rotate(angle1) {
                drawCircle(
                    color = coreColor.copy(alpha = 0.6f),
                    radius = maxRadius * 0.85f,
                    center = centerOffset,
                    style = Stroke(
                        width = 4.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(30.dp.toPx(), 15.dp.toPx()), 0f
                        )
                    )
                )
            }

            // 3. Draw Ring 2 (thin segmented inner ring)
            rotate(angle2) {
                drawCircle(
                    color = coreColor.copy(alpha = 0.4f),
                    radius = maxRadius * 0.65f,
                    center = centerOffset,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(15.dp.toPx(), 8.dp.toPx(), 4.dp.toPx(), 8.dp.toPx()), 0f
                        )
                    )
                )
            }

            // 4. Outer thin guard circle
            drawCircle(
                color = coreColor.copy(alpha = 0.2f),
                radius = maxRadius * 0.95f,
                center = centerOffset,
                style = Stroke(width = 1.dp.toPx())
            )

            // 5. Intersecting cross lines for high-tech blueprint feel
            rotate(angle1 * 0.5f) {
                val lineLength = maxRadius * 0.85f
                for (i in 0 until 4) {
                    val angleRad = Math.toRadians((i * 90).toDouble())
                    val start = Offset(
                        (centerOffset.x + maxRadius * 0.35f * cos(angleRad)).toFloat(),
                        (centerOffset.y + maxRadius * 0.35f * sin(angleRad)).toFloat()
                    )
                    val end = Offset(
                        (centerOffset.x + lineLength * cos(angleRad)).toFloat(),
                        (centerOffset.y + lineLength * sin(angleRad)).toFloat()
                    )
                    drawLine(
                        color = coreColor.copy(alpha = 0.15f),
                        start = start,
                        end = end,
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            // 6. Voice Wave Visualizations
            if (assistantState == AssistantState.LISTENING) {
                // Radial lines growing with volume input
                val lineCount = 36
                val volumeMultiplier = volume.coerceIn(0f, 1f)
                val minR = maxRadius * 0.88f
                val maxR = minR + (maxRadius * 0.25f * volumeMultiplier)
                
                for (i in 0 until lineCount) {
                    val deg = (i * (360f / lineCount))
                    val rad = Math.toRadians(deg.toDouble())
                    
                    // Vary slightly randomly to make it feel organic
                    val variance = 1f + (0.15f * sin((i * 1.5 + angle1 * 0.1).toFloat())).coerceIn(-1f, 1f)
                    val actMaxR = minR + (maxR - minR) * variance

                    val startX = (centerOffset.x + minR * cos(rad)).toFloat()
                    val startY = (centerOffset.y + minR * sin(rad)).toFloat()
                    val endX = (centerOffset.x + actMaxR * cos(rad)).toFloat()
                    val endY = (centerOffset.y + actMaxR * sin(rad)).toFloat()

                    drawLine(
                        color = Color.Red.copy(alpha = 0.7f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            } else if (assistantState == AssistantState.SPEAKING) {
                // Expanding glowing rings reflecting sound output
                val scaleFactor = waveAnim.value
                drawCircle(
                    color = JarvisSecondary.copy(alpha = (1f - scaleFactor) * 0.4f),
                    radius = maxRadius * (0.85f + 0.3f * scaleFactor),
                    center = centerOffset,
                    style = Stroke(width = (3.dp.toPx() * (1f - scaleFactor)).coerceAtLeast(0.5f))
                )
            }
        }
    }
}

@Composable
fun UserVoiceSubtitles(text: String) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
        },
        label = "subtitles"
    ) { targetText ->
        Text(
            text = targetText,
            color = if (targetText.startsWith("Error") || targetText.startsWith("Sorry")) Color.Red.copy(alpha = 0.8f) else Color.LightGray.copy(alpha = 0.8f),
            fontSize = 15.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun JarvisConsoleOutput(text: String) {
    var displayedText by remember { mutableStateOf("") }

    // Typewriter effect triggered when text updates
    LaunchedEffect(text) {
        displayedText = ""
        if (text.isNotEmpty()) {
            for (i in 1..text.length) {
                displayedText = text.take(i)
                // Speed up slightly for long replies to keep voice synced
                val delayMs = if (text.length > 100) 10L else 20L
                delay(delayMs)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp, max = 180.dp)
            .border(1.dp, JarvisPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .background(JarvisSurface.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        // Holographic console header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "CONSOLE_OUTPUT",
                color = JarvisPrimary.copy(alpha = 0.5f),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "SYS_SECURE",
                color = JarvisPrimary.copy(alpha = 0.3f),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Monospace glowing typewriter text
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp)
        ) {
            item {
                Text(
                    text = displayedText,
                    color = JarvisPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.drawBehind {
                        // Cybernetic side accent bar
                        drawLine(
                            color = JarvisPrimary.copy(alpha = 0.3f),
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 2.dp.toPx()
                        )
                    }.padding(start = 12.dp)
                )
            }
        }
    }
}

@Composable
fun LogAndManualInputArea(
    history: List<ChatHistoryEntry>,
    onSendCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showLogs by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf(TextFieldValue("")) }
    val listState = rememberLazyListState()

    // Scroll to latest item when history or log view changes
    LaunchedEffect(history.size, showLogs) {
        if (history.isNotEmpty() && showLogs) {
            listState.animateScrollToItem(history.size - 1)
        }
    }

    Column(modifier = modifier) {
        // Collapse/Expand log drawer button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showLogs = !showLogs }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (showLogs) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                contentDescription = "Toggle logs",
                tint = JarvisPrimary.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (showLogs) "HIDE TRANSCRIBE LOGS" else "VIEW TRANSCRIBE LOGS",
                color = JarvisPrimary.copy(alpha = 0.6f),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // Expanded log console
        AnimatedVisibility(
            visible = showLogs,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .border(1.dp, JarvisPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .background(JarvisSurface.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NO COMMAND RECORDS DETECTED",
                            color = Color.Gray.copy(alpha = 0.5f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(history) { entry ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = if (entry.isUser) "> COMMAND (USER)" else "< RESPONSE (JARVIS)",
                                    color = if (entry.isUser) JarvisAccent.copy(alpha = 0.6f) else JarvisSecondary.copy(alpha = 0.6f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = entry.message,
                                    color = if (entry.isUser) Color.White else JarvisPrimary.copy(alpha = 0.9f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Glowing Manual Text Input Command Line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .border(1.dp, JarvisPrimary.copy(alpha = 0.3f), RoundedCornerShape(25.dp))
                .background(JarvisSurface.copy(alpha = 0.5f), RoundedCornerShape(25.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ">",
                color = JarvisPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            TextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = {
                    Text(
                        text = "Enter terminal command, Sir...",
                        color = Color.LightGray.copy(alpha = 0.4f),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    cursorColor = JarvisPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            )

            IconButton(
                onClick = {
                    if (textInput.text.isNotBlank()) {
                        onSendCommand(textInput.text)
                        textInput = TextFieldValue("")
                    }
                },
                enabled = textInput.text.isNotBlank(),
                colors = IconButtonDefaults.iconButtonColors(contentColor = JarvisPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send command",
                    tint = if (textInput.text.isNotBlank()) JarvisPrimary else JarvisPrimary.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun SettingsDialog(
    apiKey: String,
    pitch: Float,
    rate: Float,
    onDismiss: () -> Unit,
    onSave: (String, Float, Float) -> Unit
) {
    var keyInput by remember { mutableStateOf(apiKey) }
    var pitchInput by remember { mutableFloatStateOf(pitch) }
    var rateInput by remember { mutableFloatStateOf(rate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "SYSTEM CONFIGURATION",
                color = JarvisPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = JarvisSurface,
        shape = RoundedCornerShape(16.dp),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // API Key field
                Text(
                    text = "GEMINI API KEY",
                    color = Color.LightGray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    placeholder = { Text("Paste AI API Key...", color = Color.Gray, fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisPrimary,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 16.dp)
                )

                // Voice pitch slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "VOICE PITCH",
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format("%.2f", pitchInput),
                        color = JarvisPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
                Slider(
                    value = pitchInput,
                    onValueChange = { pitchInput = it },
                    valueRange = 0.5f..1.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = JarvisPrimary,
                        activeTrackColor = JarvisPrimary,
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Voice speed slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SPEECH RATE",
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format("%.2f", rateInput),
                        color = JarvisPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
                Slider(
                    value = rateInput,
                    onValueChange = { rateInput = it },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = JarvisPrimary,
                        activeTrackColor = JarvisPrimary,
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                    )
                )
                
                Text(
                    text = "*Note: British male voice is auto-targeted when available.",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(keyInput, pitchInput, rateInput) },
                colors = ButtonDefaults.buttonColors(containerColor = JarvisPrimary, contentColor = JarvisBackground)
            ) {
                Text(
                    text = "INITIALIZE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CANCEL",
                    color = Color.LightGray,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    )
}
