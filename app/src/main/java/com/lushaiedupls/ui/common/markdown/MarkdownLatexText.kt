package com.lushaiedupls.ui.common.markdown

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lushaiedupls.ui.common.SkeletonBox
import com.lushaiedupls.ui.theme.BrandOrange
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

private const val TagContent = 0x6B617465
private const val TagTheme = 0x7468656D
private const val TagBooted = 0x626F6F74
private const val TagMeasure = 0x6D656173

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun MarkdownLatexText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    lineHeightMultiplier: Float = 1.25f,
    enableLinks: Boolean = true,
    textAlign: TextAlign = TextAlign.Start,
    onClick: (() -> Unit)? = null,
    placeholderUntilReady: Boolean = false,
    onReady: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val cssFontSizePx = fontSize.value * density.fontScale
    val prepared = remember(text) { MarkdownLatexNormalizer.normalize(text) }
    val heightState = remember { mutableIntStateOf(0) }
    var heightPx by heightState
    val measureTokenState = remember { mutableIntStateOf(0) }
    var measureToken by measureTokenState
    var containerWidthPx by remember { mutableIntStateOf(0) }
    val ready = heightPx > 1
    val estimatedHeightPx = remember(prepared, cssFontSizePx, lineHeightMultiplier) {
        val wrappedLines = prepared.split('\n').sumOf { line ->
            (line.length / 42).coerceAtLeast(1)
        }.coerceAtLeast(1)
        (cssFontSizePx * lineHeightMultiplier * wrappedLines).toInt().coerceIn(24, 2400)
    }
    val layoutHeightPx = if (ready) heightPx else estimatedHeightPx
    val layoutHeightDp = with(density) { layoutHeightPx.coerceAtLeast(1).toDp() }
    val cssColor = remember(color) { colorToCss(color) }
    val linkCssColor = remember { colorToCss(BrandOrange) }
    val alignCss = when (textAlign) {
        TextAlign.Center -> "center"
        TextAlign.End, TextAlign.Right -> "right"
        else -> "left"
    }
    val weightCss = when {
        fontWeight >= FontWeight.Bold -> 700
        fontWeight >= FontWeight.SemiBold -> 600
        fontWeight >= FontWeight.Medium -> 500
        else -> 400
    }
    val onClickRef = remember { AtomicReference<(() -> Unit)?>(onClick) }
    onClickRef.set(onClick)
    val onReadyRef = remember { AtomicReference<(() -> Unit)?>(onReady) }
    onReadyRef.set(onReady)
    val enableLinksRef = remember { AtomicReference(enableLinks) }
    enableLinksRef.set(enableLinks)
    val heightRef = remember { AtomicReference(heightState) }
    heightRef.set(heightState)

    LaunchedEffect(Unit) {
        KatexRenderer.prewarm(context)
    }
    LaunchedEffect(prepared) {
        // Content change must restart the height contract (grow and shrink).
        heightPx = 0
        measureToken += 1
    }
    LaunchedEffect(ready) {
        if (ready) onReadyRef.get()?.invoke()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(layoutHeightDp)
            .onSizeChanged { size ->
                val width = size.width
                if (width <= 1) return@onSizeChanged
                if (containerWidthPx == 0) {
                    containerWidthPx = width
                    return@onSizeChanged
                }
                if (abs(width - containerWidthPx) > 1) {
                    containerWidthPx = width
                    heightPx = 0
                    measureToken += 1
                }
            },
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(layoutHeightDp)
                .alpha(if (ready) 1f else 0f),
            factory = { viewContext ->
                WebView(viewContext).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    overScrollMode = WebView.OVER_SCROLL_NEVER
                    isNestedScrollingEnabled = false
                    clipToOutline = false
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = false
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.blockNetworkLoads = true
                    settings.loadsImagesAutomatically = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    settings.setSupportZoom(false)
                    settings.builtInZoomControls = false
                    settings.displayZoomControls = false
                    settings.useWideViewPort = false
                    settings.loadWithOverviewMode = false
                    settings.textZoom = 100
                    addJavascriptInterface(
                        KatexHeightBridge { reportedPx ->
                            if (reportedPx > 1) {
                                post {
                                    val state = heightRef.get()
                                    val current = state.intValue
                                    // Industrial hysteresis: accept grow and shrink, ignore 1px jitter.
                                    if (current == 0 || abs(reportedPx - current) > 1) {
                                        state.intValue = reportedPx
                                    }
                                }
                            }
                        },
                        "AndroidBridge",
                    )
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            if (!KatexRenderer.isKatexUrl(url)) return
                            val markdown = view.getTag(TagContent) as? String ?: return
                            val theme = view.getTag(TagTheme) as? ThemePayload ?: return
                            applyKatexContent(view, markdown, theme)
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            val uri = request.url ?: return true
                            if (!enableLinksRef.get() || onClickRef.get() != null) return true
                            return openExternalUri(view, uri)
                        }

                        @Deprecated("Deprecated in Java")
                        override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
                            if (url.isNullOrBlank()) return true
                            if (!enableLinksRef.get() || onClickRef.get() != null) return true
                            return openExternalUri(view, Uri.parse(url))
                        }
                    }
                    setOnTouchListener { v, event ->
                        val click = onClickRef.get()
                        if (click != null) {
                            if (event.action == MotionEvent.ACTION_UP) {
                                v.performClick()
                                click()
                            }
                            true
                        } else {
                            false
                        }
                    }
                }
            },
            update = { webView ->
                val theme = ThemePayload(
                    colorCss = cssColor,
                    fontSizePx = cssFontSizePx,
                    lineHeight = lineHeightMultiplier,
                    fontWeight = weightCss,
                    textAlign = alignCss,
                    linkColorCss = linkCssColor,
                )
                val prevMarkdown = webView.getTag(TagContent) as? String
                val prevTheme = webView.getTag(TagTheme) as? ThemePayload
                val prevToken = webView.getTag(TagMeasure) as? Int
                webView.setTag(TagContent, prepared)
                webView.setTag(TagTheme, theme)
                webView.setTag(TagMeasure, measureToken)
                val booted = webView.getTag(TagBooted) as? Boolean == true
                val contentChanged = prevMarkdown != prepared ||
                    prevTheme != theme ||
                    prevToken != measureToken
                if (!booted) {
                    webView.setTag(TagBooted, true)
                    webView.loadDataWithBaseURL(
                        KatexRenderer.BASE_URL,
                        KatexRenderer.document(webView.context, prepared, themeJson(theme)),
                        "text/html",
                        "utf-8",
                        null,
                    )
                } else if (KatexRenderer.isKatexUrl(webView.url) && contentChanged) {
                    applyKatexContent(webView, prepared, theme)
                }
            },
            onRelease = { webView ->
                webView.removeJavascriptInterface("AndroidBridge")
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.destroy()
            },
        )
        if (placeholderUntilReady && !ready) {
            SkeletonBox(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
            )
        }
    }
}

private data class ThemePayload(
    val colorCss: String,
    val fontSizePx: Float,
    val lineHeight: Float,
    val fontWeight: Int,
    val textAlign: String,
    val linkColorCss: String,
)

private class KatexHeightBridge(
    private val onHeightChanged: (Int) -> Unit,
) {
    @JavascriptInterface
    fun onHeight(heightPx: Int) {
        onHeightChanged(heightPx)
    }
}

private fun themeJson(theme: ThemePayload): String = JSONObject()
    .put("color", theme.colorCss)
    .put("fontSizePx", theme.fontSizePx.toDouble())
    .put("lineHeight", theme.lineHeight.toDouble())
    .put("fontWeight", theme.fontWeight)
    .put("textAlign", theme.textAlign)
    .put("linkColor", theme.linkColorCss)
    .toString()

private fun applyKatexContent(
    view: WebView,
    markdown: String,
    theme: ThemePayload,
) {
    val mdLiteral = JSONObject.quote(markdown)
    view.evaluateJavascript(
        "window.setTheme && setTheme(${themeJson(theme)}); window.renderMarkdown && renderMarkdown($mdLiteral);",
        null,
    )
}

private fun openExternalUri(view: WebView, uri: Uri): Boolean {
    val scheme = uri.scheme?.lowercase()
    if (scheme != "http" && scheme != "https" && scheme != "mailto") return true
    return try {
        view.context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        true
    } catch (_: Exception) {
        true
    }
}

private fun colorToCss(color: Color): String {
    val argb = color.toArgb()
    val a = ((argb ushr 24) and 0xFF) / 255f
    val r = (argb ushr 16) and 0xFF
    val g = (argb ushr 8) and 0xFF
    val b = argb and 0xFF
    return if (a >= 0.999f) {
        String.format("#%02X%02X%02X", r, g, b)
    } else {
        String.format("rgba(%d,%d,%d,%.3f)", r, g, b, a)
    }
}
