package com.lushaiedupls.ui.fees

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lushaiedupls.R
import com.lushaiedupls.data.mapper.FeeHistoryMappers
import com.lushaiedupls.data.mapper.FeeMonthGroup
import com.lushaiedupls.data.remote.dto.FeeHistoryResponse
import com.lushaiedupls.data.remote.dto.FeeLedgerOut
import com.lushaiedupls.data.remote.dto.FeePaymentStatus
import com.lushaiedupls.ui.parent.formatInrFromPaise
import com.lushaiedupls.ui.parent.formatIsoDate
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.TextSecondary
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val CardShape = RoundedCornerShape(16.dp)
private val PaidGreen = Color(0xFF22C55E)
private val UnpaidRed = Color(0xFFF25F5C)

@Composable
fun FeeHistorySections(
    history: FeeHistoryResponse,
    modifier: Modifier = Modifier,
    showOverallTotals: Boolean = true,
    showMonthTotals: Boolean = true,
) {
    val groups = FeeHistoryMappers.monthGroups(history.rows)
    Column(modifier = modifier.fillMaxWidth()) {
        if (showOverallTotals) {
            Text(
                text = stringResource(
                    R.string.parent_fees_totals,
                    formatInrFromPaise(history.total_amount_paise),
                    formatInrFromPaise(history.paid_amount_paise),
                    formatInrFromPaise(history.pending_amount_paise),
                ),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        groups.forEachIndexed { index, group ->
            if (index > 0) Spacer(modifier = Modifier.height(16.dp))
            FeeMonthSection(group = group, showMonthTotals = showMonthTotals)
        }
    }
}

@Composable
private fun FeeMonthSection(
    group: FeeMonthGroup,
    showMonthTotals: Boolean,
) {
    Text(
        text = formatMonthLabel(group.month),
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = BrandBlack,
        fontFamily = FontFamily.SansSerif,
    )
    if (showMonthTotals) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.parent_fees_month_totals,
                formatInrFromPaise(group.totalPaise),
                formatInrFromPaise(group.paidPaise),
                formatInrFromPaise(group.pendingPaise),
            ),
            color = TextSecondary,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
    group.rows.forEachIndexed { index, row ->
        if (index > 0) Spacer(modifier = Modifier.height(10.dp))
        FeeSubjectRowCard(row = row)
    }
}

@Composable
private fun FeeSubjectRowCard(row: FeeLedgerOut) {
    val isPaid = row.payment_status == FeePaymentStatus.PAID
    val statusColor = if (isPaid) PaidGreen else UnpaidRed
    val statusLabel = when (row.payment_status) {
        FeePaymentStatus.PAID -> {
            val paidOn = formatIsoDate(row.paid_at).takeIf { it.isNotBlank() }
            if (paidOn != null) {
                stringResource(R.string.parent_fee_paid_on, paidOn)
            } else {
                stringResource(R.string.parent_fee_paid)
            }
        }
        FeePaymentStatus.NOT_PAID -> stringResource(R.string.parent_fee_not_paid)
    }
    val title = FeeHistoryMappers.subjectLabel(row)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .border(1.dp, BorderGray.copy(alpha = 0.7f), CardShape)
            .background(BgWhite)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Payments,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                )
                Text(
                    text = formatInrFromPaise(row.amount_paise),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = statusColor,
                    fontFamily = FontFamily.SansSerif,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            val className = row.class_name?.takeIf { it.isNotBlank() }
            Text(
                text = listOfNotNull(className, statusLabel).joinToString(" · "),
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
            )
        }
    }
}

private fun formatMonthLabel(value: String): String {
    val month = runCatching { YearMonth.parse(value) }.getOrNull() ?: return value
    return "${month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${month.year}"
}
