package com.lushaiedupls.ui.admin.users

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.ParentRelationship
import com.lushaiedupls.data.remote.dto.UserOut
import com.lushaiedupls.data.remote.dto.UserRole
import com.lushaiedupls.data.remote.dto.UserStatus
import com.lushaiedupls.data.remote.parentLinkAssignUserMessage
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.admin.AdminCard
import com.lushaiedupls.ui.admin.AdminEmptyText
import com.lushaiedupls.ui.admin.AdminFilterRow
import com.lushaiedupls.ui.admin.AdminMuted
import com.lushaiedupls.ui.admin.AdminScreenHeader
import com.lushaiedupls.ui.admin.label
import com.lushaiedupls.ui.auth.components.OutlinedAuthField
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.common.viewModelFactory
import com.lushaiedupls.ui.parent.home.label
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.TextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminParentLinkUiState(
    val anchor: UserOut,
    val counterpartRole: UserRole,
    val candidates: List<UserOut> = emptyList(),
    val query: String = "",
    val selectedCounterpartId: String? = null,
    val relationship: ParentRelationship = ParentRelationship.GUARDIAN,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val selectedCounterpart: UserOut?
        get() = candidates.find { it.id == selectedCounterpartId }

    val studentUser: UserOut?
        get() = when {
            anchor.role == UserRole.STUDENT -> anchor
            else -> selectedCounterpart
        }

    val parentUser: UserOut?
        get() = when {
            anchor.role == UserRole.PARENT -> anchor
            else -> selectedCounterpart
        }

    val studentNeedsApproval: Boolean
        get() = studentUser?.status != null && studentUser?.status != UserStatus.ACTIVE

    val canAssign: Boolean
        get() = selectedCounterpartId != null &&
            !studentNeedsApproval &&
            !isSaving &&
            !isLoading
}

class AdminParentLinkViewModel(
    private val adminRepository: AdminRepository,
    anchor: UserOut,
) : ViewModel() {
    private val counterpartRole =
        if (anchor.role == UserRole.PARENT) UserRole.STUDENT else UserRole.PARENT

    private val _uiState = MutableStateFlow(
        AdminParentLinkUiState(
            anchor = anchor,
            counterpartRole = counterpartRole,
            isLoading = true,
        ),
    )
    val uiState: StateFlow<AdminParentLinkUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        refresh()
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            refresh()
        }
    }

    fun selectCounterpart(userId: String) {
        val user = _uiState.value.candidates.find { it.id == userId } ?: return
        if (user.role == UserRole.STUDENT && user.status != UserStatus.ACTIVE) return
        _uiState.update {
            it.copy(
                selectedCounterpartId = userId,
                errorMessage = null,
            )
        }
    }

    fun selectRelationship(index: Int) {
        val relationship = ParentRelationship.entries.getOrNull(index) ?: return
        _uiState.update { it.copy(relationship = relationship) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val query = _uiState.value.query.trim().takeIf { it.isNotEmpty() }
            when (
                val result = adminRepository.listUsers(
                    role = counterpartRole,
                    query = query,
                    page = 1,
                    limit = 50,
                )
            ) {
                is NetworkResult.Success -> {
                    val items = result.data.items
                        .filter { it.id != _uiState.value.anchor.id }
                        .filter { it.status != UserStatus.DELETED }
                    val selectedStillValid = items.any { it.id == _uiState.value.selectedCounterpartId }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            candidates = items,
                            selectedCounterpartId = it.selectedCounterpartId.takeIf { selectedStillValid },
                            errorMessage = null,
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        candidates = emptyList(),
                        errorMessage = result.userMessage(),
                    )
                }
            }
        }
    }

    fun assign() {
        val state = _uiState.value
        val parent = state.parentUser ?: return
        val student = state.studentUser ?: return
        if (student.status != UserStatus.ACTIVE) {
            _uiState.update {
                it.copy(errorMessage = "Approve student first")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            when (
                val result = adminRepository.createParentLink(
                    parentUserId = parent.id,
                    studentUserId = student.id,
                    relationship = state.relationship,
                )
            ) {
                is NetworkResult.Success -> {
                    val link = result.data
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            successMessage = "${link.parent.name} ↔ ${link.student.name} (${link.relationship.label()})",
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = result.parentLinkAssignUserMessage(),
                    )
                }
            }
        }
    }

    fun consumeSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    companion object {
        fun provideFactory(
            adminRepository: AdminRepository,
            anchor: UserOut,
        ): ViewModelProvider.Factory = viewModelFactory {
            AdminParentLinkViewModel(adminRepository, anchor)
        }
    }
}

@Composable
fun AdminParentLinkRoute(
    adminRepository: AdminRepository,
    anchor: UserOut,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: AdminParentLinkViewModel = viewModel(
        key = "admin-parent-link-${anchor.id}",
        factory = AdminParentLinkViewModel.provideFactory(adminRepository, anchor),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    BackHandler(onBack = onBack)
    LaunchedEffect(uiState.successMessage) {
        val message = uiState.successMessage ?: return@LaunchedEffect
        Toast.makeText(
            context,
            context.getString(
                R.string.admin_parent_link_success,
                uiState.parentUser?.name.orEmpty(),
                uiState.studentUser?.name.orEmpty(),
                uiState.relationship.label(),
            ).ifBlank { message },
            Toast.LENGTH_SHORT,
        ).show()
        viewModel.consumeSuccess()
        onSaved()
    }
    AdminParentLinkScreen(
        uiState = uiState,
        onBack = onBack,
        onQuery = viewModel::onQueryChange,
        onSelectCounterpart = viewModel::selectCounterpart,
        onSelectRelationship = viewModel::selectRelationship,
        onAssign = viewModel::assign,
        modifier = modifier,
    )
}

@Composable
fun AdminParentLinkScreen(
    uiState: AdminParentLinkUiState,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onSelectCounterpart: (String) -> Unit,
    onSelectRelationship: (Int) -> Unit,
    onAssign: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pickingStudents = uiState.counterpartRole == UserRole.STUDENT
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .imePadding()
            .padding(horizontal = 20.dp),
    ) {
        AdminScreenHeader(
            title = stringResource(R.string.admin_parent_link_title),
            onBack = onBack,
        )
        AdminMuted(
            if (pickingStudents) {
                stringResource(R.string.admin_parent_link_subtitle_for_parent, uiState.anchor.name)
            } else {
                stringResource(R.string.admin_parent_link_subtitle_for_student, uiState.anchor.name)
            },
        )
        Spacer(modifier = Modifier.height(12.dp))
        AdminCard {
            Text(
                text = uiState.anchor.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
            )
            Spacer(modifier = Modifier.height(4.dp))
            AdminMuted("${uiState.anchor.role.label()} · ${uiState.anchor.status.label()}")
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedAuthField(
            label = "",
            value = uiState.query,
            onValueChange = onQuery,
            placeholder = stringResource(
                if (pickingStudents) {
                    R.string.admin_parent_link_search_students
                } else {
                    R.string.admin_parent_link_search_parents
                },
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.admin_parent_link_relationship),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(8.dp))
        AdminFilterRow(
            labels = ParentRelationship.entries.map { it.label() },
            selectedIndex = ParentRelationship.entries.indexOf(uiState.relationship).coerceAtLeast(0),
            onSelect = onSelectRelationship,
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (uiState.studentNeedsApproval) {
            Text(
                text = stringResource(R.string.admin_parent_link_approve_student_first),
                color = BrandOrange,
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                color = BrandOrange,
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when {
                uiState.isLoading && uiState.candidates.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = BrandBlack)
                }
                uiState.candidates.isEmpty() -> AdminEmptyText(
                    stringResource(R.string.admin_parent_link_empty),
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(uiState.candidates, key = { it.id }) { user ->
                        val selectable = user.role != UserRole.STUDENT ||
                            user.status == UserStatus.ACTIVE
                        val selected = user.id == uiState.selectedCounterpartId
                        AdminCard(
                            highlighted = selected,
                            onClick = if (selectable) {
                                { onSelectCounterpart(user.id) }
                            } else {
                                null
                            },
                        ) {
                            Text(
                                text = user.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = if (selectable) BrandBlack else TextSecondary,
                                fontFamily = FontFamily.SansSerif,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            AdminMuted("${user.role.label()} · ${user.status.label()}")
                            if (!selectable) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.admin_parent_link_approve_student_first),
                                    color = BrandOrange,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.SansSerif,
                                )
                            }
                        }
                    }
                }
            }
        }
        PrimaryButton(
            text = stringResource(
                if (uiState.isSaving) R.string.loading else R.string.admin_parent_link_assign,
            ),
            onClick = onAssign,
            enabled = uiState.canAssign,
            fullyRounded = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        )
    }
}
