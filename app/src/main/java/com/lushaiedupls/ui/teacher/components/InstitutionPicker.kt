package com.lushaiedupls.ui.teacher.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.TextSecondary

private val FieldShape = RoundedCornerShape(12.dp)
private val MenuShape = RoundedCornerShape(12.dp)

@Composable
fun InstitutionSelectorDropdown(
    label: String,
    institutions: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Apartment,
) {
    if (institutions.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val canExpand = institutions.size > 1
    val safeIndex = selectedIndex.coerceIn(0, institutions.lastIndex)
    val selectedName = institutions[safeIndex]
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "institutionChevron",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        InstitutionSelectorTrigger(
            label = label,
            selectedName = selectedName,
            enabled = canExpand,
            expanded = expanded,
            chevronRotation = chevronRotation,
            icon = icon,
            onClick = {
                if (canExpand) {
                    expanded = !expanded
                }
            },
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            ) + fadeIn(animationSpec = tween(160)),
            exit = shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = tween(180),
            ) + fadeOut(animationSpec = tween(140)),
        ) {
            InstitutionDropdownMenu(
                institutions = institutions,
                selectedIndex = safeIndex,
                onSelect = { index ->
                    onSelect(index)
                    expanded = false
                },
            )
        }
    }
}

@Composable
private fun InstitutionSelectorTrigger(
    label: String,
    selectedName: String,
    enabled: Boolean,
    expanded: Boolean,
    chevronRotation: Float,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val borderColor = when {
        expanded -> BrandBlack
        else -> BorderGray.copy(alpha = 0.85f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FieldShape)
            .background(BgWhite)
            .border(1.dp, borderColor, FieldShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
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
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                fontFamily = FontFamily.SansSerif,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = selectedName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandBlack,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (enabled) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = if (expanded) BrandBlack else TextSecondary,
                modifier = Modifier
                    .size(22.dp)
                    .rotate(chevronRotation),
            )
        }
    }
}

@Composable
private fun InstitutionDropdownMenu(
    institutions: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .shadow(10.dp, MenuShape, clip = false, ambientColor = BorderGray)
            .clip(MenuShape)
            .background(BgWhite)
            .border(1.dp, BorderGray.copy(alpha = 0.75f), MenuShape),
    ) {
        institutions.forEachIndexed { index, name ->
            val selected = index == selectedIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(index) }
                    .background(if (selected) BgLight else BgWhite)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = name,
                    modifier = Modifier.weight(1f),
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = BrandBlack,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = BrandBlack,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            if (index < institutions.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    color = BorderGray.copy(alpha = 0.35f),
                )
            }
        }
    }
}
