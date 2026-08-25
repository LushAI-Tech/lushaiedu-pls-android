package com.lushaiedupls.ui.admin.invites

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.InviteCodeCreate
import com.lushaiedupls.data.remote.dto.InviteCodeOut
import com.lushaiedupls.data.remote.dto.UserRole
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.admin.AdminCard
import com.lushaiedupls.ui.admin.AdminDeleteRed
import com.lushaiedupls.ui.admin.AdminEmptyText
import com.lushaiedupls.ui.admin.AdminFilterRow
import com.lushaiedupls.ui.admin.AdminMuted
import com.lushaiedupls.ui.admin.AdminScreenHeader
import com.lushaiedupls.ui.admin.formatIsoDate
import com.lushaiedupls.ui.admin.label
import com.lushaiedupls.ui.auth.components.OutlinedAuthField
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.common.viewModelFactory
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandBlack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminInvitesUiState(
    val items: List<InviteCodeOut> = emptyList(),
    val composing: Boolean = false,
    val role: UserRole = UserRole.TEACHER,
    val note: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class AdminInvitesViewModel(
    private val adminRepository: AdminRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminInvitesUiState(isLoading = true))
    val uiState: StateFlow<AdminInvitesUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = adminRepository.listInvites()) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isLoading = false, items = result.data)
                }
                else -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    fun startCreate() = _uiState.update { it.copy(composing = true, note = "", role = UserRole.TEACHER) }
    fun cancel() = _uiState.update { it.copy(composing = false) }
    fun onNote(value: String) = _uiState.update { it.copy(note = value) }
    fun onRole(role: UserRole) = _uiState.update { it.copy(role = role) }

    fun create() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            when (
                val result = adminRepository.createInvite(
                    InviteCodeCreate(
                        role = _uiState.value.role,
                        note = _uiState.value.note.trim().ifBlank { null },
                    ),
                )
            ) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, composing = false) }
                    refresh()
                }
                else -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    fun revoke(id: String) {
        viewModelScope.launch {
            when (val result = adminRepository.revokeInvite(id)) {
                is NetworkResult.Success -> refresh()
                else -> _uiState.update { it.copy(errorMessage = result.userMessage()) }
            }
        }
    }

    companion object {
        fun provideFactory(adminRepository: AdminRepository): ViewModelProvider.Factory =
            viewModelFactory { AdminInvitesViewModel(adminRepository) }
    }
}

@Composable
fun AdminInvitesRoute(
    adminRepository: AdminRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: AdminInvitesViewModel = viewModel(
        factory = AdminInvitesViewModel.provideFactory(adminRepository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(Unit) {
        if (!uiState.composing) viewModel.refresh()
        onPauseOrDispose { }
    }
    AdminInvitesScreen(
        uiState = uiState,
        onBack = { if (uiState.composing) viewModel.cancel() else onBack() },
        onStartCreate = viewModel::startCreate,
        onNote = viewModel::onNote,
        onRole = viewModel::onRole,
        onCreate = viewModel::create,
        onRevoke = viewModel::revoke,
        onCopy = { code -> copyInvite(context, code) },
        onRetry = viewModel::refresh,
        modifier = modifier,
    )
}

private fun copyInvite(context: Context, code: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Invite code", code))
    Toast.makeText(context, context.getString(R.string.admin_invites_copied), Toast.LENGTH_SHORT).show()
}

@Composable
fun AdminInvitesScreen(
    uiState: AdminInvitesUiState,
    onBack: () -> Unit,
    onStartCreate: () -> Unit,
    onNote: (String) -> Unit,
    onRole: (UserRole) -> Unit,
    onCreate: () -> Unit,
    onRevoke: (String) -> Unit,
    onCopy: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading && uiState.items.isEmpty() && uiState.errorMessage == null ->
            StudentPageSkeleton(kind = StudentSkeletonKind.List, modifier = modifier)
        uiState.errorMessage != null && uiState.items.isEmpty() -> LoadErrorPanel(
            screenTitle = stringResource(R.string.admin_invites_title),
            message = uiState.errorMessage.orEmpty(),
            onRetry = onRetry,
            isRetrying = uiState.isLoading,
            modifier = modifier,
        )
        else -> Box(
            modifier = modifier
                .fillMaxSize()
                .background(BgWhite)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = if (uiState.composing) 24.dp else 88.dp),
            ) {
                AdminScreenHeader(
                    title = stringResource(R.string.admin_invites_title),
                    onBack = onBack,
                )
                if (uiState.composing) {
                    AdminFilterRow(
                        labels = listOf(
                            stringResource(R.string.role_teacher),
                            stringResource(R.string.role_admin),
                        ),
                        selectedIndex = if (uiState.role == UserRole.TEACHER) 0 else 1,
                        onSelect = { onRole(if (it == 0) UserRole.TEACHER else UserRole.ADMIN) },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedAuthField(
                        label = stringResource(R.string.admin_invites_note),
                        value = uiState.note,
                        onValueChange = onNote,
                        placeholder = stringResource(R.string.admin_invites_note_hint),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PrimaryButton(
                        text = stringResource(R.string.admin_invites_create),
                        onClick = onCreate,
                        enabled = !uiState.isSaving,
                        fullyRounded = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    if (uiState.items.isEmpty()) {
                        AdminEmptyText(stringResource(R.string.admin_invites_empty))
                    }
                    uiState.items.forEach { item ->
                        val status = when {
                            item.revoked_at != null -> stringResource(R.string.admin_invites_revoked)
                            item.used_at != null -> stringResource(R.string.admin_invites_used)
                            item.is_redeemable -> stringResource(R.string.admin_invites_ready)
                            else -> formatIsoDate(item.expires_at)
                        }
                        AdminCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.code,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = BrandBlack,
                                        fontFamily = FontFamily.SansSerif,
                                    )
                                    AdminMuted(
                                        listOfNotNull(
                                            item.role.label(),
                                            status,
                                            item.note,
                                        ).joinToString(" · "),
                                    )
                                }
                                IconButton(
                                    onClick = { onCopy(item.code) },
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = stringResource(
                                            R.string.admin_invites_copy,
                                        ),
                                        tint = BrandBlack,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                                if (item.is_redeemable) {
                                    IconButton(
                                        onClick = { onRevoke(item.id) },
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = stringResource(
                                                R.string.admin_invites_delete,
                                            ),
                                            tint = AdminDeleteRed,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
            if (!uiState.composing) {
                FloatingActionButton(
                    onClick = onStartCreate,
                    shape = CircleShape,
                    containerColor = BrandBlack,
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.admin_invites_new),
                    )
                }
            }
        }
    }
}
