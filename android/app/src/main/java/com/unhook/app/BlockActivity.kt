package com.unhook.app

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

/** Vises over en blokkert app i en fast periode. */
class BlockActivity : ComponentActivity() {

    companion object {
        const val EXTRA_APP = "app"
        const val EXTRA_UNTIL = "until"
        const val EXTRA_TIMEOUT = "timeout"
        const val EXTRA_DAILY = "daily"
        /** Unhook selv er låst (se «Lock Unhook too»). */
        const val EXTRA_SELF = "self"
    }

    private var self = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render(intent)
        // Tilbake skal ikke føre tilbake til den blokkerte appen.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        render(intent)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun render(intent: Intent) {
        val app = intent.getStringExtra(EXTRA_APP) ?: "Appen"
        val until = intent.getStringExtra(EXTRA_UNTIL).orEmpty()
        val timeout = intent.getBooleanExtra(EXTRA_TIMEOUT, false)
        val daily = intent.getBooleanExtra(EXTRA_DAILY, false)
        self = intent.getBooleanExtra(EXTRA_SELF, false)

        val title = TextView(this).apply {
            text = when {
                self -> "Unhook is locked"
                daily -> "Today's limit has been reached"
                timeout -> "Time is up"
                else -> "$app is locked"
            }
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(getColor(R.color.ink))
        }
        val body = TextView(this).apply {
            text = when {
                self -> "Unhook is locked together with your apps until $until. Do something else for a while."
                daily -> "All the apps on your list are locked for the rest of the day. They open again at midnight."
                timeout -> "All the apps on your list are locked until $until. Do something else for a while."
                until.isNotEmpty() -> "The lock lasts until $until. Do something else for a while."
                else -> "Do something else for a while."
            }
            textSize = 17f
            setTextColor(getColor(R.color.muted))
            setPadding(0, dp(12), 0, dp(32))
        }
        val home = Button(this).apply {
            text = "Go to home screen"
            isAllCaps = false
            textSize = 16f
            setTextColor(getColor(R.color.accent_ink))
            backgroundTintList = ColorStateList.valueOf(getColor(R.color.accent))
            setOnClickListener { goHome() }
        }
        val open = Button(this, null, android.R.attr.borderlessButtonStyle).apply {
            text = "Open Unhook"
            isAllCaps = false
            textSize = 16f
            setTextColor(getColor(R.color.accent))
            setOnClickListener {
                startActivity(Intent(this@BlockActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                finish()
            }
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(getColor(R.color.bg))
            setPadding(dp(28), dp(28), dp(28), dp(28))
            addView(title)
            addView(body)
            addView(home, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)))
            if (!self) addView(open, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)))
        }
        setContentView(content)
        // Holder innholdet unna statuslinjen og navigasjonsfeltet (appen tegnes ut til kantene).
        Insets.applyPadding(content, dp(28))
    }

    private fun goHome() {
        startActivity(Notifier.homeIntent())
        // Når Unhook er låst, lukkes den også, så den ikke ligger klar i bakgrunnen.
        if (self) finishAffinity() else finish()
    }
}
