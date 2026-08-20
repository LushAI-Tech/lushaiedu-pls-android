package com.lushaiedupls.ui.teacher.secondary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lushaiedupls.R
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.TileSelected

private val TileShape = RoundedCornerShape(18.dp)

@Composable
fun TeacherMoreScreen(
    onBack: (() -> Unit)? = null,
    onAcademicCalendar: () -> Unit,
    onAnnouncements: () -> Unit,
    onAttendance: () -> Unit,
    onTimetable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tiles = listOf(
        MoreTile(
            label = stringResource(R.string.more_academic_calendar),
            icon = Icons.Outlined.CalendarMonth,
            filled = true,
            onClick = onAcademicCalendar,
        ),
        MoreTile(
            label = stringResource(R.string.teacher_more_announcement),
            icon = Icons.Outlined.Campaign,
            filled = true,
            onClick = onAnnouncements,
        ),
        MoreTile(
            label = stringResource(R.string.section_attendance),
            icon = Icons.AutoMirrored.Outlined.FactCheck,
            filled = true,
            onClick = onAttendance,
        ),
        MoreTile(
            label = stringResource(R.string.teacher_more_set_timetable),
            icon = Icons.Outlined.EventAvailable,
            filled = true,
            onClick = onTimetable,
        ),
        MoreTile(label = null, icon = null, filled = false, onClick = {}),
        MoreTile(label = null, icon = null, filled = false, onClick = {}),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            if (onBack != null) {
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
            }
            Text(
                text = stringResource(R.string.more_title),
                modifier = Modifier.align(Alignment.Center),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = BrandBlack,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        tiles.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                row.forEach { tile ->
                    TeacherMoreTileCard(tile = tile, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

private data class MoreTile(
    val label: String?,
    val icon: ImageVector?,
    val filled: Boolean,
    val onClick: () -> Unit,
)

@Composable
private fun TeacherMoreTileCard(
    tile: MoreTile,
    modifier: Modifier = Modifier,
) {
    val bg = if (tile.filled) TileSelected else BgLight
    Column(
        modifier = modifier
            .aspectRatio(0.85f)
            .clip(TileShape)
            .background(bg)
            .then(if (tile.filled) Modifier.clickable(onClick = tile.onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (tile.filled && tile.icon != null && tile.label != null) {
            Icon(
                imageVector = tile.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = tile.label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        }
    }
}
