package de.gutscheinsafe.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.gutscheinsafe.data.Settings
import de.gutscheinsafe.ui.screens.DetailScreen
import de.gutscheinsafe.ui.screens.EditScreen
import de.gutscheinsafe.ui.screens.HomeScreen
import de.gutscheinsafe.ui.screens.IdeasScreen
import de.gutscheinsafe.ui.screens.ImageScreen
import de.gutscheinsafe.ui.screens.SettingsScreen
import kotlinx.coroutines.flow.first

sealed interface DeepLink {
    data class Voucher(val id: Long) : DeepLink
    data object Ideas : DeepLink
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Gutscheine", Icons.Filled.List),
    Tab("ideas", "Ideen", Icons.Filled.Star),
    Tab("settings", "Einstellungen", Icons.Filled.Settings),
)

@Composable
fun AppRoot(deepLink: DeepLink?, onDeepLinkHandled: () -> Unit) {
    val nav = rememberNavController()
    val vm: AppViewModel = viewModel()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    AskForNotificationPermissionOnce()

    LaunchedEffect(deepLink) {
        if (deepLink == null) return@LaunchedEffect
        nav.currentBackStackEntryFlow.first() // wait until the graph is ready
        when (deepLink) {
            is DeepLink.Voucher -> nav.navigate("detail/${deepLink.id}") { launchSingleTop = true }
            DeepLink.Ideas -> nav.switchTab("ideas")
        }
        onDeepLinkHandled()
    }

    Scaffold(
        bottomBar = {
            if (tabs.any { it.route == route }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { inner ->
        val bottom = PaddingValues(bottom = inner.calculateBottomPadding())
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(bottom).consumeWindowInsets(bottom),
        ) {
            composable("home") {
                HomeScreen(
                    vm = vm,
                    onOpen = { nav.navigate("detail/$it") },
                    onAdd = { nav.navigate("edit/0") },
                )
            }
            composable("ideas") {
                IdeasScreen(vm = vm, onOpen = { nav.navigate("detail/$it") })
            }
            composable("settings") {
                SettingsScreen(vm = vm)
            }
            composable(
                "detail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                DetailScreen(
                    vm = vm,
                    id = id,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate("edit/$id") },
                    onImage = { side -> nav.navigate("image/$id/$side") },
                )
            }
            composable(
                "edit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                EditScreen(
                    vm = vm,
                    id = id,
                    onDone = { savedId ->
                        nav.popBackStack()
                        if (id == 0L && savedId != null) nav.navigate("detail/$savedId")
                    },
                )
            }
            composable(
                "image/{id}/{side}",
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType },
                    navArgument("side") { type = NavType.StringType },
                ),
            ) { entry ->
                ImageScreen(
                    vm = vm,
                    id = entry.arguments?.getLong("id") ?: 0L,
                    initialSide = entry.arguments?.getString("side") ?: "front",
                    onClose = { nav.popBackStack() },
                )
            }
        }
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AskForNotificationPermissionOnce() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val settings = Settings(context)
        if (!settings.notificationPermissionAsked) {
            settings.notificationPermissionAsked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
