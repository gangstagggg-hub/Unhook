package com.unhook.app

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import org.json.JSONObject

/**
 * Følger med på hvilken app som er i forgrunnen ved hjelp av UsageStatsManager.
 *
 * En økt starter når en sosial app eller nettleser kommer i forgrunnen, og fortsetter
 * selv om du bytter mellom slike apper. Den avsluttes når du har vært borte fra dem
 * i [GRACE_MS], eller skjermen slås av. Når økten starter, og etter 5, 10 og 15 minutter,
 * får du et varsel som foreslår et alternativ: «Hva med å …?».
 */
class UsageMonitorService : Service() {

    companion object {
        private const val TAG = "UnhookMonitor"
        private const val TICK_MS = 3_000L
        private const val GRACE_MS = 20_000L
        private const val LIMIT_MS = 5 * 60_000L
        private const val MIN_SESSION_MS = 30_000L
        // Varsler etter 5, 10 og 15 minutter, ikke flere.
        private const val MAX_STEPS = 3
        // Minst så lang tid mellom to «du åpnet»-varsler, så du ikke får et nytt hver gang du hopper inn og ut.
        private const val REMINDER_COOLDOWN_MS = 2 * 60_000L
        // ACTIVITY_RESUMED (API 29) har samme verdi som MOVE_TO_FOREGROUND (API 21).
        private const val EVENT_RESUMED = 1

        fun startIfAllowed(c: Context) {
            if (!UsageAccess.granted(c)) return
            try {
                c.startForegroundService(Intent(c, UsageMonitorService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "Kunne ikke starte tjenesten", e)
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var usm: UsageStatsManager
    private lateinit var power: PowerManager

    private var lastQuery = 0L
    private var foreground: String? = null
    private var sessionStart = 0L
    private var sessionApp = ""
    private var lastSeenTracked = 0L
    private var notifiedSteps = 0
    private var lastBlockAt = 0L
    private var lastReminderAt = 0L

    private val loop = object : Runnable {
        override fun run() {
            try {
                tick()
            } catch (e: Exception) {
                Log.w(TAG, "Feil i overvåkingen", e)
            }
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        usm = getSystemService(UsageStatsManager::class.java)
        power = getSystemService(PowerManager::class.java)
        Notifier.createChannels(this)
        val n = Notifier.monitor(this)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(Notifier.ID_MONITOR, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(Notifier.ID_MONITOR, n)
        }
        // Se litt bakover, så vi vet hvilken app som er åpen nå.
        lastQuery = System.currentTimeMillis() - 10 * 60_000L
        handler.post(loop)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(loop)
        if (sessionStart != 0L) endSession(lastSeenTracked)
        super.onDestroy()
    }

    private fun readForeground(now: Long) {
        val events = usm.queryEvents(lastQuery, now)
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == EVENT_RESUMED) foreground = e.packageName
        }
        lastQuery = now
    }

    private fun tick() {
        if (!UsageAccess.granted(this)) {
            stopSelf()
            return
        }
        val now = System.currentTimeMillis()
        readForeground(now)

        val settings = Store.settings(this)
        val fg = if (power.isInteractive) foreground else null

        if (fg != null && fg in Apps.trackedPackages(settings)) {
            sessionApp = Apps.label(fg)
            if (sessionStart == 0L) {
                sessionStart = now
                notifiedSteps = 0
                if (now - lastReminderAt > REMINDER_COOLDOWN_MS) {
                    lastReminderAt = now
                    Notifier.opened(this, sessionApp, settings)
                }
            }
            lastSeenTracked = now
            Store.current = JSONObject().put("start", sessionStart).put("app", sessionApp).toString()

            val steps = ((now - sessionStart) / LIMIT_MS).toInt()
            if (steps > notifiedSteps && steps <= MAX_STEPS) {
                notifiedSteps = steps
                Notifier.overLimit(this, steps * 5, sessionApp, settings)
            }

            if (fg in Apps.blockedPackages(settings) && now - lastBlockAt > 4_000L) {
                Schedule.activePeriod(settings)?.let { period ->
                    lastBlockAt = now
                    block(sessionApp, period.optString("to"))
                }
            }
        } else if (sessionStart != 0L && (fg == null || now - lastSeenTracked > GRACE_MS)) {
            endSession(if (fg == null) now else lastSeenTracked)
        }
    }

    private fun endSession(end: Long) {
        if (end - sessionStart >= MIN_SESSION_MS) Store.addSession(this, sessionStart, end, sessionApp)
        sessionStart = 0L
        notifiedSteps = 0
        Store.current = null
        Notifier.cancelAlert(this)
    }

    private fun block(app: String, until: String) {
        if (Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(this, BlockActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(BlockActivity.EXTRA_APP, app)
                    .putExtra(BlockActivity.EXTRA_UNTIL, until)
            )
        } else {
            Notifier.blocked(this, app, until)
        }
    }
}
