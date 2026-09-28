package com.lushaiedupls.ui.admin.roll

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lushaiedupls.R
import com.lushaiedupls.data.remote.dto.MemberOut
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.admin.AdminEmptyText
import com.lushaiedupls.ui.admin.AdminNoticeDialog
import com.lushaiedupls.ui.admin.AdminScreenHeader
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.LushPullToRefreshBox
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.TextSecondary

private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun AdminRollEditRoute(
    adminRepository: AdminRepository,
    unitId: String,
    className: String,
    subjectName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminRollEditViewModel = viewModel(
        factory = AdminRollEditViewModel.provideFactory(
            adminRepository = adminRepository,
            unitId = unitId,
            className = className,
            subjectName = subjectName,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when {
        uiState.isLoading && uiState.members.isEmpty() && uiState.errorMessage == null ->
            StudentPageSkeleton(kind = StudentSkeletonKind.List, modifier = modifier)
        uiState.errorMessage != null && uiState.members.isEmpty() && !uiState.isLoading ->
            LoadErrorPanel(
                screenTitle = "$className · $subjectName",
                message = uiState.errorMessage.orEmpty(),
                onRetry = viewModel::refresh,
                isRetrying = uiState.isLoading,
                modifier = modifier,
            )
        else -> AdminRollEditScreen(
            uiState = uiState,
            onBack = onBack,
            onRefresh = viewModel::refresh,
            onUpdateDraft = viewModel::updateDraft,
            onToggleAutoFill = viewModel::toggleAutoFill,
            onSave = viewModel::save,
            onClearMessages = viewModel::clearMessages,
            modifier = modifier,
        )
    }
}

@Composable
fun AdminRollEditScreen(
    uiState: AdminRollEditUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onUpdateDraft: (String, String) -> Unit,
    onToggleAutoFill: () -> Unit,
    onSave: () -> Unit,
    onClearMessages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = "${uiState.className} · ${uiState.subjectName}"
    val notice = uiState.errorMessage ?: uiState.successMessage
    val isSuccess = uiState.successMessage != null

    if (notice != null) {
        AdminNoticeDialog(
            title = if (isSuccess) {
                stringResource(R.string.admin_roll_saved_title)
            } else {
                stringResource(R.string.admin_roll_error_title)
            },
            message = notice,
            onDismiss = onClearMessages,
        )
    }

    LushPullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            AdminScreenHeader(title = title, onBack = onBack)
            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.members.isEmpty()) {
                AdminEmptyText(stringResource(R.string.admin_roll_roster_empty))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.members, key = { it.student.id }) { member ->
                        MemberRollRow(
                            member = member,
                            draft = uiState.drafts[member.student.id].orEmpty(),
                            onDraftChange = { onUpdateDraft(member.student.id, it) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PrimaryButton(
                        text = stringResource(
                            if (uiState.autoFilled) {
                                R.string.admin_roll_clear
                            } else {
                                R.string.admin_roll_auto_fill
                            },
                        ),
                        onClick = onToggleAutoFill,
                        enabled = !uiState.isSaving,
                        fullyRounded = true,
                        height = 48.dp,
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryButton(
                        text = stringResource(R.string.admin_roll_save),
                        onClick = onSave,
                        enabled = !uiState.isSaving,
                        fullyRounded = true,
                        height = 48.dp,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun MemberRollRow(
    member: MemberOut,
    draft: String,
    onDraftChange: (String) -> Unit,
) {
    val avatarUrl = member.student.avatar_url?.takeIf { it.isNotBlank() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .border(1.dp, BorderGray.copy(alpha = 0.75f), CardShape)
            .background(BgWhite)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BrandBlack)
                .padding(2.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(Color.White),
                textStyle = TextStyle(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.SansSerif,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = stringResource(R.string.cd_avatar),
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                error = painterResource(R.drawable.ic_avatar_placeholder),
                placeholder = painterResource(R.drawable.ic_avatar_placeholder),
            )
        } else {
            Image(
                painter = painterResource(R.drawable.ic_avatar_placeholder),
                contentDescription = stringResource(R.string.cd_avatar),
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.student.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val email = member.student.email?.takeIf { it.isNotBlank() }
            if (email != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = email,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
