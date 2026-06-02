package com.samsungtoolkit

data class BatteryInfo(
    val levelPercent: Int,
    val statusCode: Int,
    val pluggedCode: Int,
    val voltageMillivolts: Int,
    val temperatureTenths: Int,
    val technology: String?,
    val sysHealthSoh: Int?,
    val sysCycleCount: Int?,
    val sysManufactureDate: String?,
    val sysChargeFullMah: Int?,
    val sysChargeDesignMah: Int?,
    val sysCurrentMa: Int?,
    val dumpCycleCount: Int?,
    val dumpHealthSoh: Int?,
    val dumpManufactureDate: String?,
    val dumpFirstUseDate: String?,
    val dumpChargeCounter: Int?
) {
    val cycleCount: Int? get() = dumpCycleCount ?: sysCycleCount
    val healthSoh: Int? get() = dumpHealthSoh ?: sysHealthSoh
    val firstUseDate: String? get() = dumpFirstUseDate ?: dumpManufactureDate ?: sysManufactureDate
    val temperatureCelsius: Float get() = temperatureTenths / 10f
}

sealed class BatteryReadResult {
    data class Success(val info: BatteryInfo) : BatteryReadResult()
    data class ShizukuRequired(val partial: BatteryInfo) : BatteryReadResult()
    object Unavailable : BatteryReadResult()
}
