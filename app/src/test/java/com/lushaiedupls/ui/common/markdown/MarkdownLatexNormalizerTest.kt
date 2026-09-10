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
        assertTrue(out.contains("\$\\ce{2H2 + O2 -> 2H2O}\$"))
        assertTrue(out.contains("\$\\ce{CaCO3 -> CaO + CO2}\$"))
    }

    @Test
    fun convertsCeInsideInlineMathWithoutExtraDollars() {
        val out = MarkdownLatexNormalizer.normalize("Water is \\(\\ce{H2O}\\).")
        assertEquals("Water is \$\\ce{H2O}\$.", out)
        assertFalse(out.contains("\$\$"))
    }

    @Test
    fun convertsIonsAndStates() {
        val out = MarkdownLatexNormalizer.convertChemistryToLatex("SO4^2- (aq) + Fe3+")
        assertEquals("\\ce{SO4^2- (aq) + Fe3+}", out)
    }

    @Test
    fun protectsUnderscoreFormulasFromMarkdownItalics() {
        val out = MarkdownLatexNormalizer.normalize("Sulphuric acid is H_2SO_4 in water.")
        assertTrue(out.contains("\$\\ce{H2SO4}\$"))
        assertFalse(out.contains("H_2SO_4"))
    }

    @Test
    fun rendersAceticAcidDimerLikeWikipedia() {
        val source =
            "The textbook mentions the dimerization of acetic acid: " +
                "\$2 CH_{3}COOH \\rightleftharpoons (CH_{3}COOH)_{2}\$. " +
                "Here, the formula \$CH_{3}COOH\$ and the \$(CH_{3}COOH)_{2}\$ dimer."
        val out = MarkdownLatexNormalizer.normalize(source)
        assertTrue(out.contains("\$\\ce{2 CH3COOH <=> (CH3COOH)2}\$"))
        assertTrue(out.contains("\$\\ce{CH3COOH}\$"))
        assertTrue(out.contains("\$\\ce{(CH3COOH)2}\$"))
        assertFalse(out.contains("CH_{3}COOH"))
    }

    @Test
    fun wrapsBareLatexChemistryFormulas() {
        val out = MarkdownLatexNormalizer.normalize("Acetic acid is CH_{3}COOH and the dimer is (CH_{3}COOH)_{2}.")
        assertTrue(out.contains("\$\\ce{CH3COOH}\$"))
        assertTrue(out.contains("\$\\ce{(CH3COOH)2}\$"))
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
    fun preservesCommonMarkdownInAiChat() {
        val source = """
            Hello! Today's chapter is **Solutions**. In this chapter we will learn:

            - Different types of solutions
            - Ways of expressing concentration

            ### Henry's law
            The mole fraction is important.
        """.trimIndent()
        val out = MarkdownLatexNormalizer.normalize(source)
        assertTrue(out.contains("**Solutions**"))
        assertTrue(out.contains("- Different types of solutions"))
        assertTrue(out.contains("- Ways of expressing concentration"))
        assertTrue(out.contains("### Henry's law"))
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
