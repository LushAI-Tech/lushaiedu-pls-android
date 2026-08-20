package com.lushaiedupls.ui.navigation

object AppRoutes {
    const val WELCOME = "welcome"
    const val SIGN_IN = "sign_in"
    const val CREATE_ACCOUNT = "create_account"
    const val SELECT_ROLE = "select_role"
    const val SELECT_CLASS = "select_class"
    const val SELECT_SUBJECT = "select_subject"

    const val STUDENT_SHELL = "student_shell"
    const val TEACHER_SHELL = "teacher_shell"
    const val PARENT_SHELL = "parent_shell"
    const val COMING_SOON = "coming_soon"
}

/** @deprecated Use [AppRoutes] */
@Deprecated("Use AppRoutes", ReplaceWith("AppRoutes"))
object AuthRoutes {
    const val WELCOME = AppRoutes.WELCOME
    const val SIGN_IN = AppRoutes.SIGN_IN
    const val CREATE_ACCOUNT = AppRoutes.CREATE_ACCOUNT
    const val SELECT_ROLE = AppRoutes.SELECT_ROLE
    const val SELECT_CLASS = AppRoutes.SELECT_CLASS
    const val SELECT_SUBJECT = AppRoutes.SELECT_SUBJECT
    const val HOME = AppRoutes.STUDENT_SHELL
}
