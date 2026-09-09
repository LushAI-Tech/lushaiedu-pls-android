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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.lushaiedupls.data.mock.AiSyllabusItem
import com.lushaiedupls.data.mock.StudentMockRepository
import com.lushaiedupls.data.mock.isQuestionAskMessage
import com.lushaiedupls.data.mock.isResourceAskMessage
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.ui.common.AiChatIntroSkeleton
import com.lushaiedupls.ui.common.AiMenuQuestionCardSkeleton
import com.lushaiedupls.ui.common.AiMenuSectionHeaderSkeleton
import com.lushaiedupls.ui.common.CenteredEmptyState
import com.lushaiedupls.ui.common.SkeletonBox
import com.lushaiedupls.ui.common.SkeletonLine
import com.lushaiedupls.ui.common.SlideFromRightOverlay
import com.lushaiedupls.ui.common.markdown.MarkdownLatexText
import com.lushaiedupls.data.remote.friendlyStemBindingMessage
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

private suspend fun LazyListState.animateScrollToLastItem() {
    val lastIndex = (layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
    if (layoutInfo.totalItemsCount > 0) {
        animateScrollToItem(lastIndex)
    }
}

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
        onAskAboutResource = viewModel::askAboutResource,
        onClearPendingAsk = viewModel::clearPendingAsk,
        onBackToQuestion = viewModel::returnToQuestion,
        onBackToResource = viewModel::returnToResource,
        onToggleSyllabus = viewModel::toggleSyllabusSelection,
        onSelectAllSyllabus = viewModel::selectAllSyllabus,
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
    onAskAboutResource: (AiMenuContentItem) -> Unit = {},
    onClearPendingAsk: () -> Unit = {},
    onBackToQuestion: (AiChatMessage) -> Unit = {},
    onBackToResource: (AiChatMessage) -> Unit = {},
    onToggleSyllabus: (AiSyllabusItem) -> Unit = {},
    onSelectAllSyllabus: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatTopBar(
                title = uiState.chapterTitle.ifBlank {
                    stringResource(R.string.ai_learn_title)
                },
                activeTab = uiState.menuTab,
                onBack = onBack,
                onClear = onClearChat,
                onMenu = onOpenMenu,
            )

            val blockingEmptyError = !uiState.isLoading &&
                uiState.messages.isEmpty() &&
                !uiState.errorMessage.isNullOrBlank()

            if (blockingEmptyError) {
                CenteredEmptyState(
                    message = friendlyStemBindingMessage(uiState.errorMessage.orEmpty())
                        ?: uiState.errorMessage.orEmpty(),
                    icon = Icons.Outlined.AutoStories,
                    title = stringResource(R.string.ai_subject_unavailable_title),
                    modifier = Modifier.weight(1f),
                    fillMaxSize = true,
                )
            } else {
            uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = BrandOrange,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                )
            }

            when (uiState.menuTab) {
                AiMenuTab.Chats -> {
                    val listState = rememberLazyListState()
                    val composerFocusRequester = remember { FocusRequester() }
                    val keyboardController = LocalSoftwareKeyboardController.current
                    val density = LocalDensity.current
                    var composerFocused by remember { mutableStateOf(false) }
                    val imeBottom = WindowInsets.ime.getBottom(density)

                    LaunchedEffect(uiState.messages.size, uiState.isSending, uiState.suggestions.size) {
                        if (uiState.messages.isNotEmpty() || uiState.isSending) {
                            listState.animateScrollToLastItem()
                        }
                    }
                    LaunchedEffect(composerFocused, imeBottom) {
                        if (!composerFocused && imeBottom <= 0) return@LaunchedEffect
                        withFrameNanos { }
                        listState.animateScrollToLastItem()
                    }
                    LaunchedEffect(uiState.composerFocusNonce) {
                        if (uiState.composerFocusNonce <= 0) return@LaunchedEffect
                        withFrameNanos { }
                        runCatching { composerFocusRequester.requestFocus() }
                        keyboardController?.show()
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .imePadding(),
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            item { Spacer(modifier = Modifier.height(4.dp)) }
                            if (uiState.isLoading && uiState.messages.isEmpty()) {
                                item(key = "chat_intro_skeleton") {
                                    AiChatIntroSkeleton()
                                }
                            } else {
                                items(uiState.messages, key = { it.id }) { message ->
                                    ChatBubble(
                                        message = message,
                                        onBackToQuestion = if (message.isQuestionAskMessage()) {
                                            { onBackToQuestion(message) }
                                        } else {
                                            null
                                        },
                                        onBackToResource = if (message.isResourceAskMessage()) {
                                            { onBackToResource(message) }
                                        } else {
                                            null
                                        },
                                    )
                                }
                                if (uiState.isSending) {
                                    item(key = "ai_thinking") {
                                        AiThinkingBubble()
                                    }
                                }
                                if (!uiState.isSending && uiState.suggestions.isNotEmpty()) {
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
                                if (!uiState.isSending && uiState.showQuickCheck && uiState.quickCheck != null) {
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
                            }
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BgWhite),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            HorizontalDivider(color = BorderGray.copy(alpha = 0.4f))
                            uiState.pendingAsk?.let { pending ->
                                PendingAskComposerChip(
                                    pending = pending,
                                    onClear = onClearPendingAsk,
                                    modifier = Modifier
                                        .fillMaxWidth(0.94f)
                                        .padding(top = 10.dp),
                                )
                            }
                            ChatInputBar(
                                language = uiState.language,
                                draft = uiState.draft,
                                placeholder = when (uiState.pendingAsk?.tab) {
                                    AiMenuTab.Resources -> stringResource(R.string.ai_ask_about_resource)
                                    AiMenuTab.TextbookQuestions,
                                    AiMenuTab.ExamPreparation,
                                    -> stringResource(R.string.ai_ask_about_question)
                                    else -> stringResource(R.string.ai_send_messages)
                                },
                                onDraftChange = onDraftChange,
                                onSend = onSend,
                                onLanguageSelected = onLanguageSelected,
                                focusRequester = composerFocusRequester,
                                onComposerFocusChange = { composerFocused = it },
                                modifier = Modifier
                                    .fillMaxWidth(0.94f)
                                    .padding(top = 8.dp, bottom = 8.dp),
                            )
                        }
                    }
                }
                AiMenuTab.TextbookQuestions -> {
                    TextbookQuestionsPageView(
                        questions = uiState.textbookQuestions,
                        isLoading = uiState.isMenuContentLoading,
                        onAskAboutQuestion = onAskAboutContent,
                        highlightedQuestionId = uiState.scrollToQuestionId,
                        scrollToQuestionNonce = uiState.scrollToQuestionNonce,
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
                        highlightedQuestionId = uiState.scrollToQuestionId,
                        scrollToQuestionNonce = uiState.scrollToQuestionNonce,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                }
                AiMenuTab.Resources -> {
                    ResourcesPageView(
                        resources = uiState.resources,
                        isLoading = uiState.isMenuContentLoading,
                        onOpenResource = onAskAboutResource,
                        highlightedResourceId = uiState.scrollToQuestionId,
                        scrollToResourceNonce = uiState.scrollToQuestionNonce,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                }
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
                onTabSelected = onMenuTabSelected,
                onTakeQuiz = {
                    requestDismiss()
                    onTakeQuiz()
                },
                onSyllabusClick = onToggleSyllabus,
                onSelectAllSyllabus = onSelectAllSyllabus,
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
private fun ChatBubble(
    message: AiChatMessage,
    onBackToQuestion: (() -> Unit)? = null,
    onBackToResource: (() -> Unit)? = null,
) {
    if (message.fromUser) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            val resourceTitle = message.linkedResourceTitle?.takeIf { it.isNotBlank() }
            val questionTitle = message.linkedQuestionTitle?.takeIf { it.isNotBlank() }
            if (onBackToResource != null && resourceTitle != null) {
                LinkedAskChip(
                    title = resourceTitle,
                    icon = Icons.Outlined.FolderOpen,
                    contentDescription = stringResource(R.string.cd_ai_back_to_resource),
                    onClick = onBackToResource,
                )
            } else if (onBackToQuestion != null && questionTitle != null) {
                LinkedAskChip(
                    title = questionTitle,
                    icon = Icons.Outlined.AutoStories,
                    contentDescription = stringResource(R.string.cd_ai_back_to_question),
                    onClick = onBackToQuestion,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                if (onBackToQuestion != null && questionTitle == null && onBackToResource == null) {
                    IconButton(
                        onClick = onBackToQuestion,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BgLight)
                            .border(1.dp, BorderGray.copy(alpha = 0.6f), CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoStories,
                            contentDescription = stringResource(R.string.cd_ai_back_to_question),
                            tint = BrandOrange,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                if (message.text.isNotBlank()) {
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
            }
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
private fun LinkedAskChip(
    title: String,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(HighlightPeach)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = BrandOrange,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = title,
            color = BrandBlack,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 220.dp),
        )
    }
}

@Composable
private fun PendingAskComposerChip(
    pending: PendingAsk,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = pending.item
    val chipShape = RoundedCornerShape(14.dp)
    val fallbackTitle = when (pending.tab) {
        AiMenuTab.ExamPreparation -> stringResource(R.string.ai_menu_exam)
        AiMenuTab.TextbookQuestions -> stringResource(R.string.ai_menu_textbook)
        else -> stringResource(R.string.ai_menu_resources)
    }
    val leadingIcon = when (pending.tab) {
        AiMenuTab.ExamPreparation -> Icons.Outlined.AutoAwesome
        AiMenuTab.TextbookQuestions -> Icons.Outlined.AutoStories
        else -> Icons.Outlined.FolderOpen
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(chipShape)
            .background(HighlightPeach)
            .border(1.dp, BrandOrange.copy(alpha = 0.45f), chipShape)
            .padding(start = 8.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BrandBlack),
            contentAlignment = Alignment.Center,
        ) {
            if (pending.tab == AiMenuTab.Resources && !item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.ai_asking_about_resource),
                color = BrandOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
            )
            Text(
                text = item.title.ifBlank { fallbackTitle },
                color = BrandBlack,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onClear) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.cd_ai_clear_resource),
                tint = TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    language: String,
    draft: String,
    placeholder: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onLanguageSelected: (String) -> Unit = {},
    focusRequester: FocusRequester? = null,
    onComposerFocusChange: (Boolean) -> Unit = {},
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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    focusRequester?.let { runCatching { it.requestFocus() } }
                }
                .padding(horizontal = 16.dp, vertical = 11.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (draft.isEmpty()) {
                Text(
                    text = placeholder,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                )
            }
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (focusRequester != null) {
                            Modifier
                                .focusRequester(focusRequester)
                                .onFocusChanged { onComposerFocusChange(it.isFocused) }
                        } else {
                            Modifier
                        },
                    ),
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
    highlightedQuestionId: String? = null,
    scrollToQuestionNonce: Int = 0,
    modifier: Modifier = Modifier,
) {
    if (isLoading && questions.isEmpty()) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AiMenuSectionHeaderSkeleton(titleWidth = 150.dp, trailingWidth = 52.dp)
            }
            items(3) {
                AiMenuQuestionCardSkeleton()
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
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
        val listState = rememberLazyListState()
        LaunchedEffect(scrollToQuestionNonce, highlightedQuestionId) {
            val targetId = highlightedQuestionId ?: return@LaunchedEffect
            val index = questions.indexOfFirst { it.id == targetId }
            if (index >= 0) {
                listState.animateScrollToItem(index + 1)
            }
        }
        LazyColumn(
            state = listState,
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
                val highlighted = item.id == highlightedQuestionId
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (highlighted) HighlightPeach else BgWhite,
                    ),
                    border = BorderStroke(
                        width = if (highlighted) 2.dp else 1.dp,
                        color = if (highlighted) BrandOrange else BorderGray.copy(alpha = 0.65f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                    ) {
                        AiMenuQuestionNumber(number = index + 1)
                        Spacer(modifier = Modifier.height(8.dp))
                        AiMenuQuestionContent(item = item)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AiMenuQuestionFooter(
                                subtitle = item.subtitle,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                            )
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
    highlightedQuestionId: String? = null,
    scrollToQuestionNonce: Int = 0,
    modifier: Modifier = Modifier,
) {
    if (isLoading && examPrepPyqs.isEmpty()) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AiMenuSectionHeaderSkeleton(titleWidth = 140.dp, trailingWidth = 72.dp)
            }
            items(3) {
                AiMenuQuestionCardSkeleton()
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    } else if (examPrepPyqs.isEmpty()) {
        AiEmptyStateView(
            tab = AiMenuTab.ExamPreparation,
            modifier = modifier,
        )
    } else {
        val listState = rememberLazyListState()
        LaunchedEffect(scrollToQuestionNonce, highlightedQuestionId) {
            val targetId = highlightedQuestionId ?: return@LaunchedEffect
            val index = examPrepPyqs.indexOfFirst { it.id == targetId }
            if (index >= 0) {
                listState.animateScrollToItem(index + 1)
            }
        }
        LazyColumn(
            state = listState,
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
                val highlighted = item.id == highlightedQuestionId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAskAboutPyq(item) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (highlighted) HighlightPeach else BgWhite,
                    ),
                    border = BorderStroke(
                        width = if (highlighted) 2.dp else 1.dp,
                        color = if (highlighted) BrandOrange else BorderGray.copy(alpha = 0.65f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                    ) {
                        AiMenuQuestionNumber(number = index + 1)
                        Spacer(modifier = Modifier.height(8.dp))
                        AiMenuQuestionContent(item = item)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AiMenuQuestionFooter(
                                subtitle = item.subtitle,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                            )
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
    highlightedResourceId: String? = null,
    scrollToResourceNonce: Int = 0,
    modifier: Modifier = Modifier,
) {
    if (isLoading && resources.isEmpty()) {
        ResourceVideosPage(
            resources = emptyList(),
            isLoading = true,
            onAskAboutResource = {},
            modifier = modifier,
        )
    } else if (resources.isEmpty()) {
        AiEmptyStateView(
            tab = AiMenuTab.Resources,
            onAction = null,
            modifier = modifier,
        )
    } else {
        ResourceVideosPage(
            resources = resources,
            isLoading = false,
            onAskAboutResource = onOpenResource,
            highlightedResourceId = highlightedResourceId,
            scrollToResourceNonce = scrollToResourceNonce,
            modifier = modifier,
        )
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
    onTabSelected: (AiMenuTab) -> Unit,
    onTakeQuiz: () -> Unit,
    onSyllabusClick: (AiSyllabusItem) -> Unit = {},
    onSelectAllSyllabus: () -> Unit = {},
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val showQuizButton = selectedTab == AiMenuTab.Chats
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

            AiMenuSyllabusList(
                syllabus = syllabus,
                selectedSyllabusIds = selectedSyllabusIds,
                onSyllabusClick = onSyllabusClick,
                onSelectAllSyllabus = onSelectAllSyllabus,
                modifier = Modifier.weight(1f),
            )
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
private fun AiMenuSyllabusList(
    syllabus: List<AiSyllabusItem>,
    selectedSyllabusIds: Set<String>,
    onSyllabusClick: (AiSyllabusItem) -> Unit,
    onSelectAllSyllabus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val allTopicsSelected = selectedSyllabusIds.isEmpty() ||
        (syllabus.isNotEmpty() && selectedSyllabusIds.containsAll(syllabus.map { it.id }))
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        SyllabusRow(
            item = AiSyllabusItem(
                id = "",
                title = stringResource(R.string.ai_menu_all_topics),
                progressLabel = null,
            ),
            selected = allTopicsSelected,
            onClick = onSelectAllSyllabus,
        )
        if (syllabus.isNotEmpty()) {
            HorizontalDivider(color = BorderGray.copy(alpha = 0.35f))
        }
        syllabus.forEachIndexed { index, item ->
            SyllabusRow(
                item = item,
                selected = !allTopicsSelected && item.id in selectedSyllabusIds,
                onClick = { onSyllabusClick(item) },
            )
            if (index != syllabus.lastIndex) {
                HorizontalDivider(color = BorderGray.copy(alpha = 0.35f))
            }
        }
        Spacer(modifier = Modifier.height(72.dp))
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
private fun AiMenuQuestionNumber(number: Int) {
    Text(
        text = "Q $number",
        color = BrandOrange,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
    )
}

@Composable
private fun AiMenuQuestionContent(item: AiMenuContentItem) {
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
}

@Composable
private fun AiMenuQuestionFooter(
    subtitle: String?,
    modifier: Modifier = Modifier,
) {
    if (subtitle.isNullOrBlank()) {
        Spacer(modifier = modifier.width(1.dp))
        return
    }
    Text(
        text = subtitle,
        modifier = modifier,
        color = TextSecondary,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontFamily = FontFamily.SansSerif,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
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
