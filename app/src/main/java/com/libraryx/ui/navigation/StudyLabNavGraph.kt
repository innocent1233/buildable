package com.libraryx.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.libraryx.ui.components.AppScaffold
import com.libraryx.ui.screens.backup.BackupScreen
import com.libraryx.ui.screens.dashboard.DashboardScreen
import com.libraryx.ui.screens.modeselect.ModeSelectScreen
import com.libraryx.ui.screens.monthlydues.MonthlyDuesScreen
import com.libraryx.ui.screens.notfound.NotFoundScreen
import com.libraryx.ui.screens.reports.ReportsScreen
import com.libraryx.ui.screens.saaslogin.SaasLoginScreen
import com.libraryx.ui.screens.seatmap.SeatMapScreen
import com.libraryx.ui.screens.settings.SettingsScreen
import com.libraryx.ui.screens.studentportal.StudentPortalScreen
import com.libraryx.ui.screens.students.StudentsScreen

/**
 * Root NavHost — mirrors src/App.tsx's `<Routes>` tree, with the same entry-point redirect:
 * `startDestination = Routes.SAAS_LOGIN` (because App.tsx redirects "/" to "/saas/login").
 *
 * The `SaasAuthViewModel` is scoped here (not inside each screen) so it survives navigation
 * events exactly like `<SaasAuthProvider>` wrapped the whole `<Routes>` tree.
 *
 * `onGoogleSignInRequested` is a callback supplied by [MainActivity], which owns the
 * Credential Manager API (the native replacement for `signInWithPopup`).
 */
@Composable
fun StudyLabNavGraph(
    onGoogleSignInRequested: (onToken: (String?) -> Unit) -> Unit,
    sessionHolder: SaasSessionHolder
) {
    val navController: NavHostController = rememberNavController()
    val authViewModel: SaasAuthViewModel = hiltViewModel()
    val authState by authViewModel.state.collectAsState()
    var darkTheme by remember { mutableStateOf(true) }

    // Mirror SaasStudyLabBridge.tsx: bind/clear the session-scoped Firebase repository
    // whenever authentication state changes.
    LaunchedEffect(authState.subAdmin) {
        if (authState.subAdmin != null) sessionHolder.bind(authState.subAdmin!!)
        else sessionHolder.clear()
    }

    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route ?: ""

    val authenticatedRoutes = setOf(
        Routes.SAAS_DASHBOARD, Routes.SAAS_STUDENTS, Routes.SAAS_MONTHLY_DUES,
        Routes.SAAS_SEAT_MAP, Routes.SAAS_REPORTS, Routes.SAAS_BACKUP, Routes.SAAS_SETTINGS
    )

    NavHost(navController = navController, startDestination = Routes.SAAS_LOGIN) {

        composable(Routes.SAAS_LOGIN) {
            SaasLoginScreen(
                viewModel = authViewModel,
                onAuthenticated = { navController.navigate(Routes.SAAS_DASHBOARD) { popUpTo(Routes.SAAS_LOGIN) { inclusive = true } } },
                onGoogleSignInRequested = onGoogleSignInRequested,
                onNavigateToStudentPortal = { navController.navigate(Routes.SAAS_STUDENT_PORTAL) }
            )
        }

        composable(Routes.SAAS_STUDENT_PORTAL) {
            StudentPortalScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MODE_SELECT) {
            ModeSelectScreen(
                onSelectSaas = { navController.navigate(Routes.SAAS_LOGIN) },
                onSelectSolo = { /* Solo mode is not the current routing target; navigate home */ navController.navigate(Routes.SAAS_LOGIN) }
            )
        }

        composable(Routes.NOT_FOUND) {
            NotFoundScreen(onNavigateHome = { navController.navigate(Routes.SAAS_DASHBOARD) { popUpTo(0) } })
        }

        // ── Authenticated SaaS screens ──────────────────────────────────────
        // Each shares the same AppScaffold shell (= AppLayout.tsx) so the nav drawer
        // is available on every authenticated screen.

        composable(Routes.SAAS_DASHBOARD) {
            GuardAuthenticated(authState, navController) {
                AppScaffold(
                    currentRoute = Routes.SAAS_DASHBOARD,
                    labName = authState.subAdmin?.labName ?: "",
                    onNavigate = { navController.navigate(it) { launchSingleTop = true } },
                    onToggleTheme = { darkTheme = !darkTheme }
                ) { padding ->
                    androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
                        DashboardScreen(
                            onAddStudent = { navController.navigate(Routes.SAAS_STUDENTS) },
                            onViewStudents = { navController.navigate(Routes.SAAS_STUDENTS) },
                            onViewReports = { navController.navigate(Routes.SAAS_REPORTS) }
                        )
                    }
                }
            }
        }

        composable(Routes.SAAS_STUDENTS) {
            GuardAuthenticated(authState, navController) {
                AppScaffold(currentRoute = Routes.SAAS_STUDENTS, labName = authState.subAdmin?.labName ?: "",
                    onNavigate = { navController.navigate(it) { launchSingleTop = true } },
                    onToggleTheme = { darkTheme = !darkTheme }) { padding ->
                    androidx.compose.foundation.layout.Box(Modifier.padding(padding)) { StudentsScreen() }
                }
            }
        }

        composable(Routes.SAAS_MONTHLY_DUES) {
            GuardAuthenticated(authState, navController) {
                AppScaffold(currentRoute = Routes.SAAS_MONTHLY_DUES, labName = authState.subAdmin?.labName ?: "",
                    onNavigate = { navController.navigate(it) { launchSingleTop = true } },
                    onToggleTheme = { darkTheme = !darkTheme }) { padding ->
                    androidx.compose.foundation.layout.Box(Modifier.padding(padding)) { MonthlyDuesScreen() }
                }
            }
        }

        composable(Routes.SAAS_SEAT_MAP) {
            GuardAuthenticated(authState, navController) {
                AppScaffold(currentRoute = Routes.SAAS_SEAT_MAP, labName = authState.subAdmin?.labName ?: "",
                    onNavigate = { navController.navigate(it) { launchSingleTop = true } },
                    onToggleTheme = { darkTheme = !darkTheme }) { padding ->
                    androidx.compose.foundation.layout.Box(Modifier.padding(padding)) { SeatMapScreen() }
                }
            }
        }

        composable(Routes.SAAS_REPORTS) {
            GuardAuthenticated(authState, navController) {
                AppScaffold(currentRoute = Routes.SAAS_REPORTS, labName = authState.subAdmin?.labName ?: "",
                    onNavigate = { navController.navigate(it) { launchSingleTop = true } },
                    onToggleTheme = { darkTheme = !darkTheme }) { padding ->
                    androidx.compose.foundation.layout.Box(Modifier.padding(padding)) { ReportsScreen() }
                }
            }
        }

        composable(Routes.SAAS_BACKUP) {
            GuardAuthenticated(authState, navController) {
                AppScaffold(currentRoute = Routes.SAAS_BACKUP, labName = authState.subAdmin?.labName ?: "",
                    onNavigate = { navController.navigate(it) { launchSingleTop = true } },
                    onToggleTheme = { darkTheme = !darkTheme }) { padding ->
                    androidx.compose.foundation.layout.Box(Modifier.padding(padding)) { BackupScreen() }
                }
            }
        }

        composable(Routes.SAAS_SETTINGS) {
            GuardAuthenticated(authState, navController) {
                AppScaffold(currentRoute = Routes.SAAS_SETTINGS, labName = authState.subAdmin?.labName ?: "",
                    onNavigate = { navController.navigate(it) { launchSingleTop = true } },
                    onToggleTheme = { darkTheme = !darkTheme }) { padding ->
                    androidx.compose.foundation.layout.Box(Modifier.padding(padding)) { SettingsScreen() }
                }
            }
        }
    }
}

/** Guards a route to require authentication — redirects to login if not signed in. */
@Composable
private fun GuardAuthenticated(
    authState: SaasAuthViewModel.UiState,
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    LaunchedEffect(authState.loading, authState.subAdmin) {
        if (!authState.loading && authState.subAdmin == null) {
            navController.navigate(Routes.SAAS_LOGIN) { popUpTo(0) { inclusive = true } }
        }
    }
    if (authState.subAdmin != null) content()
}
