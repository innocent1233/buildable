package com.libraryx.ui.navigation

/**
 * Mirrors the route paths declared in src/App.tsx. The Solo-mode routes
 * (`/mode-select`, `/student-portal`) are kept even though current routing redirects
 * "/" straight to "/saas/login" — see ModeSelect.tsx, which is unreachable in the present
 * build but preserved here in case Solo mode is re-enabled at the root route.
 */
object Routes {
    const val SAAS_LOGIN = "saas/login"
    const val SAAS_DASHBOARD = "saas/subadmin/dashboard"
    const val SAAS_STUDENTS = "saas/subadmin/students"
    const val SAAS_MONTHLY_DUES = "saas/subadmin/monthly-dues"
    const val SAAS_SEAT_MAP = "saas/subadmin/seat-map"
    const val SAAS_REPORTS = "saas/subadmin/reports"
    const val SAAS_BACKUP = "saas/subadmin/backup"
    const val SAAS_SETTINGS = "saas/subadmin/settings"
    const val SAAS_STUDENT_PORTAL = "saas/student"
    const val MODE_SELECT = "mode-select"
    const val NOT_FOUND = "not-found"
}
