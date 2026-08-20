package com.lushaiedupls.ui.common.markdown

import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lushaiedupls.ui.theme.BgLight
import com.lushaiedupls.ui.theme.BrandOrange
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import org.commonmark.node.SoftLineBreak

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
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val textSizePx = with(density) { fontSize.toPx() }
    val textColor = color.toArgb()
    val prepared = remember(text) { MarkdownLatexNormalizer.normalize(text) }
    val markwon = remember(context, textSizePx, textColor) {
        createMarkwon(context, textSizePx, textColor)
    }
    val typeface = remember(fontWeight) {
        when {
            fontWeight >= FontWeight.Bold -> Typeface.create("sans-serif", Typeface.BOLD)
            fontWeight >= FontWeight.Medium -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
            else -> Typeface.SANS_SERIF
        }
    }
    val gravity = when (textAlign) {
        TextAlign.Center -> Gravity.CENTER
        TextAlign.End, TextAlign.Right -> Gravity.END or Gravity.CENTER_VERTICAL
        else -> Gravity.START or Gravity.CENTER_VERTICAL
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                includeFontPadding = false
                setTextIsSelectable(false)
                isFocusable = false
                isClickable = false
                isLongClickable = false
                isFocusableInTouchMode = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = { view ->
            view.setTextColor(textColor)
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
            view.setLineSpacing(0f, lineHeightMultiplier)
            view.typeface = typeface
            view.gravity = gravity
            view.movementMethod = if (enableLinks) LinkMovementMethod.getInstance() else null
            view.linksClickable = enableLinks
            if (!enableLinks) {
                view.setOnTouchListener { _, _ -> false }
            }
            markwon.setMarkdown(view, prepared)
            view.requestLayout()
        },
    )
}

private fun createMarkwon(
    context: android.content.Context,
    textSizePx: Float,
    textColor: Int,
): Markwon = Markwon.builder(context)
    .usePlugin(MarkwonInlineParserPlugin.create())
    .usePlugin(
        JLatexMathPlugin.create(textSizePx) { builder ->
            builder.inlinesEnabled(true)
            builder.blocksEnabled(true)
            builder.theme().inlineTextColor(textColor)
            builder.theme().blockTextColor(textColor)
            builder.errorHandler { _, _ ->
                ColorDrawable(android.graphics.Color.TRANSPARENT)
            }
        },
    )
    .usePlugin(StrikethroughPlugin.create())
    .usePlugin(TablePlugin.create(context))
    .usePlugin(HtmlPlugin.create())
    .usePlugin(
        object : AbstractMarkwonPlugin() {
            override fun configureTheme(builder: MarkwonTheme.Builder) {
                builder
                    .linkColor(BrandOrange.toArgb())
                    .isLinkUnderlined(true)
                    .headingBreakHeight(0)
                    .headingTextSizeMultipliers(floatArrayOf(1.2f, 1.12f, 1.06f, 1f, 1f, 1f))
                    .codeTextColor(textColor)
                    .codeBackgroundColor(BgLight.toArgb())
                    .codeBlockBackgroundColor(BgLight.toArgb())
                    .codeTextSize((textSizePx * 0.92f).toInt().coerceAtLeast(1))
            }

            override fun configureVisitor(builder: MarkwonVisitor.Builder) {
                builder.on(SoftLineBreak::class.java) { visitor, _ ->
                    visitor.forceNewLine()
                }
            }
        },
    )
    .build()
