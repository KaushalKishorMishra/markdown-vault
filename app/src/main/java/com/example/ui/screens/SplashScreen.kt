package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.R
import kotlinx.coroutines.delay


@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation states
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(40f) }

    LaunchedEffect(key1 = true) {
        // Logo entry animation (scale and fade)
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
        
        // Brand text entry animation (fade and slide up)
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
        )
        textOffsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )

        // Hold splash screen for some time
        delay(1500)
        
        // Exit animation (fade out everything)
        scale.animateTo(
            targetValue = 1.2f,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 400)
        )
        textAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 400)
        )
        
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            )
            .padding(bottom = 56.dp) // Bottom offset spacing
    ) {
        // 1. Center Section: Animated App Logo
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(130.dp)
                .scale(scale.value)
                .alpha(alpha.value)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "App Logo",
                tint = androidx.compose.ui.graphics.Color.Unspecified,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Bottom Section: Brand Name & Tagline
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .alpha(textAlpha.value)
                .offset(y = textOffsetY.value.dp)
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "Markdown Vault",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your Notes. Secure. Anywhere.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
