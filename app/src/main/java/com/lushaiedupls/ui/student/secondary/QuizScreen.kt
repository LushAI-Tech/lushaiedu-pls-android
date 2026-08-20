package com.lushaiedupls.ui.student.secondary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.QuizQuestion
import com.lushaiedupls.ui.common.markdown.MarkdownLatexText
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.TextSecondary
import kotlin.math.roundToInt

private val CardShape = RoundedCornerShape(16.dp)
private val OptionShape = RoundedCornerShape(14.dp)
private val ResultGreen = Color(0xFF16A34A)
private val ResultRed = Color(0xFFDC2626)

@Composable
fun QuizScreen(
    questions: List<QuizQuestion>,
    selectedByQuestionId: Map<String, String>,
    isSubmitting: Boolean,
    result: QuizResultUi?,
    errorMessage: String?,
    onSelectAnswer: (questionId: String, answer: String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (result != null) {
        QuizResultsPane(
            result = result,
            onDone = onBack,
            modifier = modifier,
        )
        return
    }

    var index by remember { mutableIntStateOf(0) }
    val question = questions.getOrNull(index) ?: return
    val selected = selectedByQuestionId[question.id]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = BrandBlack,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = stringResource(R.string.quiz_title),
                modifier = Modifier.align(Alignment.Center),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = BrandBlack,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            questions.indices.forEach { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (i <= index) BrandBlack else BgLight),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(BgLight)
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.quiz_question_of, index + 1, questions.size),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(BrandBlack)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = question.difficulty,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                MarkdownLatexText(
                    text = question.prompt,
                    color = BrandBlack,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeightMultiplier = 24f / 16f,
                    enableLinks = false,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            question.options.forEachIndexed { optIndex, option ->
                QuizOptionRow(
                    letter = ('A' + optIndex).toString(),
                    option = option,
                    selected = selected == option,
                    onClick = { onSelectAnswer(question.id, option) },
                )
            }
            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = ResultRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgLight)
                    .clickable(enabled = index > 0 && !isSubmitting) {
                        if (index > 0) index -= 1
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.quiz_back),
                    color = BrandBlack,
                    fontWeight = FontWeight.Bold,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandBlack)
                    .clickable(enabled = !isSubmitting) {
                        if (index < questions.lastIndex) {
                            index += 1
                        } else {
                            onSubmit()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = if (index < questions.lastIndex) {
                            stringResource(R.string.quiz_next)
                        } else {
                            stringResource(R.string.quiz_finish)
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizOptionRow(
    letter: String,
    option: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) BrandBlack else BorderGray,
                    shape = OptionShape,
                )
                .clip(OptionShape)
                .background(BgWhite)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BrandBlack),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = letter, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            MarkdownLatexText(
                text = option,
                color = BrandBlack,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                lineHeightMultiplier = 22f / 15f,
                enableLinks = false,
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(OptionShape)
                .clickable(onClick = onClick),
        )
    }
}

@Composable
private fun QuizResultsPane(
    result: QuizResultUi,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val passed = result.passed ?: (result.percentage >= 50.0)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.quiz_results_title),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = BrandBlack,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .border(1.dp, BorderGray.copy(alpha = 0.75f), CardShape)
                    .background(BgWhite)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "${result.percentage.roundToInt()}%",
                    color = BrandBlack,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (passed) ResultGreen else ResultRed)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (passed) R.string.quiz_passed else R.string.quiz_keep_practicing,
                        ),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ScoreChip(
                        value = result.correct.toString(),
                        label = stringResource(R.string.quiz_correct_count),
                    )
                    ScoreChip(
                        value = result.incorrect.toString(),
                        label = stringResource(R.string.quiz_incorrect_count),
                    )
                    ScoreChip(
                        value = result.unanswered.toString(),
                        label = stringResource(R.string.quiz_unanswered_count),
                    )
                }
            }
            if (result.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.quiz_review),
                    color = BrandBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Spacer(modifier = Modifier.height(10.dp))
                result.items.forEachIndexed { index, item ->
                    ResultAnswerCard(index = index + 1, item = item)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BrandBlack)
                .clickable(onClick = onDone),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.quiz_done),
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ScoreChip(
    value: String,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = BrandBlack, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ResultAnswerCard(
    index: Int,
    item: QuizAnswerResultUi,
) {
    val tint = if (item.isCorrect) ResultGreen else ResultRed
    val showCorrectAnswer = !item.correctAnswer.isNullOrBlank() &&
        !sameAnswer(item.correctAnswer, item.studentAnswer)
    val showExplanation = !item.explanation.isNullOrBlank() &&
        !sameAnswer(item.explanation, item.correctAnswer) &&
        !sameAnswer(item.explanation, item.studentAnswer)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray.copy(alpha = 0.7f), CardShape)
            .clip(CardShape)
            .background(BgWhite)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.quiz_question_n, index),
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(tint)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    text = stringResource(
                        if (item.isCorrect) R.string.quiz_status_correct else R.string.quiz_status_incorrect,
                    ),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        MarkdownLatexText(
            text = item.prompt,
            color = BrandBlack,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            enableLinks = false,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.quiz_your_answer),
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        MarkdownLatexText(
            text = item.studentAnswer.ifBlank { "—" },
            color = tint,
            fontSize = 14.sp,
            enableLinks = false,
        )
        if (showCorrectAnswer) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.quiz_correct_answer),
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            MarkdownLatexText(
                text = item.correctAnswer.orEmpty(),
                color = ResultGreen,
                fontSize = 14.sp,
                enableLinks = false,
            )
        }
        if (showExplanation) {
            Spacer(modifier = Modifier.height(8.dp))
            MarkdownLatexText(
                text = item.explanation.orEmpty(),
                color = BrandBlack,
                fontSize = 13.sp,
                enableLinks = false,
            )
        }
    }
}

private fun sameAnswer(left: String?, right: String?): Boolean {
    val a = left?.trim().orEmpty()
    val b = right?.trim().orEmpty()
    return a.isNotEmpty() && a.equals(b, ignoreCase = true)
}
