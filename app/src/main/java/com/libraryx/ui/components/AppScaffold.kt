package com.libraryx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.libraryx.ui.navigation.Routes
import com.libraryx.ui.theme.StudyLabColors
import kotlinx.coroutines.launch

/**
 * Compose port of src/components/AppLayout.tsx. Radix's collapsible `<Sidebar>` becomes a
 * `ModalNavigationDrawer` (the standard mobile-first equivalent — a permanent rail doesn't
 * suit phone width), and the `<SidebarTrigger>` becomes the menu icon in the [TopAppBar].
 * `ThemeToggle` and the green "System Online" pulse dot are preserved as-is.
 */
private data class NavItem(val title: String, val route: String, val icon: ImageVector)

private fun navItems(): List<NavItem> = listOf(
    NavItem("Dashboard", Routes.SAAS_DASHBOARD, Icons.Filled.Dashboard),
    NavItem("Students", Routes.SAAS_STUDENTS, Icons.Filled.People),
    NavItem("Monthly Dues", Routes.SAAS_MONTHLY_DUES, Icons.Filled.CalendarMonth),
    NavItem("Seat Map", Routes.SAAS_SEAT_MAP, Icons.Filled.Map),
    NavItem("Reports", Routes.SAAS_REPORTS, Icons.Filled.BarChart),
    NavItem("Backup & Restore", Routes.SAAS_BACKUP, Icons.Filled.Save),
    NavItem("Settings", Routes.SAAS_SETTINGS, Icons.Filled.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    currentRoute: String,
    labName: String,
    onNavigate: (String) -> Unit,
    onToggleTheme: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(StudyLabColors.NeonGreen.copy(alpha = 0.15f))
                    )
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("LIBRARY X", style = MaterialTheme.typography.titleMedium, color = StudyLabColors.NeonGreen)
                        Text(labName, style = MaterialTheme.typography.labelSmall, color = StudyLabColors.TextMuted)
                    }
                }
                Text(
                    "NAVIGATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = StudyLabColors.TextMuted,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                )
                navItems().forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationDrawerItem(
                        label = { Text(item.title) },
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        selected = selected,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigate(item.route)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Dashboard, contentDescription = "Menu")
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(StudyLabColors.NeonGreen)
                            )
                            Text(
                                "  SYSTEM ONLINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = StudyLabColors.TextMuted
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onToggleTheme) {
                            Icon(Icons.Filled.Settings, contentDescription = "Toggle theme")
                        }
                    }
                )
            }
        ) { padding ->
            content(padding)
        }
    }
}
