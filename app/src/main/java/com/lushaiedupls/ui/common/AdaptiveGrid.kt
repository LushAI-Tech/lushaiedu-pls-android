package com.lushaiedupls.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Simple responsive grid built from Rows (matches existing phone layouts).
 */
@Composable
fun <T> AdaptiveChunkedGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    compactColumns: Int = 2,
    mediumColumns: Int = 3,
    expandedColumns: Int = 4,
    horizontalSpacing: Dp = 12.dp,
    verticalSpacing: Dp = 12.dp,
    itemContent: @Composable (item: T, index: Int) -> Unit,
) {
    val columns = rememberLushWindowWidth().gridColumns(
        compact = compactColumns,
        medium = mediumColumns,
        expanded = expandedColumns,
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
    ) {
        items.chunked(columns).forEachIndexed { rowIndex, rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            ) {
                rowItems.forEachIndexed { localIndex, item ->
                    val index = rowIndex * columns + localIndex
                    Box(modifier = Modifier.weight(1f)) {
                        itemContent(item, index)
                    }
                }
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
