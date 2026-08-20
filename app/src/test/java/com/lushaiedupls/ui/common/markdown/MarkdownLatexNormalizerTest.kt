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
        assertEquals("See\n\$\$\n\\frac{a}{b}\n\$\$\nnext.", out)
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
