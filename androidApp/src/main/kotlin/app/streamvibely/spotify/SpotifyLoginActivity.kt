package app.streamvibely.spotify

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

class SpotifyLoginActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }

        // Top action bar
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(32, 24, 32, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }

        val title = TextView(this).apply {
            text = "Spotify Web Login"
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val pasteBtn = Button(this).apply {
            text = "Paste sp_dc"
            setOnClickListener { showPasteDialog() }
        }

        val closeBtn = Button(this).apply {
            text = "Close"
            setOnClickListener { finish() }
        }

        bar.addView(title)
        bar.addView(pasteBtn)
        bar.addView(closeBtn)
        root.addView(bar)

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                12,
            )
        }
        root.addView(progressBar)

        val webContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
        }

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }

        webContainer.addView(webView)
        root.addView(webContainer)
        setContentView(root)

        setupWebView()
        webView.loadUrl(SpotifyLoginUrls.LOGIN_URL)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString()
                checkCookies(url)
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = android.view.View.GONE
                checkCookies(url)
            }
        }
    }

    private fun checkCookies(url: String?) {
        val cookies = CookieManager.getInstance().getCookie("https://accounts.spotify.com")
        val spDc = SpotifyLoginUrls.extractSpDc(cookies)
        if (!spDc.isNullOrBlank()) {
            onSessionCaptured(spDc)
        } else if (SpotifyLoginUrls.isPostAuth(url)) {
            // Re-check root cookies
            val alt = CookieManager.getInstance().getCookie("https://open.spotify.com")
            val spDcAlt = SpotifyLoginUrls.extractSpDc(alt)
            if (!spDcAlt.isNullOrBlank()) {
                onSessionCaptured(spDcAlt)
            }
        }
    }

    private fun onSessionCaptured(spDc: String) {
        Toast.makeText(this, "Spotify login detected!", Toast.LENGTH_SHORT).show()
        val data = Intent().apply {
            putExtra(EXTRA_SP_DC, spDc)
        }
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    private fun showPasteDialog() {
        val input = EditText(this).apply {
            hint = "Paste sp_dc cookie"
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString().orEmpty().trim()
            if (clipText.isNotBlank()) {
                setText(clipText)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Enter Spotify sp_dc")
            .setMessage("Paste your Spotify sp_dc cookie from browser devtools:")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotBlank()) {
                    onSessionCaptured(text)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_SP_DC = "extra_sp_dc"
    }
}
