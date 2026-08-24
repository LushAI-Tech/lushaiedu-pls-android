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
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FolderOpen
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
import com.lushaiedupls.ui.common.SkeletonBox
import com.lushaiedupls.ui.common.SkeletonLine
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
                                verticalArrangement = Arrangement.spacedBy(14.dp),
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
                                item { Spacer(modifier = Modifier.height(100.dp)) }
                            }
                        }

                        // Bottom message bar with white background
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(BgWhite)
                                .navigationBarsPadding(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            HorizontalDivider(color = BorderGray.copy(alpha = 0.4f))
                            ChatInputBar(
                                language = uiState.language,
                                draft = uiState.draft,
                                onDraftChange = onDraftChange,
                                onSend = onSend,
                                onLanguageSelected = onLanguageSelected,
                                modifier = Modifier
                                    .fillMaxWidth(0.94f)
                                    .padding(vertical = 10.dp),
                            )
                        }
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 8.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text = message.text,
                modifier = Modifier
                    .widthIn(max = 290.dp)
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
                .padding(top = 2.dp, bottom = 6.dp, end = 12.dp),
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
                    .background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
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
                        onClick = if (!isAnswered) { { onOption(option) } } else null,
                    )
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(ChipShape)
                        .clickable(enabled = !isAnswered) { onOption(option) },
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Translate,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = language,
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                )
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
                val popupOffset = with(density) {
                    IntOffset(
                        x = 0,
                        y = -(96.dp.roundToPx()),
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
                            .width(136.dp)
                            .shadow(12.dp, LanguageMenuShape, clip = false)
                            .clip(LanguageMenuShape)
                            .background(BgWhite)
                            .border(1.dp, BorderGray.copy(alpha = 0.7f), LanguageMenuShape)
                            .padding(4.dp),
                    ) {
                        listOf("English", "Mizo").forEach { lang ->
                            val selected = lang == language
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) BgLight else Color.Transparent)
                                    .clickable {
                                        onLanguageSelected(lang)
                                        expanded = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
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
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = lang,
                                        color = if (selected) BrandBlack else TextSecondary,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (selected) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Medium
                                        },
                                        fontFamily = FontFamily.SansSerif,
                                        maxLines = 1,
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = BrandOrange,
                                        modifier = Modifier.size(14.dp),
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
        StudentPageSkeleton(
            kind = StudentSkeletonKind.TextbookQuestions,
            modifier = modifier.fillMaxSize(),
        )
    } else if (questions.isEmpty()) {
        AiEmptyStateView(
            tab = AiMenuTab.TextbookQuestions,
            onAction = {
                onAskAboutQuestion(
                    AiMenuContentItem(
                        id = "",
                        sectionId = "",
                        title = "Ask tutor a textbook question",
                    ),
                )
            },
            modifier = modifier,
        )
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
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(32.dp)
                                    .clip(ChipShape)
                                    .background(BrandBlack)
                                    .clickable { onAskAboutQuestion(item) }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoAwesome,
                                        contentDescription = null,
                                        tint = BrandOrange,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Text(
                                        text = "Ask",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.5.sp,
                                        fontFamily = FontFamily.SansSerif,
                                    )
                                }
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
        StudentPageSkeleton(
            kind = StudentSkeletonKind.ExamPreparation,
            modifier = modifier.fillMaxSize(),
        )
    } else if (examPrepPyqs.isEmpty()) {
        AiEmptyStateView(
            tab = AiMenuTab.ExamPreparation,
            modifier = modifier,
        )
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
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(32.dp)
                                    .clip(ChipShape)
                                    .background(BrandBlack)
                                    .clickable { onAskAboutPyq(item) }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoAwesome,
                                        contentDescription = null,
                                        tint = BrandOrange,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Text(
                                        text = "Ask",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.5.sp,
                                        fontFamily = FontFamily.SansSerif,
                                    )
                                }
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
        StudentPageSkeleton(
            kind = StudentSkeletonKind.Resources,
            modifier = modifier.fillMaxSize(),
        )
    } else if (resources.isEmpty()) {
        AiEmptyStateView(
            tab = AiMenuTab.Resources,
            onAction = null,
            modifier = modifier,
        )
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

@Composable
private fun AiEmptyStateView(
    tab: AiMenuTab,
    onAction: (() -> Unit)? = null,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = if (compact) 8.dp else 16.dp, vertical = if (compact) 12.dp else 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = BgLight.copy(alpha = 0.65f)),
            border = BorderStroke(1.dp, BorderGray.copy(alpha = 0.55f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (compact) 16.dp else 22.dp, vertical = if (compact) 20.dp else 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Icon Badge
                Box(
                    modifier = Modifier
                        .size(if (compact) 54.dp else 66.dp)
                        .clip(CircleShape)
                        .background(BrandOrange.copy(alpha = 0.10f))
                        .border(1.dp, BrandOrange.copy(alpha = 0.22f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = when (tab) {
                            AiMenuTab.Resources -> Icons.Outlined.FolderOpen
                            AiMenuTab.TextbookQuestions -> Icons.Outlined.AutoStories
                            AiMenuTab.ExamPreparation -> Icons.AutoMirrored.Outlined.Assignment
                            else -> Icons.Outlined.AutoAwesome
                        },
                        contentDescription = null,
                        tint = BrandOrange,
                        modifier = Modifier.size(if (compact) 26.dp else 32.dp),
                    )
                }

                Spacer(modifier = Modifier.height(if (compact) 12.dp else 16.dp))

                // Title
                Text(
                    text = when (tab) {
                        AiMenuTab.Resources -> "No Figures or Resources Yet"
                        AiMenuTab.TextbookQuestions -> "No Textbook Questions Yet"
                        AiMenuTab.ExamPreparation -> "No Past Exam Questions Found"
                        else -> "No Items Available"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 15.sp else 17.sp,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle description
                Text(
                    text = when (tab) {
                        AiMenuTab.Resources -> "Figures, diagrams, and downloadable study attachments for this chapter will appear here once added."
                        AiMenuTab.TextbookQuestions -> "Practice questions and textbook exercises for this chapter will appear here once published."
                        AiMenuTab.ExamPreparation -> "Previous years' exam questions for this chapter haven't been mapped yet. You can practice with AI tutor in Chat!"
                        else -> "Content for this section will be available soon."
                    },
                    color = TextSecondary,
                    fontSize = if (compact) 12.sp else 13.5.sp,
                    lineHeight = if (compact) 17.sp else 20.sp,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 280.dp),
                )

                if (onAction != null && tab != AiMenuTab.Resources && tab != AiMenuTab.ExamPreparation) {
                    Spacer(modifier = Modifier.height(if (compact) 14.dp else 20.dp))

                    // Action pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(BgWhite)
                            .border(1.dp, BorderGray.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .clickable { onAction() }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                tint = BrandOrange,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = when (tab) {
                                    AiMenuTab.TextbookQuestions -> "Ask AI Tutor a question"
                                    else -> "Chat with AI Tutor"
                                },
                                color = BrandBlack,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.SansSerif,
                            )
                        }
                    }
                }
            }
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
                    tab = AiMenuTab.TextbookQuestions,
                    onItemClick = onContentClick,
                    modifier = Modifier.weight(1f),
                )
                AiMenuTab.Resources -> MenuContentList(
                    items = resources,
                    isLoading = isMenuContentLoading,
                    tab = AiMenuTab.Resources,
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
                            isMenuContentLoading && quizHistory.isEmpty() -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    repeat(3) {
                                        SkeletonLine(modifier = Modifier.fillMaxWidth(0.7f), height = 12.dp)
                                        SkeletonLine(modifier = Modifier.fillMaxWidth(0.4f), height = 10.dp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
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
    tab: AiMenuTab,
    onItemClick: (AiMenuContentItem) -> Unit,
    showThumbnail: Boolean = false,
    modifier: Modifier = Modifier,
) {
    when {
        isLoading && items.isEmpty() -> {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(4) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (showThumbnail) {
                            SkeletonBox(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(8.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            SkeletonLine(modifier = Modifier.fillMaxWidth(0.75f), height = 12.dp)
                            Spacer(modifier = Modifier.height(6.dp))
                            SkeletonLine(modifier = Modifier.fillMaxWidth(0.4f), height = 10.dp)
                        }
                    }
                    HorizontalDivider(color = BorderGray.copy(alpha = 0.25f))
                }
            }
        }
        items.isEmpty() -> {
            AiEmptyStateView(
                tab = tab,
                compact = true,
                onAction = if (tab == AiMenuTab.Resources || tab == AiMenuTab.ExamPreparation) null else {
                    {
                        onItemClick(
                            AiMenuContentItem(
                                id = "",
                                sectionId = "",
                                title = when (tab) {
                                    AiMenuTab.TextbookQuestions -> "Ask tutor a textbook question"
                                    else -> "Ask tutor a question"
                                },
                            ),
                        )
                    }
                },
                modifier = modifier,
            )
        }
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
