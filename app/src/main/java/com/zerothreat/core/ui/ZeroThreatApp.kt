package com.zerothreat.core.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zerothreat.core.data.AppPreferences
import com.zerothreat.core.ui.dashboard.DashboardScreen
import com.zerothreat.core.ui.database.DatabaseScreen
import com.zerothreat.core.ui.manual.ManualCheckScreen
import com.zerothreat.core.ui.settings.SettingsScreen
import com.zerothreat.core.ui.theme.*
import kotlinx.coroutines.delay

// ==================== APP ENTRY ====================
@Composable
fun ZeroThreatApp() {
    ZeroThreatTheme {
        val navController = rememberNavController()
        val context = LocalContext.current
        val appPreferences = remember { AppPreferences(context) }
        val hazeState = rememberUiSurfaceState()

        val items = listOf(
            Screen.Dashboard,
            Screen.ManualCheck,
            Screen.Database,
            Screen.Settings
        )

        CompositionLocalProvider(LocalUiSurfaceState provides hazeState) {
            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route
            ) {
                // ── Splash ──
                composable(Screen.Splash.route) {
                    SplashScreen(
                        onSplashFinished = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    )
                }

                // ── Main App (with bottom nav) ──
                composable(Screen.Dashboard.route) {
                    MainScaffold(
                        hazeState = hazeState,
                        items = items,
                        appPreferences = appPreferences,
                        startRoute = Screen.Dashboard.route
                    )
                }
            }
        }
    }
}

// ==================== MAIN SCAFFOLD (bottom nav) ====================
@Composable
fun MainScaffold(
    hazeState: UiSurfaceState,
    items: List<Screen>,
    appPreferences: AppPreferences,
    startRoute: String
) {
    val innerNavController = rememberNavController()

    Scaffold(
        modifier = Modifier.appBackground(hazeState),
        containerColor = DarkBackground,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .appContainer(
                            hazeState = hazeState,
                            cornerRadius = 24.dp,
                            thin = true
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            contentColor = TextSecondary,
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets(0, 0, 0, 0)
                        ) {
                            val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination

                            items.forEach { screen ->
                                val selected = currentDestination?.hierarchy
                                    ?.any { it.route == screen.route } == true
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) screen.selectedIcon
                                            else screen.icon,
                                            contentDescription = screen.title,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = null,
                                    alwaysShowLabel = false,
                                    selected = selected,
                                    onClick = {
                                        innerNavController.navigate(screen.route) {
                                            popUpTo(
                                                innerNavController.graph
                                                    .findStartDestination().id
                                            ) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = TextWhite,
                                        unselectedIconColor = TextWhite.copy(alpha = 0.82f),
                                        indicatorColor = SurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = innerNavController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(navController = innerNavController)
            }
            composable(Screen.ManualCheck.route) {
                ManualCheckScreen()
            }
            composable(Screen.Database.route) {
                DatabaseScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    appPreferences = appPreferences,
                    onNavigateBack = { innerNavController.popBackStack() }
                )
            }
        }
    }
}

// ==================== SPLASH SCREEN ====================
@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {

    // Auto-navigate after 2.5 seconds
    LaunchedEffect(Unit) {
        delay(2500)
        onSplashFinished()
    }

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shield_scale"
    )

    val fadeIn by produceState(initialValue = 0f) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(900, easing = EaseOutCubic)
        ) { value, _ -> this.value = value }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF011B1A),
                        Color(0xFF042F2E),
                        Color(0xFF011B1A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // ── Background glow orb ──
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.Center)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonTeal.copy(alpha = pulseAlpha),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // ── Second smaller glow bottom ──
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomCenter)
                .offset(y = (-60).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CyberTeal.copy(alpha = pulseAlpha * 0.6f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // ── Main content ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(alpha = fadeIn),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Shield icon with pulse
            Box(
                modifier = Modifier
                    .size((110 * pulseScale).dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NeonTeal.copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Outer ring
                Surface(
                    modifier = Modifier.size(88.dp),
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp, NeonTeal.copy(alpha = pulseAlpha + 0.4f)
                    )
                ) {}

                // Inner shield
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = NeonTeal.copy(alpha = pulseAlpha + 0.55f),
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App name
            Text(
                text = "ZeroThreat",
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 36.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Your links. Analyzed. Secured.",
                color = NeonTeal.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Bottom badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CardBackground,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, BorderColor
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = NeonTeal.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Local-only · Zero data leaks",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

// ==================== SCREENS ====================
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    object Splash      : Screen("splash",    "Splash",    Icons.Rounded.Home,     Icons.Filled.Home)
    object Dashboard   : Screen("dashboard", "Home",      Icons.Rounded.Home,     Icons.Filled.Home)
    object ManualCheck : Screen("manual",    "Check",     Icons.Rounded.Search,   Icons.Filled.Search)
    object Database    : Screen("database",  "Database",  Icons.Rounded.Storage,  Icons.Filled.Storage)
    object Settings    : Screen("settings",  "Settings",  Icons.Rounded.Settings, Icons.Filled.Settings)
}