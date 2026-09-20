package com.example.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class BlockedShortsActivity : ComponentActivity() {

    companion object {
        const val EXTRA_REASON = "extra_reason"
        const val EXTRA_SECONDS_WATCHED = "extra_seconds_watched"
        const val EXTRA_LIMIT_SECONDS = "extra_limit_seconds"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val reason = intent.getStringExtra(EXTRA_REASON) ?: "time_limit_expired"
        val secondsWatched = intent.getIntExtra(EXTRA_SECONDS_WATCHED, 60)
        val limitSeconds = intent.getIntExtra(EXTRA_LIMIT_SECONDS, 60)

        setContent {
            MyApplicationTheme {
                BlockedScreen(
                    reason = reason,
                    secondsWatched = secondsWatched,
                    limitSeconds = limitSeconds,
                    onGoHome = {
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(homeIntent)
                        finish()
                    },
                    onOpenApp = {
                        val mainIntent = Intent(this@BlockedShortsActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(mainIntent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun BlockedScreen(
    reason: String,
    secondsWatched: Int,
    limitSeconds: Int,
    onGoHome: () -> Unit,
    onOpenApp: () -> Unit
) {
    val darkBg = Color(0xFF090D16)
    val cardBg = Color(0xFF131A2B)
    val accentRed = Color(0xFFF43F5E)
    val cyanAccent = Color(0xFF38BDF8)
    val emeraldAccent = Color(0xFF10B981)

    // Reason explanation string
    val reasonDesc = when (reason) {
        "swiped_to_next" -> "Strict Mode: Swiping to a 2nd video is blocked. You watched your 1 allowed short."
        "cooldown_active" -> "Cooldown active! You have already used your allowed short for this focus window."
        else -> "Time limit of ${limitSeconds}s reached on your first short. Infinite feed locked."
    }

    Scaffold(
        containerColor = darkBg,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Shield Banner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(accentRed.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                        .border(2.dp, accentRed, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield Icon",
                        tint = accentRed,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Doomscroll Intercepted!",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = reasonDesc,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF94A3B8),
                        lineHeight = 20.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            // Interactive Breathing Reset Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelfImprovement,
                            contentDescription = null,
                            tint = cyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mindful Dopamine Reset",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = cyanAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    BreathingExerciseCircle()

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = emeraldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "1 Short Watched",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFCBD5E1),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        Text(
                            text = "${secondsWatched}s / ${limitSeconds}s limit",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = cyanAccent,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            // Bottom Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onGoHome,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = cyanAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("leave_youtube_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = Color(0xFF090D16)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Leave to Home Screen",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color(0xFF090D16),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                OutlinedButton(
                    onClick = onOpenApp,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("open_dashboard_button")
                ) {
                    Text(
                        text = "Open Shorts Shield Settings",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun BreathingExerciseCircle() {
    var phaseIndex by remember { mutableIntStateOf(0) }
    val phases = listOf("Breathe In", "Hold", "Breathe Out", "Relax")

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            phaseIndex = (phaseIndex + 1) % phases.size
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val currentPhase = phases[phaseIndex]

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(130.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF38BDF8).copy(alpha = 0.35f),
                        Color(0xFF818CF8).copy(alpha = 0.15f),
                        Color.Transparent
                    )
                )
            )
            .border(2.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), CircleShape)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = currentPhase,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = "4s pulse",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF94A3B8)
                )
            )
        }
    }
}
