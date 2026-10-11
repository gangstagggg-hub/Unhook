package com.unhook.app

import org.json.JSONObject

/** Kobler navnene i web-appen til Android-pakkenavn. */
object Apps {
    private val BROWSERS = listOf(
        "com.android.chrome", "org.mozilla.firefox", "com.sec.android.app.sbrowser",
        "com.opera.browser", "com.brave.browser", "com.microsoft.emmx",
        "com.duckduckgo.mobile.android", "com.vivaldi.browser"
    )

    private val known: Map<String, List<String>> = linkedMapOf(
        "Instagram" to listOf("com.instagram.android", "com.instagram.lite"),
        "TikTok" to listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
        "Facebook" to listOf("com.facebook.katana", "com.facebook.lite"),
        "YouTube" to listOf("com.google.android.youtube"),
        "X" to listOf("com.twitter.android"),
        "Snapchat" to listOf("com.snapchat.android"),
        "Reddit" to listOf("com.reddit.frontpage"),
        "Threads" to listOf("com.instagram.barcelona"),
        "Pinterest" to listOf("com.pinterest"),
        "Browser" to BROWSERS,
        // Eldre navn fra den norske versjonen, så lagrede innstillinger fortsatt virker.
        "Nettleser" to BROWSERS
    )

    /** Et kjent navn gir pakkene til appen. Et navn med punktum tolkes som et pakkenavn. */
    fun packagesFor(name: String): List<String> {
        val trimmed = name.trim()
        known.entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) }?.let { return it.value }
        return if (trimmed.contains('.')) listOf(trimmed) else emptyList()
    }

    /** Pakkene for en rad i app-listen: valgt pakkenavn hvis det finnes, ellers kjent navn. */
    private fun packagesFor(o: JSONObject): List<String> {
        val pkg = o.optString("pkg")
        return if (pkg.isNotBlank()) listOf(pkg) else packagesFor(o.optString("name"))
    }

    private val customLabels = mutableMapOf<String, String>()

    fun label(pkg: String): String = known.entries.firstOrNull { pkg in it.value }?.key ?: customLabels[pkg] ?: pkg

    /** Apper som er slått på i blokkeringslisten. */
    fun blockedPackages(settings: JSONObject): Set<String> {
        val out = mutableSetOf<String>()
        val arr = settings.optJSONArray("blocked") ?: return out
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optBoolean("on")) out += packagesFor(o)
        }
        return out
    }

    /** Egen tid (i millisekunder) for en app på lista, eller 0 hvis den bruker hovedtiden. */
    fun ownLimitMs(settings: JSONObject, pkg: String): Long {
        val arr = settings.optJSONArray("blocked") ?: return 0L
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (!o.optBoolean("on")) continue
            val minutes = o.optInt("limit", 0)
            if (minutes > 0 && pkg in packagesFor(o)) return minutes * 60_000L
        }
        return 0L
    }

    /** Alt som teller som scrolling: kjente sosiale apper, nettlesere og egne apper fra listen. */
    fun trackedPackages(settings: JSONObject): Set<String> {
        val out = known.values.flatten().toMutableSet()
        val arr = settings.optJSONArray("blocked") ?: return out
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { o ->
                val pkgs = packagesFor(o)
                pkgs.forEach { customLabels[it] = o.optString("name") }
                out += pkgs
            }
        }
        return out
    }
}
