package com.unhook.app

import android.app.Activity
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal

/** Fingeravtrykk (og annen biometri) via Androids innebygde BiometricPrompt. Krever Android 9 eller nyere. */
object Biometric {

    /** Er biometri tilgjengelig og satt opp på telefonen? */
    @Suppress("DEPRECATION")
    fun available(a: Activity): Boolean {
        if (Build.VERSION.SDK_INT < 28) return false
        if (Build.VERSION.SDK_INT >= 30) {
            val bm = a.getSystemService(BiometricManager::class.java) ?: return false
            return bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
        }
        if (Build.VERSION.SDK_INT == 29) {
            val bm = a.getSystemService(BiometricManager::class.java) ?: return false
            return bm.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
        }
        return a.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
    }

    /** Viser fingeravtrykk-vinduet. [onResult] får true hvis brukeren ble godkjent, ellers false. */
    fun authenticate(a: Activity, onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < 28) {
            onResult(false)
            return
        }
        val executor = a.mainExecutor
        var done = false
        val finish: (Boolean) -> Unit = { ok ->
            if (!done) {
                done = true
                onResult(ok)
            }
        }
        val prompt = BiometricPrompt.Builder(a)
            .setTitle("Unlock Unhook")
            .setNegativeButton("Use code", executor) { _, _ -> finish(false) }
            .build()
        prompt.authenticate(
            CancellationSignal(),
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    finish(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    finish(false)
                }
            }
        )
    }
}
