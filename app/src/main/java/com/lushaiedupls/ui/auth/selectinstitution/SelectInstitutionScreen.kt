package com.lushaiedupls.ui.auth.selectinstitution

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.session.UserSessionStore
import com.lushaiedupls.ui.auth.components.LushAiEduBrandHeader
import com.lushaiedupls.ui.auth.components.OnboardingStepHeader
import com.lushaiedupls.ui.auth.components.SelectionNavButtons
import com.lushaiedupls.ui.auth.components.SelectionTile
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.LushAIEdu_PLSTheme

@Composable
fun SelectInstitutionRoute(
    userSessionStore: UserSessionStore,
    studentRepository: StudentRepository,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SelectInstitutionViewModel = viewModel(
        factory = SelectInstitutionViewModel.provideFactory(userSessionStore, studentRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SelectInstitutionScreen(
        uiState = uiState,
        onInstitutionSelected = viewModel::onInstitutionSelected,
        onBack = onBack,
        onContinue = {
            if (viewModel.validateAndSave()) onContinue()
        },
        modifier = modifier,
    )
}

@Composable
fun SelectInstitutionScreen(
    uiState: SelectInstitutionUiState,
    onInstitutionSelected: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LushAiEduBrandHeader(logoSize = 104.dp)
        Spacer(modifier = Modifier.height(28.dp))
        OnboardingStepHeader(
            title = stringResource(R.string.select_institution),
            step = 1,
            totalSteps = 3,
        )
        Spacer(modifier = Modifier.height(24.dp))

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = BrandBlack)
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    uiState.institutions.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            row.forEach { option ->
                                SelectionTile(
                                    label = option.name,
                                    selected = option.id == uiState.selectedInstitutionId,
                                    onClick = { onInstitutionSelected(option.id) },
                                    icon = Icons.Outlined.Apartment,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = error, color = BrandOrange, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        SelectionNavButtons(
            onBack = onBack,
            onContinue = onContinue,
            continueEnabled = !uiState.isLoading && !uiState.selectedInstitutionId.isNullOrBlank(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SelectInstitutionPreview() {
    LushAIEdu_PLSTheme {
        SelectInstitutionScreen(
            uiState = SelectInstitutionUiState(
                institutions = listOf(
                    InstitutionOption("1", "Main Institution"),
                    InstitutionOption("2", "North Campus"),
                ),
                selectedInstitutionId = "1",
            ),
            onInstitutionSelected = {},
            onBack = {},
            onContinue = {},
        )
    }
}
