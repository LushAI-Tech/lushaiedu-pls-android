package com.lushaiedupls.ui.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

fun NavHostController.navigateToRoleTab(
    route: String,
    moreRoute: String,
) {
    if (route == moreRoute) {
        if (popBackStack(moreRoute, inclusive = false)) return
        navigate(moreRoute) {
            popUpTo(graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
        }
        return
    }
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
