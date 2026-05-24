package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.Screen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2200) // Pulse and show beautiful animation, then transition
        onTimeout()
    }

    // Pulsing core animation scale
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val tealColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // High fidelity drawing representing Medical Wholesale symbol
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background clinical aura
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = tealColor,
                        radius = (size.minDimension / 2.3f) * pulseScale,
                        alpha = 0.15f
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = size.minDimension / 1.8f,
                        style = Stroke(width = 2.dp.toPx()),
                        alpha = 0.3f
                    )
                }

                // Modern vector-style cross + pill illustration
                Canvas(modifier = Modifier.size(60.dp)) {
                    val w = size.width
                    val h = size.height
                    val thickness = w * 0.28f

                    // Draw classic healthcare vertical bar
                    drawRect(
                        color = primaryColor,
                        topLeft = Offset((w - thickness) / 2, 0f),
                        size = androidx.compose.ui.geometry.Size(thickness, h)
                    )
                    // Draw healthcare horizontal bar
                    drawRect(
                        color = primaryColor,
                        topLeft = Offset(0f, (h - thickness) / 2),
                        size = androidx.compose.ui.geometry.Size(w, thickness)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "PharmaStore Systems",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Text(
                text = "Wholesale Pharmacy Distribution Suite",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.secondary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
        }

        // Bottom tagline matching canonical branding guidelines
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Secure Business Logistics Layer • Offline First",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                letterSpacing = 1.sp
            )
        }
    }
}
