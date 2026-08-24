package com.lushaiedupls.ui.common.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownLatexNormalizerTest {
    @Test
    fun convertsInlineParenDelimiters() {
        val out = MarkdownLatexNormalizer.normalize("The formula is \\(E = mc^2\\).")
        assertEquals("The formula is \$E = mc^2\$.", out)
    }

    @Test
    fun convertsDisplayBracketDelimiters() {
        val out = MarkdownLatexNormalizer.normalize("See\n\\[\\frac{a}{b}\\]\nnext.")
        assertTrue(out.contains("\$\$\n\\frac{a}{b}\n\$\$"))
    }

    @Test
    fun convertsDoubleEscapedDelimiters() {
        val out = MarkdownLatexNormalizer.normalize("\\\\(E = mc^2\\\\)")
        assertEquals("\$E = mc^2\$", out)
    }

    @Test
    fun wrapsBareLatexCommands() {
        val out = MarkdownLatexNormalizer.normalize("Rate is \\frac{a}{b} here")
        assertEquals("Rate is \$\\frac{a}{b}\$ here", out)
    }

    @Test
    fun handlesNestedBracesInQuadraticFormula() {
        val source = "Formula: x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a} for a \\neq 0."
        val out = MarkdownLatexNormalizer.normalize(source)
        assertTrue(out.contains("\$x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}\$"))
        assertTrue(out.contains("\$a \\neq 0\$"))
    }

    @Test
    fun convertsChemistryEquations() {
        val source = "Combustion: \\ce{2H2 + O2 -> 2H2O} and \\ce{CaCO3 -> CaO + CO2}."
        val out = MarkdownLatexNormalizer.normalize(source)
        assertTrue(out.contains("\$\\mathrm{2H_{2} + O_{2} \\rightarrow  2H_{2}O}\$"))
        assertTrue(out.contains("\$\\mathrm{CaCO_{3} \\rightarrow  CaO + CO_{2}}\$"))
    }

    @Test
    fun convertsAlignAndEquationEnvironmentsToAligned() {
        val source = """
            \begin{align}
            a &= b + c \\
            d &= e + f
            \end{align}
        """.trimIndent()
        val out = MarkdownLatexNormalizer.normalize(source)
        assertTrue(out.contains("\\begin{aligned}"))
        assertTrue(out.contains("\\end{aligned}"))
        assertFalse(out.contains("\\begin{align}"))
    }

    @Test
    fun normalizesTextCommandsToMboxForJLatexMath() {
        val source = "\$\\text{speed} = \\frac{\\text{distance}}{\\text{time}}\$"
        val out = MarkdownLatexNormalizer.normalize(source)
        assertTrue(out.contains("\\mbox{speed}"))
        assertTrue(out.contains("\\mbox{distance}"))
        assertTrue(out.contains("\\mbox{time}"))
    }

    @Test
    fun convertsCalculusAndGreekFormulas() {
        val source = "Compute \\int_{0}^{\\pi} \\sin(x) dx = 2 and \\lim_{x \\to 0} \\frac{\\sin x}{x} = 1."
        val out = MarkdownLatexNormalizer.normalize(source)
        assertTrue(out.contains("\\int_{0}^{\\pi}"))
        assertTrue(out.contains("\\lim_{x \\to 0}"))
    }

    @Test
    fun convertsDegreesAndTemperatures() {
        val source = "The angle is 45\\degree and boiling point is 100^\\circ C."
        val out = MarkdownLatexNormalizer.normalize(source)
        assertTrue(out.contains("^{\\circ}"))
        assertTrue(out.contains("^{\\circ}\\mathrm{C}"))
    }

    @Test
    fun convertsMatrixAndCasesEnvironments() {
        val source = """
            \begin{cases}
            2x + y = 5 \\
            x - y = 1
            \end{cases}
        """.trimIndent()
        val out = MarkdownLatexNormalizer.normalize(source)
        assertTrue(out.contains("\\begin{cases}"))
        assertTrue(out.contains("\\end{cases}"))
    }

    @Test
    fun convertsMathFencesAndLeavesCodeFences() {
        val source = """
            Use `code` then:
            ```math
            x^2 + y^2 = z^2
            ```
            and kotlin:
            ```kotlin
            val n = 1
            ```
        """.trimIndent()
        val out = MarkdownLatexNormalizer.normalize(source)
        assertTrue(out.contains("\$\$\nx^2 + y^2 = z^2\n\$\$"))
        assertTrue(out.contains("```kotlin\nval n = 1\n```"))
        assertFalse(out.contains("```math"))
    }
}
