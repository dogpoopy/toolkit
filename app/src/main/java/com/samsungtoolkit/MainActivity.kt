package com.samsungtoolkit

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.samsungtoolkit.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var pendingFeature: Feature? = null

    private val shizukuListener = Shizuku.OnRequestPermissionResultListener { code, result ->
        if (code == ShizukuHelper.REQUEST_CODE) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                ShizukuHelper.bindService(packageName)
                pendingFeature?.let { executeAction(it.action) }
            } else {
                toast(R.string.shizuku_permission_denied)
            }
            pendingFeature = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        applyEdgeToEdge()
        Shizuku.addRequestPermissionResultListener(shizukuListener)
        if (ShizukuHelper.hasPermission()) ShizukuHelper.bindService(packageName)
        setupList()
    }

    override fun onDestroy() {
        super.onDestroy()
        ShizukuHelper.unbindService()
        Shizuku.removeRequestPermissionResultListener(shizukuListener)
    }

    private fun applyEdgeToEdge() {
        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, binding.root).apply {
            isAppearanceLightStatusBars = !isNight
            isAppearanceLightNavigationBars = !isNight
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.updatePadding(top = bars.top)
            binding.recyclerView.updatePadding(bottom = bars.bottom + dp(24))
            insets
        }
    }

    private fun setupList() {
        val adapter = DashboardAdapter(::onFeatureClick)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            this.adapter = adapter
        }
        adapter.submitList(DashboardAdapter.buildItems(FeatureRegistry.getSections()))
    }

    private fun onFeatureClick(feature: Feature) {
        val needsShizuku = feature.permission == PermissionLevel.SHIZUKU &&
                feature.action !is FeatureAction.ShowBatteryStats
        when {
            !needsShizuku                 -> executeAction(feature.action)
            ShizukuHelper.hasPermission() -> executeAction(feature.action)
            ShizukuHelper.isRunning()     -> { pendingFeature = feature; ShizukuHelper.requestPermission() }
            else                          -> showShizukuSetup()
        }
    }

    private fun executeAction(action: FeatureAction) {
        when (action) {
            is FeatureAction.LaunchActivity  -> safeLaunch(action.pkg, action.cls)
            is FeatureAction.ChoiceDialog    -> showChoiceDialog(action)
            is FeatureAction.ShowBatteryStats -> showBatteryStats()
            is FeatureAction.LaunchDeXTouchpad -> launchDeXTouchpad()
        }
    }

    private fun safeLaunch(pkg: String, cls: String) {
        try {
            startActivity(Intent().apply {
                component = ComponentName(pkg, cls)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: ActivityNotFoundException) { toast(R.string.error_not_found)  }
          catch (e: SecurityException)          { toast(R.string.error_permission) }
          catch (_: Exception)                  { toast(R.string.error_unavailable) }
    }

    private fun showChoiceDialog(action: FeatureAction.ChoiceDialog) {
        AlertDialog.Builder(this)
            .setTitle(action.title)
            .setItems(action.choices.map { it.first }.toTypedArray()) { _, i ->
                executeAction(action.choices[i].second)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showBatteryStats() {
        BatteryStatsBottomSheet().apply {
            onOpenShizukuSetup = { showShizukuSetup() }
        }.show(supportFragmentManager, BatteryStatsBottomSheet.TAG)
    }

    private fun launchDeXTouchpad() {
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                ShizukuHelper.launchNonExportedActivity(
                    FeatureRegistry.SYSTEMUI_PKG,
                    FeatureRegistry.DEX_TOUCHPAD_CLS
                )
            }
            if (!ok) toast(R.string.error_not_found)
        }
    }

    private fun showShizukuSetup() {
        ShizukuSetupBottomSheet().show(supportFragmentManager, ShizukuSetupBottomSheet.TAG)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(resId: Int) = Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
}
