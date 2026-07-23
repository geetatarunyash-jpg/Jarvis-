package com.jarvis.assistant

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class AppLauncher(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    fun launchByName(spokenName: String): Boolean {
        val target = normalize(spokenName)
        val launchableApps = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            0
        )

        val exact = launchableApps.firstOrNull {
            normalize(it.loadLabel(pm).toString()) == target
        }
        val partial = launchableApps.firstOrNull {
            normalize(it.loadLabel(pm).toString()).contains(target) ||
                target.contains(normalize(it.loadLabel(pm).toString()))
        }

        val match = exact ?: partial ?: return false
        val packageName = match.activityInfo.packageName
        val launchIntent = pm.getLaunchIntentForPackage(packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return true
    }

    private fun normalize(s: String) = s.lowercase().trim().replace(Regex("[^a-z0-9 ]"), "")
}
