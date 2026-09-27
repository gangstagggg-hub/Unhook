package com.unhook.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

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
