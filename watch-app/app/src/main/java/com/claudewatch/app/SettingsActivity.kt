package com.claudewatch.app

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.claudewatch.app.network.ConnectionStatus
import com.claudewatch.app.relay.RelayClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class SettingsActivity : Activity() {

    private lateinit var phoneStatusText: TextView
    private lateinit var connectionStatusText: TextView
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        phoneStatusText = findViewById(R.id.phoneStatusText)
        connectionStatusText = findViewById(R.id.connectionStatusText)

        loadPhoneInfo()
    }

    private fun loadPhoneInfo() {
        scope.launch {
            try {
                val nodes = withContext(Dispatchers.IO) {
                    Wearable.getNodeClient(this@SettingsActivity).connectedNodes.await()
                }
                if (nodes.isNotEmpty()) {
                    val node = nodes.first()
                    phoneStatusText.text = node.displayName
                    phoneStatusText.setTextColor(getColor(android.R.color.holo_green_light))
                } else {
                    phoneStatusText.text = "No phone connected"
                    phoneStatusText.setTextColor(getColor(android.R.color.holo_red_light))
                }
            } catch (e: Exception) {
                phoneStatusText.text = "Error: ${e.message}"
                phoneStatusText.setTextColor(getColor(android.R.color.holo_red_light))
            }

            // Show WebSocket relay status
            val wsStatus = RelayClient.onWebSocketStatus
            connectionStatusText.text = when {
                wsStatus != null -> "Relay active"
                else -> "Relay idle"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
