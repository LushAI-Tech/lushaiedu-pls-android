package com.lushaiedupls.ui.common.markdown

/**
 * Prepares tutor/quiz markdown so Markwon's LaTeX plugin can render it.
 *
 * Models commonly emit `\(...\)` / `\[...\]`, extra JSON escaping, ` ```math `
 * fences, or bare commands like `\frac{a}{b}`. Markwon looks for `$...$` / `$$...$$`.
 */
object MarkdownLatexNormalizer {
    private val mathFenceLangs = setOf("math", "latex", "tex")
    private val displayBracket = Regex("""\\\[(.+?)\\\]""", RegexOption.DOT_MATCHES_ALL)
    private val inlineParen = Regex("""\\\((.+?)\\\)""", RegexOption.DOT_MATCHES_ALL)
    private val beginEquation = Regex(
        """\\begin\{(?:equation\*?|align\*?|displaymath)\}(.+?)\\end\{(?:equation\*?|align\*?|displaymath)\}""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val extraBackslashes = Regex("""\\{2,}(?=[\\(\)\[\]]|[a-zA-Z])""")
    private val textCommand = Regex("""\\text\{([^{}]*)\}""")
    private val inlineBacktickMath = Regex("""`(\s*\\\((?:.+?)\\\)\s*)`""", RegexOption.DOT_MATCHES_ALL)
    private val latexChunk = Regex("""\\[a-zA-Z]+(?:\s*\{[^{}]*\})*""")

    fun normalize(source: String): String {
        val text = source.replace("\r\n", "\n")
        val out = StringBuilder(text.length)
        var index = 0
        while (index < text.length) {
            val fenceStart = text.indexOf("```", index)
            if (fenceStart < 0) {
                out.append(convertMarkup(text.substring(index)))
                break
            }
            out.append(convertMarkup(text.substring(index, fenceStart)))
            val afterTicks = fenceStart + 3
            val lineEnd = text.indexOf('\n', afterTicks).let { end ->
                if (end < 0) text.length else end
            }
            val lang = text.substring(afterTicks, lineEnd).trim().lowercase()
            val close = text.indexOf("```", lineEnd)
            if (close < 0) {
                out.append(convertMarkup(text.substring(fenceStart)))
                break
            }
            val bodyStart = if (lineEnd < text.length) lineEnd + 1 else lineEnd
            val body = text.substring(bodyStart, close)
            if (lang in mathFenceLangs) {
                out.append("\n$$\n").append(prepareLatex(body.trim())).append("\n$$\n")
            } else {
                out.append(text.substring(fenceStart, close + 3))
            }
            index = close + 3
        }
        return out.toString()
    }

    private fun convertMarkup(raw: String): String {
        var converted = extraBackslashes.replace(raw) { "\\" }
        converted = inlineBacktickMath.replace(converted) { match ->
            match.groupValues[1].trim()
        }
        converted = textCommand.replace(converted) { match ->
            "\\mathrm{${match.groupValues[1]}}"
        }
        converted = beginEquation.replace(converted) { match ->
            "\n$$\n${prepareLatex(match.groupValues[1].trim())}\n$$\n"
        }
        converted = displayBracket.replace(converted) { match ->
            "$$\n${prepareLatex(match.groupValues[1].trim())}\n$$"
        }
        converted = inlineParen.replace(converted) { match ->
            "$${prepareLatex(match.groupValues[1].trim())}$"
        }
        return wrapBareLatex(converted)
    }

    private fun prepareLatex(latex: String): String =
        textCommand.replace(latex) { match -> "\\mathrm{${match.groupValues[1]}}" }

    private fun wrapBareLatex(text: String): String {
        if (!text.contains('\\')) return text
        val math = mathRanges(text)
        return latexChunk.replace(text) { match ->
            val start = match.range.first
            if (math.any { start in it }) match.value else "$${match.value}$"
        }
    }

    private fun mathRanges(text: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        var i = 0
        while (i < text.length) {
            if (text.startsWith("$$", i)) {
                val end = text.indexOf("$$", i + 2)
                if (end < 0) break
                ranges += i until (end + 2)
                i = end + 2
            } else if (text[i] == '$') {
                val end = text.indexOf('$', i + 1)
                if (end < 0) break
                ranges += i..end
                i = end + 1
            } else {
                i++
            }
        }
        return ranges
    }
}
