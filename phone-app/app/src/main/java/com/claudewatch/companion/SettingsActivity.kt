package com.claudewatch.companion

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.claudewatch.companion.databinding.ActivitySettingsBinding
import com.claudewatch.companion.wakeword.WakeWordService

class SettingsActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "claude_companion_prefs"
        private const val KEY_SERVER_ADDRESS = "server_address"
        private const val KEY_KIOSK_MODE = "kiosk_mode"
        private const val KEY_AUTO_RETRY = "auto_retry"
        private const val KEY_WAKE_WORD = "wake_word"
        private const val DEFAULT_SERVER = "192.168.1.100:5567"

        fun getServerAddress(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_SERVER_ADDRESS, DEFAULT_SERVER) ?: DEFAULT_SERVER
        }

        fun isKioskModeEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_KIOSK_MODE, false)
        }

        fun isAutoRetryEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_AUTO_RETRY, false)
        }

        fun isWakeWordEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_WAKE_WORD, false)
        }
    }

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Load current settings
        binding.serverAddressInput.setText(getServerAddress(this))
        binding.kioskModeSwitch.isChecked = isKioskModeEnabled(this)
        binding.autoRetrySwitch.isChecked = isAutoRetryEnabled(this)
        binding.wakeWordSwitch.isChecked = isWakeWordEnabled(this)

        // Permission click listeners
        binding.permissionMicRow.setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            })
        }
        binding.permissionNotifRow.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                })
            } else {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                })
            }
        }
        binding.permissionOverlayRow.setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.fromParts("package", packageName, null)
            })
        }

        refreshPermissionStatus()

        // Save button
        binding.saveButton.setOnClickListener {
            saveSettings()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStatus()
    }

    private fun refreshPermissionStatus() {
        val hasMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val hasOverlay = Settings.canDrawOverlays(this)

        setPermissionStatus(binding.permissionMicStatus, hasMic)
        setPermissionStatus(binding.permissionNotifStatus, hasNotif)
        setPermissionStatus(binding.permissionOverlayStatus, hasOverlay)
    }

    private fun setPermissionStatus(view: android.widget.TextView, granted: Boolean) {
        if (granted) {
            view.setText(R.string.permission_granted)
            view.setTextColor(ContextCompat.getColor(this, R.color.status_connected))
        } else {
            view.setText(R.string.permission_not_granted)
            view.setTextColor(ContextCompat.getColor(this, R.color.status_disconnected))
        }
    }

    private fun saveSettings() {
        val serverAddress = binding.serverAddressInput.text.toString().trim()
        val kioskMode = binding.kioskModeSwitch.isChecked
        val autoRetry = binding.autoRetrySwitch.isChecked
        val wakeWord = binding.wakeWordSwitch.isChecked

        if (serverAddress.isEmpty()) {
            binding.serverAddressInput.error = "Server address is required"
            return
        }

        prefs.edit()
            .putString(KEY_SERVER_ADDRESS, serverAddress)
            .putBoolean(KEY_KIOSK_MODE, kioskMode)
            .putBoolean(KEY_AUTO_RETRY, autoRetry)
            .putBoolean(KEY_WAKE_WORD, wakeWord)
            .apply()

        if (wakeWord) {
            when (WakeWordService.start(this)) {
                WakeWordService.Companion.StartResult.NO_MIC_PERMISSION ->
                    Toast.makeText(this, "Wake word requires microphone permission", Toast.LENGTH_LONG).show()
                WakeWordService.Companion.StartResult.NO_ACCESS_KEY ->
                    Toast.makeText(this, "Wake word access key not configured", Toast.LENGTH_LONG).show()
                WakeWordService.Companion.StartResult.NO_OVERLAY_PERMISSION ->
                    Toast.makeText(this, "Enable 'Draw over other apps' for wake word on lock screen", Toast.LENGTH_LONG).show()
                WakeWordService.Companion.StartResult.OK -> {}
            }
        } else {
            WakeWordService.stop(this)
        }

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
