package com.example.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.feature.history.HistoryScreen
import com.example.feature.history.HistoryViewModel
import com.example.feature.result.ResultScreen
import com.example.feature.result.ResultViewModel
import com.example.feature.scanner.ScannerScreen
import com.example.feature.scanner.ScannerViewModel
import com.example.feature.settings.SettingsScreen
import com.example.feature.settings.SettingsViewModel

object ScanovaDestinations {
    const val ROUTE_SCANNER = "scanner"
    const val ROUTE_HISTORY = "history"
    const val ROUTE_SETTINGS = "settings"
    const val ROUTE_RESULT = "result/{scanId}"

    fun resultRoute(scanId: Long): String = "result/$scanId"
}

data class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TOP_LEVEL_DESTINATIONS = listOf(
    TopLevelDestination(
        route = ScanovaDestinations.ROUTE_SCANNER,
        labelRes = R.string.nav_scanner,
        selectedIcon = Icons.Filled.QrCodeScanner,
        unselectedIcon = Icons.Outlined.QrCodeScanner,
        testTag = "nav_tab_scanner"
    ),
    TopLevelDestination(
        route = ScanovaDestinations.ROUTE_HISTORY,
        labelRes = R.string.nav_history,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_tab_history"
    ),
    TopLevelDestination(
        route = ScanovaDestinations.ROUTE_SETTINGS,
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_tab_settings"
    )
)

@Composable
fun ScanovaNavGraph(
    scannerViewModel: ScannerViewModel,
    historyViewModel: HistoryViewModel,
    resultViewModel: ResultViewModel,
    settingsViewModel: SettingsViewModel,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val showNavigationShell = currentRoute in setOf(
        ScanovaDestinations.ROUTE_SCANNER,
        ScanovaDestinations.ROUTE_HISTORY,
        ScanovaDestinations.ROUTE_SETTINGS
    )

    val configuration = LocalConfiguration.current
    val isExpandedScreen = configuration.screenWidthDp >= 600

    fun navigateToTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showNavigationShell && !isExpandedScreen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                    TOP_LEVEL_DESTINATIONS.forEach { dest ->
                        val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToTopLevel(dest.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = stringResource(dest.labelRes)
                                )
                            },
                            label = {
                                Text(text = stringResource(dest.labelRes))
                            },
                            modifier = Modifier.testTag(dest.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showNavigationShell && isExpandedScreen) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("side_navigation_rail")
                ) {
                    TOP_LEVEL_DESTINATIONS.forEach { dest ->
                        val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTopLevel(dest.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = stringResource(dest.labelRes)
                                )
                            },
                            label = {
                                Text(text = stringResource(dest.labelRes))
                            },
                            modifier = Modifier.testTag(dest.testTag)
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                NavHost(
                    navController = navController,
                    startDestination = ScanovaDestinations.ROUTE_SCANNER
                ) {
                    composable(ScanovaDestinations.ROUTE_SCANNER) {
                        ScannerScreen(
                            viewModel = scannerViewModel,
                            onNavigateToResult = { scanId ->
                                navController.navigate(ScanovaDestinations.resultRoute(scanId))
                            },
                            onNavigateToHistory = {
                                navigateToTopLevel(ScanovaDestinations.ROUTE_HISTORY)
                            }
                        )
                    }

                    composable(ScanovaDestinations.ROUTE_HISTORY) {
                        HistoryScreen(
                            viewModel = historyViewModel,
                            onOpenScanDetail = { scanId ->
                                navController.navigate(ScanovaDestinations.resultRoute(scanId))
                            },
                            onNavigateToScanner = {
                                navigateToTopLevel(ScanovaDestinations.ROUTE_SCANNER)
                            }
                        )
                    }

                    composable(ScanovaDestinations.ROUTE_SETTINGS) {
                        SettingsScreen(
                            viewModel = settingsViewModel
                        )
                    }

                    composable(
                        route = ScanovaDestinations.ROUTE_RESULT,
                        arguments = listOf(
                            navArgument("scanId") { type = NavType.LongType }
                        )
                    ) { entry ->
                        val scanId = entry.arguments?.getLong("scanId") ?: -1L
                        ResultScreen(
                            scanId = scanId,
                            viewModel = resultViewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onScanAnother = {
                                scannerViewModel.resumeScanning()
                                navController.popBackStack(
                                    route = ScanovaDestinations.ROUTE_SCANNER,
                                    inclusive = false
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
