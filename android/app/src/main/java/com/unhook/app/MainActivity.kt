package com.unhook.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

class MainActivity : ComponentActivity() {

    companion object {
        const val REQ_NOTIFICATIONS = 1
    }

    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        web = WebView(this).apply {
            setBackgroundColor(getColor(R.color.bg))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            addJavascriptInterface(Bridge(this@MainActivity), "Android")
            loadUrl("file:///android_asset/index.html")
        }
        setContentView(web)

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

    @Deprecated("Enkel løsning uten Activity Result API")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        web.evaluateJavascript("window.unhookResume && window.unhookResume()", null)
    }

    override fun onResume() {
        super.onResume()
        UsageMonitorService.startIfAllowed(this)
        // Oppdater skjermen når du kommer tilbake fra innstillinger.
        web.evaluateJavascript("window.unhookResume && window.unhookResume()", null)
    }
}
