package com.zerothreat.app.ui.splash

import androidx. compose.animation.core.Animatable
import androidx. compose.animation.core.tween
import androidx.compose.foundation. Image
import androidx.compose.foundation.background
import androidx.compose.foundation. layout.*
import androidx.compose.material3.MaterialTheme
import androidx. compose.material3.Text
import androidx.compose.runtime. Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose. ui.Alignment
import androidx. compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui. res.painterResource
import androidx. compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui. unit.sp
import com.zerothreat.app. R
import com.zerothreat.app.ui.theme. DarkBackground
import com.zerothreat.app.ui.theme.ElectricPurple
import com.zerothreat.app. ui.theme.TextPrimary
import com.zerothreat.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.8f) }

    LaunchedEffect(key1 = true) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
        delay(2500)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier. alpha(alpha.value)
        ) {
            Image(
                painter = painterResource(id = R.drawable.shield_logo),
                contentDescription = "ZeroThreat Shield",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ZeroThreat",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Protecting You from Cyber Threats",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Loading indicator dots
            Row(
                horizontalArrangement = Arrangement. spacedBy(8.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(ElectricPurple, shape = androidx.compose.foundation.shape.CircleShape)
                    )
                }
            }
        }
    }
}