package com.unhook.app

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process

object UsageAccess {
    /** Har brukeren slått på «Tilgang til bruksdata» for Unhook? */
    fun granted(c: Context): Boolean {
        val ops = c.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
