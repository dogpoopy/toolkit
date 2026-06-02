package com.samsungtoolkit

import android.os.BatteryManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class BatteryStatsBottomSheet : BottomSheetDialogFragment() {

    var onOpenShizukuSetup: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_battery, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadStats(view)
    }

    private fun loadStats(root: View) {
        val progress = root.findViewById<ProgressBar>(R.id.progress)
        val layoutContent = root.findViewById<LinearLayout>(R.id.layoutContent)
        val bannerShizuku = root.findViewById<LinearLayout>(R.id.bannerShizuku)
        val btnUpgrade = root.findViewById<Button>(R.id.btnUpgradeShizuku)

        progress.isVisible = true
        layoutContent.isVisible = false
        bannerShizuku.isVisible = false

        lifecycleScope.launch {
            val result = BatteryReader.read(requireContext())
            progress.isVisible = false
            layoutContent.isVisible = true
            when (result) {
                is BatteryReadResult.Success -> bind(root, result.info)
                is BatteryReadResult.ShizukuRequired -> {
                    bind(root, result.partial)
                    bannerShizuku.isVisible = true
                    btnUpgrade.setOnClickListener {
                        dismissAllowingStateLoss()
                        onOpenShizukuSetup?.invoke()
                    }
                }
                BatteryReadResult.Unavailable -> bind(root, null)
            }
        }
    }

    private fun bind(root: View, info: BatteryInfo?) {
        val ring = root.findViewById<CircularProgressIndicator>(R.id.progressHealth)
        val tvPct = root.findViewById<TextView>(R.id.tvHealthPct)
        val tvLabel = root.findViewById<TextView>(R.id.tvHealthLabel)
        val soh = info?.healthSoh
        ring.max = 100
        ring.setProgressCompat(soh ?: 0, soh != null)
        tvPct.text = if (soh != null) "$soh%" else "—"
        tvLabel.text = when {
            soh == null -> getString(R.string.not_available)
            soh >= 80 -> getString(R.string.health_good)
            soh >= 60 -> getString(R.string.health_fair)
            else -> getString(R.string.health_poor)
        }
        if (soh != null) {
            ring.setIndicatorColor(ContextCompat.getColor(requireContext(), when {
                soh >= 80 -> R.color.health_good
                soh >= 60 -> R.color.health_fair
                else -> R.color.health_poor
            }))
        }
        fun set(id: Int, v: String?) { root.findViewById<TextView>(id).text = v ?: "—" }
        set(R.id.tvCycles, info?.cycleCount?.toString())
        set(R.id.tvDate, info?.firstUseDate?.let { formatDate(it) })
        set(R.id.tvCap, capacityStr(info))
        set(R.id.tvLevel, info?.let { "${it.levelPercent}%" })
        set(R.id.tvVoltage, info?.let { "${it.voltageMillivolts} mV" })
        set(R.id.tvTemp, info?.let { "%.1f °C".format(it.temperatureCelsius) })
        set(R.id.tvCurrent, info?.sysCurrentMa?.let { "${abs(it)} mA" })
        set(R.id.tvTech, info?.technology)
        set(R.id.tvStatus, info?.let { statusStr(it.statusCode, it.pluggedCode) })
    }

    private fun capacityStr(info: BatteryInfo?): String? = when {
        info == null -> null
        info.sysChargeFullMah != null && info.sysChargeDesignMah != null ->
            "${info.sysChargeFullMah} / ${info.sysChargeDesignMah} mAh"
        info.sysChargeFullMah != null -> "${info.sysChargeFullMah} mAh"
        else -> null
    }

    private fun statusStr(status: Int, plugged: Int): String {
        val plug = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> " (AC)"
            BatteryManager.BATTERY_PLUGGED_USB -> " (USB)"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> " (Wireless)"
            else -> ""
        }
        return when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging$plug"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
            else -> "Unknown"
        }
    }

    private fun formatDate(raw: String): String {
        if (raw.matches(Regex("""^\d{8}$"""))) return try {
            val d = SimpleDateFormat("yyyyMMdd", Locale.US).parse(raw)!!
            SimpleDateFormat("MMM d, yyyy", Locale.US).format(d)
        } catch (_: Exception) { raw }
        Regex("""^(\d{4}-\d{2}-\d{2})""").find(raw)?.let { m ->
            return try {
                val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(m.value)!!
                SimpleDateFormat("MMM d, yyyy", Locale.US).format(d)
            } catch (_: Exception) { raw }
        }
        raw.toLongOrNull()?.takeIf { it > 1_000_000_000L }?.let {
            return SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(it * 1000L))
        }
        return raw.take(30)
    }

    companion object { const val TAG = "BatteryStatsSheet" }
}
