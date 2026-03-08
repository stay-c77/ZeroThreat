package com.zerothreat.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
                                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                                    val currentDestination = navBackStackEntry?.destination

                                    items.forEach { screen ->
                                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                        NavigationBarItem(
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) screen.selectedIcon else screen.icon,
                                                    contentDescription = screen.title,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            },
                                            label = null,
                                            alwaysShowLabel = false,
                                            selected = selected,
                                            onClick = {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
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
                    navController = navController,
                    startDestination = Screen.Dashboard.route,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(Screen.Dashboard.route) {
                        DashboardScreen()
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
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    object Dashboard : Screen("dashboard", "Home", Icons.Rounded.Home, Icons.Filled.Home)
    object ManualCheck : Screen("manual", "Check", Icons.Rounded.Search, Icons.Filled.Search)
    object Database : Screen("database", "Database", Icons.Rounded.Storage, Icons.Filled.Storage)
    object Settings : Screen("settings", "Settings", Icons.Rounded.Settings, Icons.Filled.Settings)
}
