package com.lushaiedupls.ui.admin.more

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
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VpnKey
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
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.TileSelected

private val TileShape = RoundedCornerShape(18.dp)

@Composable
fun AdminMoreScreen(
    onFees: () -> Unit,
    onFeedback: () -> Unit,
    onInvites: () -> Unit,
    onInstitutions: () -> Unit,
    onPeriods: () -> Unit,
    onCalendar: () -> Unit,
    onAnnouncements: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tiles = listOf(
        MoreTile(stringResource(R.string.admin_fees_title), Icons.Outlined.Payments, onFees),
        MoreTile(stringResource(R.string.admin_feedback_title), Icons.Outlined.Forum, onFeedback),
        MoreTile(stringResource(R.string.admin_invites_title), Icons.Outlined.VpnKey, onInvites),
        MoreTile(stringResource(R.string.admin_institutions_title), Icons.Outlined.Apartment, onInstitutions),
        MoreTile(stringResource(R.string.admin_periods_title), Icons.Outlined.Schedule, onPeriods),
        MoreTile(stringResource(R.string.admin_calendar_title), Icons.Outlined.EventAvailable, onCalendar),
        MoreTile(stringResource(R.string.admin_announce_title), Icons.Outlined.Campaign, onAnnouncements),
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
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
private fun MoreTileCard(
    tile: MoreTile,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .aspectRatio(0.85f)
            .clip(TileShape)
            .background(TileSelected)
            .clickable(onClick = tile.onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
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
