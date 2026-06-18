package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MarkdownPreview(
    markdownString: String,
    modifier: Modifier = Modifier
) {
    val htmlTemplate = remember {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <!-- KaTeX for LaTeX rendering -->
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.css">
            <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.js"></script>
            <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/contrib/auto-render.min.js"></script>
            
            <!-- Mermaid.js for diagrams -->
            <script src="https://cdn.jsdelivr.net/npm/mermaid@9.4.3/dist/mermaid.min.js"></script>
            
            <!-- Marked.js for markdown rendering -->
            <script src="https://cdn.jsdelivr.net/npm/marked@5.1.2/marked.min.js"></script>

            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    color: #E6E1E5; /* ArtisticTextMax light color */
                    background-color: #1C1B1F; /* ArtisticBg dark theme background */
                    margin: 0;
                    padding: 20px;
                    line-height: 1.6;
                    font-size: 16px;
                }
                h1, h2, h3, h4, h5, h6 {
                    color: #D0BCFF; /* ArtisticPrimary display headers */
                    font-weight: 700;
                    margin-top: 1.6em;
                    margin-bottom: 0.6em;
                }
                h1 { border-bottom: 1px solid #49454F; padding-bottom: 0.3em; font-size: 1.8em; }
                h2 { border-bottom: 1px solid #49454F; padding-bottom: 0.2em; font-size: 1.5em; }
                p {
                    margin-bottom: 1.25em;
                }
                code {
                    font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, Courier, monospace;
                    background-color: #2B2930; /* ArtisticCardBg code blocks */
                    padding: 0.2em 0.4em;
                    border-radius: 4px;
                    font-size: 85%;
                    color: #B4E197; /* WarmSage glow accent code text */
                }
                pre {
                    background-color: #2B2930;
                    padding: 16px;
                    overflow: auto;
                    border-radius: 8px;
                    border: 1px solid #49454F;
                    margin-bottom: 1.5em;
                }
                pre code {
                    background-color: transparent;
                    padding: 0;
                    color: #E6E1E5;
                }
                a {
                    color: #D0BCFF;
                    text-decoration: none;
                }
                a:hover {
                    text-decoration: underline;
                }
                blockquote {
                    border-left: 4px solid #D0BCFF;
                    color: #CAC4D0; /* ArtisticTextSecondary quotes */
                    padding-left: 16px;
                    margin: 1.5em 0;
                    font-style: italic;
                }
                table {
                    border-collapse: collapse;
                    width: 100%;
                    margin-bottom: 24px;
                }
                th, 
                td {
                    border: 1px solid #49454F;
                    padding: 8px 12px;
                    text-align: left;
                }
                th {
                    background-color: #2B2930;
                }
                ul, ol {
                    padding-left: 20px;
                    margin-bottom: 1.5em;
                }
                li {
                    margin-bottom: 0.4em;
                }
                .mermaid {
                    background-color: #2B2930 !important;
                    padding: 16px;
                    border-radius: 8px;
                    display: flex;
                    justify-content: center;
                    margin: 20px 0;
                    overflow-x: auto;
                }
            </style>
        </head>
        <body>
            <div id="content"></div>

            <script>
                // Set up marked.js options
                marked.setOptions({
                    gfm: true,
                    breaks: true
                });

                // Override mermaid parse error to prevent native Alert/Crash
                window.mermaidError = null;
                mermaid.parseError = function(err, hash) {
                    window.mermaidError = err;
                    console.error("Mermaid parse error: ", err);
                };

                // Initialize mermaid with gorgeous dark-neutral options matching standard themes
                mermaid.initialize({
                    startOnLoad: false,
                    theme: 'dark',
                    securityLevel: 'loose',
                    themeVariables: {
                        background: '#2B2930',
                        primaryColor: '#D0BCFF',
                        secondaryColor: '#381E72',
                        lineColor: '#CAC4D0',
                        primaryTextColor: '#E6E1E5',
                        textColor: '#E6E1E5'
                    }
                });

                function updateMarkdown(rawMarkdown) {
                    try {
                        // Render standard markdown via marked.js
                        var renderedHtml = marked.parse(rawMarkdown);
                        var contentDiv = document.getElementById('content');
                        contentDiv.innerHTML = renderedHtml;

                        // Process LaTeX/Math expressions with KaTeX
                        renderMathInElement(contentDiv, {
                            delimiters: [
                                {left: "$$", right: "$$", display: true},
                                {left: "$", right: "$", display: false},
                                {left: "\\(", right: "\\)", display: false},
                                {left: "\\[", right: "\\]", display: true}
                            ],
                            throwOnError: false
                        });

                        // Render Mermaid.js diagrams
                        // Find all code blocks marked as language-mermaid (case-insensitive)
                        var codeElements = contentDiv.querySelectorAll('pre code.language-mermaid, pre code.language-mermaid-chart');
                        codeElements.forEach(function(codeElement, idx) {
                            var preElement = codeElement.parentElement;
                            var mermaidContainer = document.createElement('div');
                            mermaidContainer.className = 'mermaid';
                            mermaidContainer.id = 'mermaid-chart-' + idx;
                            mermaidContainer.textContent = codeElement.textContent;
                            preElement.replaceWith(mermaidContainer);
                        });

                        // Run mermaid processor
                        window.mermaidError = null;
                        mermaid.init(undefined, '.mermaid');

                        // Check if a parse error was flagged during initialization
                        if (window.mermaidError) {
                            var errors = contentDiv.querySelectorAll('.mermaid[data-processed="false"], .mermaid:empty');
                            errors.forEach(function(errEl) {
                                errEl.innerHTML = "<div style='color: #F87171; padding: 12px; border: 1px dashed #F87171; border-radius: 6px; font-family: monospace; font-size: 12px;'><b>Mermaid Diagram Error:</b><br>" + window.mermaidError.toString().replace(/</g, "&lt;").replace(/>/g, "&gt;") + "</div>";
                            });
                        }

                    } catch (e) {
                        document.getElementById('content').innerHTML = "<div style='color: #EF4444; padding: 12px; border: 1px solid #EF4444; border-radius: 6px;'><b>Rendering Error:</b> " + e.message + "</div>";
                    }
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    var webViewRef: WebView? = null

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Trigger initial render when page loads
                        // Escape the markdown for JavaScript insertion
                        val escapedMarkdown = escapeMarkdownForJs(markdownString)
                        evaluateJavascript("javascript:updateMarkdown('$escapedMarkdown')", null)
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        android.util.Log.d("HTML_CONSOLE", "${consoleMessage?.message()}")
                        return true
                    }
                }
                loadDataWithBaseURL("https://local.assets", htmlTemplate, "text/html", "UTF-8", null)
                webViewRef = this
            }
        },
        update = { webView ->
            val escapedMarkdown = escapeMarkdownForJs(markdownString)
            webView.evaluateJavascript("javascript:updateMarkdown('$escapedMarkdown')", null)
        },
        modifier = modifier.fillMaxSize()
    )
}

private fun escapeMarkdownForJs(raw: String): String {
    return raw
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\n", "\\n")
        .replace("\r", "")
}
