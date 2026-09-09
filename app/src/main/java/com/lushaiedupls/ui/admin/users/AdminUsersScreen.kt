package com.lushaiedupls.ui.admin.users

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.remote.dto.Gender
import com.lushaiedupls.data.remote.dto.UserOut
import com.lushaiedupls.data.remote.dto.UserRole
import com.lushaiedupls.data.remote.dto.UserStatus
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.admin.AdminActionRow
import com.lushaiedupls.ui.admin.AdminCard
import com.lushaiedupls.ui.admin.AdminDeleteRed
import com.lushaiedupls.ui.admin.AdminEmptyText
import com.lushaiedupls.ui.admin.AdminFilterRow
import com.lushaiedupls.ui.admin.AdminMuted
import com.lushaiedupls.ui.admin.AdminScreenHeader
import com.lushaiedupls.ui.admin.label
import com.lushaiedupls.ui.auth.components.OutlinedAuthField
import com.lushaiedupls.ui.auth.components.PrimaryButton
import com.lushaiedupls.ui.common.FilterRowListLoading
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.LushPullToRefreshBox
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.TextSecondary
import coil.compose.AsyncImage

private val CreateRoles = listOf(UserRole.STUDENT, UserRole.PARENT, UserRole.TEACHER)
private val Genders = listOf(Gender.MALE, Gender.FEMALE, Gender.OTHER)

@Composable
fun AdminUsersRoute(
    adminRepository: AdminRepository,
    modifier: Modifier = Modifier,
    focusUserId: String? = null,
    focusEventId: Long = 0L,
) {
    val viewModel: AdminUsersViewModel = viewModel(
        factory = AdminUsersViewModel.provideFactory(adminRepository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(focusEventId) {
        if (focusEventId == 0L) return@LaunchedEffect
        viewModel.openPendingQueue(focusUserId)
    }
    LifecycleResumeEffect(Unit) {
        val state = viewModel.uiState.value
        // Avoid cancelling the ViewModel's initial load; refresh when idle or after return.
        if (!state.composing && state.createdPassword == null && !state.isLoading) {
            viewModel.refresh()
        }
        onPauseOrDispose { }
    }
    if (uiState.composing || uiState.createdPassword != null) {
        BackHandler { viewModel.cancelCompose() }
    }
    var assignmentTeacherId by rememberSaveable { mutableStateOf<String?>(null) }
    var parentLinkAnchorId by rememberSaveable { mutableStateOf<String?>(null) }
    val assignmentTeacher = uiState.users.find { it.id == assignmentTeacherId }
    val parentLinkAnchor = uiState.users.find { it.id == parentLinkAnchorId }
    when {
        assignmentTeacher != null -> AdminTeacherAssignmentRoute(
            adminRepository = adminRepository,
            teacherId = assignmentTeacher.id,
            teacherName = assignmentTeacher.name,
            onBack = { assignmentTeacherId = null },
            onSaved = {
                assignmentTeacherId = null
                viewModel.refresh()
            },
            modifier = modifier,
        )
        parentLinkAnchor != null -> AdminParentLinkRoute(
            adminRepository = adminRepository,
            anchor = parentLinkAnchor,
            onBack = { parentLinkAnchorId = null },
            onSaved = {
                parentLinkAnchorId = null
                viewModel.refresh()
            },
            modifier = modifier,
        )
        else -> AdminUsersScreen(
            uiState = uiState,
            onFilter = viewModel::setFilter,
            onQuery = viewModel::onQueryChange,
            onLoadMore = viewModel::loadMore,
            onApprove = viewModel::approve,
            onReject = viewModel::reject,
            onSuspend = viewModel::suspendUser,
            onReactivate = viewModel::reactivate,
            onStartCreate = viewModel::startCreate,
            onStartEdit = viewModel::startEdit,
            onCancel = viewModel::cancelCompose,
            onName = viewModel::onName,
            onPhone = viewModel::onPhone,
            onEmail = viewModel::onEmail,
            onAddress = viewModel::onAddress,
            onDob = viewModel::onDob,
            onRole = viewModel::onRole,
            onGender = viewModel::onGender,
            onClassId = viewModel::onClassId,
            onSave = viewModel::saveForm,
            onCopyPassword = { password -> copyPassword(context, password) },
            onRetry = viewModel::refresh,
            onAssignInstitution = { user -> assignmentTeacherId = user.id },
            onParentLink = { user -> parentLinkAnchorId = user.id },
            modifier = modifier,
        )
    }
}

private fun copyPassword(context: Context, password: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Temporary password", password))
    Toast.makeText(context, context.getString(R.string.admin_users_password_copied), Toast.LENGTH_SHORT).show()
}

@Composable
fun AdminUsersScreen(
    uiState: AdminUsersUiState,
    onFilter: (AdminUserFilter) -> Unit,
    onQuery: (String) -> Unit,
    onLoadMore: () -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onSuspend: (String) -> Unit,
    onReactivate: (String) -> Unit,
    onStartCreate: () -> Unit,
    onStartEdit: (UserOut) -> Unit,
    onCancel: () -> Unit,
    onName: (String) -> Unit,
    onPhone: (String) -> Unit,
    onEmail: (String) -> Unit,
    onAddress: (String) -> Unit,
    onDob: (String) -> Unit,
    onRole: (UserRole) -> Unit,
    onGender: (Gender?) -> Unit,
    onClassId: (String?) -> Unit,
    onSave: () -> Unit,
    onCopyPassword: (String) -> Unit,
    onRetry: () -> Unit,
    onAssignInstitution: (UserOut) -> Unit = {},
    onParentLink: (UserOut) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val filters = AdminUserFilter.entries
    when {
        uiState.errorMessage != null && uiState.users.isEmpty() &&
            !uiState.composing && uiState.createdPassword == null -> LoadErrorPanel(
            screenTitle = stringResource(R.string.admin_users_title),
            message = uiState.errorMessage.orEmpty(),
            onRetry = onRetry,
            isRetrying = uiState.isLoading || uiState.isRefreshing,
            modifier = modifier,
        )
        else -> Box(
            modifier = modifier
                .fillMaxSize()
                .background(BgWhite)
                .imePadding(),
        ) {
            val showFab = !uiState.composing && uiState.createdPassword == null
            var managing by rememberSaveable { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
            ) {
                AdminScreenHeader(
                    title = when {
                        uiState.createdPassword != null -> stringResource(R.string.admin_users_created)
                        uiState.composing && uiState.editingId != null -> stringResource(R.string.admin_users_edit)
                        uiState.composing -> stringResource(R.string.admin_users_add)
                        else -> stringResource(R.string.admin_users_title)
                    },
                    onBack = if (uiState.composing || uiState.createdPassword != null) onCancel else null,
                    actions = if (showFab) {
                        {
                            IconButton(
                                onClick = { managing = !managing },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = stringResource(R.string.admin_manage_users),
                                    tint = if (managing) BrandOrange else BrandBlack,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    } else {
                        null
                    },
                )
                when {
                    uiState.createdPassword != null -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp),
                    ) {
                        CreatedPasswordCard(
                            name = uiState.createdName.orEmpty(),
                            password = uiState.createdPassword,
                            onCopy = { onCopyPassword(uiState.createdPassword) },
                            onDone = onCancel,
                        )
                    }
                    uiState.composing -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp),
                    ) {
                        UserForm(
                            uiState = uiState,
                            onName = onName,
                            onPhone = onPhone,
                            onEmail = onEmail,
                            onAddress = onAddress,
                            onDob = onDob,
                            onRole = onRole,
                            onGender = onGender,
                            onClassId = onClassId,
                            onSave = onSave,
                        )
                    }
                    else -> LushPullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = onRetry,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        UserList(
                            uiState = uiState,
                            filters = filters,
                            onFilter = onFilter,
                            onQuery = onQuery,
                            onLoadMore = onLoadMore,
                            onApprove = onApprove,
                            onReject = onReject,
                            onSuspend = onSuspend,
                            onReactivate = onReactivate,
                            onStartEdit = onStartEdit,
                            onAssignInstitution = onAssignInstitution,
                            onParentLink = onParentLink,
                            showFab = showFab,
                            managing = managing,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            if (showFab) {
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
                        contentDescription = stringResource(R.string.admin_users_add),
                    )
                }
            }
        }
    }
}

@Composable
private fun UserList(
    uiState: AdminUsersUiState,
    filters: List<AdminUserFilter>,
    onFilter: (AdminUserFilter) -> Unit,
    onQuery: (String) -> Unit,
    onLoadMore: () -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onSuspend: (String) -> Unit,
    onReactivate: (String) -> Unit,
    onStartEdit: (UserOut) -> Unit,
    onAssignInstitution: (UserOut) -> Unit,
    onParentLink: (UserOut) -> Unit,
    showFab: Boolean,
    managing: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState, onLoadMore) {
        var previousIndex = listState.firstVisibleItemIndex
        var previousOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow {
            Triple(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1) to
                    listState.layoutInfo.totalItemsCount,
            )
        }.collect { (index, offset, lastAndTotal) ->
            val (lastVisible, total) = lastAndTotal
            val scrolledDown = index > previousIndex ||
                (index == previousIndex && offset > previousOffset)
            previousIndex = index
            previousOffset = offset
            if (scrolledDown && total > 0 && lastVisible >= total - 1) {
                onLoadMore()
            }
        }
    }
    LaunchedEffect(uiState.filter) {
        listState.scrollToItem(0)
    }
    LaunchedEffect(uiState.highlightedUserId, uiState.users) {
        val focusId = uiState.highlightedUserId ?: return@LaunchedEffect
        val index = uiState.users.indexOfFirst { it.id == focusId }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedAuthField(
            label = "",
            value = uiState.query,
            onValueChange = onQuery,
            placeholder = stringResource(R.string.admin_users_search),
        )
        Spacer(modifier = Modifier.height(12.dp))
        AdminFilterRow(
            labels = filters.map { it.filterLabel() },
            selectedIndex = filters.indexOf(uiState.filter),
            onSelect = { onFilter(filters[it]) },
        )
        Spacer(modifier = Modifier.height(16.dp))
        uiState.errorMessage?.let { message ->
            Text(text = message, color = TextSecondary, fontSize = 13.sp, fontFamily = FontFamily.SansSerif)
            Spacer(modifier = Modifier.height(8.dp))
        }
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = if (showFab) 88.dp else 24.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (uiState.users.isEmpty()) {
                item {
                    if (uiState.isLoading) {
                        FilterRowListLoading()
                    } else {
                        AdminEmptyText(
                            text = stringResource(R.string.admin_users_empty),
                            icon = Icons.Outlined.People,
                            modifier = Modifier.fillParentMaxSize(),
                            fillMaxSize = true,
                        )
                    }
                }
            } else {
                items(uiState.users, key = { it.id }) { user ->
                    UserCard(
                        user = user,
                        subtitle = if (user.status == UserStatus.PENDING_APPROVAL) {
                            uiState.enrollmentByUserId[user.id]
                        } else {
                            user.email?.takeIf { it.isNotBlank() }
                        },
                        highlighted = user.id == uiState.highlightedUserId,
                        managing = managing,
                        onApprove = onApprove,
                        onReject = onReject,
                        onSuspend = onSuspend,
                        onReactivate = onReactivate,
                        onEdit = onStartEdit,
                        onAssignInstitution = onAssignInstitution,
                        onParentLink = onParentLink,
                    )
                }
            }
            if (uiState.isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = BrandBlack,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserForm(
    uiState: AdminUsersUiState,
    onName: (String) -> Unit,
    onPhone: (String) -> Unit,
    onEmail: (String) -> Unit,
    onAddress: (String) -> Unit,
    onDob: (String) -> Unit,
    onRole: (UserRole) -> Unit,
    onGender: (Gender?) -> Unit,
    onClassId: (String?) -> Unit,
    onSave: () -> Unit,
) {
    uiState.errorMessage?.let { message ->
        Text(text = message, color = TextSecondary, fontSize = 13.sp, fontFamily = FontFamily.SansSerif)
        Spacer(modifier = Modifier.height(8.dp))
    }
    AdminFilterRow(
        labels = CreateRoles.map { it.label() },
        selectedIndex = CreateRoles.indexOf(uiState.role).coerceAtLeast(0),
        onSelect = { onRole(CreateRoles[it]) },
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedAuthField(
        label = stringResource(R.string.full_name),
        value = uiState.name,
        onValueChange = onName,
        placeholder = stringResource(R.string.full_name_placeholder),
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedAuthField(
        label = stringResource(R.string.phone),
        value = uiState.phone,
        onValueChange = onPhone,
        placeholder = stringResource(R.string.phone_placeholder),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedAuthField(
        label = stringResource(R.string.email_address),
        value = uiState.email,
        onValueChange = onEmail,
        placeholder = stringResource(R.string.email_placeholder),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
    )
    if (uiState.editingId == null) {
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedAuthField(
            label = stringResource(R.string.admin_users_dob),
            value = uiState.dob,
            onValueChange = onDob,
            placeholder = stringResource(R.string.admin_users_dob_hint),
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedAuthField(
        label = stringResource(R.string.address),
        value = uiState.address,
        onValueChange = onAddress,
        placeholder = stringResource(R.string.address_placeholder),
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.gender),
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = BrandBlack,
    )
    Spacer(modifier = Modifier.height(6.dp))
    AdminFilterRow(
        labels = Genders.map { it.label() },
        selectedIndex = Genders.indexOf(uiState.gender),
        onSelect = { onGender(Genders[it]) },
    )
    if (uiState.role == UserRole.STUDENT && uiState.classes.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.select_class),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = BrandBlack,
        )
        Spacer(modifier = Modifier.height(6.dp))
        AdminFilterRow(
            labels = uiState.classes.map { it.name },
            selectedIndex = uiState.classes.indexOfFirst { it.id == uiState.classId },
            onSelect = { onClassId(uiState.classes[it].id) },
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    PrimaryButton(
        text = stringResource(
            if (uiState.editingId == null) R.string.admin_users_create else R.string.admin_users_save,
        ),
        onClick = onSave,
        enabled = !uiState.isWorking,
        fullyRounded = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CreatedPasswordCard(
    name: String,
    password: String,
    onCopy: () -> Unit,
    onDone: () -> Unit,
) {
    AdminCard {
        Text(
            text = stringResource(R.string.admin_users_temp_password),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(6.dp))
        AdminMuted(stringResource(R.string.admin_users_temp_password_hint, name))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = password,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        AdminActionRow(
            actions = listOf(stringResource(R.string.admin_users_copy_password) to onCopy),
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    PrimaryButton(
        text = stringResource(R.string.admin_users_done),
        onClick = onDone,
        fullyRounded = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun UserCard(
    user: UserOut,
    subtitle: String?,
    highlighted: Boolean,
    managing: Boolean,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onSuspend: (String) -> Unit,
    onReactivate: (String) -> Unit,
    onEdit: (UserOut) -> Unit,
    onAssignInstitution: (UserOut) -> Unit,
    onParentLink: (UserOut) -> Unit,
) {
    val avatarUrl = user.avatar_url?.takeIf { it.isNotBlank() }
    AdminCard(highlighted = highlighted) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                    text = user.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    AdminMuted(subtitle)
                }
            }
            if (managing) {
                UserEditSuspendIcons(
                    user = user,
                    onEdit = { onEdit(user) },
                    onSuspend = { onSuspend(user.id) },
                    onReactivate = { onReactivate(user.id) },
                )
            }
        }
        if (user.status == UserStatus.PENDING_APPROVAL) {
            AdminActionRow(
                actions = listOf(
                    stringResource(R.string.admin_approve) to { onApprove(user.id) },
                    stringResource(R.string.admin_reject) to { onReject(user.id) },
                ),
                destructiveIndex = 1,
            )
        } else if (user.status == UserStatus.ACTIVE) {
            when (user.role) {
                UserRole.TEACHER -> AdminActionRow(
                    actions = listOf(
                        stringResource(R.string.admin_teacher_assign_action) to {
                            onAssignInstitution(user)
                        },
                    ),
                )
                UserRole.PARENT -> AdminActionRow(
                    actions = listOf(
                        stringResource(R.string.admin_parent_link_action_parent) to {
                            onParentLink(user)
                        },
                    ),
                )
                UserRole.STUDENT -> AdminActionRow(
                    actions = listOf(
                        stringResource(R.string.admin_parent_link_action_student) to {
                            onParentLink(user)
                        },
                    ),
                )
                UserRole.ADMIN -> Unit
            }
        }
    }
}

@Composable
private fun UserEditSuspendIcons(
    user: UserOut,
    onEdit: () -> Unit,
    onSuspend: () -> Unit,
    onReactivate: () -> Unit,
) {
    if (user.status != UserStatus.DELETED && user.role != UserRole.ADMIN) {
        IconButton(
            onClick = onEdit,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.admin_users_edit),
                tint = BrandBlack,
                modifier = Modifier.size(20.dp),
            )
        }
    }
    when (user.status) {
        UserStatus.ACTIVE -> IconButton(
            onClick = onSuspend,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Block,
                contentDescription = stringResource(R.string.admin_suspend),
                tint = AdminDeleteRed,
                modifier = Modifier.size(22.dp),
            )
        }
        UserStatus.SUSPENDED -> IconButton(
            onClick = onReactivate,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Replay,
                contentDescription = stringResource(R.string.admin_reactivate),
                tint = BrandBlack,
                modifier = Modifier.size(22.dp),
            )
        }
        else -> Unit
    }
}

@Composable
private fun AdminUserFilter.filterLabel(): String = when (this) {
    AdminUserFilter.Pending -> stringResource(R.string.admin_filter_pending)
    AdminUserFilter.Students -> stringResource(R.string.admin_stat_students)
    AdminUserFilter.Teachers -> stringResource(R.string.admin_stat_teachers)
    AdminUserFilter.Parents -> stringResource(R.string.admin_stat_parents)
    AdminUserFilter.Admins -> stringResource(R.string.role_admin)
    AdminUserFilter.All -> stringResource(R.string.admin_filter_all)
}
