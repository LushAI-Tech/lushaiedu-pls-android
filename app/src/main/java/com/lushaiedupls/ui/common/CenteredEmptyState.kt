package com.lushaiedupls.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.TextSecondary

/**
 * Centered empty-state block with icon + message for list/detail pages with no data.
 *
 * Uses a screen-relative min height so the block centers near the middle of the
 * visible screen even when placed inside a vertical scroll column.
 */
@Composable
fun CenteredEmptyState(
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    title: String? = null,
    fillMaxSize: Boolean = false,
    /** Smaller footprint for nested sections / cards (not full-page empties). */
    compact: Boolean = false,
    footer: (@Composable () -> Unit)? = null,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val centeredMinHeight = when {
        compact -> 200.dp
        else -> (screenHeight - 168.dp).coerceAtLeast(360.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (fillMaxSize) {
                    Modifier
                        .fillMaxSize()
                        .heightIn(min = centeredMinHeight)
                } else {
                    Modifier.heightIn(min = centeredMinHeight)
                },
            )
            .padding(horizontal = 28.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BrandOrange.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandOrange,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    color = BrandBlack,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = message,
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
            if (footer != null) {
                Spacer(modifier = Modifier.height(16.dp))
                footer()
            }
        }
    }
}

@Composable
fun CenteredEmptyStateMuted(
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BgLight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandBlack,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                color = TextSecondary,
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )
        }
    }
}
