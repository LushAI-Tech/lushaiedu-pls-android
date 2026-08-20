package com.lushaiedupls.ui.teacher.overlays

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.TeacherDayPeriod
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.TextSecondary

private val CardShape = RoundedCornerShape(20.dp)
private val FieldShape = RoundedCornerShape(14.dp)
private val PillShape = RoundedCornerShape(50)
private val ChipShape = RoundedCornerShape(50)
private val DangerRed = Color(0xFFF25F5C)
private val EmphasizedGray = Color(0xFF4B5563)

@Composable
fun TeacherScrimDialog(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clickable(enabled = false) {},
            ) {
                content()
            }
        }
    }
}

@Composable
fun AddStudentOverlay(
    onDismiss: () -> Unit,
    onSendInvites: (String) -> Unit = {},
) {
    val context = LocalContext.current
    var phones by remember { mutableStateOf("") }
    val sentMessage = stringResource(R.string.teacher_invites_sent)

    TeacherScrimDialog(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(BgWhite)
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(R.string.teacher_phone_number),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BrandBlack,
            )
            Spacer(modifier = Modifier.height(10.dp))
            OverlayTextField(
                value = phones,
                onValueChange = { phones = it },
                placeholder = stringResource(R.string.teacher_phone_numbers_hint),
                minHeight = 120.dp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryPillButton(
                label = stringResource(R.string.teacher_send_invites),
                onClick = {
                    onSendInvites(phones)
                    Toast.makeText(context, sentMessage, Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InviteParentOverlay(
    studentName: String,
    onDismiss: () -> Unit,
    onLink: () -> Unit = {},
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("Guardian") }
    val relationships = listOf("Guardian", "Father", "Mother", "Other")
    val linkedMessage = stringResource(R.string.teacher_parent_linked)

    TeacherScrimDialog(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(BgWhite)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.teacher_invite_parent_title),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = BrandBlack,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderGray.copy(alpha = 0.7f), CardShape)
                    .background(BgWhite, CardShape)
                    .padding(14.dp),
            ) {
                OverlayTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = stringResource(R.string.teacher_invite_parent_hint),
                    minHeight = 48.dp,
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmphasizedGray)
                        .clickable { },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.teacher_invite_parent_for, studentName),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.teacher_no_org_parents),
                color = TextSecondary,
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
            )
            Spacer(modifier = Modifier.height(14.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                relationships.forEach { label ->
                    SelectChip(
                        label = label,
                        selected = relationship == label,
                        onClick = { relationship = label },
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            PrimaryPillButton(
                label = stringResource(R.string.teacher_link_parent_student),
                onClick = {
                    onLink()
                    Toast.makeText(context, linkedMessage, Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
            )
        }
    }
}

@Composable
fun SetReminderOverlay(
    onDismiss: () -> Unit,
    onSave: (enabled: Boolean, minutes: Int) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(true) }
    var minutes by remember { mutableStateOf("30") }
    val savedMessage = stringResource(R.string.teacher_reminders_saved)

    TeacherScrimDialog(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(BgWhite)
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.teacher_reminder_enabled),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BrandBlack,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = BrandOrange,
                        checkedThumbColor = Color.White,
                    ),
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.teacher_minutes_before_lesson),
                fontSize = 14.sp,
                color = BrandBlack,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OverlayTextField(
                value = minutes,
                onValueChange = { value ->
                    if (value.length <= 3 && value.all { it.isDigit() }) minutes = value
                },
                placeholder = "30",
                minHeight = 48.dp,
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryPillButton(
                label = stringResource(R.string.teacher_save_reminders),
                onClick = {
                    onSave(enabled, minutes.toIntOrNull() ?: 30)
                    Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.teacher_reminder_device_note),
                    color = TextSecondary,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetSessionOverlay(
    subjects: List<String>,
    onDismiss: () -> Unit,
    onDone: (subject: String, room: String) -> Unit = { _, _ -> },
    onClear: () -> Unit = {},
) {
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull().orEmpty()) }
    var room by remember { mutableStateOf("") }

    TeacherScrimDialog(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(BgWhite)
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(R.string.teacher_set_session_title),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = BrandBlack,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.teacher_session_subject),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = BrandBlack,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                subjects.forEach { subject ->
                    SelectChip(
                        label = subject,
                        selected = selectedSubject == subject,
                        onClick = { selectedSubject = subject },
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.teacher_session_room),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = BrandBlack,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OverlayTextField(
                value = room,
                onValueChange = { room = it },
                placeholder = stringResource(R.string.teacher_session_room_hint),
                minHeight = 48.dp,
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, DangerRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable {
                            onClear()
                            onDismiss()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.teacher_clear_slot),
                        color = DangerRed,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgLight)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.danger_zone_cancel),
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandBlack)
                        .clickable {
                            onDone(selectedSubject, room)
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.teacher_session_done),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun DayDetailOverlay(
    periods: List<TeacherDayPeriod>,
    onDismiss: () -> Unit,
) {
    TeacherScrimDialog(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .clip(CardShape)
                .background(BgWhite)
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(R.string.teacher_day_detail_title),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = BrandBlack,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.teacher_day_detail_subtitle),
                color = TextSecondary,
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                periods.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { period ->
                            DayPeriodCard(period = period, modifier = Modifier.weight(1f))
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayPeriodCard(
    period: TeacherDayPeriod,
    modifier: Modifier = Modifier,
) {
    val bg = if (period.emphasized) EmphasizedGray else BgWhite
    val fg = if (period.emphasized) Color.White else BrandBlack
    val secondary = if (period.emphasized) Color.White.copy(alpha = 0.8f) else TextSecondary
    Column(
        modifier = modifier
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (period.emphasized) {
                    Modifier.background(bg)
                } else {
                    Modifier
                        .border(1.dp, BorderGray.copy(alpha = 0.75f), RoundedCornerShape(14.dp))
                        .background(bg)
                },
            )
            .padding(12.dp),
    ) {
        Text(text = period.timeLabel, color = secondary, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = period.title,
            color = fg,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
        if (period.attendanceLabel != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = period.attendanceLabel, color = secondary, fontSize = 11.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun OverlayTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minHeight: androidx.compose.ui.unit.Dp,
    singleLine: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(FieldShape)
            .border(1.dp, BorderGray.copy(alpha = 0.7f), FieldShape)
            .background(BgLight)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(text = placeholder, color = TextSecondary, fontSize = 14.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            cursorBrush = SolidColor(BrandBlack),
            textStyle = TextStyle(
                color = BrandBlack,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SelectChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(ChipShape)
            .border(
                width = 1.dp,
                color = if (selected) BrandBlack else BorderGray,
                shape = ChipShape,
            )
            .background(BgWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 13.sp,
            color = if (selected) BrandBlack else TextSecondary,
        )
    }
}

@Composable
private fun PrimaryPillButton(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(PillShape)
            .background(BrandBlack)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )
    }
}
