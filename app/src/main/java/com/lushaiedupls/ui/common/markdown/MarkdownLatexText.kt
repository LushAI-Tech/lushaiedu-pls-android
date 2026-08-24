package com.lushaiedupls.ui.common.markdown

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
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
    onClick: (() -> Unit)? = null,
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
            view.movementMethod = if (enableLinks && onClick == null) LinkMovementMethod.getInstance() else null
            view.linksClickable = enableLinks && onClick == null
            if (onClick != null) {
                view.isClickable = true
                view.setOnClickListener { onClick() }
                view.setOnTouchListener(null)
            } else if (!enableLinks) {
                view.isClickable = false
                view.setOnClickListener(null)
                view.setOnTouchListener { _, _ -> false }
            } else {
                view.isClickable = true
                view.setOnClickListener(null)
                view.setOnTouchListener(null)
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
            builder.errorHandler { latex, _ ->
                FallbackLatexDrawable(latex, textColor, textSizePx)
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
                    .headingTextSizeMultipliers(floatArrayOf(1.28f, 1.18f, 1.10f, 1.04f, 1f, 1f))
                    .blockMargin((textSizePx * 0.75f).toInt())
                    .blockQuoteWidth((textSizePx * 0.22f).toInt().coerceAtLeast(3))
                    .blockQuoteColor(BrandOrange.toArgb())
                    .codeTextColor(textColor)
                    .codeBackgroundColor(BgLight.toArgb())
                    .codeBlockBackgroundColor(BgLight.toArgb())
                    .codeTypeface(Typeface.MONOSPACE)
                    .codeTextSize((textSizePx * 0.90f).toInt().coerceAtLeast(1))
                    .bulletWidth((textSizePx * 0.32f).toInt().coerceAtLeast(4))
            }

            override fun configureVisitor(builder: MarkwonVisitor.Builder) {
                builder.on(SoftLineBreak::class.java) { visitor, _ ->
                    visitor.forceNewLine()
                }
            }
        },
    )
    .build()

private class FallbackLatexDrawable(
    latex: String,
    textColor: Int,
    private val textSizePx: Float,
) : Drawable() {
    private val cleanText = latex.replace("\n", " ").trim()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = textSizePx * 0.92f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        val y = b.top.toFloat() + textSizePx * 0.85f
        canvas.drawText(cleanText, b.left.toFloat() + 4f, y, paint)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    override fun getIntrinsicWidth(): Int = (paint.measureText(cleanText) + 8).toInt().coerceAtLeast(1)
    override fun getIntrinsicHeight(): Int = (textSizePx * 1.25f).toInt().coerceAtLeast(1)
}
