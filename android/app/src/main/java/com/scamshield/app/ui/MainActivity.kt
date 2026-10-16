package com.scamshield.app.ui

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.scamshield.app.R
import com.scamshield.app.network.RetrofitClient
import com.scamshield.app.services.FloatingShieldService

class MainActivity : AppCompatActivity() {

    private lateinit var switchFloatingBubble: SwitchCompat
    private lateinit var switchAccessibility: SwitchCompat
    private lateinit var switchCallScreening: SwitchCompat
    private lateinit var switchTransferBrake: SwitchCompat

    private lateinit var tvStatusHeadline: TextView
    private lateinit var tvStatusSubtext: TextView
    private lateinit var tvStatusBadge: TextView
    private lateinit var viewStatusIndicator: View
    private lateinit var btnGrantPermissions: Button

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_OVERLAY_PERMISSION = 2001
        private const val REQUEST_CALL_SCREENING_ROLE = 2002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        updateProtectionStatus()
    }

    private fun initViews() {
        switchFloatingBubble = findViewById(R.id.switch_floating_bubble)
        switchAccessibility = findViewById(R.id.switch_accessibility)
        switchCallScreening = findViewById(R.id.switch_call_screening)
        switchTransferBrake = findViewById(R.id.switch_transfer_brake)

        tvStatusHeadline = findViewById(R.id.tv_status_headline)
        tvStatusSubtext = findViewById(R.id.tv_status_subtext)
        tvStatusBadge = findViewById(R.id.tv_status_badge)
        viewStatusIndicator = findViewById(R.id.view_status_indicator)
        btnGrantPermissions = findViewById(R.id.btn_grant_permissions)

        // Action Cards Navigation
        findViewById<View>(R.id.card_action_search).setOnClickListener {
            startActivity(Intent(this, DirectSearchActivity::class.java))
        }

        findViewById<View>(R.id.card_action_call).setOnClickListener {
            startActivity(Intent(this, CallDefenseActivity::class.java))
        }

        findViewById<View>(R.id.card_action_chat).setOnClickListener {
            startActivity(Intent(this, ChatDefenseActivity::class.java))
        }

        findViewById<View>(R.id.card_action_report).setOnClickListener {
            startActivity(Intent(this, ReportScamActivity::class.java))
        }
    }

    private fun setupListeners() {
        // Floating Bubble Switch
        switchFloatingBubble.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (Settings.canDrawOverlays(this)) {
                    startFloatingServiceSafely()
                } else {
                    requestOverlayPermissionSafely()
                }
            } else {
                try {
                    stopService(Intent(this, FloatingShieldService::class.java))
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping FloatingShieldService: ${e.message}")
                }
            }
            updateProtectionStatus()
        }

        // Accessibility Switch
        switchAccessibility.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                Toast.makeText(this, "Enable 'Scam Shield' in Accessibility Services to scan chat & banking", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Open Android Settings > Accessibility to enable Scam Shield", Toast.LENGTH_LONG).show()
            }
        }

        // Call Screening Switch
        switchCallScreening.setOnClickListener {
            requestCallScreeningRoleSafely()
        }

        // One-Click Grant All Permissions
        btnGrantPermissions.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermissionSafely()
            } else {
                requestCallScreeningRoleSafely()
            }
        }
    }

    private fun startFloatingServiceSafely() {
        try {
            if (Settings.canDrawOverlays(this)) {
                val intent = Intent(this, FloatingShieldService::class.java)
                startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not start FloatingShieldService: ${e.message}")
        }
    }

    private fun requestOverlayPermissionSafely() {
        if (!Settings.canDrawOverlays(this)) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
            } catch (e: Exception) {
                try {
                    val fallbackIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    startActivityForResult(fallbackIntent, REQUEST_OVERLAY_PERMISSION)
                } catch (e2: Exception) {
                    Toast.makeText(this, "Please allow 'Display over other apps' in Settings", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun requestCallScreeningRoleSafely() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val roleManager = getSystemService(Context.ROLE_SERVICE) as? RoleManager
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                    if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                        startActivityForResult(intent, REQUEST_CALL_SCREENING_ROLE)
                    } else {
                        Toast.makeText(this, "Call Screening Role is already active!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting Call Screening Role: ${e.message}")
            }
        }
    }

    private fun updateProtectionStatus() {
        val hasOverlay = Settings.canDrawOverlays(this)
        switchFloatingBubble.isChecked = hasOverlay

        if (hasOverlay) {
            tvStatusHeadline.text = "🟢 Active Protection Online"
            tvStatusHeadline.setTextColor(Color.parseColor("#34D399"))
            tvStatusSubtext.text = "Floating Bubble, Call Screening, and Transfer Brake are armed and active."
            tvStatusBadge.text = "ARMED"
            tvStatusBadge.setTextColor(Color.parseColor("#34D399"))
            btnGrantPermissions.visibility = View.GONE
            startFloatingServiceSafely()
        } else {
            tvStatusHeadline.text = "🟡 Tap to Arm Shield"
            tvStatusHeadline.setTextColor(Color.parseColor("#FBBF24"))
            tvStatusSubtext.text = "Enable 'Display over other apps' permission so Scam Shield can show floating risk indicators over LINE, Telegram, and Phone calls."
            tvStatusBadge.text = "SETUP REQUIRED"
            tvStatusBadge.setTextColor(Color.parseColor("#FBBF24"))
            btnGrantPermissions.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        updateProtectionStatus()
    }
}
