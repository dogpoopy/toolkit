package com.samsungtoolkit

object FeatureRegistry {

    private const val S = "com.android.settings"
    private const val P = "com.android.phone"
    const val SYSTEMUI_PKG = "com.android.systemui"
    const val DEX_TOUCHPAD_CLS = "com.android.systemui.dextouchpad.activity.TouchpadActivity"

    private const val DEX_MODE = "com.android.settings.Settings\$DexModeActivity"
    private const val DEX_ENTRY = "com.android.settings.Settings\$DexEntryScreenSettingsActivity"
    private const val DEX_KEYBOARD = "com.android.settings.Settings\$DexModeKeyboardSettingsActivity"
    private const val DEX_MOUSE = "com.android.settings.Settings\$DexModeMouseSettingsActivity"
    private const val DEX_SPEN = "com.android.settings.Settings\$DexModeSpenSettingsActivity"
    private const val DEX_GESTURE_CTRL = "com.android.settings.Settings\$DexModeTouchGestureSettingsActivity"
    private const val DEX_GESTURE_OPT = "com.android.settings.Settings\$DexModeTouchGestureOptionSelectionActivity"
    private const val TALKBACK_STYLE = "com.android.settings.Settings\$DexTalkbackSettingsDisplayStyleActivity"
    private const val TALKBACK_POS = "com.android.settings.Settings\$DexTalkbackSettingsDisplayPositionActivity"
    private const val PHONE_INFO_V2 = "com.android.phone.settings.hiddenmenu.PhoneInformationV2"

    fun getSections(): List<FeatureSection> = listOf(
        batterySection(), deXSection(), networkSection(), diagnosticsSection()
    )

    private fun batterySection() = FeatureSection("battery", "Battery", listOf(
        Feature(
            id = "battery_stats",
            title = "Battery Statistics",
            subtitle = "Health · Cycles · First use date · Voltage · Temperature",
            permission = PermissionLevel.SHIZUKU,
            action = FeatureAction.ShowBatteryStats
        )
    ))

    private fun deXSection() = FeatureSection("dex", "Samsung DeX", listOf(
        Feature("dex_start", "Samsung DeX", "Start or manage DeX mode", action = FeatureAction.LaunchActivity(S, DEX_MODE)),
        Feature("dex_about", "About DeX", "Introduction and requirements", action = FeatureAction.LaunchActivity(S, DEX_ENTRY)),
        Feature("dex_keyboard", "Keyboard", "Language and input method", action = FeatureAction.LaunchActivity(S, DEX_KEYBOARD)),
        Feature("dex_mouse", "Mouse & Trackpad", "Pointer speed and button assignments", action = FeatureAction.LaunchActivity(S, DEX_MOUSE)),
        Feature("dex_spen", "S Pen", "S Pen behaviour in DeX", action = FeatureAction.LaunchActivity(S, DEX_SPEN)),
        Feature(
            id = "dex_gestures",
            title = "Touchpad Gestures",
            subtitle = "Gestures and advanced option mapping",
            action = FeatureAction.ChoiceDialog(
                title = "Touchpad Gestures",
                choices = listOf(
                    "Touchpad gestures" to FeatureAction.LaunchActivity(S, DEX_GESTURE_CTRL),
                    "Advanced option mapping" to FeatureAction.LaunchActivity(S, DEX_GESTURE_OPT)
                )
            )
        ),
        Feature(
            id = "dex_touchpad_overlay",
            title = "DeX Touchpad Overlay",
            subtitle = "Native desktop trackpad on your screen",
            permission = PermissionLevel.SHIZUKU,
            action = FeatureAction.LaunchDeXTouchpad
        ),
        Feature("dex_tb_style", "TalkBack Display Style", "Accessibility display style in DeX", action = FeatureAction.LaunchActivity(S, TALKBACK_STYLE)),
        Feature("dex_tb_pos", "TalkBack Display Position", "Accessibility display position in DeX", action = FeatureAction.LaunchActivity(S, TALKBACK_POS))
    ))

    private fun networkSection() = FeatureSection("network", "Network", listOf(
        Feature("phone_info", "Phone Information", "Set preferred network type (5G/LTE), signal info", action = FeatureAction.LaunchActivity(P, PHONE_INFO_V2)),
        Feature("field_test", "Field Test Mode", "Detailed signal strength and cell info", action = FeatureAction.DialCode("*#0011#"))
    ))

    private fun diagnosticsSection() = FeatureSection("diagnostics", "Diagnostics", listOf(
        Feature("sysdump", "SysDump", "Run, copy or delete system logs (*#9900#)", action = FeatureAction.DialCode("*#9900#")),
        Feature("hw_test", "Hardware Test", "Screen, sensors, speakers (*#0*#)", action = FeatureAction.DialCode("*#0*#")),
        Feature("service_mode", "Service Mode", "Factory service menu (*#197328640#)", action = FeatureAction.DialCode("*#197328640#"))
    ))
}
