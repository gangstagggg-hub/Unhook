package com.unhook.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var root: FrameLayout
    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Appen tegnes ut til kantene. Polstringen under holder innholdet unna statuslinjen og navigasjonsfeltet.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        web = WebView(this).apply {
            setBackgroundColor(getColor(R.color.bg))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                // Nettadresser (for eksempel personvernerklæringen) åpnes i nettleseren, ikke inne i appen.
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val scheme = request.url.scheme
                    if (scheme == "http" || scheme == "https") {
                        try {
                            this@MainActivity.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                        } catch (e: Exception) {
                            // Ingen nettleser installert
                        }
                        return true
                    }
                    return false
                }
            }
            addJavascriptInterface(Bridge(this@MainActivity), "Android")
            loadUrl("file:///android_asset/index.html")
        }

        root = FrameLayout(this).apply {
            setBackgroundColor(getColor(R.color.bg))
            addView(
                web,
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            )
        }
        setContentView(root)
        Insets.applyPadding(root)

        // Tilbake-knappen: lukk ark eller gå til «I dag» først, og lukk appen først når du er der.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                web.evaluateJavascript("window.unhookBack ? window.unhookBack() : false") { result ->
                    if (result != "true") {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        })
        // Tillatelsene spørres om i oppsettsveiledningen i web-appen.
    }

    /** Kalles fra web-appen når utseendet endres, så feltene rundt følger lys eller mørk modus. */
    fun applyBarTheme(dark: Boolean) {
        val color = Color.parseColor(if (dark) "#0E141A" else "#E6EBEE")
        root.setBackgroundColor(color)
        web.setBackgroundColor(color)
        val controller = WindowCompat.getInsetsController(window, root)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
    }

    /** Sender svaret fra fingeravtrykk-vinduet til web-appen. */
    fun sendBioResult(ok: Boolean) {
        web.evaluateJavascript("window.unhookBio && window.unhookBio($ok)", null)
    }

    override fun onPause() {
        super.onPause()
        // Web-appen bruker tidspunktet til å låse seg igjen etter en stund borte.
        web.evaluateJavascript("window.unhookLeft && window.unhookLeft()", null)
    }

    /** Hvis Unhook er låst sammen med appene, vises låseskjermen i stedet for innholdet. */
    private fun checkSelfLock() {
        val until = Store.unhookLockedUntil(this)
        if (until == 0L) {
            web.visibility = View.VISIBLE
            return
        }
        web.visibility = View.INVISIBLE
        val hm = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(until))
        startActivity(
            Intent(this, BlockActivity::class.java)
                .putExtra(BlockActivity.EXTRA_SELF, true)
                .putExtra(BlockActivity.EXTRA_UNTIL, hm)
        )
    }

    override fun onResume() {
        super.onResume()
        checkSelfLock()
        UsageMonitorService.startIfAllowed(this)
        // Oppdater skjermen når du kommer tilbake fra innstillinger eller et tillatelsesvindu.
        web.evaluateJavascript("window.unhookResume && window.unhookResume()", null)
    }
}
