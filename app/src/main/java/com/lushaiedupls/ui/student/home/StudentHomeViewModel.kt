package com.lushaiedupls.ui.student.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.mapper.StudentUiMappers
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.needsAdminApproval
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.AuthRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.common.reloadUiFlags
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudentHomeViewModel(
    private val userSessionStore: UserSessionStore,
    private val studentRepository: StudentRepository,
    private val authRepository: AuthRepository? = null,
) : ViewModel() {

    private val cachedOverview = studentRepository.getCachedOverview()
    private val cachedProgress = studentRepository.getCachedProgressOverview()

    private val _uiState = MutableStateFlow(
        StudentHomeUiState(
            displayName = cachedOverview?.student?.name?.ifBlank { userSessionStore.getDisplayName() }
                ?: userSessionStore.getDisplayName(),
            avatarUrl = resolveAvatarUrl(cachedOverview?.student?.avatar_url),
            avatarCacheKey = userSessionStore.getAvatarRevision(),
            isLoading = cachedOverview == null,
            notificationCount = cachedOverview?.unread_notifications ?: 0,
            overviewMetrics = cachedOverview
                ?.let { StudentUiMappers.overviewMetrics(it, cachedProgress) }
                .orEmpty(),
            sessionSummary = cachedOverview?.let(StudentUiMappers::sessionSummary),
            attendancePreview = cachedOverview?.let(StudentUiMappers::attendancePreview).orEmpty(),
        ),
    )
    val uiState: StateFlow<StudentHomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            studentRepository.unreadNotificationCount.collect { count ->
                if (count != null) {
                    _uiState.update { it.copy(notificationCount = count) }
                }
            }
        }
        refresh()
        prefetchAiLearn()
    }

    private fun prefetchAiLearn() {
        viewModelScope.launch(Dispatchers.IO) {
            studentRepository.prefetchAiLearn()
        }
    }

    fun refresh(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val hasContent = _uiState.value.overviewMetrics.isNotEmpty()
            val (loading, refreshing) = reloadUiFlags(_uiState.value.isLoading, hasContent)
            _uiState.update {
                it.copy(
                    isLoading = loading,
                    isRefreshing = refreshing,
                    errorMessage = null,
                )
            }
            coroutineScope {
                val overviewDeferred = async {
                    studentRepository.overview(forceRefresh = forceRefresh || refreshing)
                }
                val progressDeferred = async {
                    studentRepository.progressOverview(forceRefresh = forceRefresh || refreshing)
                }
                val result = overviewDeferred.await()
                val progress = (progressDeferred.await() as? NetworkResult.Success)?.data
                when (result) {
                    is NetworkResult.Success -> {
                        val overview = result.data
                        val overviewAvatar = resolveAvatarUrl(overview.student.avatar_url)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                needsApproval = false,
                                errorMessage = null,
                                displayName = overview.student.name.ifBlank {
                                    userSessionStore.getDisplayName()
                                },
                                avatarUrl = overviewAvatar,
                                avatarCacheKey = userSessionStore.getAvatarRevision(),
                                notificationCount = overview.unread_notifications,
                                overviewMetrics = StudentUiMappers.overviewMetrics(overview, progress),
                                sessionSummary = StudentUiMappers.sessionSummary(overview),
                                attendancePreview = StudentUiMappers.attendancePreview(overview),
                            )
                        }
                        if (overviewAvatar == null) {
                            val fromMe = fetchAvatarFromMe()
                            if (!fromMe.isNullOrBlank()) {
                                _uiState.update {
                                    it.copy(
                                        avatarUrl = fromMe,
                                        avatarCacheKey = userSessionStore.getAvatarRevision(),
                                    )
                                }
                            }
                        }
                    }
                    else -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            needsApproval = result.needsAdminApproval(),
                            errorMessage = if (_uiState.value.overviewMetrics.isEmpty()) result.userMessage() else null,
                        )
                    }
                }
            }
        }
    }

    private fun resolveAvatarUrl(remote: String?): String? {
        val sessionUrl = AuthRepository.normalizeAvatarUrl(userSessionStore.getAvatarUrl())
        val remoteUrl = AuthRepository.normalizeAvatarUrl(remote)
        // Prefer session when they diverge so a fresh upload isn't overwritten by a stale overview.
        if (sessionUrl != null && remoteUrl != null && sessionUrl != remoteUrl) {
            return sessionUrl
        }
        val url = remoteUrl ?: sessionUrl
        url?.let { userSessionStore.setAvatarUrl(it) }
        return url
    }

    private suspend fun fetchAvatarFromMe(): String? {
        val repo = authRepository ?: return userSessionStore.getAvatarUrl()
        return when (val result = repo.me()) {
            is NetworkResult.Success -> resolveAvatarUrl(result.data.avatar_url)
            else -> userSessionStore.getAvatarUrl()
        }
    }

    companion object {
        fun provideFactory(
            userSessionStore: UserSessionStore,
            studentRepository: StudentRepository,
            authRepository: AuthRepository? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            StudentHomeViewModel(userSessionStore, studentRepository, authRepository)
        }
    }
}
