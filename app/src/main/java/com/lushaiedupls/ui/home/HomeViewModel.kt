package com.lushaiedupls.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.repository.SessionRepository
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refreshSession()
        observeSessionExpiry()
    }

    fun refreshSession() {
        _uiState.update {
            it.copy(
                isLoading = false,
                isLoggedIn = sessionRepository.isLoggedIn(),
                message = null,
            )
        }
    }

    fun logout() {
        sessionRepository.logout()
        _uiState.update {
            it.copy(isLoggedIn = false, message = null)
        }
    }

    private fun observeSessionExpiry() {
        viewModelScope.launch {
            sessionRepository.sessionExpired.collect {
                _uiState.update {
                    it.copy(isLoggedIn = false, message = "Session expired. Please sign in again.")
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            sessionRepository: SessionRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            HomeViewModel(sessionRepository)
        }
    }
}
