package com.lushaiedupls.ui.common.markdown

/**
 * Normalizes tutor and quiz markdown/LaTeX so Markwon and JLatexMath render all
 * formulas, chemical equations, matrices, and markdown structures correctly.
 */
object MarkdownLatexNormalizer {
    private val mathFenceLangs = setOf("math", "latex", "tex")
    private val displayBracket = Regex("""\\\[(.+?)\\\]""", RegexOption.DOT_MATCHES_ALL)
    private val inlineParen = Regex("""\\\((.+?)\\\)""", RegexOption.DOT_MATCHES_ALL)
    private val inlineBacktickMath = Regex("""`(\s*\\\((?:.+?)\\\)\s*)`""", RegexOption.DOT_MATCHES_ALL)
    private val chemistryCe = Regex("""\\ce\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}""", RegexOption.DOT_MATCHES_ALL)

    private val environmentRegex = Regex(
        """\\begin\{(equation\*?|align\*?|gather\*?|split|multline\*?|displaymath|aligned|cases|matrix|pmatrix|bmatrix|vmatrix|Bmatrix|Vmatrix|array)\}(.+?)\\end\{\1\}""",
        RegexOption.DOT_MATCHES_ALL,
    )

    private val mathCommandStarters = setOf(
        "frac", "dfrac", "tfrac", "cfrac", "sqrt", "sum", "int", "iint", "iiint", "oint", "prod", "lim",
        "alpha", "beta", "gamma", "delta", "epsilon", "varepsilon", "zeta", "eta", "theta", "vartheta",
        "iota", "kappa", "lambda", "mu", "nu", "xi", "pi", "varpi", "rho", "varrho", "sigma", "varsigma",
        "tau", "upsilon", "phi", "varphi", "chi", "psi", "omega",
        "Gamma", "Delta", "Theta", "Lambda", "Xi", "Pi", "Sigma", "Upsilon", "Phi", "Psi", "Omega",
        "times", "div", "pm", "mp", "cdot", "bullet", "circ", "approx", "sim", "simeq", "cong", "neq",
        "ne", "le", "ge", "leq", "geq", "ll", "gg", "equiv", "propto",
        "in", "notin", "subset", "subseteq", "supset", "supseteq", "cup", "cap", "setminus", "forall", "exists", "nabla",
        "sin", "cos", "tan", "cot", "sec", "csc", "arcsin", "arccos", "arctan", "sinh", "cosh", "tanh",
        "log", "ln", "exp", "det", "dim", "ker", "deg", "max", "min", "sup", "inf",
        "partial", "infty", "hbar", "ell", "Re", "Im", "wp", "aleph",
        "rightarrow", "leftarrow", "Rightarrow", "Leftarrow", "leftrightarrow", "Leftrightarrow",
        "to", "implies", "iff", "longleftrightarrow", "Longrightarrow", "Longleftrightarrow",
        "uparrow", "downarrow", "rightleftharpoons",
        "degree", "angstrom", "mathrm", "mathbf", "mathit", "mathbb", "mathcal", "mathscr", "mathfrak", "mathsf", "mathtt",
        "text", "mbox", "boldsymbol", "bm", "cancel", "hat", "bar", "vec", "dot", "ddot", "tilde", "overline", "underline",
    )

    fun normalize(source: String): String {
        if (source.isBlank()) return source
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
        var text = cleanJsonEscaping(raw)

        // Convert inline backtick math e.g. `\( ... \)`
        text = inlineBacktickMath.replace(text) { match -> match.groupValues[1].trim() }

        // Convert environments e.g. \begin{equation}...\end{equation}, \begin{align}...\end{align}
        text = environmentRegex.replace(text) { match ->
            val envName = match.groupValues[1]
            val envBody = match.groupValues[2].trim()
            val targetEnv = when {
                envName.startsWith("align") || envName.startsWith("equation") ||
                    envName.startsWith("gather") || envName == "split" ||
                    envName.startsWith("multline") || envName == "displaymath" -> "aligned"
                else -> envName
            }
            "\n$$\n\\begin{$targetEnv}\n${prepareLatex(envBody)}\n\\end{$targetEnv}\n$$\n"
        }

        // Convert display brackets \[ ... \]
        text = displayBracket.replace(text) { match ->
            "\n$$\n${prepareLatex(match.groupValues[1].trim())}\n$$\n"
        }

        // Convert inline parens \( ... \)
        text = inlineParen.replace(text) { match ->
            "$${prepareLatex(match.groupValues[1].trim())}$"
        }

        // Convert chemical formulas \ce{...}
        text = chemistryCe.replace(text) { match ->
            val chem = match.groupValues[1].trim()
            "$${convertChemistryToLatex(chem)}$"
        }

        // Convert degrees and temperatures e.g. 45\degree, 100^\circ C
        text = text.replace(Regex("""(\d+(?:\.\d+)?)\s*(?:\\degree|\^\\circ|\^\{\\circ\})\s*([CFcf])\b""")) { match ->
            "\$${match.groupValues[1]}^{\\circ}\\mathrm{${match.groupValues[2].uppercase()}}\$"
        }
        text = text.replace(Regex("""(\d+(?:\.\d+)?)\s*(?:\\degree|\^\\circ|\^\{\\circ\})\b""")) { match ->
            "\$${match.groupValues[1]}^{\\circ}\$"
        }

        // Normalize LaTeX inside existing $...$ and $$...$$
        text = normalizeExistingMathDelimiters(text)

        // Wrap bare LaTeX commands and formulas
        text = wrapBareLatexFormulas(text)

        return text
    }

    private fun cleanJsonEscaping(source: String): String {
        if (!source.contains('\\')) return source
        var s = source
        // Replace \\( with \( and \\) with \)
        s = s.replace("\\\\(", "\\(").replace("\\\\)", "\\)")
        // Replace \\[ with \[ and \\] with \]
        s = s.replace("\\\\[", "\\[").replace("\\\\]", "\\]")
        // Replace triple backslashes with single
        s = s.replace("\\\\\\", "\\")
        return s
    }

    private fun normalizeExistingMathDelimiters(text: String): String {
        val out = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            if (text.startsWith("$$", i)) {
                val end = text.indexOf("$$", i + 2)
                if (end < 0) {
                    out.append(text.substring(i))
                    break
                }
                val body = text.substring(i + 2, end).trim()
                out.append("\n$$\n").append(prepareLatex(body)).append("\n$$\n")
                i = end + 2
            } else if (text[i] == '$') {
                val end = findInlineMathEnd(text, i + 1)
                if (end < 0) {
                    out.append('$')
                    i++
                } else {
                    val body = text.substring(i + 1, end).trim()
                    out.append('$').append(prepareLatex(body)).append('$')
                    i = end + 1
                }
            } else {
                out.append(text[i])
                i++
            }
        }
        return out.toString()
    }

    private fun findInlineMathEnd(text: String, start: Int): Int {
        var j = start
        while (j < text.length) {
            if (text[j] == '$' && text[j - 1] != '\\') {
                // Ensure not a price like $100 or empty $$
                if (j == start) return -1
                return j
            }
            if (text[j] == '\n') return -1 // inline math shouldn't cross multiple lines
            j++
        }
        return -1
    }

    fun prepareLatex(latex: String): String {
        var s = latex.trim()
        if (s.isEmpty()) return s

        // Normalize text commands for JLatexMath: \text{...} -> \mbox{...}
        s = s.replace(Regex("""\\text\{([^{}]*)\}""")) { match ->
            "\\mbox{${match.groupValues[1]}}"
        }

        // Normalize degrees
        s = s.replace("\\degree", "^{\\circ}")
        s = s.replace(Regex("""\^\\circ(?!\w)"""), "^{\\circ}")
        s = s.replace(Regex("""\^\{\\circ\}\s*C\b"""), "^{\\circ}\\mathrm{C}")

        // Normalize fractions
        s = s.replace("\\dfrac", "\\frac")
        s = s.replace("\\tfrac", "\\frac")
        s = s.replace("\\cfrac", "\\frac")

        // Normalize implications & arrows
        s = s.replace("\\implies", "\\Longrightarrow")
        s = s.replace("\\iff", "\\Longleftrightarrow")
        s = s.replace("-->", "\\rightarrow")
        s = s.replace("<--", "\\leftarrow")
        s = s.replace("<=>", "\\rightleftharpoons")

        // Normalize bold macros
        s = s.replace(Regex("""\\(?:boldsymbol|bm)\{([^{}]*)\}""")) { match ->
            "\\mathbf{${match.groupValues[1]}}"
        }

        // Normalize cancel
        s = s.replace(Regex("""\\cancel\{([^{}]*)\}""")) { match ->
            "\\not{${match.groupValues[1]}}"
        }

        // Align environments inside math blocks
        s = s.replace(Regex("""\\begin\{align\*?\}"""), "\\begin{aligned}")
        s = s.replace(Regex("""\\end\{align\*?\}"""), "\\end{aligned}")
        s = s.replace(Regex("""\\begin\{equation\*?\}"""), "\\begin{aligned}")
        s = s.replace(Regex("""\\end\{equation\*?\}"""), "\\end{aligned}")

        return s
    }

    private fun convertChemistryToLatex(chem: String): String {
        var s = chem.trim()
        s = s.replace("->", "\\rightarrow ")
        s = s.replace("<=>", "\\rightleftharpoons ")
        s = s.replace("<->", "\\rightleftharpoons ")
        // Convert chemical formulas like H2O, CaCO3, SO4^2-, Fe^3+
        s = Regex("""([A-Z][a-z]?)(\d+)""").replace(s) { match ->
            "${match.groupValues[1]}_{${match.groupValues[2]}}"
        }
        s = Regex("""\^([0-9]+[+-]|[+-])""").replace(s) { match ->
            "^{${match.groupValues[1]}}"
        }
        return "\\mathrm{$s}"
    }

    private fun wrapBareLatexFormulas(text: String): String {
        if (!text.contains('\\')) return text
        val mathRanges = findMathRanges(text)
        val out = StringBuilder(text.length)
        var i = 0

        while (i < text.length) {
            val inExistingMath = mathRanges.any { i in it }
            if (inExistingMath || text[i] != '\\' || i + 1 >= text.length || !text[i + 1].isLetter()) {
                out.append(text[i])
                i++
                continue
            }

            // Extract command name
            var cmdEnd = i + 1
            while (cmdEnd < text.length && text[cmdEnd].isLetter()) {
                cmdEnd++
            }
            val commandName = text.substring(i + 1, cmdEnd)

            if (commandName !in mathCommandStarters) {
                out.append(text[i])
                i++
                continue
            }

            // Found a bare math command starter! Parse the full compound expression forward
            var exprEnd = cmdEnd
            var changed = true
            while (changed && exprEnd < text.length) {
                changed = false
                // Consume balanced { ... }
                if (exprEnd < text.length && text[exprEnd] == '{') {
                    val close = findClosingBrace(text, exprEnd, '{', '}')
                    if (close > exprEnd) {
                        exprEnd = close + 1
                        changed = true
                    }
                }
                // Consume balanced [ ... ]
                if (exprEnd < text.length && text[exprEnd] == '[') {
                    val close = findClosingBrace(text, exprEnd, '[', ']')
                    if (close > exprEnd) {
                        exprEnd = close + 1
                        changed = true
                    }
                }
                // Consume subscripts and superscripts like _i, _{i+1}, ^2, ^{2n}
                if (exprEnd < text.length && (text[exprEnd] == '^' || text[exprEnd] == '_')) {
                    exprEnd++
                    if (exprEnd < text.length && text[exprEnd] == '{') {
                        val close = findClosingBrace(text, exprEnd, '{', '}')
                        if (close > exprEnd) {
                            exprEnd = close + 1
                            changed = true
                        }
                    } else if (exprEnd < text.length && (text[exprEnd].isLetterOrDigit() || text[exprEnd] == '\\')) {
                        exprEnd++
                        changed = true
                    }
                }
                // Consume chained math operators and subsequent expressions
                val tailMatch = Regex("""^(\s*[+\-*=/<>~±]\s*|\s*\\(?:times|pm|mp|cdot|div|approx|neq|leq|geq|rightarrow|leftarrow)\s*)""").find(text.substring(exprEnd))
                if (tailMatch != null) {
                    val opEnd = exprEnd + tailMatch.value.length
                    if (opEnd < text.length && (text[opEnd].isLetterOrDigit() || text[opEnd] == '\\' || text[opEnd] == '{' || text[opEnd] == '(')) {
                        exprEnd = opEnd
                        // Advance over operand or next command
                        if (exprEnd < text.length && text[exprEnd] == '\\') {
                            var nextCmdEnd = exprEnd + 1
                            while (nextCmdEnd < text.length && text[nextCmdEnd].isLetter()) nextCmdEnd++
                            exprEnd = nextCmdEnd
                        } else {
                            while (exprEnd < text.length && (text[exprEnd].isLetterOrDigit() || text[exprEnd] == '.')) exprEnd++
                        }
                        changed = true
                    }
                }
            }

            // Check if preceded by a left-hand operand with operator e.g. "x = \frac..." or "a \neq..." or "n \in..."
            var exprStart = i
            val prefixBefore = text.substring(0, i)
            val isRelationCmd = commandName in setOf("neq", "ne", "leq", "geq", "le", "ge", "approx", "sim", "equiv", "in", "notin", "equiv")
            val lhsRegex = if (isRelationCmd) {
                Regex("""(?:^|[\s(\[{,;:])([a-zA-Z](?:_[a-zA-Z0-9]+)?\s*)$""")
            } else {
                Regex("""(?:^|[\s(\[{,;:])([a-zA-Z](?:_[a-zA-Z0-9]+)?\s*(?:=|[+\-*<>])\s*)$""")
            }
            val lhsMatch = lhsRegex.find(prefixBefore)
            if (lhsMatch != null) {
                val fullLhs = lhsMatch.groupValues[1]
                val trimmedLhs = fullLhs.trim()
                val isStopWord = trimmedLhs.equals("is", ignoreCase = true) ||
                    trimmedLhs.equals("in", ignoreCase = true) ||
                    trimmedLhs.equals("to", ignoreCase = true) ||
                    trimmedLhs.equals("if", ignoreCase = true) ||
                    trimmedLhs.equals("or", ignoreCase = true) ||
                    trimmedLhs.equals("as", ignoreCase = true) ||
                    trimmedLhs.equals("at", ignoreCase = true)
                if (fullLhs.isNotBlank() && !isStopWord && out.endsWith(fullLhs)) {
                    out.setLength(out.length - fullLhs.length)
                    exprStart -= fullLhs.length
                }
            }

            // Consume trailing simple RHS operand e.g. "\neq 0", "\approx 3.14", "\leq 10"
            if (commandName in setOf("neq", "ne", "leq", "geq", "le", "ge", "approx", "sim", "equiv", "in", "notin", "pm", "mp", "times", "div", "cdot")) {
                val rhsMatch = Regex("""^(\s*(?:-?\d+(?:\.\d+)?|[a-zA-Z](?:_[a-zA-Z0-9]+)?|\\[a-zA-Z]+))""").find(text.substring(exprEnd))
                if (rhsMatch != null) {
                    exprEnd += rhsMatch.value.length
                }
            }

            val rawFormula = text.substring(exprStart, exprEnd)
            out.append('$').append(prepareLatex(rawFormula.trim())).append('$')
            i = exprEnd
        }
        return out.toString()
    }

    private fun findClosingBrace(text: String, start: Int, openChar: Char, closeChar: Char): Int {
        var depth = 0
        for (j in start until text.length) {
            if (text[j] == openChar) depth++
            else if (text[j] == closeChar) {
                depth--
                if (depth == 0) return j
            }
        }
        return -1
    }

    private fun findMathRanges(text: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        var i = 0
        while (i < text.length) {
            if (text.startsWith("$$", i)) {
                val end = text.indexOf("$$", i + 2)
                if (end < 0) break
                ranges += i until (end + 2)
                i = end + 2
            } else if (text[i] == '$') {
                val end = findInlineMathEnd(text, i + 1)
                if (end < 0) {
                    i++
                } else {
                    ranges += i..end
                    i = end + 1
                }
            } else {
                i++
            }
        }
        return ranges
    }
}

