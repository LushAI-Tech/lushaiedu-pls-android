package com.lushaiedupls.ui.auth.selectrole

enum class UserRole {
    Teacher,
    Student,
    Admin,
    Parents,
}

data class RoleChoice(
    val role: UserRole,
    val label: String,
    val requiresInviteCode: Boolean,
)

data class SelectRoleUiState(
    val roles: List<RoleChoice> = emptyList(),
    val selectedRole: UserRole? = UserRole.Student,
    val inviteCode: String = "",
    val isLoadingRoles: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val finishedRoute: String? = null,
) {
    val selectedChoice: RoleChoice?
        get() = roles.firstOrNull { it.role == selectedRole }

    val requiresInviteCode: Boolean
        get() = selectedChoice?.requiresInviteCode == true
}
