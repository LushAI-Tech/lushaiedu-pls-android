package com.lushaiedupls.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.TextSecondary
import kotlin.math.abs

private val ChipTrackShape = RoundedCornerShape(50)
private val ChipSpring = spring<Float>(dampingRatio = 0.78f, stiffness = 380f)

private data class ChipRect(val x: Float, val width: Float)

@Composable
fun AnimatedFilterChipRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
) {
    if (options.isEmpty()) return
    val safeIndex = selectedIndex.coerceIn(0, options.lastIndex)
    val positions = remember { mutableStateMapOf<Int, ChipRect>() }
    var measured by remember { mutableStateOf(false) }
    SideEffect {
        positions.keys.filter { it >= options.size }.forEach { positions.remove(it) }
    }
    val target = positions[safeIndex]
    val animationSpec = if (measured) ChipSpring else snap()
    val animatedX by animateFloatAsState(
        targetValue = target?.x ?: 0f,
        animationSpec = animationSpec,
        label = "filterChipX",
    )
    val animatedW by animateFloatAsState(
        targetValue = target?.width ?: 0f,
        animationSpec = animationSpec,
        label = "filterChipW",
    )
    SideEffect {
        if (target != null) measured = true
    }
    Column(modifier = modifier.fillMaxWidth()) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                fontFamily = FontFamily.SansSerif,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ChipTrackShape)
                .background(BgLight)
                .horizontalScroll(rememberScrollState())
                .padding(4.dp)
                .drawBehind {
                    if (animatedW > 0f) {
                        drawRoundRect(
                            color = BrandBlack,
                            topLeft = Offset(animatedX, 0f),
                            size = Size(animatedW, size.height),
                            cornerRadius = CornerRadius(size.height / 2f),
                        )
                    }
                },
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                options.forEachIndexed { index, name ->
                    key(index, name) {
                        val selected = index == safeIndex
                        val textColor by animateColorAsState(
                            targetValue = if (selected) Color.White else BrandBlack,
                            animationSpec = tween(220),
                            label = "filterChipText",
                        )
                        val interactionSource = remember { MutableInteractionSource() }
                        val pressed by interactionSource.collectIsPressedAsState()
                        val pressScale by animateFloatAsState(
                            targetValue = if (pressed) 0.94f else 1f,
                            animationSpec = spring(dampingRatio = 0.72f, stiffness = 500f),
                            label = "filterChipPress",
                        )
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .onGloballyPositioned { coords ->
                                    val parent = coords.parentCoordinates?.parentCoordinates
                                    val offset = if (parent != null && parent.isAttached && coords.isAttached) {
                                        parent.localPositionOf(coords, Offset.Zero)
                                    } else {
                                        coords.positionInParent()
                                    }
                                    val width = coords.size.width.toFloat()
                                    if (width < 1f) return@onGloballyPositioned
                                    val next = ChipRect(x = offset.x, width = width)
                                    val prev = positions[index]
                                    if (prev == null ||
                                        abs(prev.x - next.x) > 0.5f ||
                                        abs(prev.width - next.width) > 0.5f
                                    ) {
                                        positions[index] = next
                                    }
                                }
                                .clickable(
                                    enabled = enabled,
                                    interactionSource = interactionSource,
                                    indication = null,
                                ) { onSelect(index) }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = name,
                                color = textColor,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.SansSerif,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = pressScale
                                    scaleY = pressScale
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterRowListLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = BrandBlack,
            strokeWidth = 3.dp,
            modifier = Modifier.size(32.dp),
        )
    }
}
