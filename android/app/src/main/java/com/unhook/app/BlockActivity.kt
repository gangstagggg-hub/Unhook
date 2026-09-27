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
    }

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

        val title = TextView(this).apply {
            text = "$app er blokkert"
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(getColor(R.color.ink))
        }
        val body = TextView(this).apply {
            text = if (until.isNotEmpty()) "Blokkeringen varer til $until. Gjør noe annet en stund." else "Gjør noe annet en stund."
            textSize = 17f
            setTextColor(getColor(R.color.muted))
            setPadding(0, dp(12), 0, dp(32))
        }
        val home = Button(this).apply {
            text = "Til hjemskjermen"
            isAllCaps = false
            textSize = 16f
            setTextColor(getColor(R.color.accent_ink))
            backgroundTintList = ColorStateList.valueOf(getColor(R.color.accent))
            setOnClickListener { goHome() }
        }
        val open = Button(this, null, android.R.attr.borderlessButtonStyle).apply {
            text = "Åpne Unhook"
            isAllCaps = false
            textSize = 16f
            setTextColor(getColor(R.color.accent))
            setOnClickListener {
                startActivity(Intent(this@BlockActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                finish()
            }
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(getColor(R.color.bg))
            setPadding(dp(28), dp(28), dp(28), dp(28))
            addView(title)
            addView(body)
            addView(home, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)))
            addView(open, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)))
        })
    }

    private fun goHome() {
        startActivity(Notifier.homeIntent())
        finish()
    }
}
