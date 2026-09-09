package com.lushaiedupls.ui.admin.notifications

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.mapper.StudentUiMappers
import com.lushaiedupls.data.mock.AppNotification
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AdminRepository
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.LushPullToRefreshBox
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.common.viewModelFactory
import com.lushaiedupls.ui.student.secondary.NotificationsScreen
import com.lushaiedupls.ui.student.secondary.NotificationsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminNotificationsViewModel(
    private val adminRepository: AdminRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationsUiState(isLoading = true))
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val hasContent = _uiState.value.notifications.isNotEmpty()
            _uiState.update {
                it.copy(
                    isLoading = !hasContent,
                    isRefreshing = hasContent,
                    errorMessage = null,
                )
            }
            when (val result = adminRepository.notifications()) {
                is NetworkResult.Success -> {
                    val list = StudentUiMappers.notifications(result.data)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            notifications = list,
                        )
                    }
                    adminRepository.setUnreadNotificationCount(list.count { it.unread })
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

    fun markAllRead() {
        viewModelScope.launch {
            val unread = _uiState.value.notifications.filter { it.unread }
            _uiState.update { state ->
                state.copy(notifications = state.notifications.map { it.copy(unread = false) })
            }
            adminRepository.setUnreadNotificationCount(0)
            unread.forEach { adminRepository.markNotificationRead(it.id) }
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch {
            val current = _uiState.value.notifications.find { it.id == id } ?: return@launch
            if (!current.unread) return@launch
            _uiState.update { state ->
                state.copy(
                    notifications = state.notifications.map { n ->
                        if (n.id == id) n.copy(unread = false) else n
                    },
                )
            }
            adminRepository.markNotificationRead(id)
        }
    }

    companion object {
        fun provideFactory(adminRepository: AdminRepository): ViewModelProvider.Factory =
            viewModelFactory { AdminNotificationsViewModel(adminRepository) }
    }
}

@Composable
fun AdminNotificationsRoute(
    adminRepository: AdminRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: AdminNotificationsViewModel = viewModel(
        factory = AdminNotificationsViewModel.provideFactory(adminRepository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    when {
        state.isLoading && state.notifications.isEmpty() && state.errorMessage == null ->
            StudentPageSkeleton(
                kind = StudentSkeletonKind.Notifications,
                title = stringResource(R.string.parent_notifications_title),
                modifier = modifier,
            )
        state.errorMessage != null && state.notifications.isEmpty() -> LoadErrorPanel(
            screenTitle = stringResource(R.string.parent_notifications_title),
            message = state.errorMessage.orEmpty(),
            onRetry = viewModel::refresh,
            isRetrying = state.isLoading || state.isRefreshing,
            modifier = modifier,
        )
        else -> LushPullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = modifier.fillMaxSize(),
        ) {
            NotificationsScreen(
                notifications = state.notifications,
                onBack = onBack,
                onMarkAllRead = viewModel::markAllRead,
                onOpenNotification = { item: AppNotification -> viewModel.markRead(item.id) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
