package com.unhook.app

import org.json.JSONObject
import java.util.Calendar

object Schedule {
    /** Perioden som er aktiv nå, eller null. Dagene følger JavaScript: 0 = søndag, 1 = mandag. */
    fun activePeriod(settings: JSONObject, now: Calendar = Calendar.getInstance()): JSONObject? {
        val arr = settings.optJSONArray("periods") ?: return null
        val day = now.get(Calendar.DAY_OF_WEEK) - 1
        val minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        for (i in 0 until arr.length()) {
            val p = arr.optJSONObject(i) ?: continue
            val days = p.optJSONArray("days") ?: continue
            if ((0 until days.length()).none { days.optInt(it) == day }) continue
            val from = toMinutes(p.optString("from", "00:00"))
            val to = toMinutes(p.optString("to", "00:00"))
            val active = if (from < to) minute in from until to else minute >= from || minute < to
            if (active) return p
        }
        return null
    }

    private fun toMinutes(hm: String): Int {
        val parts = hm.split(":")
        return (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
    }
}
