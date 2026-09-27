package com.unhook.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

class MainActivity : ComponentActivity() {

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

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        UsageMonitorService.startIfAllowed(this)
        // Oppdater skjermen når du kommer tilbake fra innstillinger.
        web.evaluateJavascript("window.unhookResume && window.unhookResume()", null)
    }
}
