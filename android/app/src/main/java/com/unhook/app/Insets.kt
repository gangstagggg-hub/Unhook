package com.unhook.app

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Fra Android 15 tegnes appen helt ut til kantene, bak statuslinjen og navigasjonsfeltet.
 * Denne funksjonen legger polstring rundt innholdet, så ingenting havner bak dem. Tastaturet regnes med.
 */
object Insets {
    fun applyPadding(view: View, extraPx: Int = 0) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout() or
                    WindowInsetsCompat.Type.ime()
            )
            v.setPadding(bars.left + extraPx, bars.top + extraPx, bars.right + extraPx, bars.bottom + extraPx)
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(view)
    }
}
