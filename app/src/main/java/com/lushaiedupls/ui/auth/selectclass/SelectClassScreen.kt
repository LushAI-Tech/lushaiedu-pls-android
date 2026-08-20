package com.lushaiedupls.ui.auth.selectclass

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
import androidx.compose.material.icons.outlined.School
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
import com.lushaiedupls.ui.auth.components.SelectionNavButtons
import com.lushaiedupls.ui.auth.components.SelectionTile
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.LushAIEdu_PLSTheme
import com.lushaiedupls.ui.theme.TextSecondary

@Composable
fun SelectClassRoute(
    userSessionStore: UserSessionStore,
    studentRepository: StudentRepository,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SelectClassViewModel = viewModel(
        factory = SelectClassViewModel.provideFactory(userSessionStore, studentRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SelectClassScreen(
        uiState = uiState,
        onClassSelected = viewModel::onClassSelected,
        onBack = onBack,
        onContinue = {
            if (viewModel.validateAndSave()) onContinue()
        },
        modifier = modifier,
    )
}

@Composable
fun SelectClassScreen(
    uiState: SelectClassUiState,
    onClassSelected: (String) -> Unit,
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
        LushAiEduBrandHeader(logoSize = 96.dp)
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = stringResource(
                if (uiState.allowMultiSelect) R.string.select_classes else R.string.select_class,
            ),
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            fontFamily = FontFamily.SansSerif,
        )
        if (uiState.allowMultiSelect) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.select_classes_hint),
                color = TextSecondary,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
            )
        }
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
                    uiState.classes.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            row.forEach { option ->
                                SelectionTile(
                                    label = option.name,
                                    selected = option.id in uiState.selectedClassIds,
                                    onClick = { onClassSelected(option.id) },
                                    icon = Icons.Outlined.School,
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
            continueEnabled = !uiState.isLoading && uiState.selectedClassIds.isNotEmpty(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SelectClassPreview() {
    LushAIEdu_PLSTheme {
        SelectClassScreen(
            uiState = SelectClassUiState(
                classes = listOf(
                    ClassOption("1", "Class IX"),
                    ClassOption("2", "Class X"),
                ),
                selectedClassIds = setOf("1"),
            ),
            onClassSelected = {},
            onBack = {},
            onContinue = {},
        )
    }
}
