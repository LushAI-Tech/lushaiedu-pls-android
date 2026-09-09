package com.lushaiedupls.ui.auth.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lushaiedupls.R
import com.lushaiedupls.ui.splash.LocalBrandAnchors
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class OrbitShell(
    /** Shared major-axis radius as a fraction of canvas half-size. */
    val radiusFactor: Float,
    /** Ellipse squash — classic atom shells are quite flat. */
    val aspectY: Float,
    /** Shell tilt in the plane of the page (0° / 60° / 120°). */
    val tiltDegrees: Float,
    val color: Color,
)

private data class Electron(
    val shellIndex: Int,
    /** Phase in turns (0..1). */
    val phase: Float,
    val sizeDp: Float,
    val color: Color,
    /** +1 / -1 orbital direction. */
    val direction: Float,
)

private data class ElectronPose(
    val position: Offset,
    /** Negative = behind logo, positive = in front. */
    val depth: Float,
    val sizeDp: Float,
    val color: Color,
)

/**
 * Scientific atom emblem: three Bohr-style elliptical shells at 60° offsets,
 * with electrons that pass behind and in front of the centered logo nucleus.
 */
@Composable
fun AtomOrbitLogo(
    logoSize: Dp,
    modifier: Modifier = Modifier,
) {
    val logoWidth = logoSize * (554f / 736f)
    val canvasSize = logoSize * 1.92f
    val drawnLogoSize = logoSize * 0.78f
    val drawnLogoWidth = logoWidth * 0.78f
    val brandAnchors = LocalBrandAnchors.current
    val transition = rememberInfiniteTransition(label = "science_atom_logo")

    val glowPulse by transition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3_200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "nucleus_glow",
    )

    // Exact 360° cycles keep the loop seamless (cos/sin match at wrap).
    val shell0 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shell_0",
    )
    val shell1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10_500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shell_1",
    )
    val shell2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(13_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shell_2",
    )

    val shellDegrees = remember(shell0, shell1, shell2) {
        floatArrayOf(shell0, shell1, shell2)
    }

    // Iconic ⚛️ geometry: three equal shells, 60° apart.
    val shells = remember {
        listOf(
            OrbitShell(0.78f, 0.38f, 0f, BrandOrange),
            OrbitShell(0.78f, 0.38f, 60f, BrandBlack),
            OrbitShell(0.78f, 0.38f, 120f, BrandOrange),
        )
    }

    val electrons = remember {
        listOf(
            Electron(0, 0.00f, 4.4f, BrandOrange, 1f),
            Electron(0, 0.50f, 3.2f, BrandBlack, 1f),
            Electron(1, 0.18f, 4.4f, BrandBlack, -1f),
            Electron(1, 0.68f, 3.2f, BrandOrange, -1f),
            Electron(2, 0.34f, 4.4f, BrandOrange, 1f),
            Electron(2, 0.84f, 3.2f, BrandBlack, 1f),
        )
    }

    Box(
        modifier = modifier.size(canvasSize),
        contentAlignment = Alignment.Center,
    ) {
        // Back shells + electrons behind the nucleus
        Canvas(modifier = Modifier.size(canvasSize)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val half = size.minDimension / 2f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        BrandOrange.copy(alpha = 0.18f * glowPulse),
                        BrandOrange.copy(alpha = 0.06f * glowPulse),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = half * 0.42f,
                ),
                center = center,
                radius = half * 0.42f,
            )

            drawShellHalves(
                shells = shells,
                glowPulse = glowPulse,
                front = false,
            )

            electronPoses(
                electrons = electrons,
                shells = shells,
                half = half,
                center = center,
                shellDegrees = shellDegrees,
            ).filter { it.depth < 0f }
                .forEach { drawElectron(it, glowPulse) }
        }

        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = stringResource(R.string.cd_app_logo),
            modifier = Modifier
                .width(drawnLogoWidth)
                .height(drawnLogoSize)
                .onGloballyPositioned { coords ->
                    brandAnchors?.logoBounds = coords.boundsInWindow()
                },
            contentScale = ContentScale.Fit,
        )

        // Front shells + electrons in front of the nucleus
        Canvas(modifier = Modifier.size(canvasSize)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val half = size.minDimension / 2f

            drawShellHalves(
                shells = shells,
                glowPulse = glowPulse,
                front = true,
            )

            electronPoses(
                electrons = electrons,
                shells = shells,
                half = half,
                center = center,
                shellDegrees = shellDegrees,
            ).filter { it.depth >= 0f }
                .forEach { drawElectron(it, glowPulse) }
        }
    }
}

private fun DrawScope.drawShellHalves(
    shells: List<OrbitShell>,
    glowPulse: Float,
    front: Boolean,
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val half = size.minDimension / 2f
    val startAngle = if (front) 0f else 180f
    val alphaScale = if (front) 1f else 0.68f

    shells.forEach { shell ->
        val radius = half * shell.radiusFactor
        val ovalWidth = radius * 2f
        val ovalHeight = radius * 2f * shell.aspectY
        val topLeft = Offset(center.x - ovalWidth / 2f, center.y - ovalHeight / 2f)

        rotate(degrees = shell.tiltDegrees, pivot = center) {
            // Fine scientific orbit stroke
            drawArc(
                color = shell.color.copy(alpha = 0.28f * glowPulse * alphaScale),
                startAngle = startAngle,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(ovalWidth, ovalHeight),
                style = Stroke(width = 1.35.dp.toPx(), cap = StrokeCap.Round),
            )
            // Soft outer halo so the shell reads clearly on white
            drawArc(
                color = shell.color.copy(alpha = 0.10f * glowPulse * alphaScale),
                startAngle = startAngle,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(ovalWidth, ovalHeight),
                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

private fun electronPoses(
    electrons: List<Electron>,
    shells: List<OrbitShell>,
    half: Float,
    center: Offset,
    shellDegrees: FloatArray,
): List<ElectronPose> {
    return electrons.map { electron ->
        val shell = shells[electron.shellIndex]
        val radius = half * shell.radiusFactor
        val angleDeg = shellDegrees[electron.shellIndex] * electron.direction + electron.phase * 360f
        val angle = angleDeg * (PI.toFloat() / 180f)

        val localX = cos(angle) * radius
        val localY = sin(angle) * radius * shell.aspectY
        val tiltRad = shell.tiltDegrees * (PI.toFloat() / 180f)
        val cosT = cos(tiltRad)
        val sinT = sin(tiltRad)

        ElectronPose(
            position = Offset(
                x = center.x + localX * cosT - localY * sinT,
                y = center.y + localX * sinT + localY * cosT,
            ),
            depth = sin(angle),
            sizeDp = electron.sizeDp,
            color = electron.color,
        )
    }
}

private fun DrawScope.drawElectron(
    pose: ElectronPose,
    glowPulse: Float,
) {
    // Continuous depth: -1 (fully behind) → +1 (fully in front).
    // Smoothstep keeps shade/size changes soft across the handoff.
    val linearT = ((pose.depth + 1f) * 0.5f).coerceIn(0f, 1f)
    val t = linearT * linearT * (3f - 2f * linearT)

    val shade = 0.52f + 0.48f * t
    val scale = 0.84f + 0.16f * t
    val highlight = 0.10f + 0.36f * t
    val r = pose.sizeDp.dp.toPx() * scale

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                pose.color.copy(alpha = 0.30f * glowPulse * shade),
                Color.Transparent,
            ),
            center = pose.position,
            radius = r * 2.8f,
        ),
        center = pose.position,
        radius = r * 2.8f,
    )
    drawCircle(
        color = pose.color.copy(alpha = 0.94f * shade),
        center = pose.position,
        radius = r,
    )
    drawCircle(
        color = Color.White.copy(alpha = highlight),
        center = Offset(
            x = pose.position.x - r * 0.28f,
            y = pose.position.y - r * 0.28f,
        ),
        radius = r * 0.26f,
    )
}
