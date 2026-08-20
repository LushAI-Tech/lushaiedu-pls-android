package com.lushaiedupls.ui.auth.selectrole

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lushaiedupls.R
import com.lushaiedupls.ui.auth.components.LushAiEduBrandHeader
import com.lushaiedupls.ui.auth.components.OutlinedAuthField
import com.lushaiedupls.ui.auth.components.SelectionNavButtons
import com.lushaiedupls.ui.auth.components.SelectionTile
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.LushAIEdu_PLSTheme

@Composable
fun SelectRoleRoute(
    onBack: () -> Unit,
    onContinueToClass: () -> Unit,
    onFinished: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SelectRoleViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.finishedRoute) {
        val route = uiState.finishedRoute ?: return@LaunchedEffect
        viewModel.clearFinishedRoute()
        onFinished(route)
    }
    SelectRoleScreen(
        uiState = uiState,
        onRoleSelected = viewModel::onRoleSelected,
        onInviteCodeChange = viewModel::onInviteCodeChange,
        onBack = onBack,
        onContinue = {
            viewModel.submitRole(onContinueToClass = onContinueToClass)
        },
        modifier = modifier,
    )
}

@Composable
fun SelectRoleScreen(
    uiState: SelectRoleUiState,
    onRoleSelected: (UserRole) -> Unit,
    onInviteCodeChange: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LushAiEduBrandHeader(logoSize = 96.dp)
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.select_role),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
            Spacer(modifier = Modifier.height(24.dp))

            when {
                uiState.isLoadingRoles && uiState.roles.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = BrandBlack)
                    }
                }
                else -> {
                    uiState.roles.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            row.forEach { choice ->
                                SelectionTile(
                                    label = choice.label,
                                    selected = uiState.selectedRole == choice.role,
                                    onClick = { onRoleSelected(choice.role) },
                                    icon = iconForRole(choice.role),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }

            if (uiState.requiresInviteCode) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedAuthField(
                    label = stringResource(R.string.invite_code_label),
                    value = uiState.inviteCode,
                    onValueChange = onInviteCodeChange,
                    placeholder = stringResource(R.string.invite_code_hint),
                )
            }

            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = error, color = BrandOrange, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SelectionNavButtons(
            onBack = onBack,
            onContinue = onContinue,
            continueEnabled = !uiState.isLoading &&
                !uiState.isLoadingRoles &&
                uiState.selectedRole != null,
            continueLabel = if (uiState.isLoading) {
                stringResource(R.string.loading)
            } else {
                stringResource(R.string.continue_label)
            },
        )
    }
}

private fun iconForRole(role: UserRole): ImageVector = when (role) {
    UserRole.Teacher -> Icons.Outlined.EditNote
    UserRole.Student -> Icons.Outlined.School
    UserRole.Admin -> Icons.Outlined.Shield
    UserRole.Parents -> Icons.Outlined.FamilyRestroom
}

@Preview(showBackground = true)
@Composable
private fun SelectRolePreview() {
    LushAIEdu_PLSTheme {
        SelectRoleScreen(
            uiState = SelectRoleUiState(
                isLoadingRoles = false,
                roles = listOf(
                    RoleChoice(UserRole.Teacher, "Teacher", true),
                    RoleChoice(UserRole.Student, "Student", false),
                    RoleChoice(UserRole.Admin, "Admin", true),
                    RoleChoice(UserRole.Parents, "Parents", false),
                ),
            ),
            onRoleSelected = {},
            onInviteCodeChange = {},
            onBack = {},
            onContinue = {},
        )
    }
}
