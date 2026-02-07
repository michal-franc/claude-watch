package com.claudewatch.app.relay

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.claudewatch.app.MainActivity
import com.google.android.gms.wearable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * WearableListenerService on the watch that receives messages and channel data
 * from the phone relay and dispatches them to RelayClient.
 */
class RelayMessageService : WearableListenerService() {

    companion object {
        private const val TAG = "RelayMsgService"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "Message received: ${messageEvent.path}")

        when (messageEvent.path) {
            RelayClient.PATH_HTTP_RESPONSE -> {
                RelayClient.onHttpResponse(messageEvent.data)
            }
            RelayClient.PATH_WS_MESSAGE -> {
                RelayClient.onWsMessage(messageEvent.data)
                tryLaunchForPermission(messageEvent.data)
            }
            RelayClient.PATH_WS_STATUS -> {
                RelayClient.onWsStatus(messageEvent.data)
            }
            RelayClient.PATH_AUDIO_UPLOAD_RESPONSE -> {
                RelayClient.onAudioUploadResponse(messageEvent.data)
            }
            else -> {
                Log.w(TAG, "Unknown message path: ${messageEvent.path}")
            }
        }
    }

    private fun tryLaunchForPermission(data: ByteArray) {
        try {
            val json = JSONObject(String(data, Charsets.UTF_8))
            if (json.optString("type") != "permission") return

            Log.i(TAG, "Permission request received — waking screen and launching app")

            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "claudewatch:permission"
            )
            wakeLock.acquire(5_000L)

            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                putExtra("from_permission", true)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching for permission", e)
        }
    }

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        val path = channel.path
        Log.d(TAG, "Channel opened: $path")

        // Audio download data comes via channel: /relay/audio/download/data/{requestId}
        if (path.startsWith("/relay/audio/download/data/")) {
            val requestId = path.removePrefix("/relay/audio/download/data/")
            scope.launch {
                try {
                    val inputStream = Wearable.getChannelClient(this@RelayMessageService)
                        .getInputStream(channel).await()

                    val buffer = ByteArrayOutputStream()
                    val chunk = ByteArray(8192)
                    while (true) {
                        val bytesRead = inputStream.read(chunk)
                        if (bytesRead == -1) break
                        buffer.write(chunk, 0, bytesRead)
                    }
                    inputStream.close()

                    val audioData = buffer.toByteArray()
                    Log.d(TAG, "Received audio download: $requestId (${audioData.size} bytes)")
                    RelayClient.onAudioDownloadData(requestId, audioData)
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading audio download channel", e)
                    RelayClient.onAudioDownloadData(requestId, byteArrayOf())
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
