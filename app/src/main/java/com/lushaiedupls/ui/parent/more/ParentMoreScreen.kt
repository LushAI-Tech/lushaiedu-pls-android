package com.lushaiedupls.ui.parent.more

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
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
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
fun ParentMoreScreen(
    onTimetable: () -> Unit,
    onFees: () -> Unit,
    onFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tiles = listOf(
        MoreTile(
            label = stringResource(R.string.tab_timetable),
            icon = Icons.Outlined.Schedule,
            filled = true,
            onClick = onTimetable,
        ),
        MoreTile(
            label = stringResource(R.string.parent_fees_title),
            icon = Icons.Outlined.Payments,
            filled = true,
            onClick = onFees,
        ),
        MoreTile(
            label = stringResource(R.string.parent_feedback_title),
            icon = Icons.Outlined.Forum,
            filled = true,
            onClick = onFeedback,
        ),
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
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.more_title),
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
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        }
    }
}
