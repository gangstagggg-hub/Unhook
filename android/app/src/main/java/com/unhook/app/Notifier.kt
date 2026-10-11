package com.unhook.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import org.json.JSONObject
import kotlin.random.Random

object Notifier {
    private const val CHANNEL_MONITOR = "monitor"
    private const val CHANNEL_ALERTS = "alerts"
    const val ID_MONITOR = 1
    private const val ID_ALERT = 2

    fun createChannels(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_MONITOR, "Monitoring", NotificationManager.IMPORTANCE_MIN).apply {
                description = "Shown while Unhook keeps track of your screen time"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Scroll alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Suggestions when you open an app and after 5, 10 and 15 minutes, and when an app is locked"
            }
        )
    }

    fun homeIntent(): Intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_HOME)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun openApp(c: Context): PendingIntent = PendingIntent.getActivity(
        c, 0,
        Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun homeAction(c: Context): Notification.Action {
        val pi = PendingIntent.getActivity(
            c, 1, homeIntent(), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Action.Builder(Icon.createWithResource(c, R.drawable.ic_stat_unhook), "Go to home screen", pi).build()
    }

    fun monitor(c: Context): Notification = Notification.Builder(c, CHANNEL_MONITOR)
        .setSmallIcon(R.drawable.ic_stat_unhook)
        .setContentTitle("Unhook is watching")
        .setContentText("You get a suggestion when you open one of your apps.")
        .setContentIntent(openApp(c))
        .setOngoing(true)
        .build()

    private var lastAlt: String? = null

    /** Velger et alternativ fra listen (ikke det samme som sist) og lager «How about …?». */
    private fun suggestion(settings: JSONObject): String {
        val arr = settings.optJSONArray("alternatives")
        val all = (0 until (arr?.length() ?: 0)).map { arr!!.optString(it) }.filter { it.isNotBlank() }
        val pool = if (all.size > 1) all.filter { it != lastAlt } else all
        val alt = if (pool.isNotEmpty()) pool[Random.nextInt(pool.size)] else "Putting the phone away for a while"
        lastAlt = alt
        var t = alt.trim().trimEnd('.', '!', '?')
        val acronym = t.length > 1 && t[1].isUpperCase()
        if (!acronym) t = t.replaceFirstChar { it.lowercase() }
        return "How about $t?"
    }

    private fun suggestionNotification(c: Context, title: String, body: String): Notification =
        Notification.Builder(c, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_unhook)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(openApp(c))
            .addAction(homeAction(c))
            .setAutoCancel(true)
            .build()

    /** Sendes med en gang du åpner en av de valgte appene. */
    fun opened(c: Context, app: String, settings: JSONObject) {
        val why = settings.optString("why").trim()
        val body = if (why.isNotEmpty()) why else "You opened $app."
        post(c, ID_ALERT, suggestionNotification(c, suggestion(settings), body))
    }

    /** Sendes etter 5, 10 og 15 minutter. */
    fun overLimit(c: Context, minutes: Int, app: String, settings: JSONObject) {
        val why = settings.optString("why").trim()
        val body = buildString {
            append("You have been on $app for $minutes minutes.")
            if (why.isNotEmpty()) append("\n\n").append(why)
        }
        post(c, ID_ALERT, suggestionNotification(c, suggestion(settings), body))
    }

    /** Brukes når Unhook ikke har lov til å vise blokkeringsskjermen over andre apper. */
    fun blocked(c: Context, app: String, until: String) {
        val n = Notification.Builder(c, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_unhook)
            .setContentTitle("$app is locked")
            .setContentText("The lock lasts until $until.")
            .setContentIntent(openApp(c))
            .addAction(homeAction(c))
            .setAutoCancel(true)
            .build()
        post(c, ID_ALERT, n)
    }

    fun cancelAlert(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.cancel(ID_ALERT)
    }

    private fun post(c: Context, id: Int, n: Notification) {
        if (Build.VERSION.SDK_INT >= 33 &&
            c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        c.getSystemService(NotificationManager::class.java).notify(id, n)
    }
}
