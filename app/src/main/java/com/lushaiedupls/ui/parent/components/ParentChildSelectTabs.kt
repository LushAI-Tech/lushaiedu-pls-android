package com.lushaiedupls.ui.parent.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lushaiedupls.R
import com.lushaiedupls.data.remote.dto.LinkedStudentOut
import com.lushaiedupls.ui.common.AnimatedFilterChipRow
import com.lushaiedupls.ui.theme.BrandBlack

@Composable
fun ParentChildChipRow(
    children: List<LinkedStudentOut>,
    selectedStudentId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (children.isEmpty()) return
    val selectedIndex = children
        .indexOfFirst { it.student.id == selectedStudentId }
        .coerceAtLeast(0)
    AnimatedFilterChipRow(
        options = children.map { it.student.name },
        selectedIndex = selectedIndex,
        onSelect = { index -> onSelect(children[index].student.id) },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun ParentAttendanceChildHeader(
    children: List<LinkedStudentOut>,
    selectedStudentId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp),
    ) {
        Text(
            text = stringResource(R.string.attendance_title),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        if (children.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            ParentChildChipRow(
                children = children,
                selectedStudentId = selectedStudentId,
                onSelect = onSelect,
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
fun ParentChildSelectTabs(
    children: List<LinkedStudentOut>,
    selectedStudentId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ParentChildChipRow(
        children = children,
        selectedStudentId = selectedStudentId,
        onSelect = onSelect,
        modifier = modifier.padding(horizontal = 12.dp, vertical = 12.dp),
    )
}
