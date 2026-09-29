package com.unhook.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Deler data mellom bakgrunnstjenesten og web-appen.
 * Innstillinger kommer fra web-appen; ferdige økter venter her til web-appen henter dem.
 */
object Store {
    private const val PREFS = "unhook"
    private const val MAX_PENDING = 500

    /** Økten som pågår akkurat nå, som JSON, eller null. Lever bare i minnet. */
    @Volatile
    var current: String? = null

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun today(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    /** Tid brukt i dag i de valgte appene, i millisekunder. Nullstilles ved midnatt. */
    @Synchronized
    fun dailyUsed(c: Context): Long {
        val p = prefs(c)
        return if (p.getString("dailyDay", "") == today()) p.getLong("dailyMs", 0L) else 0L
    }

    /** Legger til brukt tid og returnerer summen for i dag. */
    @Synchronized
    fun addDaily(c: Context, deltaMs: Long): Long {
        val p = prefs(c)
        val day = today()
        val base = if (p.getString("dailyDay", "") == day) p.getLong("dailyMs", 0L) else 0L
        val total = base + deltaMs
        p.edit().putString("dailyDay", day).putLong("dailyMs", total).apply()
        return total
    }

    fun saveSettings(c: Context, json: String) {
        prefs(c).edit().putString("settings", json).apply()
    }

    fun settings(c: Context): JSONObject = try {
        JSONObject(prefs(c).getString("settings", null) ?: "{}")
    } catch (e: Exception) {
        JSONObject()
    }

    @Synchronized
    fun addSession(c: Context, start: Long, end: Long, app: String) {
        val arr = try { JSONArray(prefs(c).getString("pending", "[]")) } catch (e: Exception) { JSONArray() }
        arr.put(JSONObject().put("start", start).put("end", end).put("app", app))
        while (arr.length() > MAX_PENDING) arr.remove(0)
        prefs(c).edit().putString("pending", arr.toString()).apply()
    }

    @Synchronized
    fun takeSessions(c: Context): String {
        val s = prefs(c).getString("pending", "[]") ?: "[]"
        prefs(c).edit().putString("pending", "[]").apply()
        return s
    }
}
