package com.samsungtoolkit

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object ShizukuHelper {

    const val SHIZUKU_PKG = "moe.shizuku.privileged.api"
    const val REQUEST_CODE = 1001

    fun isInstalled(pm: PackageManager): Boolean = try {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(SHIZUKU_PKG, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) { false }

    fun isRunning(): Boolean = try { Shizuku.pingBinder() } catch (_: Throwable) { false }

    fun hasPermission(): Boolean {
        if (!isRunning()) return false
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) { false }
    }

    fun requestPermission() {
        try { Shizuku.requestPermission(REQUEST_CODE) } catch (_: Throwable) {}
    }

    fun runShellCommand(command: String): String? {
        if (!hasPermission()) return null
        return try {
            val proc = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            val out = proc.inputStream.bufferedReader().readText().trim()
            proc.waitFor()
            out.ifEmpty { null }
        } catch (_: Throwable) { null }
    }

    fun launchNonExportedActivity(pkg: String, cls: String): Boolean {
        val out = runShellCommand("am start -n $pkg/$cls 2>&1") ?: return false
        val l = out.lowercase()
        return "error" !in l && "exception" !in l && "denied" !in l
    }
}
