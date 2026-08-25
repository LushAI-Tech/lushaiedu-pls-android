package com.lushaiedupls.ui.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lushaiedupls.data.mapper.TeacherUiMappers
import com.lushaiedupls.ui.common.AppBackNav
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.TextSecondary

val AdminCardShape = RoundedCornerShape(16.dp)
val AdminChipShape = RoundedCornerShape(50)
val AdminDeleteRed = Color(0xFFF25F5C)
val AdminPaidGreen = Color(0xFF22C55E)

@Composable
fun AdminScreenHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (onBack != null) {
            AppBackNav(
                onBack = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = BrandBlack,
            fontFamily = FontFamily.SansSerif,
        )
        if (actions != null) {
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions()
            }
        }
    }
}

@Composable
fun AdminFilterRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Text(
                text = label,
                color = if (selected) Color.White else BrandBlack,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier
                    .clip(AdminChipShape)
                    .background(if (selected) BrandBlack else BgLight)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
fun AdminCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AdminCardShape)
            .border(1.dp, BorderGray.copy(alpha = 0.7f), AdminCardShape)
            .background(BgWhite, AdminCardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
fun AdminLeadingIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    background: Color = BgLight,
    tint: Color = BrandBlack,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun AdminSubjectLeadingIcon(
    name: String,
    code: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BrandBlack),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(TeacherUiMappers.subjectIcon(name, code)),
            contentDescription = name,
            modifier = Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(Color.White),
        )
    }
}

@Composable
fun AdminEmptyText(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 14.sp,
        fontFamily = FontFamily.SansSerif,
        modifier = Modifier.padding(vertical = 12.dp),
    )
}

@Composable
fun AdminMuted(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 13.sp,
        fontFamily = FontFamily.SansSerif,
    )
}

@Composable
fun AdminActionRow(
    actions: List<Pair<String, () -> Unit>>,
    destructiveIndex: Int? = null,
) {
    if (actions.isEmpty()) return
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        actions.forEachIndexed { index, (label, onClick) ->
            val destructive = index == destructiveIndex
            Text(
                text = label,
                color = if (destructive) AdminDeleteRed else BrandBlack,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier
                    .clip(AdminChipShape)
                    .border(
                        1.dp,
                        if (destructive) AdminDeleteRed else BorderGray,
                        AdminChipShape,
                    )
                    .clickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}
