package com.samsungtoolkit

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ShizukuSetupBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_shizuku, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tvStatus = view.findViewById<TextView>(R.id.tvShizukuStatus)
        val tvSteps = view.findViewById<TextView>(R.id.tvShizukuSteps)
        val btnPrimary = view.findViewById<Button>(R.id.btnPrimary)
        val btnDismiss = view.findViewById<Button>(R.id.btnDismiss)
        val pm = requireContext().packageManager
        val installed = ShizukuHelper.isInstalled(pm)
        val running = ShizukuHelper.isRunning()
        val permitted = ShizukuHelper.hasPermission()
        when {
            !installed -> {
                tvStatus.text = getString(R.string.shizuku_not_installed)
                tvSteps.text = getString(R.string.shizuku_steps_install)
                btnPrimary.text = getString(R.string.install_shizuku)
                btnPrimary.isVisible = true
                btnPrimary.setOnClickListener {
                    startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${ShizukuHelper.SHIZUKU_PKG}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
            !running -> {
                tvStatus.text = getString(R.string.shizuku_not_running)
                tvSteps.text = getString(R.string.shizuku_steps_start)
                btnPrimary.isVisible = false
            }
            !permitted -> {
                tvStatus.text = getString(R.string.shizuku_needs_permission)
                tvSteps.text = getString(R.string.shizuku_steps_grant)
                btnPrimary.text = getString(R.string.grant_permission)
                btnPrimary.isVisible = true
                btnPrimary.setOnClickListener {
                    ShizukuHelper.requestPermission()
                    dismissAllowingStateLoss()
                }
            }
            else -> {
                tvStatus.text = getString(R.string.shizuku_ready)
                tvSteps.text = ""
                btnPrimary.isVisible = false
            }
        }
        btnDismiss.setOnClickListener { dismissAllowingStateLoss() }
    }

    companion object { const val TAG = "ShizukuSetupSheet" }
}
