package com.lushaiedupls.ui.common.markdown

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebSettings
import android.webkit.WebView
import org.json.JSONObject

internal object KatexRenderer {
    const val BASE_URL = "file:///android_asset/katex/"
    const val RENDER_URL = BASE_URL + "render.html"

    @Volatile
    private var started = false

    @Volatile
    private var template: String? = null

    fun prewarm(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        Handler(Looper.getMainLooper()).post {
            runCatching { ensureTemplate(app) }
            runCatching {
                WebView(app).apply {
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.blockNetworkLoads = true
                    loadUrl(RENDER_URL)
                    postDelayed({ runCatching { destroy() } }, 10_000)
                }
            }
        }
    }

    fun ensureTemplate(context: Context): String {
        template?.let { return it }
        synchronized(this) {
            template?.let { return it }
            return context.assets.open("katex/render.html").bufferedReader().use { it.readText() }
                .also { template = it }
        }
    }

    fun document(context: Context, markdown: String, themeJson: String): String {
        val mdLiteral = JSONObject.quote(markdown)
        val boot = """
            <script>
            (function(){
              var md = $mdLiteral;
              var theme = $themeJson;
              function boot(){
                try {
                  if (window.setTheme) setTheme(theme);
                  if (window.renderMarkdown) renderMarkdown(md);
                } catch (e) {}
              }
              if (window.renderMarkdown) boot();
              else window.addEventListener('load', boot);
            })();
            </script>
        """.trimIndent()
        return ensureTemplate(context).replace("</body>", "$boot\n</body>")
    }

    fun isKatexUrl(url: String?): Boolean {
        if (url.isNullOrBlank() || url == "about:blank") return false
        return url.startsWith(BASE_URL)
    }
}
