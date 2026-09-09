package com.lushaiedupls.ui.admin.institutions

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.lushaiedupls.data.remote.dto.InstitutionCreate
import com.lushaiedupls.data.remote.dto.InstitutionOut
import com.lushaiedupls.data.remote.dto.InstitutionUpdate
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.admin.AdminCard
import com.lushaiedupls.ui.admin.AdminDeleteConfirmDialog
import com.lushaiedupls.ui.admin.AdminDeleteRed
import com.lushaiedupls.ui.admin.AdminEmptyText
import com.lushaiedupls.ui.admin.AdminLeadingIcon
import com.lushaiedupls.ui.admin.AdminMuted
import com.lushaiedupls.ui.admin.AdminNoticeDialog
import com.lushaiedupls.ui.admin.AdminScreenHeader
import com.lushaiedupls.ui.auth.components.OutlinedAuthField
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.LushPullToRefreshBox
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.common.viewModelFactory
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminInstitutionsUiState(
    val items: List<InstitutionOut> = emptyList(),
    val composing: Boolean = false,
    val editingId: String? = null,
    val name: String = "",
    val sortOrder: String = "0",
    val isActive: Boolean = true,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class AdminInstitutionsViewModel(
    private val adminRepository: AdminRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminInstitutionsUiState(isLoading = true))
    val uiState: StateFlow<AdminInstitutionsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val hasContent = _uiState.value.items.isNotEmpty()
            _uiState.update {
                it.copy(
                    isLoading = !hasContent,
                    isRefreshing = hasContent,
                    errorMessage = null,
                )
            }
            when (val result = adminRepository.listInstitutions(includeInactive = true)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        items = result.data.sortedWith(compareBy({ it.sort_order }, { it.name })),
                    )
                }
                else -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = result.userMessage(),
                    )
                }
            }
        }
    }

    fun startCreate() = _uiState.update {
        it.copy(
            composing = true,
            editingId = null,
            name = "",
            sortOrder = ((it.items.maxOfOrNull { item -> item.sort_order } ?: -1) + 1)
                .coerceAtLeast(0)
                .toString(),
            isActive = true,
            errorMessage = null,
        )
    }

    fun startEdit(item: InstitutionOut) = _uiState.update {
        it.copy(
            composing = true,
            editingId = item.id,
            name = item.name,
            sortOrder = item.sort_order.toString(),
            isActive = item.is_active,
            errorMessage = null,
        )
    }

    fun cancel() = _uiState.update { it.copy(composing = false, editingId = null) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun onName(value: String) = _uiState.update { it.copy(name = value) }
    fun onSortOrder(value: String) =
        _uiState.update { it.copy(sortOrder = value.filter { ch -> ch.isDigit() }.take(4)) }
    fun toggleActive() = _uiState.update { it.copy(isActive = !it.isActive) }

    fun save() {
        val state = _uiState.value
        val name = state.name.trim()
        if (name.isBlank()) return
        val sortOrder = state.sortOrder.toIntOrNull() ?: 0
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = if (state.editingId == null) {
                adminRepository.createInstitution(
                    InstitutionCreate(
                        name = name,
                        sort_order = sortOrder,
                        is_active = true,
                    ),
                )
            } else {
                adminRepository.updateInstitution(
                    state.editingId,
                    InstitutionUpdate(
                        name = name,
                        sort_order = sortOrder,
                        is_active = state.isActive,
                    ),
                )
            }
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, composing = false, editingId = null) }
                    refresh()
                }
                else -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            when (val result = adminRepository.deleteInstitution(id)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(isSaving = false, composing = false, editingId = null)
                    }
                    refresh()
                }
                else -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    companion object {
        fun provideFactory(adminRepository: AdminRepository): ViewModelProvider.Factory =
            viewModelFactory { AdminInstitutionsViewModel(adminRepository) }
    }
}

@Composable
fun AdminInstitutionsRoute(
    adminRepository: AdminRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: AdminInstitutionsViewModel = viewModel(
        factory = AdminInstitutionsViewModel.provideFactory(adminRepository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        if (!uiState.composing) viewModel.refresh()
        onPauseOrDispose { }
    }
    BackHandler(enabled = uiState.composing) { viewModel.cancel() }
    AdminInstitutionsScreen(
        uiState = uiState,
        onBack = { if (uiState.composing) viewModel.cancel() else onBack() },
        onStartCreate = viewModel::startCreate,
        onStartEdit = viewModel::startEdit,
        onName = viewModel::onName,
        onSortOrder = viewModel::onSortOrder,
        onToggleActive = viewModel::toggleActive,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        onRetry = viewModel::refresh,
        onDismissError = viewModel::clearError,
        modifier = modifier,
    )
}

@Composable
fun AdminInstitutionsScreen(
    uiState: AdminInstitutionsUiState,
    onBack: () -> Unit,
    onStartCreate: () -> Unit,
    onStartEdit: (InstitutionOut) -> Unit,
    onName: (String) -> Unit,
    onSortOrder: (String) -> Unit,
    onToggleActive: () -> Unit,
    onSave: () -> Unit,
    onDelete: (String) -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading && uiState.items.isEmpty() && uiState.errorMessage == null ->
            StudentPageSkeleton(kind = StudentSkeletonKind.List, modifier = modifier)
        uiState.errorMessage != null && uiState.items.isEmpty() && !uiState.composing -> LoadErrorPanel(
            screenTitle = stringResource(R.string.admin_institutions_title),
            message = uiState.errorMessage.orEmpty(),
            onRetry = onRetry,
            isRetrying = uiState.isLoading || uiState.isRefreshing,
            modifier = modifier,
        )
        else -> {
            var managing by rememberSaveable { mutableStateOf(false) }
            var pendingDelete by remember { mutableStateOf<InstitutionOut?>(null) }
            LushPullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRetry,
                modifier = modifier.fillMaxSize(),
            ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BgWhite)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp),
                ) {
                    AdminScreenHeader(
                        title = when {
                            uiState.composing && uiState.editingId == null ->
                                stringResource(R.string.admin_add_institution)
                            uiState.composing -> stringResource(R.string.admin_edit_institution)
                            else -> stringResource(R.string.admin_institutions_title)
                        },
                        onBack = onBack,
                        actions = if (!uiState.composing) {
                            {
                                IconButton(
                                    onClick = { managing = !managing },
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = stringResource(R.string.admin_manage_institutions),
                                        tint = if (managing) BrandOrange else BrandBlack,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                    )
                    if (uiState.composing) {
                        OutlinedAuthField(
                            label = stringResource(R.string.admin_institution_name),
                            value = uiState.name,
                            onValueChange = onName,
                            placeholder = stringResource(R.string.admin_institution_name_hint),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedAuthField(
                            label = stringResource(R.string.admin_sort_order),
                            value = uiState.sortOrder,
                            onValueChange = onSortOrder,
                            placeholder = stringResource(R.string.admin_sort_order_hint),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        if (uiState.editingId != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.admin_active),
                                    modifier = Modifier.weight(1f),
                                    fontFamily = FontFamily.SansSerif,
                                    color = BrandBlack,
                                )
                                Switch(
                                    checked = uiState.isActive,
                                    onCheckedChange = { onToggleActive() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = BgWhite,
                                        checkedTrackColor = BrandOrange,
                                    ),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        PrimaryButton(
                            text = stringResource(R.string.admin_save_institution),
                            onClick = onSave,
                            enabled = uiState.name.isNotBlank() && !uiState.isSaving,
                            fullyRounded = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        AdminMuted(stringResource(R.string.admin_institutions_hint))
                        Spacer(modifier = Modifier.height(16.dp))
                        if (uiState.items.isEmpty()) {
                        AdminEmptyText(
                            text = stringResource(R.string.admin_institutions_empty),
                            icon = Icons.Outlined.Apartment,
                        )
                        }
                        uiState.items.forEach { item ->
                            AdminCard(onClick = if (managing) ({ onStartEdit(item) }) else null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AdminLeadingIcon(icon = Icons.Outlined.Apartment)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = BrandBlack,
                                            fontFamily = FontFamily.SansSerif,
                                        )
                                        AdminMuted(
                                            if (item.is_active) {
                                                stringResource(R.string.admin_active)
                                            } else {
                                                stringResource(R.string.admin_inactive)
                                            },
                                        )
                                    }
                                    if (managing) {
                                        IconButton(
                                            onClick = { onStartEdit(item) },
                                            modifier = Modifier.size(40.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Edit,
                                                contentDescription = stringResource(R.string.admin_edit_institution),
                                                tint = BrandBlack,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                        IconButton(
                                            onClick = { pendingDelete = item },
                                            enabled = !uiState.isSaving,
                                            modifier = Modifier.size(40.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.DeleteOutline,
                                                contentDescription = stringResource(R.string.admin_delete_institution),
                                                tint = AdminDeleteRed,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        PrimaryButton(
                            text = stringResource(R.string.admin_add_institution),
                            onClick = onStartCreate,
                            trailingArrow = true,
                            fullyRounded = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                uiState.errorMessage?.let { message ->
                    AdminNoticeDialog(
                        message = message,
                        onDismiss = onDismissError,
                    )
                }
                pendingDelete?.let { item ->
                    AdminDeleteConfirmDialog(
                        title = stringResource(R.string.admin_delete_confirm_title),
                        message = stringResource(
                            R.string.admin_delete_confirm_named,
                            item.name,
                        ) + "\n\n" + stringResource(R.string.admin_delete_confirm_institution),
                        onConfirm = {
                            val id = item.id
                            pendingDelete = null
                            onDelete(id)
                        },
                        onDismiss = { pendingDelete = null },
                        isWorking = uiState.isSaving,
                    )
                }
            }
            }
        }
    }
}
