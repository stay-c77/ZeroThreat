package com.zerothreat.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.zerothreat.app.data.AppPreferences
import com. zerothreat.app.ui.alerts.ThreatAlertDialog
import com.zerothreat.app. ui.alerts.ThreatLevel
import com.zerothreat.app.ui.dashboard.DashboardScreen
import com.zerothreat.app. ui.info.AboutScreen
import com.zerothreat.app.ui.info. HelpScreen
import com.zerothreat.app.ui.info. PrivacyPolicyScreen
import com.zerothreat.app.ui.manual.ManualCheckScreen
import com.zerothreat.app.ui.mode.AppMode
import com.zerothreat.app.ui.mode.ModeSelectionScreen
import com.zerothreat.app.ui.onboarding.OnboardingScreen
import com.zerothreat.app.ui.permissions.PermissionRequestScreen
import com.zerothreat.app.ui.settings. SettingsScreen
import com. zerothreat.app.ui.splash.SplashScreen
import com.zerothreat.app. ui.theme.*

class MainActivity : ComponentActivity() {
    private lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appPreferences = AppPreferences(this)

        setContent {
            ZeroThreatTheme {
                ZeroThreatApp(appPreferences)
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Permissions : Screen("permissions")
    object ModeSelection : Screen("mode")
    object Dashboard : Screen("dashboard")
    object ManualCheck : Screen("manual")
    object Settings : Screen("settings")
    object Privacy : Screen("privacy")
    object About : Screen("about")
    object Help : Screen("help")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZeroThreatApp(appPreferences: AppPreferences) {
    val navController = rememberNavController()
    var showBottomBar by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?. route

    val startDestination = remember {
        when {
            ! appPreferences.onboardingCompleted -> Screen.Splash.route
            ! appPreferences.permissionsGranted -> Screen.Permissions.route
            !appPreferences.modeSelected -> Screen.ModeSelection.route
            else -> Screen.Dashboard.route
        }
    }

    LaunchedEffect(currentRoute) {
        showBottomBar = currentRoute in listOf(
            Screen.Dashboard.route,
            Screen.ManualCheck.route
        )
    }

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth()
                        .background(
                            color = PureBlack,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    NavigationBar(
                        containerColor = PureBlack,
                        contentColor = TextSecondary,
                        tonalElevation = 0.dp,
                        modifier = Modifier.background(
                            color = PureBlack,
                            shape = RoundedCornerShape(16.dp)
                        )
                    ) {
                        NavigationBarItem(
                            selected = currentRoute == Screen.Dashboard.route,
                            onClick = {
                                navController.navigate(Screen.Dashboard.route) {
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                            label = { Text("Home") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ElectricPurpleLight,
                                selectedTextColor = ElectricPurpleLight,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = CardBackground
                            )
                        )

                        NavigationBarItem(
                            selected = currentRoute == Screen.ManualCheck.route,
                            onClick = {
                                navController.navigate(Screen.ManualCheck.route) {
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(Icons.Default.Search, contentDescription = "Check Link") },
                            label = { Text("Check") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ElectricPurpleLight,
                                selectedTextColor = ElectricPurpleLight,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = CardBackground
                            )
                        )

                        NavigationBarItem(
                            selected = currentRoute == Screen.Settings.route,
                            onClick = {
                                navController.navigate(Screen.Settings.route) {
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ElectricPurpleLight,
                                selectedTextColor = ElectricPurpleLight,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = CardBackground
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        when {
                            dragAmount > 100 && currentRoute != Screen.Dashboard.route -> {
                                navController.navigate(Screen.Dashboard.route) {
                                    launchSingleTop = true
                                }
                            }
                            dragAmount < -100 && currentRoute != Screen.Settings.route -> {
                                if (currentRoute == Screen.Dashboard.route) {
                                    navController.navigate(Screen.ManualCheck.route) {
                                        launchSingleTop = true
                                    }
                                } else {
                                    navController.navigate(Screen.Settings.route) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(paddingValues)
            ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onSplashFinished = {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinish = {
                        appPreferences.onboardingCompleted = true
                        appPreferences.isFirstLaunch = false
                        navController.navigate(Screen. Permissions.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Permissions. route) {
                PermissionRequestScreen(
                    onPermissionsGranted = {
                        appPreferences.permissionsGranted = true
                        navController.navigate(Screen.ModeSelection.route) {
                            popUpTo(Screen.Permissions. route) { inclusive = true }
                        }
                    },
                    onSkip = {
                        appPreferences.permissionsGranted = true
                        navController.navigate(Screen.ModeSelection.route) {
                            popUpTo(Screen.Permissions.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.ModeSelection.route) {
                ModeSelectionScreen(
                    appPreferences = appPreferences,
                    onModeSelected = { mode ->
                        appPreferences. modeSelected = true
                        appPreferences.selectedMode = mode. name
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.ModeSelection.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen()
            }

            composable(Screen.ManualCheck.route) {
                var showAlert by remember { mutableStateOf(false) }
                var checkedUrl by remember { mutableStateOf("") }

                ManualCheckScreen(
                    onCheckUrl = { url ->
                        checkedUrl = url
                        showAlert = true
                    }
                )

                if (showAlert) {
                    ThreatAlertDialog(
                        url = checkedUrl,
                        threatLevel = ThreatLevel. PHISHING,
                        reason = "This URL matches known phishing patterns",
                        onBlock = { showAlert = false },
                        onViewSafely = { showAlert = false },
                        onIgnore = { showAlert = false },
                        onReport = { showAlert = false },
                        onDismiss = { showAlert = false }
                    )
                }
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    appPreferences = appPreferences,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToPrivacy = {
                        navController.navigate(Screen.Privacy. route)
                    },
                    onNavigateToAbout = {
                        navController.navigate(Screen.About.route)
                    },
                    onNavigateToHelp = {
                        navController.navigate(Screen.Help. route)
                    }
                )
            }

            composable(Screen.Privacy.route) {
                PrivacyPolicyScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.About.route) {
                AboutScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Help.route) {
                HelpScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}