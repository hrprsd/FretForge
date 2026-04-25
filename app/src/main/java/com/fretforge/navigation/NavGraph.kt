package com.fretforge.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fretforge.ui.home.TaskLibraryScreen
import com.fretforge.ui.group.GroupReviewScreen
import com.fretforge.ui.practice.PracticeScreen
import com.fretforge.ui.summary.SummaryScreen
import com.fretforge.ui.history.HistoryScreen
import com.fretforge.ui.group.GroupsScreen

@Composable
fun FretForgeAppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Define when to show Bottom Bar
    val showBottomBar = currentRoute in listOf("home", "groups", "history", "settings")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val items = listOf(
                        Triple("home", "Home", Icons.Filled.Home),
                        Triple("groups", "Groups", Icons.Filled.List),
                        Triple("history", "History", Icons.Filled.History),
                        Triple("settings", "Settings", Icons.Filled.Settings)
                    )
                    items.forEach { (route, label, icon) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = currentRoute == route,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { TaskLibraryScreen(navController) }
            composable("groups") { GroupsScreen(navController) }
            composable("history") { HistoryScreen(navController) }
            composable("settings") { Text("Settings Placeholder") }
            composable(
                "group_review/{taskIds}?groupId={groupId}",
                arguments = listOf(
                    navArgument("taskIds") { type = NavType.StringType },
                    navArgument("groupId") { type = NavType.LongType; defaultValue = 0L }
                )
            ) { backStackEntry ->
                GroupReviewScreen(
                    navController, 
                    backStackEntry.arguments?.getString("taskIds") ?: "",
                    backStackEntry.arguments?.getLong("groupId") ?: 0L
                )
            }
            composable(
                "practice/{groupId}",
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) {
                PracticeScreen(navController)
            }
            composable(
                "summary/{sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) {
                SummaryScreen(navController)
            }
        }
    }
}
