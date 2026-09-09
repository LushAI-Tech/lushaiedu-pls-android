package com.lushaiedupls.ui.student.fees

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.lushaiedupls.data.mapper.FeeHistoryMappers
import com.lushaiedupls.data.remote.NetworkResult
import com.lushaiedupls.data.remote.dto.FeeHistoryResponse
import com.lushaiedupls.data.remote.userMessage
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.ui.common.AppBackNav
import com.lushaiedupls.ui.common.CenteredEmptyState
import com.lushaiedupls.ui.common.LoadErrorPanel
import com.lushaiedupls.ui.common.LushPullToRefreshBox
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.common.viewModelFactory
import com.lushaiedupls.ui.fees.FeeHistorySections
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudentFeesUiState(
    val history: FeeHistoryResponse? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

class StudentFeesViewModel(
    private val studentRepository: StudentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentFeesUiState(isLoading = true))
    val uiState: StateFlow<StudentFeesUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val hasContent = _uiState.value.history != null
            _uiState.update {
                it.copy(
                    isLoading = !hasContent,
                    isRefreshing = hasContent,
                    errorMessage = null,
                )
            }
            when (val result = studentRepository.myFeeHistory(month = "all")) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        history = FeeHistoryMappers.subjectFeeHistoryOnly(result.data),
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

    companion object {
        fun provideFactory(studentRepository: StudentRepository): ViewModelProvider.Factory =
            viewModelFactory { StudentFeesViewModel(studentRepository) }
    }
}

@Composable
fun StudentFeesRoute(
    studentRepository: StudentRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: StudentFeesViewModel = viewModel(
        factory = StudentFeesViewModel.provideFactory(studentRepository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    StudentFeesScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
fun StudentFeesScreen(
    uiState: StudentFeesUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = uiState.history?.rows.orEmpty()
    when {
        uiState.isLoading && rows.isEmpty() && uiState.errorMessage == null ->
            StudentPageSkeleton(
                kind = StudentSkeletonKind.Home,
                title = stringResource(R.string.parent_fees_title),
                modifier = modifier,
            )
        uiState.errorMessage != null && rows.isEmpty() -> LushPullToRefreshBox(
            isRefreshing = uiState.isLoading || uiState.isRefreshing,
            onRefresh = onRetry,
            modifier = modifier.fillMaxSize(),
        ) {
            LoadErrorPanel(
                screenTitle = stringResource(R.string.parent_fees_title),
                message = uiState.errorMessage.orEmpty(),
                onRetry = onRetry,
                isRetrying = uiState.isLoading || uiState.isRefreshing,
                modifier = Modifier.fillMaxSize(),
            )
        }
        else -> LushPullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRetry,
            modifier = modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgWhite)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 24.dp),
            ) {
                AppBackNav(onBack = onBack)
                Text(
                    text = stringResource(R.string.parent_fees_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (rows.isEmpty()) {
                    CenteredEmptyState(
                        message = stringResource(R.string.parent_fees_empty),
                        icon = Icons.Outlined.Payments,
                    )
                } else {
                    uiState.history?.let { FeeHistorySections(history = it) }
                }
            }
        }
    }
}
