package com.samsungtoolkit

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object BatteryReader {

    private val P_HEALTH = listOf(
        "/sys/class/power_supply/battery/batt_asoc",
        "/sys/class/power_supply/battery/capacity_raw"
    )
    private val P_CYCLES = listOf(
        "/sys/class/power_supply/battery/cycle_count",
        "/sys/class/power_supply/battery/charge_cycle_count"
    )
    private val P_DATE = listOf(
        "/sys/class/power_supply/battery/first_use_date",
        "/sys/class/power_supply/battery/manufacture_date",
        "/sys/class/power_supply/battery/batt_manufacture_date"
    )
    private val P_FULL = listOf("/sys/class/power_supply/battery/charge_full")
    private val P_DESIGN = listOf("/sys/class/power_supply/battery/charge_full_design")
    private val P_CURRENT = listOf(
        "/sys/class/power_supply/battery/current_now",
        "/sys/class/power_supply/battery/batt_current_ua_now"
    )

    suspend fun read(ctx: Context): BatteryReadResult = withContext(Dispatchers.IO) {
        val t1 = readTier1(ctx) ?: return@withContext BatteryReadResult.Unavailable
        val t2 = readTier2()
        val t3 = if (ShizukuHelper.hasPermission()) readTier3() else null

        val info = BatteryInfo(
            levelPercent = t1.level,
            statusCode = t1.status,
            pluggedCode = t1.plugged,
            voltageMillivolts = t1.voltage,
            temperatureTenths = t1.temp,
            technology = t1.tech,
            sysHealthSoh = t2.healthSoh,
            sysCycleCount = t2.cycleCount,
            sysManufactureDate = t2.mfgDate,
            sysChargeFullMah = t2.fullMah,
            sysChargeDesignMah = t2.designMah,
            sysCurrentMa = t2.currentMa,
            dumpCycleCount = t3?.cycleCount,
            dumpHealthSoh = t3?.healthSoh,
            dumpManufactureDate = t3?.mfgDate,
            dumpFirstUseDate = t3?.firstUseDate,
            dumpChargeCounter = t3?.chargeCounter
        )

        if (ShizukuHelper.isRunning() && !ShizukuHelper.hasPermission())
            BatteryReadResult.ShizukuRequired(info)
        else
            BatteryReadResult.Success(info)
    }

    private data class T1(
        val level: Int, val status: Int, val plugged: Int,
        val voltage: Int, val temp: Int, val tech: String?
    )

    private fun readTier1(ctx: Context): T1? {
        val i = ctx.applicationContext
            .registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        val raw = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100).takeIf { it > 0 } ?: 100
        return T1(
            level = raw * 100 / scale,
            status = i.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN),
            plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
            voltage = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0),
            temp = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0),
            tech = i.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
        )
    }

    private data class T2(
        val healthSoh: Int?, val cycleCount: Int?, val mfgDate: String?,
        val fullMah: Int?, val designMah: Int?, val currentMa: Int?
    )

    private fun readTier2(): T2 {
        val fullRaw = readIntNode(P_FULL)
        val designRaw = readIntNode(P_DESIGN)
        val curRaw = readIntNode(P_CURRENT)
        return T2(
            healthSoh = readIntNode(P_HEALTH),
            cycleCount = readIntNode(P_CYCLES),
            mfgDate = readStrNode(P_DATE),
            fullMah = fullRaw?.let { it / 1000 },
            designMah = designRaw?.let { it / 1000 },
            currentMa = curRaw?.let { it / 1000 }
        )
    }

    private fun readIntNode(paths: List<String>): Int? {
        for (p in paths) try {
            val v = File(p).readText().trim().toIntOrNull() ?: continue
            if (v != 0) return v
        } catch (_: Exception) {}
        return null
    }

    private fun readStrNode(paths: List<String>): String? {
        for (p in paths) try {
            val v = File(p).readText().trim()
            if (v.isNotBlank() && v != "0") return v
        } catch (_: Exception) {}
        return null
    }

    private data class T3(
        val cycleCount: Int?, val healthSoh: Int?,
        val mfgDate: String?, val firstUseDate: String?,
        val chargeCounter: Int?
    )

    private fun readTier3(): T3? {
        val raw = ShizukuHelper.runShellCommand("dumpsys battery") ?: return null
        return T3(
            cycleCount = dumpInt(raw, "Charge cycle count", "cycle count"),
            healthSoh = dumpInt(raw, "State of Health \\(%\\)", "batt_asoc"),
            mfgDate = dumpStr(raw, "Manufacturing date", "manufacture_date"),
            firstUseDate = dumpStr(raw, "First usage date", "first_use_date"),
            chargeCounter = dumpInt(raw, "Charge counter", "charge counter")
        )
    }

    private fun dumpInt(raw: String, vararg keys: String): Int? {
        for (k in keys)
            Regex("""(?i)$k\s*:\s*(-?\d+)""").find(raw)
                ?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        return null
    }

    private fun dumpStr(raw: String, vararg keys: String): String? {
        for (k in keys)
            Regex("""(?i)$k\s*:\s*(.+)""").find(raw)
                ?.groupValues?.get(1)?.trim()
                ?.takeIf { it.isNotBlank() && it != "0" }
                ?.let { return it }
        return null
    }
}
