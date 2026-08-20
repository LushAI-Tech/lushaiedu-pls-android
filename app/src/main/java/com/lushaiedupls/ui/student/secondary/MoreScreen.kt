package com.lushaiedupls.ui.student.secondary

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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lushaiedupls.R
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.ui.common.ApprovalNeededPanel
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.TileSelected

private val TileShape = RoundedCornerShape(18.dp)

@Composable
fun MoreRoute(
    studentRepository: StudentRepository,
    onAcademicCalendar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoreViewModel = viewModel(
        factory = MoreViewModel.provideFactory(studentRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MoreScreen(
        onAcademicCalendar = onAcademicCalendar,
        needsApproval = uiState.needsApproval,
        modifier = modifier,
    )
}

@Composable
fun MoreScreen(
    onAcademicCalendar: () -> Unit,
    needsApproval: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (needsApproval) {
        ApprovalNeededPanel(
            screenTitle = stringResource(R.string.more_title),
            featureLabel = stringResource(R.string.approval_needed_feature_more),
            modifier = modifier,
        )
    } else {
        MoreTilesContent(
            onAcademicCalendar = onAcademicCalendar,
            modifier = modifier,
        )
    }
}

@Composable
private fun MoreTitle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.more_title),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = BrandBlack,
        )
    }
}

@Composable
private fun MoreTilesContent(
    onAcademicCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tiles = listOf(
        MoreTile(
            label = stringResource(R.string.more_academic_calendar),
            icon = Icons.Outlined.CalendarMonth,
            filled = true,
            onClick = onAcademicCalendar,
        ),
        MoreTile(label = null, icon = null, filled = false, onClick = {}),
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
        MoreTitle()
        Spacer(modifier = Modifier.height(12.dp))
        tiles.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                row.forEach { tile ->
                    MoreTileCard(tile = tile, modifier = Modifier.weight(1f))
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
private fun MoreTileCard(
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
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        }
    }
}
