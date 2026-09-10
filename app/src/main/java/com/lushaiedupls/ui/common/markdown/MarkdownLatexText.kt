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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lushaiedupls.ui.theme.BrandOrange
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicReference

private const val KatexRenderUrl = "file:///android_asset/katex/render.html"
private const val TagContent = 0x6B617465
private const val TagTheme = 0x7468656D

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
) {
    val density = LocalDensity.current
    val cssFontSizePx = fontSize.value * density.fontScale
    val prepared = remember(text) { MarkdownLatexNormalizer.normalize(text) }
    val heightState = remember { mutableIntStateOf(0) }
    val ready = heightState.intValue > 0
    val contentHeightDp = with(density) { heightState.intValue.coerceAtLeast(1).toDp() }
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
    val enableLinksRef = remember { AtomicReference(enableLinks) }
    enableLinksRef.set(enableLinks)

    Box(modifier = modifier.fillMaxWidth()) {
        // Pre-display while KaTeX WebView measures (never leave the bubble blank).
        if (!ready) {
            Text(
                text = prepared,
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontFamily = FontFamily.SansSerif,
                textAlign = textAlign,
                lineHeight = fontSize * lineHeightMultiplier,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                // Keep a measurable height so the WebView can load/JS-measure;
                // hide it until the real content height arrives.
                .height(if (ready) contentHeightDp else 1.dp)
                .alpha(if (ready) 1f else 0f),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    overScrollMode = WebView.OVER_SCROLL_NEVER
                    isNestedScrollingEnabled = false
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = false
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    settings.loadsImagesAutomatically = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    settings.setSupportZoom(false)
                    settings.builtInZoomControls = false
                    settings.displayZoomControls = false
                    settings.useWideViewPort = false
                    settings.loadWithOverviewMode = false
                    settings.textZoom = 100
                    addJavascriptInterface(
                        KatexHeightBridge { heightPx ->
                            if (heightPx > 0) {
                                post {
                                    if (heightState.intValue != heightPx) {
                                        heightState.intValue = heightPx
                                    }
                                }
                            }
                        },
                        "AndroidBridge",
                    )
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
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
                    loadUrl(KatexRenderUrl)
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
                webView.setTag(TagContent, prepared)
                webView.setTag(TagTheme, theme)
                if (prevMarkdown != prepared || prevTheme != theme) {
                    if (webView.url == KatexRenderUrl) {
                        applyKatexContent(webView, prepared, theme)
                    }
                }
            },
            onRelease = { webView ->
                webView.removeJavascriptInterface("AndroidBridge")
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.destroy()
            },
        )
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

private fun applyKatexContent(
    view: WebView,
    markdown: String,
    theme: ThemePayload,
) {
    val themeJson = JSONObject()
        .put("color", theme.colorCss)
        .put("fontSizePx", theme.fontSizePx.toDouble())
        .put("lineHeight", theme.lineHeight.toDouble())
        .put("fontWeight", theme.fontWeight)
        .put("textAlign", theme.textAlign)
        .put("linkColor", theme.linkColorCss)
        .toString()
    val mdLiteral = JSONObject.quote(markdown)
    view.evaluateJavascript(
        "window.setTheme && setTheme($themeJson); window.renderMarkdown && renderMarkdown($mdLiteral);",
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
