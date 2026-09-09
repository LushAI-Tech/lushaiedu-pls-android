package com.lushaiedupls.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lushaiedupls.R
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// System splash icon — must match splash_icon.xml for a seamless first handoff.
private val SystemSplashLogoBoxWidth = 144.dp
private val SystemSplashLogoBoxHeight = 192.dp
private val SystemSplashLogoPaddingH = 44.dp
private val SystemSplashLogoPaddingV = 56.dp

// Target animated splash logo size (after smooth grow from system size).
private val SplashLogoBoxWidth = 168.dp
private val SplashLogoBoxHeight = 224.dp
private val SplashLogoPaddingH = 28.dp
private val SplashLogoPaddingV = 36.dp

@Composable
fun LushSplashScreen(
    onFinished: () -> Unit,
    onSystemSplashReady: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val currentOnFinished by rememberUpdatedState(onFinished)
    val currentOnSystemSplashReady by rememberUpdatedState(onSystemSplashReady)
    val brandAnchors = LocalBrandAnchors.current
    val density = LocalDensity.current

    val introSizeProgress = remember { Animatable(0f) }
    val wordmarkAlpha = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }
    val backgroundAlpha = remember { Animatable(1f) }
    val handoffProgress = remember { Animatable(0f) }

    var rootWindowBounds by remember { mutableStateOf<Rect?>(null) }
    var splashLogoWindow by remember { mutableStateOf<Rect?>(null) }
    var splashLogoImageWindow by remember { mutableStateOf<Rect?>(null) }
    var splashWordmarkWindow by remember { mutableStateOf<Rect?>(null) }
    var systemSplashReported by remember { mutableStateOf(false) }

    var showWordmark by remember { mutableStateOf(false) }
    var handoffActive by remember { mutableStateOf(false) }
    var startLogoLocal by remember { mutableStateOf<Rect?>(null) }
    var startWordmarkLocal by remember { mutableStateOf<Rect?>(null) }
    var endLogoLocal by remember { mutableStateOf<Rect?>(null) }
    var endWordmarkLocal by remember { mutableStateOf<Rect?>(null) }

    val introProgress = introSizeProgress.value
    val logoBoxWidth = lerpDp(SystemSplashLogoBoxWidth, SplashLogoBoxWidth, introProgress)
    val logoBoxHeight = lerpDp(SystemSplashLogoBoxHeight, SplashLogoBoxHeight, introProgress)
    val logoPaddingH = lerpDp(SystemSplashLogoPaddingH, SplashLogoPaddingH, introProgress)
    val logoPaddingV = lerpDp(SystemSplashLogoPaddingV, SplashLogoPaddingV, introProgress)

    LaunchedEffect(Unit) {
        while (!systemSplashReported) {
            delay(16)
        }
        delay(32)

        showWordmark = true
        coroutineScope {
            launch {
                introSizeProgress.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
            }
            launch {
                wordmarkAlpha.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
            }
        }
        delay(600)

        val deadline = System.nanoTime() + 900_000_000L
        while (
            (brandAnchors?.logoBounds == null || brandAnchors.wordmarkBounds == null) &&
            System.nanoTime() < deadline
        ) {
            delay(16)
        }
        // One frame so Welcome layout and anchor bounds are settled.
        delay(32)

        val root = rootWindowBounds
        val fromLogo = splashLogoImageWindow ?: splashLogoWindow
        val fromWordmark = splashWordmarkWindow
        val toLogo = brandAnchors?.logoBounds
        val toWordmark = brandAnchors?.wordmarkBounds

        if (root != null && fromLogo != null && fromWordmark != null && toLogo != null && toWordmark != null) {
            startLogoLocal = fromLogo.toLocal(root)
            startWordmarkLocal = fromWordmark.toLocal(root)
            endLogoLocal = toLogo.toLocal(root)
            endWordmarkLocal = toWordmark.toLocal(root)
            handoffActive = true

            handoffProgress.snapTo(0f)
            handoffProgress.animateTo(
                1f,
                tween(720, easing = FastOutSlowInEasing),
            )

            // Hold at exact Welcome positions before revealing Welcome underneath.
            delay(160)
            coroutineScope {
                launch {
                    exitAlpha.animateTo(0f, tween(280, easing = FastOutSlowInEasing))
                }
                launch {
                    backgroundAlpha.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
                }
            }
        } else {
            coroutineScope {
                launch {
                    exitAlpha.animateTo(0f, tween(360, easing = FastOutSlowInEasing))
                }
                launch {
                    delay(60)
                    backgroundAlpha.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
                }
            }
        }

        currentOnFinished()
    }

    val progress = handoffProgress.value
    val logoRect = if (handoffActive) {
        lerpRect(startLogoLocal!!, endLogoLocal!!, progress)
    } else {
        null
    }
    val wordmarkRect = if (handoffActive) {
        lerpRect(startWordmarkLocal!!, endWordmarkLocal!!, progress)
    } else {
        null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { rootWindowBounds = it.boundsInWindow() },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = backgroundAlpha.value }
                .background(BgWhite),
        )

        if (!handoffActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(logoBoxWidth, logoBoxHeight)
                    .graphicsLayer { alpha = exitAlpha.value }
                    .onGloballyPositioned { coords ->
                        splashLogoWindow = coords.boundsInWindow()
                        if (!systemSplashReported && introSizeProgress.value == 0f) {
                            systemSplashReported = true
                            currentOnSystemSplashReady()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = logoPaddingH, vertical = logoPaddingV)
                        .onGloballyPositioned { coords ->
                            splashLogoImageWindow = coords.boundsInWindow()
                        },
                    contentScale = ContentScale.Fit,
                )
            }

            if (showWordmark) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(logoBoxHeight + 12.dp))
                    SplashWordmark(
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = wordmarkAlpha.value * exitAlpha.value
                            }
                            .onGloballyPositioned {
                                splashWordmarkWindow = it.boundsInWindow()
                            },
                    )
                }
            }
        } else if (logoRect != null && wordmarkRect != null) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(logoRect.left.roundToInt(), logoRect.top.roundToInt())
                    }
                    .size(
                        width = with(density) { logoRect.width.toDp() },
                        height = with(density) { logoRect.height.toDp() },
                    )
                    .graphicsLayer { alpha = exitAlpha.value },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(wordmarkRect.left.roundToInt(), wordmarkRect.top.roundToInt())
                    }
                    .size(
                        width = with(density) { wordmarkRect.width.toDp() },
                        height = with(density) { wordmarkRect.height.toDp() },
                    )
                    .graphicsLayer { alpha = exitAlpha.value },
                contentAlignment = Alignment.Center,
            ) {
                SplashWordmark()
            }
        }
    }
}

@Composable
private fun SplashWordmark(
    modifier: Modifier = Modifier,
    fontSizeSp: Int = 32,
) {
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = BrandBlack,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                ),
            ) {
                append("Lush")
            }
            withStyle(
                SpanStyle(
                    color = BrandOrange,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                ),
            ) {
                append("AI")
            }
            withStyle(
                SpanStyle(
                    color = BrandBlack,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                ),
            ) {
                append("Edu")
            }
        },
        fontSize = fontSizeSp.sp,
        textAlign = TextAlign.Center,
        letterSpacing = (-0.5).sp,
        modifier = modifier,
    )
}

private fun lerpDp(start: Dp, end: Dp, fraction: Float): Dp {
    return (start.value + (end.value - start.value) * fraction).dp
}

private fun Rect.toLocal(rootWindow: Rect): Rect {
    return Rect(
        left = left - rootWindow.left,
        top = top - rootWindow.top,
        right = right - rootWindow.left,
        bottom = bottom - rootWindow.top,
    )
}

/** Interpolates position and size so [progress] = 1 matches [end] exactly. */
private fun lerpRect(start: Rect, end: Rect, progress: Float): Rect {
    return Rect(
        left = start.left + (end.left - start.left) * progress,
        top = start.top + (end.top - start.top) * progress,
        right = start.right + (end.right - start.right) * progress,
        bottom = start.bottom + (end.bottom - start.bottom) * progress,
    )
}
