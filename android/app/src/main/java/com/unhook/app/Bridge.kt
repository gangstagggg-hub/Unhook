package com.unhook.app

import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.webkit.JavascriptInterface

/** Metodene web-appen når via window.Android. */
class Bridge(private val activity: Activity) {

    @JavascriptInterface
    fun hasUsageAccess(): Boolean = UsageAccess.granted(activity)

    @JavascriptInterface
    fun openUsageAccessSettings() {
        activity.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    @JavascriptInterface
    fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(activity)

    @JavascriptInterface
    fun openOverlaySettings() {
        activity.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${activity.packageName}"))
        )
    }

    @JavascriptInterface
    fun notificationsEnabled(): Boolean =
        activity.getSystemService(NotificationManager::class.java).areNotificationsEnabled()

    @JavascriptInterface
    fun openNotificationSettings() {
        activity.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
        )
    }

    /** Web-appen sender hvorfor, alternativer, blokkerte apper og perioder hver gang noe lagres. */
    @JavascriptInterface
    fun syncSettings(json: String) {
        Store.saveSettings(activity, json)
    }

    /** Returnerer ferdige økter som JSON og tømmer køen. */
    @JavascriptInterface
    fun takeSessions(): String = Store.takeSessions(activity)

    @JavascriptInterface
    fun currentSession(): String = Store.current ?: "null"
}
