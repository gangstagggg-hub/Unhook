package com.unhook.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Starter overvåkingen igjen etter omstart eller oppdatering av appen. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            UsageMonitorService.startIfAllowed(context)
        }
    }
}
