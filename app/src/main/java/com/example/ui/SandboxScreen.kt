package com.example.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwipeVertical
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FocusCyan
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.FocusIndigo
import com.example.ui.theme.FocusRose
import kotlinx.coroutines.delay

data class SampleShort(
    val id: String,
    val title: String,
    val category: String,
    val channel: String,
    val likes: String,
    val comments: String,
    val soundTitle: String,
    val colors: List<Color>
)

val sampleShorts = listOf(
    SampleShort(
        id = "focus_clarity",
        title = "4-7-8 Breathing Technique for Instant Calm",
        category = "Mindfulness",
        channel = "@MindfulDaily",
        likes = "142K",
        comments = "1.8K",
        soundTitle = "Deep Focus Ambient Waves",
        colors = listOf(Color(0xFF0F172A), Color(0xFF0369A1), Color(0xFF0284C7))
    ),
    SampleShort(
        id = "stop_doomscrolling",
        title = "One Mental Rule to Stop Infinite Scrolling",
        category = "Productivity",
        channel = "@ProductivityLab",
        likes = "289K",
        comments = "3.2K",
        soundTitle = "Focus State Alpha Beats",
        colors = listOf(Color(0xFF1E1B4B), Color(0xFF4338CA), Color(0xFF6366F1))
    ),
    SampleShort(
        id = "stoic_wisdom",
        title = "Marcus Aurelius on Controlling Your Attention",
        category = "Philosophy",
        channel = "@StoicMindset",
        likes = "98K",
        comments = "890",
        soundTitle = "Original Audio - Stoic Echoes",
        colors = listOf(Color(0xFF18181B), Color(0xFF3F3F46), Color(0xFF71717A))
    )
)

@Composable
fun SandboxScreen(
    viewModel: FocusViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val timeLimitSeconds by viewModel.timeLimitSeconds.collectAsState()
    val isShieldActive by viewModel.isShieldActive.collectAsState()
    val strictOneShort by viewModel.strictOneShort.collectAsState()
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()

    var isPlaying by remember { mutableStateOf(false) }
    var isBlocked by remember { mutableStateOf(false) }
    var blockReason by remember { mutableStateOf("time_limit_expired") }
    var selectedShort by remember { mutableStateOf(sampleShorts[0]) }
    var remainingTime by remember { mutableIntStateOf(timeLimitSeconds) }

    // When playing, run the countdown timer for the first short
    LaunchedEffect(isPlaying, isBlocked, selectedShort, timeLimitSeconds) {
        if (isPlaying && !isBlocked) {
            remainingTime = timeLimitSeconds
            while (remainingTime > 0) {
                delay(1000)
                remainingTime--
                if (remainingTime <= 0) {
                    // Time limit expired! Block future shorts
                    isBlocked = true
                    blockReason = "time_limit_expired"
                    triggerHaptic(context, hapticEnabled)
                    viewModel.recordSandboxSession(timeLimitSeconds, "time_limit_expired")
                    break
                }
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sandbox Explanation Header
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = FocusCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Focus Sandbox Simulator",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Test the exact algorithm: You are granted access to Video #1 only. After ${timeLimitSeconds}s (or if you try swiping to Video #2), all future shorts are immediately locked.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        lineHeight = 18.sp
                    )
                )
            }
        }

        // Live Simulated Video Player Container
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    2.dp,
                    if (isBlocked) FocusRose else if (isPlaying) FocusCyan else Color(0xFF334155),
                    RoundedCornerShape(24.dp)
                )
                .testTag("sandbox_player_card")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 14f)
                    .clip(RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying && !isBlocked) {
                    // Native Simulated Short Player
                    SimulatedShortPlayerView(
                        short = selectedShort,
                        remainingTime = remainingTime,
                        totalLimit = timeLimitSeconds,
                        onSwipeToNext = {
                            isBlocked = true
                            blockReason = "swiped_to_next"
                            triggerHaptic(context, hapticEnabled)
                            val elapsed = (timeLimitSeconds - remainingTime).coerceAtLeast(1)
                            viewModel.recordSandboxSession(elapsed, "swiped_to_next")
                        }
                    )
                } else if (isBlocked) {
                    // Blocked Intercept View inside player
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF1E1B4B), Color(0xFF090D16))
                                )
                            )
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(FocusRose.copy(alpha = 0.2f))
                                    .border(2.dp, FocusRose, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null,
                                    tint = FocusRose,
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Future Shorts Blocked!",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (blockReason == "swiped_to_next") {
                                    "Strict Mode: You watched the 1 allowed Short. Swiping to video 2 is intercepted!"
                                } else {
                                    "Time limit of ${timeLimitSeconds}s expired. Your focus is safeguarded."
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFCBD5E1),
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            BreathingExerciseCircle()

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    isBlocked = false
                                    isPlaying = false
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FocusCyan),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reset_sandbox_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color.Black
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Reset Sandbox",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // Ready to Play Idle Screen
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(FocusCyan.copy(alpha = 0.2f))
                                .border(2.dp, FocusCyan, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = FocusCyan,
                                modifier = Modifier.size(46.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Ready for Allowed Short",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Limit: 1 video • ${timeLimitSeconds} seconds",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8)
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                isPlaying = true
                                isBlocked = false
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FocusCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("start_single_short_button")
                        ) {
                            Text(
                                text = "Watch First Short Only",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }

        // Sample Short Selector Cards
        Text(
            text = "Select Sample Focus Video",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        sampleShorts.forEach { sample ->
            val isSelected = selectedShort.id == sample.id
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF1E293B) else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isSelected) FocusCyan else Color(0xFF334155),
                        RoundedCornerShape(16.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sample.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "${sample.category} • ${sample.channel}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FocusCyan
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            selectedShort = sample
                            isPlaying = true
                            isBlocked = false
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) FocusCyan else Color.Transparent
                        )
                    ) {
                        Text(
                            text = if (isSelected && isPlaying) "Playing" else "Select & Test",
                            color = if (isSelected) Color.Black else Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Native Jetpack Compose Simulated Shorts Reel Player
 * Renders realistic Shorts interface with zero WebView/GPU crashes,
 * simulated video motion, audio indicator, and swipe gesture detection.
 */
@Composable
fun SimulatedShortPlayerView(
    short: SampleShort,
    remainingTime: Int,
    totalLimit: Int,
    onSwipeToNext: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "player_ambient")
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_shift"
    )

    val discRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc_rotation"
    )

    val soundWaveScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sound_wave"
    )

    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        short.colors[0],
                        short.colors.getOrElse(1) { Color(0xFF1E293B) },
                        Color.Black
                    ),
                    startY = gradientShift * 200f
                )
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        accumulatedDrag += dragAmount
                        if (accumulatedDrag < -60f) {
                            // User swiped upwards towards video #2!
                            accumulatedDrag = 0f
                            onSwipeToNext()
                        }
                    },
                    onDragEnd = {
                        accumulatedDrag = 0f
                    }
                )
            }
    ) {
        // Ambient simulated video motion pulse
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size((140 * soundWaveScale).dp)
                .clip(CircleShape)
                .background(short.colors.last().copy(alpha = 0.25f))
        )

        // Overlay Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP HUD: Allowance and live countdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.78f))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = FocusEmerald,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Video 1 of 1 (Allowed)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "•", color = Color(0xFF64748B))
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = FocusCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${remainingTime}s left",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = FocusCyan,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // MIDDLE / BOTTOM: Shorts UI (Channel, Title, Actions)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Left Column: Channel & Video Details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Channel row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(FocusCyan)
                        ) {
                            Text(
                                text = short.channel.take(2).uppercase().replace("@", ""),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = short.channel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Subscribe",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Video Title
                    Text(
                        text = short.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Sound track ticker
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = FocusCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = short.soundTitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right Column: Shorts Action Buttons
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ShortsActionButton(icon = Icons.Default.ThumbUp, label = short.likes)
                    ShortsActionButton(icon = Icons.Default.ThumbDown, label = "Dislike")
                    ShortsActionButton(icon = Icons.Default.Comment, label = short.comments)
                    ShortsActionButton(icon = Icons.Default.Share, label = "Share")

                    // Spinning sound disc
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .rotate(discRotation)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .border(2.dp, Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = FocusCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Floating Bottom Trigger: User swipes or taps to simulate going to 2nd video
        Button(
            onClick = onSwipeToNext,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.88f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .border(1.dp, FocusRose.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                .testTag("simulate_swipe_button")
        ) {
            Icon(
                imageVector = Icons.Default.SwipeVertical,
                contentDescription = null,
                tint = FocusRose,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Simulate Swiping to Next Short (Swipe Up)",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
fun ShortsActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

private fun triggerHaptic(context: Context, enabled: Boolean) {
    if (!enabled) return
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(200)
        }
    } catch (_: Exception) {
    }
}
