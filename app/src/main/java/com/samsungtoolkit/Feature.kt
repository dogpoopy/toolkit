package com.samsungtoolkit

enum class PermissionLevel { FREE, SHIZUKU }

sealed class FeatureAction {
    data class LaunchActivity(val pkg: String, val cls: String) : FeatureAction()
    data class ChoiceDialog(
        val title: String,
        val choices: List<Pair<String, LaunchActivity>>
    ) : FeatureAction()
    object ShowBatteryStats : FeatureAction()
    object LaunchDeXTouchpad : FeatureAction()
}

data class Feature(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val permission: PermissionLevel = PermissionLevel.FREE,
    val action: FeatureAction
)

data class FeatureSection(
    val id: String,
    val title: String,
    val features: List<Feature>
)
