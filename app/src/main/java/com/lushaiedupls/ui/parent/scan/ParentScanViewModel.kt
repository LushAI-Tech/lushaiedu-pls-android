package com.lushaiedupls.ui.parent.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.ParentRelationship
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.ParentRepository
import com.lushaiedupls.ui.common.extractParentLinkToken
import com.lushaiedupls.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ParentScanViewModel(
    private val parentRepository: ParentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParentScanUiState())
    val uiState: StateFlow<ParentScanUiState> = _uiState.asStateFlow()

    fun onTokenChange(value: String) {
        _uiState.update { it.copy(token = value, errorMessage = null, successMessage = null) }
    }

    fun onRelationshipSelected(relationship: ParentRelationship) {
        _uiState.update { it.copy(relationship = relationship) }
    }

    fun onScanned(raw: String) {
        onTokenChange(extractParentLinkToken(raw))
        redeem()
    }

    fun consumeSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun redeem() {
        val token = extractParentLinkToken(_uiState.value.token)
        if (token.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Scan the student's QR, or paste the code.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
            when (
                val result = parentRepository.redeemLink(token, _uiState.value.relationship)
            ) {
                is NetworkResult.Success -> {
                    val name = result.data.student.name
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            token = "",
                            successMessage = "Linked to $name",
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = result.userMessage())
                }
            }
        }
    }

    companion object {
        fun provideFactory(parentRepository: ParentRepository): ViewModelProvider.Factory =
            viewModelFactory { ParentScanViewModel(parentRepository) }
    }
}
