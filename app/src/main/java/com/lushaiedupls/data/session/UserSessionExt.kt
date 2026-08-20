package com.lushaiedupls.data.session

import com.lushaiedupls.ui.auth.selectrole.UserRole
import com.lushaiedupls.ui.navigation.AppRoutes

fun UserSessionStore.destinationForRole(): String {
    return when (getRole()) {
        UserRole.Student -> AppRoutes.STUDENT_SHELL
        UserRole.Teacher -> AppRoutes.TEACHER_SHELL
        UserRole.Parents -> AppRoutes.PARENT_SHELL
        UserRole.Admin -> AppRoutes.COMING_SOON
        null -> AppRoutes.STUDENT_SHELL
    }
}
