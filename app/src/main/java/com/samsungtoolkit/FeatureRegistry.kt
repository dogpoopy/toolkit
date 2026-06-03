package com.samsungtoolkit

object FeatureRegistry {

    private const val SETTINGS = "com.android.settings"
    private const val PHONE    = "com.android.phone"

    const val SYSTEMUI_PKG     = "com.android.systemui"
    const val DEX_TOUCHPAD_CLS = "com.android.systemui.dextouchpad.activity.TouchpadActivity"

    private const val DEX_MODE  = "com.android.settings.Settings\$DexModeActivity"
    private const val DEX_ENTRY = "com.android.settings.Settings\$DexEntryScreenSettingsActivity"

    private const val PHONE_INFO_V2 = "com.android.phone.settings.hiddenmenu.PhoneInformationV2"

    fun getSections(): List<FeatureSection> = listOf(
        batterySection(),
        deXSection(),
        networkSection(),
        diagnosticsSection()
    )

    private fun batterySection() = FeatureSection("battery", "Battery", listOf(
        Feature(
            id         = "battery_stats",
            title      = "Battery Statistics",
            subtitle   = "Health · Cycles · First use date · Voltage · Temperature",
            permission = PermissionLevel.SHIZUKU,
            action     = FeatureAction.ShowBatteryStats
        )
    ))

    private fun deXSection() = FeatureSection("dex", "Samsung DeX", listOf(
        Feature(
            id       = "dex_start",
            title    = "Samsung DeX",
            subtitle = "Start or manage DeX mode",
            action   = FeatureAction.LaunchActivity(SETTINGS, DEX_MODE)
        ),
        Feature(
            id       = "dex_about",
            title    = "About DeX",
            subtitle = "Introduction and requirements",
            action   = FeatureAction.LaunchActivity(SETTINGS, DEX_ENTRY)
        ),
        Feature(
            id         = "dex_touchpad_overlay",
            title      = "DeX Touchpad Overlay",
            subtitle   = "Native desktop trackpad on your phone screen",
            permission = PermissionLevel.SHIZUKU,
            action     = FeatureAction.LaunchDeXTouchpad
        )
    ))

    private fun networkSection() = FeatureSection("network", "Network", listOf(
        Feature(
            id       = "phone_info",
            title    = "Phone Information",
            subtitle = "Set preferred network type (5G/LTE) and signal diagnostics",
            action   = FeatureAction.LaunchActivity(PHONE, PHONE_INFO_V2)
        ),
        Feature(
            id       = "field_test",
            title    = "Field Test Mode",
            subtitle = "Detailed signal strength and cell information",
            action   = FeatureAction.LaunchActivity(
                "com.samsung.android.app.telephonyui",
                "com.samsung.android.app.telephonyui.debugapp.FieldTestActivity"
            )
        )
    ))

    private fun diagnosticsSection() = FeatureSection("diagnostics", "Diagnostics", listOf(
        Feature(
            id       = "sysdump",
            title    = "SysDump",
            subtitle = "Export system logs for battery and diagnostic data",
            action   = FeatureAction.LaunchActivity(
                "com.samsung.android.logmanager",
                "com.samsung.android.logmanager.start.SplashActivity"
            )
        ),
        Feature(
            id       = "hw_test",
            title    = "Hardware Test",
            subtitle = "Screen, sensors, buttons and speakers",
            action   = FeatureAction.LaunchActivity(
                "com.sec.factory",
                "com.sec.factory.HwTestMain"
            )
        )
    ))
}
