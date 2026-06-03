package com.samsungtoolkit

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku

object ShizukuHelper {

    const val SHIZUKU_PKG = "moe.shizuku.privileged.api"
    const val REQUEST_CODE = 1001

    private var shellService: IShellService? = null
    private var currentArgs: Shizuku.UserServiceArgs? = null

    val isServiceBound: Boolean get() = shellService != null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            shellService = IShellService.Stub.asInterface(binder)
        }
        override fun onServiceDisconnected(name: ComponentName) {
            shellService = null
        }
    }

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

    fun bindService(packageName: String) {
        if (!hasPermission() || isServiceBound) return
        val args = Shizuku.UserServiceArgs(
            ComponentName(packageName, ShellService::class.java.name)
        ).daemon(false).version(1)
        currentArgs = args
        try { Shizuku.bindUserService(args, connection) } catch (_: Throwable) {}
    }

    fun unbindService() {
        val args = currentArgs ?: return
        try { Shizuku.unbindUserService(args, connection, true) } catch (_: Throwable) {}
        shellService = null
        currentArgs = null
    }

    fun runShellCommand(command: String): String? = try {
        shellService?.run(command)?.ifEmpty { null }
    } catch (_: Throwable) { null }

    fun launchNonExportedActivity(pkg: String, cls: String): Boolean {
        val out = runShellCommand("am start -n $pkg/$cls 2>&1") ?: return false
        val l = out.lowercase()
        return "error" !in l && "exception" !in l && "denied" !in l
    }
}
