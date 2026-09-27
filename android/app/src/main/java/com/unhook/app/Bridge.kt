package com.unhook.app

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.webkit.JavascriptInterface
import org.json.JSONArray
import org.json.JSONObject

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

    /** Spør om varseltillatelse (Android 13+). Er den avslått før, åpnes innstillingene i stedet. */
    @JavascriptInterface
    fun requestNotifications() {
        val needsAsk = Build.VERSION.SDK_INT >= 33 &&
            activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsAsk && !asked) {
            asked = true
            activity.runOnUiThread {
                activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), MainActivity.REQ_NOTIFICATIONS)
            }
        } else {
            openNotificationSettings()
        }
    }
    private var asked = false

    @JavascriptInterface
    fun ignoresBatteryOptimizations(): Boolean =
        activity.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(activity.packageName)

    /** App-info for Unhook, der brukeren kan sette Batteri til «Ubegrenset». */
    @JavascriptInterface
    fun openAppSettings() {
        activity.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${activity.packageName}"))
        )
    }

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

    /** Alle apper med eget ikon i app-skuffen, sortert etter navn: [{name, pkg}]. */
    @JavascriptInterface
    fun installedApps(): String {
        val pm = activity.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcher, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { it.first != activity.packageName }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }
        val arr = JSONArray()
        apps.forEach { (pkg, name) -> arr.put(JSONObject().put("name", name).put("pkg", pkg)) }
        return arr.toString()
    }

    @JavascriptInterface
    fun currentSession(): String = Store.current ?: "null"
}
