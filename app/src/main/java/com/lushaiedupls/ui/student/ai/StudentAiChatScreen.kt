package com.lushaiedupls.ui.student.ai

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.AiChatMessage
import com.lushaiedupls.data.mock.AiMenuContentItem
import com.lushaiedupls.data.mock.AiMenuTab
import com.lushaiedupls.data.mock.AiQuickCheck
import com.lushaiedupls.data.mock.AiQuizHistoryItem
import com.lushaiedupls.data.mock.AiSyllabusItem
import com.lushaiedupls.data.mock.StudentMockRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.ui.common.SlideFromRightOverlay
import com.lushaiedupls.ui.common.StudentPageSkeleton
import com.lushaiedupls.ui.common.StudentSkeletonKind
import com.lushaiedupls.ui.common.markdown.MarkdownLatexText
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.LushAIEdu_PLSTheme
import com.lushaiedupls.ui.theme.TextSecondary

private val BubbleShape = RoundedCornerShape(16.dp)
private val ChipShape = RoundedCornerShape(50)
private val InputShape = RoundedCornerShape(22.dp)
private val CardShape = RoundedCornerShape(22.dp)
private val LanguageMenuShape = RoundedCornerShape(16.dp)
private val HighlightPeach = Color(0xFFFFEFE6)
private val SyllabusText = Color(0xFF3A4256)
private val QuickCheckGreen = Color(0xFF16A34A)
private val QuickCheckRed = Color(0xFFDC2626)

@Composable
fun StudentAiChatRoute(
    subjectId: String,
    studentRepository: StudentRepository,
    onBack: () -> Unit,
    onTakeQuiz: (chapterId: String, sectionIds: List<String>) -> Unit = { _, _ -> },
    chapterId: String? = null,
    modifier: Modifier = Modifier,
    viewModel: StudentAiChatViewModel = viewModel(
        key = listOf(subjectId, chapterId.orEmpty()).joinToString("-"),
        factory = StudentAiChatViewModel.provideFactory(
            studentRepository,
            subjectId,
            chapterId?.takeIf { it.isNotBlank() },
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StudentAiChatScreen(
        uiState = uiState,
        onBack = onBack,
        onClearChat = viewModel::clearChat,
        onOpenMenu = viewModel::openMenu,
        onCloseMenu = viewModel::closeMenu,
        onMenuTabSelected = viewModel::selectMenuTab,
        onDraftChange = viewModel::onDraftChange,
        onSend = viewModel::sendDraft,
        onSuggestion = viewModel::sendSuggestion,
        onQuickOption = viewModel::selectQuickOption,
        onLanguageSelected = viewModel::setLanguage,
        onTakeQuiz = { onTakeQuiz(uiState.chapterId, viewModel.selectedSectionIds()) },
        onAskAboutContent = viewModel::askAboutContent,
        onToggleSyllabus = viewModel::toggleSyllabusSelection,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentAiChatScreen(
    uiState: StudentAiChatUiState,
    onBack: () -> Unit,
    onClearChat: () -> Unit,
    onOpenMenu: () -> Unit,
    onCloseMenu: () -> Unit,
    onMenuTabSelected: (AiMenuTab) -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onSuggestion: (String) -> Unit,
    onQuickOption: (String) -> Unit,
    onLanguageSelected: (String) -> Unit = {},
    onTakeQuiz: () -> Unit = {},
    onAskAboutContent: (AiMenuContentItem) -> Unit = {},
    onToggleSyllabus: (AiSyllabusItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatTopBar(
                title = uiState.chapterTitle,
                activeTab = uiState.menuTab,
                onBack = onBack,
                onClear = onClearChat,
                onMenu = onOpenMenu,
            )

            when (uiState.menuTab) {
                AiMenuTab.Chats -> {
                    val listState = rememberLazyListState()

                    LaunchedEffect(uiState.messages.size, uiState.isSending) {
                        if (uiState.messages.isNotEmpty() || uiState.isSending) {
                            val targetIndex = (uiState.messages.size + (if (uiState.isSending) 1 else 0)).coerceAtLeast(0)
                            listState.animateScrollToItem(targetIndex)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        if (uiState.isLoading && uiState.messages.isEmpty()) {
                            StudentPageSkeleton(
                                kind = StudentSkeletonKind.Chat,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                item { Spacer(modifier = Modifier.height(4.dp)) }
                                items(uiState.messages, key = { it.id }) { message ->
                                    ChatBubble(message = message)
                                }
                                if (uiState.isSending) {
                                    item(key = "ai_thinking") {
                                        AiThinkingBubble()
                                    }
                                }
                                if (uiState.suggestions.isNotEmpty()) {
                                    item {
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            uiState.suggestions.forEach { suggestion ->
                                                SuggestionChip(
                                                    text = suggestion,
                                                    onClick = { onSuggestion(suggestion) },
                                                )
                                            }
                                        }
                                    }
                                }
                                if (uiState.showQuickCheck && uiState.quickCheck != null) {
                                    item {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        QuickCheckCard(
                                            quickCheck = uiState.quickCheck,
                                            selectedOption = uiState.selectedQuickOption,
                                            isAnswered = uiState.quickCheckAnswered,
                                            explanation = uiState.quickCheckExplanation,
                                            onOption = onQuickOption,
                                        )
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(88.dp)) }
                            }
                        }

                        ChatInputBar(
                            language = uiState.language,
                            draft = uiState.draft,
                            onDraftChange = onDraftChange,
                            onSend = onSend,
                            onLanguageSelected = onLanguageSelected,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        )
                    }
                }
                AiMenuTab.TextbookQuestions -> {
                    TextbookQuestionsPageView(
                        questions = uiState.textbookQuestions,
                        isLoading = uiState.isMenuContentLoading,
                        onAskAboutQuestion = onAskAboutContent,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                }
                AiMenuTab.ExamPreparation -> {
                    ExamPreparationPageView(
                        examPrepPyqs = uiState.examPrepPyqs,
                        isLoading = uiState.isMenuContentLoading,
                        onAskAboutPyq = onAskAboutContent,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                }
                AiMenuTab.Resources -> {
                    ResourcesPageView(
                        resources = uiState.resources,
                        isLoading = uiState.isMenuContentLoading,
                        onOpenResource = onAskAboutContent,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (uiState.showMenu) {
        SlideFromRightOverlay(
            onDismiss = onCloseMenu,
            panelWidthFraction = 0.78f,
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp))
                .background(BgWhite),
        ) { requestDismiss ->
            AiChatsMenuOverlay(
                selectedTab = uiState.menuTab,
                syllabus = uiState.syllabus,
                selectedSyllabusIds = uiState.selectedSyllabusIds,
                textbookQuestions = uiState.textbookQuestions,
                examPrepPyqs = uiState.examPrepPyqs,
                resources = uiState.resources,
                quizHistory = uiState.quizHistory,
                isMenuContentLoading = uiState.isMenuContentLoading,
                onTabSelected = onMenuTabSelected,
                onTakeQuiz = {
                    requestDismiss()
                    onTakeQuiz()
                },
                onContentClick = { item ->
                    requestDismiss()
                    onAskAboutContent(item)
                },
                onSyllabusClick = onToggleSyllabus,
                onDismiss = requestDismiss,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            )
        }
    }
}

@Composable
private fun ChatTopBar(
    title: String,
    activeTab: AiMenuTab,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onMenu: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.cd_ai_back),
                tint = BrandBlack,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        Row(modifier = Modifier.align(Alignment.CenterEnd)) {
            if (activeTab == AiMenuTab.Chats) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.cd_ai_delete),
                        tint = BrandOrange,
                    )
                }
            }
            IconButton(onClick = onMenu) {
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = stringResource(R.string.cd_ai_menu),
                    tint = BrandBlack,
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: AiChatMessage) {
    if (message.fromUser) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Text(
                text = message.text,
                modifier = Modifier
                    .background(BrandBlack, BubbleShape)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
            )
        }
    } else {
        MarkdownLatexText(
            text = message.text,
            color = BrandBlack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 12.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeightMultiplier = 20f / 14f,
        )
    }
}

@Composable
private fun AiThinkingBubble(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking_wave")
    val dot1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot1",
    )
    val dot2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, delayMillis = 120, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot2",
    )
    val dot3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, delayMillis = 240, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot3",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(BgLight)
                .border(1.dp, BorderGray.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .offset(y = dot1Offset.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(BrandBlack),
                )
                Box(
                    modifier = Modifier
                        .offset(y = dot2Offset.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(BrandBlack),
                )
                Box(
                    modifier = Modifier
                        .offset(y = dot3Offset.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(BrandBlack),
                )
            }
        }
    }
}

@Composable
private fun SuggestionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = 34.dp)
            .border(1.dp, BrandBlack, ChipShape)
            .clip(ChipShape)
            .background(BgLight)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandBlack,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.SansSerif,
            maxLines = 1,
        )
    }
}

@Composable
private fun QuickCheckCard(
    quickCheck: AiQuickCheck,
    selectedOption: String?,
    isAnswered: Boolean,
    explanation: String?,
    onOption: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, CardShape, clip = false)
            .clip(CardShape)
            .background(BgWhite)
            .border(1.dp, BorderGray.copy(alpha = 0.5f), CardShape)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.ai_quick_check),
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(10.dp))
        MarkdownLatexText(
            text = quickCheck.question,
            color = BrandBlack,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            lineHeightMultiplier = 22f / 15f,
        )
        Spacer(modifier = Modifier.height(14.dp))
        val correctOption = quickCheck.correctIndex?.let { quickCheck.options.getOrNull(it) }
        quickCheck.options.forEach { option ->
            val selected = selectedOption == option
            val isCorrectOption = option == correctOption
            val bg = when {
                !isAnswered && selected -> BrandBlack
                !isAnswered -> BgLight
                isAnswered && isCorrectOption -> QuickCheckGreen
                isAnswered && selected && !isCorrectOption -> QuickCheckRed
                else -> BgLight
            }
            val textColor = when {
                !isAnswered && selected -> Color.White
                !isAnswered -> BrandBlack
                isAnswered && (isCorrectOption || (selected && !isCorrectOption)) -> Color.White
                else -> BrandBlack
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .heightIn(min = 44.dp)
                    .clip(ChipShape)
                    .background(bg)
                    .clickable(enabled = !isAnswered) { onOption(option) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                MarkdownLatexText(
                    text = option,
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeightMultiplier = 20f / 14f,
                    enableLinks = false,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (isAnswered && !explanation.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            MarkdownLatexText(
                text = explanation,
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeightMultiplier = 18f / 13f,
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    language: String,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onLanguageSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var languageButtonWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .onSizeChanged { languageButtonWidthPx = it.width }
                .clip(ChipShape)
                .background(BrandBlack)
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = language,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = if (expanded) {
                        Icons.Outlined.KeyboardArrowUp
                    } else {
                        Icons.Outlined.KeyboardArrowDown
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
            }

            if (expanded) {
                val menuWidthDp = with(density) { languageButtonWidthPx.toDp() }
                val popupOffset = with(density) {
                    IntOffset(
                        x = 0,
                        y = -(44.dp.roundToPx() + 104.dp.roundToPx()),
                    )
                }
                Popup(
                    alignment = Alignment.TopStart,
                    offset = popupOffset,
                    onDismissRequest = { expanded = false },
                    properties = PopupProperties(focusable = true),
                ) {
                    Column(
                        modifier = Modifier
                            .width(menuWidthDp)
                            .shadow(12.dp, LanguageMenuShape, clip = false)
                            .clip(LanguageMenuShape)
                            .background(BgWhite)
                            .border(1.dp, BorderGray.copy(alpha = 0.7f), LanguageMenuShape)
                            .padding(vertical = 4.dp),
                    ) {
                        listOf("English", "Mizo").forEach { lang ->
                            val selected = lang == language
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onLanguageSelected(lang)
                                        expanded = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Translate,
                                        contentDescription = null,
                                        tint = if (selected) BrandOrange else TextSecondary,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = lang,
                                        color = if (selected) BrandBlack else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Medium
                                        },
                                        fontFamily = FontFamily.SansSerif,
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = BrandOrange,
                                        modifier = Modifier.size(13.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 44.dp)
                .clip(InputShape)
                .background(BgLight)
                .padding(horizontal = 16.dp, vertical = 11.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (draft.isEmpty()) {
                Text(
                    text = stringResource(R.string.ai_send_messages),
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                )
            }
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(
                    color = BrandBlack,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                ),
                cursorBrush = SolidColor(BrandBlack),
            )
        }

        IconButton(
            onClick = onSend,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(BrandBlack),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Send,
                contentDescription = stringResource(R.string.cd_ai_send),
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun TextbookQuestionsPageView(
    questions: List<AiMenuContentItem>,
    isLoading: Boolean,
    onAskAboutQuestion: (AiMenuContentItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isLoading && questions.isEmpty()) {
        MenuLoading(modifier = modifier.fillMaxSize())
    } else if (questions.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.ai_menu_no_questions),
                color = TextSecondary,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.ai_menu_textbook),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BrandBlack,
                        fontFamily = FontFamily.SansSerif,
                    )
                    Text(
                        text = "${questions.size} items",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        fontFamily = FontFamily.SansSerif,
                    )
                }
            }
            itemsIndexed(questions, key = { index, item -> item.id.ifBlank { index.toString() } }) { index, item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BgWhite),
                    border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.65f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BgLight)
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = "Q ${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = BrandBlack,
                                    fontFamily = FontFamily.SansSerif,
                                )
                            }
                            if (!item.subtitle.isNullOrBlank()) {
                                Text(
                                    text = item.subtitle,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.SansSerif,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        MarkdownLatexText(
                            text = item.title,
                            color = BrandBlack,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeightMultiplier = 20f / 14f,
                        )
                        if (!item.imageUrl.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            AsyncImage(
                                model = item.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Fit,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(ChipShape)
                                    .background(BrandBlack)
                                    .clickable { onAskAboutQuestion(item) }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Ask tutor about this question",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.SansSerif,
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ExamPreparationPageView(
    examPrepPyqs: List<AiMenuContentItem>,
    isLoading: Boolean,
    onAskAboutPyq: (AiMenuContentItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isLoading && examPrepPyqs.isEmpty()) {
        MenuLoading(modifier = modifier.fillMaxSize())
    } else if (examPrepPyqs.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No exam preparation questions found for this chapter.",
                color = TextSecondary,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.ai_menu_exam),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BrandBlack,
                        fontFamily = FontFamily.SansSerif,
                    )
                    Text(
                        text = "${examPrepPyqs.size} questions",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        fontFamily = FontFamily.SansSerif,
                    )
                }
            }
            itemsIndexed(examPrepPyqs, key = { index, item -> item.id.ifBlank { index.toString() } }) { index, item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAskAboutPyq(item) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BgWhite),
                    border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.65f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BgLight)
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = "Q ${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = BrandBlack,
                                    fontFamily = FontFamily.SansSerif,
                                )
                            }
                            if (!item.subtitle.isNullOrBlank()) {
                                Text(
                                    text = item.subtitle,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.SansSerif,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        MarkdownLatexText(
                            text = item.title,
                            color = BrandBlack,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeightMultiplier = 20f / 14f,
                        )
                        if (!item.imageUrl.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            AsyncImage(
                                model = item.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Fit,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(ChipShape)
                                    .background(BrandBlack)
                                    .clickable { onAskAboutPyq(item) }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Ask tutor about this question",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.SansSerif,
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ResourcesPageView(
    resources: List<AiMenuContentItem>,
    isLoading: Boolean,
    onOpenResource: (AiMenuContentItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isLoading && resources.isEmpty()) {
        MenuLoading(modifier = modifier.fillMaxSize())
    } else if (resources.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.ai_menu_no_resources),
                color = TextSecondary,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.ai_menu_resources),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BrandBlack,
                        fontFamily = FontFamily.SansSerif,
                    )
                    Text(
                        text = "${resources.size} files",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        fontFamily = FontFamily.SansSerif,
                    )
                }
            }
            itemsIndexed(resources, key = { index, item -> item.id.ifBlank { index.toString() } }) { _, item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenResource(item) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BgWhite),
                    border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.65f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BgLight),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!item.imageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = item.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                                    contentDescription = null,
                                    tint = BrandOrange,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                color = BrandBlack,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.SansSerif,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!item.subtitle.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = item.subtitle,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(ChipShape)
                                .background(BgLight)
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Ask AI",
                                color = BrandBlack,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif,
                            )
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiChatsMenuOverlay(
    selectedTab: AiMenuTab,
    syllabus: List<AiSyllabusItem>,
    selectedSyllabusIds: Set<String> = emptySet(),
    textbookQuestions: List<AiMenuContentItem> = emptyList(),
    examPrepPyqs: List<AiMenuContentItem> = emptyList(),
    resources: List<AiMenuContentItem> = emptyList(),
    quizHistory: List<AiQuizHistoryItem> = emptyList(),
    isMenuContentLoading: Boolean = false,
    onTabSelected: (AiMenuTab) -> Unit,
    onTakeQuiz: () -> Unit,
    onContentClick: (AiMenuContentItem) -> Unit = {},
    onSyllabusClick: (AiSyllabusItem) -> Unit = {},
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val showQuizButton = selectedTab == AiMenuTab.Chats || selectedTab == AiMenuTab.ExamPreparation
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showQuizButton) 136.dp else 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(
                    text = stringResource(R.string.ai_menu_title),
                    modifier = Modifier.align(Alignment.Center),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = BrandBlack,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MenuTabChip(
                    label = stringResource(R.string.ai_menu_chats),
                    selected = selectedTab == AiMenuTab.Chats,
                    onClick = { onTabSelected(AiMenuTab.Chats) },
                )
                MenuTabChip(
                    label = stringResource(R.string.ai_menu_textbook),
                    selected = selectedTab == AiMenuTab.TextbookQuestions,
                    onClick = { onTabSelected(AiMenuTab.TextbookQuestions) },
                )
                MenuTabChip(
                    label = stringResource(R.string.ai_menu_exam),
                    selected = selectedTab == AiMenuTab.ExamPreparation,
                    onClick = { onTabSelected(AiMenuTab.ExamPreparation) },
                )
                MenuTabChip(
                    label = stringResource(R.string.ai_menu_resources),
                    selected = selectedTab == AiMenuTab.Resources,
                    onClick = { onTabSelected(AiMenuTab.Resources) },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                AiMenuTab.Chats -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        syllabus.forEachIndexed { index, item ->
                            SyllabusRow(
                                item = item,
                                selected = item.id in selectedSyllabusIds,
                                onClick = { onSyllabusClick(item) },
                            )
                            if (index != syllabus.lastIndex) {
                                HorizontalDivider(color = BorderGray.copy(alpha = 0.35f))
                            }
                        }
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
                AiMenuTab.TextbookQuestions -> MenuContentList(
                    items = textbookQuestions,
                    isLoading = isMenuContentLoading,
                    emptyText = stringResource(R.string.ai_menu_no_questions),
                    onItemClick = onContentClick,
                    modifier = Modifier.weight(1f),
                )
                AiMenuTab.Resources -> MenuContentList(
                    items = resources,
                    isLoading = isMenuContentLoading,
                    emptyText = stringResource(R.string.ai_menu_no_resources),
                    showThumbnail = true,
                    onItemClick = onContentClick,
                    modifier = Modifier.weight(1f),
                )
                AiMenuTab.ExamPreparation -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        if (examPrepPyqs.isNotEmpty()) {
                            Text(
                                text = "Past exam questions (PYQs)",
                                color = BrandBlack,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            examPrepPyqs.forEachIndexed { index, item ->
                                MenuContentRow(
                                    item = item,
                                    showThumbnail = false,
                                    onClick = { onContentClick(item) },
                                )
                                if (index != examPrepPyqs.lastIndex) {
                                    HorizontalDivider(color = BorderGray.copy(alpha = 0.35f))
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                        if (syllabus.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.ai_exam_section_practice),
                                color = BrandBlack,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            syllabus.forEachIndexed { index, item ->
                                SyllabusRow(
                                    item = item,
                                    selected = item.id in selectedSyllabusIds,
                                    onClick = { onSyllabusClick(item) },
                                )
                                if (index != syllabus.lastIndex) {
                                    HorizontalDivider(color = BorderGray.copy(alpha = 0.35f))
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                        Text(
                            text = stringResource(R.string.ai_exam_past_attempts),
                            color = BrandBlack,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        when {
                            isMenuContentLoading && quizHistory.isEmpty() -> MenuLoading()
                            quizHistory.isEmpty() -> Text(
                                text = stringResource(R.string.ai_exam_no_history),
                                color = TextSecondary,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                            else -> quizHistory.forEachIndexed { index, item ->
                                QuizHistoryRow(item = item)
                                if (index != quizHistory.lastIndex) {
                                    HorizontalDivider(color = BorderGray.copy(alpha = 0.35f))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }

        if (showQuizButton) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
                    .fillMaxWidth(0.72f)
                    .height(48.dp)
                    .clip(ChipShape)
                    .background(BrandBlack)
                    .clickable(onClick = onTakeQuiz),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.ai_take_quiz),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

@Composable
private fun MenuTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(ChipShape)
            .border(1.dp, if (selected) BrandBlack else BorderGray.copy(alpha = 0.6f), ChipShape)
            .background(if (selected) BrandBlack else BgWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            maxLines = 1,
        )
    }
}

@Composable
private fun MenuContentList(
    items: List<AiMenuContentItem>,
    isLoading: Boolean,
    emptyText: String,
    onItemClick: (AiMenuContentItem) -> Unit,
    showThumbnail: Boolean = false,
    modifier: Modifier = Modifier,
) {
    when {
        isLoading && items.isEmpty() -> MenuLoading(modifier = modifier)
        items.isEmpty() -> Text(
            text = emptyText,
            color = TextSecondary,
            fontSize = 14.sp,
            modifier = modifier.padding(vertical = 24.dp),
        )
        else -> Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            items.forEachIndexed { index, item ->
                MenuContentRow(
                    item = item,
                    showThumbnail = showThumbnail,
                    onClick = { onItemClick(item) },
                )
                if (index != items.lastIndex) {
                    HorizontalDivider(color = BorderGray.copy(alpha = 0.35f))
                }
            }
        }
    }
}

@Composable
private fun MenuLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = BrandBlack,
            strokeWidth = 2.dp,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun MenuContentRow(
    item: AiMenuContentItem,
    showThumbnail: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (showThumbnail) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgLight),
                contentAlignment = Alignment.Center,
            ) {
                if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = SyllabusText,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontFamily = FontFamily.SansSerif,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (!item.subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun QuizHistoryRow(item: AiQuizHistoryItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
    ) {
        Text(
            text = item.title,
            color = SyllabusText,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.subtitle,
            color = TextSecondary,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun SyllabusRow(
    item: AiSyllabusItem,
    selected: Boolean,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) HighlightPeach else BgWhite)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(
                start = if (item.indented) 28.dp else 8.dp,
                end = 8.dp,
                top = 12.dp,
                bottom = 12.dp,
            ),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "•  ${item.title}",
            modifier = Modifier.weight(1f),
            color = SyllabusText,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontFamily = FontFamily.SansSerif,
        )
        if (item.progressLabel != null) {
            Text(
                text = item.progressLabel,
                color = SyllabusText,
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun StudentAiChatPreview() {
    val session = StudentMockRepository().aiChatSession("chemistry")
    val pack = session.packFor("English")
    LushAIEdu_PLSTheme {
        StudentAiChatScreen(
            uiState = StudentAiChatUiState(
                chapterTitle = "Chapter: 1",
                messages = pack.messages,
                suggestions = pack.suggestions,
                quickCheck = pack.quickCheck,
                syllabus = session.syllabus,
                textbookQuestions = session.textbookQuestions,
                examPrepPyqs = session.examPrepPyqs,
                resources = session.resources,
                quizHistory = session.quizHistory,
                language = "English",
                showQuickCheck = true,
            ),
            onBack = {},
            onClearChat = {},
            onOpenMenu = {},
            onCloseMenu = {},
            onMenuTabSelected = {},
            onDraftChange = {},
            onSend = {},
            onSuggestion = {},
            onQuickOption = {},
        )
    }
}
