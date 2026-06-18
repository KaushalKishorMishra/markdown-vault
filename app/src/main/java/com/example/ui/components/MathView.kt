package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MathView(
    latex: String,
    modifier: Modifier = Modifier,
    isBlock: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    // Generate clean hex colors for CSS styling in WebView
    val bgHex = String.format("#%06X", 0xFFFFFF and backgroundColor.toArgb())
    val textHex = String.format("#%06X", 0xFFFFFF and textColor.toArgb())

    val htmlTemplate = remember(latex, bgHex, textHex, isBlock) {
        val displayStyleMode = if (isBlock) "true" else "false"
        // Ensure backslashes are properly escaped in the JavaScript string literal
        val escapedLatex = latex
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("$", "\\$")

        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <!-- KaTeX CSS + JS CDN -->
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.css">
            <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.js"></script>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    color: $textHex;
                    background-color: $bgHex;
                    margin: 0;
                    padding: 8px;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    overflow-x: auto;
                    overflow-y: hidden;
                    box-sizing: border-box;
                    min-height: 48px;
                }
                #math-renderer {
                    font-size: 15px;
                    text-align: center;
                    width: 100%;
                }
                /* Hide scrollbars but keep functionality */
                body::-webkit-scrollbar {
                    display: none;
                }
            </style>
        </head>
        <body>
            <div id="math-renderer">Evaluating math...</div>
            <script>
                document.addEventListener("DOMContentLoaded", function() {
                    try {
                        katex.render(`$escapedLatex`, document.getElementById('math-renderer'), {
                            displayMode: $displayStyleMode,
                            throwOnError: false
                        });
                    } catch (e) {
                        document.getElementById('math-renderer').innerHTML = 
                            "<span style='color: #F87171;'>Math Error: " + e.message.replace(/</g, "&lt;").replace(/>/g, "&gt;") + "</span>";
                    }
                });
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Box(modifier = modifier.background(backgroundColor)) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = false
                        allowContentAccess = false
                    }
                    webViewClient = WebViewClient()
                    setBackgroundColor(backgroundColor.toArgb())
                    loadDataWithBaseURL("https://local.math.renderer", htmlTemplate, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL("https://local.math.renderer", htmlTemplate, "text/html", "UTF-8", null)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
