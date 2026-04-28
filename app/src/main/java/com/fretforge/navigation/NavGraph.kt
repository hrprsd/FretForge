package com.fretforge.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.fretforge.ui.components.LocalDrawerState
import com.fretforge.ui.group.GroupReviewScreen
import com.fretforge.ui.group.GroupsScreen
import com.fretforge.ui.history.HistoryScreen
import com.fretforge.ui.home.TaskLibraryScreen
import com.fretforge.ui.practice.PracticeScreen
import com.fretforge.ui.settings.SettingsScreen
import com.fretforge.ui.songs.SongsScreen
import com.fretforge.ui.summary.SummaryScreen
import com.fretforge.ui.theme.*
import com.fretforge.ui.tuner.TunerScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FretForgeAppNavigation() {
    val navController  = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute   = backStackEntry?.destination?.route

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope       = rememberCoroutineScope()

    val exerciseRoutes = setOf("home", "groups", "history", "settings")
    val showBottomBar  = currentRoute in exerciseRoutes

    val currentSection = when {
        currentRoute == "songs" -> "songs"
        currentRoute == "tuner" -> "tuner"
        else                    -> "exercises"
    }

    CompositionLocalProvider(LocalDrawerState provides drawerState) {
        ModalNavigationDrawer(
            drawerState   = drawerState,
            drawerContent = {
                FretForgeDrawer(
                    currentSection  = currentSection,
                    onSectionSelect = { section ->
                        scope.launch { drawerState.close() }
                        when (section) {
                            "exercises" -> navController.navigate("home") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true
                            }
                            "songs" -> navController.navigate("songs") {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                            "tuner" -> navController.navigate("tuner") {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar(
                            containerColor = DarkCard,
                            tonalElevation = 0.dp
                        ) {
                            listOf(
                                Triple("home",     "Home",    Icons.Filled.Home),
                                Triple("groups",   "Groups",  Icons.Filled.List),
                                Triple("history",  "History", Icons.Filled.History),
                                Triple("settings", "Settings",Icons.Filled.Settings)
                            ).forEach { (route, label, icon) ->
                                NavigationBarItem(
                                    icon     = { Icon(icon, contentDescription = label) },
                                    label    = {
                                        Text(label, style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (currentRoute == route) FontWeight.Bold else FontWeight.Normal
                                        ))
                                    },
                                    selected = currentRoute == route,
                                    onClick  = {
                                        navController.navigate(route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState    = true
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor   = OnDarkPrimary,
                                        selectedTextColor   = AmberGold,
                                        indicatorColor      = AmberGold,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController    = navController,
                    startDestination = "home",
                    modifier         = Modifier.padding(innerPadding)
                ) {
                    composable("home")    { TaskLibraryScreen(navController) }
                    composable("groups")  { GroupsScreen(navController) }
                    composable("history") { HistoryScreen(navController) }
                    composable("settings") { SettingsScreen() }
                    composable("songs")  { SongsScreen() }
                    composable("tuner")  { TunerScreen() }
                    composable(
                        "group_review/{taskIds}?groupId={groupId}",
                        arguments = listOf(
                            navArgument("taskIds") { type = NavType.StringType },
                            navArgument("groupId") { type = NavType.LongType; defaultValue = 0L }
                        )
                    ) { entry ->
                        GroupReviewScreen(
                            navController,
                            entry.arguments?.getString("taskIds") ?: "",
                            entry.arguments?.getLong("groupId") ?: 0L
                        )
                    }
                    composable(
                        "practice/{groupId}",
                        arguments = listOf(navArgument("groupId") { type = NavType.StringType })
                    ) { PracticeScreen(navController) }
                    composable(
                        "summary/{sessionId}",
                        arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
                    ) { SummaryScreen(navController) }
                }
            }
        }
    }
}

// ── Drawer ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FretForgeDrawer(
    currentSection: String,
    onSectionSelect: (String) -> Unit
) {
    ModalDrawerSheet(drawerContainerColor = DarkCard) {
        // Branded header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(AmberGoldDark.copy(alpha = 0.4f), ElectricBlueDark.copy(alpha = 0.3f))
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    "FretForge",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        brush = Brush.horizontalGradient(listOf(AmberGoldLight, ElectricBlueLight))
                    )
                )
                Text("Guitar Practice App", color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(12.dp))

        DrawerNavItem(
            icon    = Icons.Filled.FitnessCenter,
            label   = "Exercises",
            section = "exercises",
            current = currentSection,
            onClick = onSectionSelect
        )
        DrawerNavItem(
            icon    = Icons.Filled.MusicNote,
            label   = "Practice Songs",
            section = "songs",
            current = currentSection,
            onClick = onSectionSelect
        )
        DrawerNavItem(
            icon    = Icons.Filled.GraphicEq,
            label   = "Tune Guitar",
            section = "tuner",
            current = currentSection,
            onClick = onSectionSelect
        )
    }
}

@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    label: String,
    section: String,
    current: String,
    onClick: (String) -> Unit
) {
    val selected = section == current
    val bg = if (selected) AmberGoldDim else androidx.compose.ui.graphics.Color.Transparent
    val tint = if (selected) AmberGold else TextSecondary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        onClick = { onClick(section) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(16.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (selected) AmberGold else OnDarkSurface
            )
        }
    }
}
