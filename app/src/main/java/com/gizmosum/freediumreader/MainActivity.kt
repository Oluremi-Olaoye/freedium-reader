package com.gizmosum.freediumreader

import android.content.Intent
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : AppCompatActivity() {

    private lateinit var urlInput: EditText
    private lateinit var webView: WebView

    private val prefs by lazy {
        getSharedPreferences("freedium_reader", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        urlInput = findViewById(R.id.urlInput)
        webView = findViewById(R.id.webView)

        val openButton: Button = findViewById(R.id.openButton)
        val backButton: Button = findViewById(R.id.backButton)
        val bookmarkButton: Button = findViewById(R.id.bookmarkButton)
        val themeButton: Button = findViewById(R.id.themeButton)

        setupWebView()

        openButton.setOnClickListener {
            openEnteredUrl()
        }

        backButton.setOnClickListener {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                Toast.makeText(
                    this,
                    "No previous page",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        bookmarkButton.setOnClickListener {
            bookmarkCurrentPage()
        }

        themeButton.setOnClickListener {
            toggleTheme()
        }

        handleIncomingShare(intent)
    }

    private fun setupWebView() {

        webView.webViewClient = WebViewClient()

        val settings: WebSettings = webView.settings

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadsImagesAutomatically = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false

        settings.userAgentString =
            "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
    }

    private fun openEnteredUrl() {

        val url = urlInput.text.toString().trim()

        if (url.isEmpty()) {
            Toast.makeText(
                this,
                "Paste a Medium URL first",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!isMediumUrl(url)) {
            Toast.makeText(
                this,
                "Please enter a valid Medium URL",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        saveToHistory(url)

        webView.loadUrl(url)
    }

    private fun isMediumUrl(url: String): Boolean {

        return try {
            val uri = android.net.Uri.parse(url)

            val host = uri.host?.lowercase() ?: return false

            host == "medium.com" ||
                    host.endsWith(".medium.com")
        } catch (e: Exception) {
            false
        }
    }

    private fun handleIncomingShare(intent: Intent?) {

        if (intent?.action != Intent.ACTION_SEND) {
            return
        }

        if (intent.type != "text/plain") {
            return
        }

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: return

        val mediumUrl = extractMediumUrl(sharedText)

        if (mediumUrl != null) {

            urlInput.setText(mediumUrl)

            saveToHistory(mediumUrl)

            webView.loadUrl(mediumUrl)
        }
    }

    private fun extractMediumUrl(text: String): String? {

        val words = text.split("\\s+".toRegex())

        for (word in words) {

            val cleaned = word
                .trim()
                .trimEnd('.', ',', ')', ']', '}', ';')

            if (isMediumUrl(cleaned)) {
                return cleaned
            }
        }

        return null
    }

    private fun saveToHistory(url: String) {

        val history = prefs.getStringSet(
            "history",
            emptySet()
        )?.toMutableSet() ?: mutableSetOf()

        history.remove(url)
        history.add(url)

        prefs.edit()
            .putStringSet("history", history)
            .apply()
    }

    private fun bookmarkCurrentPage() {

        val currentUrl = webView.url

        if (currentUrl.isNullOrEmpty()) {
            Toast.makeText(
                this,
                "Open an article first",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val bookmarks = prefs.getStringSet(
            "bookmarks",
            emptySet()
        )?.toMutableSet() ?: mutableSetOf()

        if (bookmarks.contains(currentUrl)) {

            bookmarks.remove(currentUrl)

            Toast.makeText(
                this,
                "Bookmark removed",
                Toast.LENGTH_SHORT
            ).show()

        } else {

            bookmarks.add(currentUrl)

            Toast.makeText(
                this,
                "Article bookmarked",
                Toast.LENGTH_SHORT
            ).show()
        }

        prefs.edit()
            .putStringSet("bookmarks", bookmarks)
            .apply()
    }

    private fun toggleTheme() {

        val currentMode = prefs.getBoolean(
            "dark_mode",
            false
        )

        val newMode = !currentMode

        prefs.edit()
            .putBoolean("dark_mode", newMode)
            .apply()

        if (newMode) {

            AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_YES
            )

        } else {

            AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    override fun onNewIntent(intent: Intent?) {

        super.onNewIntent(intent)

        setIntent(intent)

        handleIncomingShare(intent)
    }

    override fun onBackPressed() {

        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {

        webView.apply {
            stopLoading()
            webViewClient = null
            destroy()
        }

        super.onDestroy()
    }
}
