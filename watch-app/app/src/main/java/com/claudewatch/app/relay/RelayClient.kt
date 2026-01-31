package com.claudewatch.app.relay

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton relay client that routes all watch network requests through
 * the phone via Wearable DataLayer API instead of direct OkHttp calls.
 *
 * - MessageClient for small payloads (HTTP requests/responses, WebSocket messages)
 * - ChannelClient for large payloads (audio upload/download)
 */
object RelayClient {
    private const val TAG = "RelayClient"
    private const val HTTP_TIMEOUT_MS = 30_000L
    private const val AUDIO_TIMEOUT_MS = 60_000L

    // Message paths
    const val PATH_WS_CONNECT = "/relay/ws/connect"
    const val PATH_WS_DISCONNECT = "/relay/ws/disconnect"
    const val PATH_WS_MESSAGE = "/relay/ws/message"
    const val PATH_WS_STATUS = "/relay/ws/status"
    const val PATH_HTTP_REQUEST = "/relay/http/request"
    const val PATH_HTTP_RESPONSE = "/relay/http/response"
    const val PATH_AUDIO_UPLOAD = "/relay/audio/upload"
    const val PATH_AUDIO_UPLOAD_RESPONSE = "/relay/audio/upload/response"
    const val PATH_AUDIO_DOWNLOAD = "/relay/audio/download"
    const val PATH_AUDIO_DOWNLOAD_DATA = "/relay/audio/download/data"

    private var context: Context? = null
    private var phoneNodeId: String? = null

    // Pending HTTP request/response tracking
    private val pendingHttpRequests = ConcurrentHashMap<String, CompletableDeferred<JSONObject>>()
    // Pending audio upload responses
    private val pendingAudioUploads = ConcurrentHashMap<String, CompletableDeferred<String>>()
    // Pending audio download data
    private val pendingAudioDownloads = ConcurrentHashMap<String, CompletableDeferred<ByteArray>>()

    // WebSocket message callback
    var onWebSocketMessage: ((String) -> Unit)? = null
    var onWebSocketStatus: ((String) -> Unit)? = null

    fun init(ctx: Context) {
        context = ctx.applicationContext
    }

    /**
     * Resolve the connected phone node ID. Caches result, re-resolves on failure.
     */
    private suspend fun getPhoneNodeId(): String {
        phoneNodeId?.let { return it }

        val ctx = context ?: throw IllegalStateException("RelayClient not initialized")
        val nodes = Wearable.getNodeClient(ctx).connectedNodes.await()
        val node = nodes.firstOrNull()
            ?: throw IllegalStateException("No connected phone node found")

        phoneNodeId = node.id
        Log.i(TAG, "Resolved phone node: ${node.displayName} (${node.id})")
        return node.id
    }

    private fun clearPhoneNode() {
        phoneNodeId = null
    }

    // --- WebSocket relay ---

    suspend fun wsConnect() {
        try {
            val ctx = context ?: return
            val nodeId = getPhoneNodeId()
            Wearable.getMessageClient(ctx)
                .sendMessage(nodeId, PATH_WS_CONNECT, byteArrayOf())
                .await()
            Log.d(TAG, "Sent ws/connect to phone")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send ws/connect", e)
            clearPhoneNode()
            throw e
        }
    }

    suspend fun wsDisconnect() {
        try {
            val ctx = context ?: return
            val nodeId = getPhoneNodeId()
            Wearable.getMessageClient(ctx)
                .sendMessage(nodeId, PATH_WS_DISCONNECT, byteArrayOf())
                .await()
            Log.d(TAG, "Sent ws/disconnect to phone")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send ws/disconnect", e)
            clearPhoneNode()
        }
    }

    // --- HTTP relay ---

    /**
     * Send an HTTP request through the phone relay and wait for the response.
     *
     * @param method HTTP method (GET, POST, etc.)
     * @param path URL path (e.g., "/api/abort")
     * @param body Optional JSON body string
     * @param headers Optional headers as JSON object
     * @return Response as JSONObject with "status" (int), "body" (string), "success" (bool)
     */
    suspend fun httpRequest(
        method: String,
        path: String,
        body: String? = null,
        headers: Map<String, String>? = null
    ): JSONObject {
        val ctx = context ?: throw IllegalStateException("RelayClient not initialized")
        val requestId = UUID.randomUUID().toString()

        val requestJson = JSONObject().apply {
            put("request_id", requestId)
            put("method", method)
            put("path", path)
            if (body != null) put("body", body)
            if (headers != null) {
                val headersJson = JSONObject()
                headers.forEach { (k, v) -> headersJson.put(k, v) }
                put("headers", headersJson)
            }
        }

        val deferred = CompletableDeferred<JSONObject>()
        pendingHttpRequests[requestId] = deferred

        try {
            val nodeId = getPhoneNodeId()
            Wearable.getMessageClient(ctx)
                .sendMessage(nodeId, PATH_HTTP_REQUEST, requestJson.toString().toByteArray())
                .await()
            Log.d(TAG, "Sent HTTP request: $method $path (id=$requestId)")

            return withTimeout(HTTP_TIMEOUT_MS) {
                deferred.await()
            }
        } catch (e: Exception) {
            pendingHttpRequests.remove(requestId)
            if (e is TimeoutCancellationException) {
                Log.e(TAG, "HTTP request timed out: $method $path")
                clearPhoneNode()
            } else {
                Log.e(TAG, "HTTP request failed: $method $path", e)
                clearPhoneNode()
            }
            throw e
        }
    }

    // --- Audio upload via ChannelClient ---

    /**
     * Upload audio file bytes to the phone for transcription.
     *
     * @param audioBytes The audio file content
     * @param responseMode "audio" or "text"
     * @return Server response body as string
     */
    suspend fun uploadAudio(audioBytes: ByteArray, responseMode: String): String {
        val ctx = context ?: throw IllegalStateException("RelayClient not initialized")
        val requestId = UUID.randomUUID().toString()

        val deferred = CompletableDeferred<String>()
        pendingAudioUploads[requestId] = deferred

        try {
            val nodeId = getPhoneNodeId()

            // Send metadata first via MessageClient
            val meta = JSONObject().apply {
                put("request_id", requestId)
                put("response_mode", responseMode)
                put("size", audioBytes.size)
            }
            Wearable.getMessageClient(ctx)
                .sendMessage(nodeId, PATH_AUDIO_UPLOAD, meta.toString().toByteArray())
                .await()

            // Send audio data via ChannelClient
            val channel = Wearable.getChannelClient(ctx)
                .openChannel(nodeId, "/relay/audio/upload/data/$requestId")
                .await()

            val os = Wearable.getChannelClient(ctx).getOutputStream(channel).await()
            withContext(Dispatchers.IO) {
                os.write(audioBytes)
                os.flush()
                os.close()
            }
            Wearable.getChannelClient(ctx).close(channel).await()

            Log.d(TAG, "Sent audio upload: ${audioBytes.size} bytes (id=$requestId)")

            return withTimeout(AUDIO_TIMEOUT_MS) {
                deferred.await()
            }
        } catch (e: Exception) {
            pendingAudioUploads.remove(requestId)
            Log.e(TAG, "Audio upload failed", e)
            clearPhoneNode()
            throw e
        }
    }

    // --- Audio download via ChannelClient ---

    /**
     * Download audio from server via phone relay.
     *
     * @param audioPath The audio URL path (e.g., "/api/audio/xyz")
     * @return Audio bytes
     */
    suspend fun downloadAudio(audioPath: String): ByteArray {
        val ctx = context ?: throw IllegalStateException("RelayClient not initialized")
        val requestId = UUID.randomUUID().toString()

        val deferred = CompletableDeferred<ByteArray>()
        pendingAudioDownloads[requestId] = deferred

        try {
            val nodeId = getPhoneNodeId()
            val requestJson = JSONObject().apply {
                put("request_id", requestId)
                put("path", audioPath)
            }
            Wearable.getMessageClient(ctx)
                .sendMessage(nodeId, PATH_AUDIO_DOWNLOAD, requestJson.toString().toByteArray())
                .await()

            Log.d(TAG, "Sent audio download request: $audioPath (id=$requestId)")

            return withTimeout(AUDIO_TIMEOUT_MS) {
                deferred.await()
            }
        } catch (e: Exception) {
            pendingAudioDownloads.remove(requestId)
            Log.e(TAG, "Audio download failed", e)
            clearPhoneNode()
            throw e
        }
    }

    // --- Response dispatchers (called by RelayMessageService) ---

    fun onHttpResponse(data: ByteArray) {
        try {
            val json = JSONObject(String(data))
            val requestId = json.optString("request_id")
            val deferred = pendingHttpRequests.remove(requestId)
            if (deferred != null) {
                deferred.complete(json)
                Log.d(TAG, "HTTP response dispatched: $requestId")
            } else {
                Log.w(TAG, "No pending request for HTTP response: $requestId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error dispatching HTTP response", e)
        }
    }

    fun onAudioUploadResponse(data: ByteArray) {
        try {
            val json = JSONObject(String(data))
            val requestId = json.optString("request_id")
            val body = json.optString("body", "")
            val deferred = pendingAudioUploads.remove(requestId)
            if (deferred != null) {
                deferred.complete(body)
                Log.d(TAG, "Audio upload response dispatched: $requestId")
            } else {
                Log.w(TAG, "No pending request for audio upload response: $requestId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error dispatching audio upload response", e)
        }
    }

    fun onAudioDownloadData(requestId: String, data: ByteArray) {
        val deferred = pendingAudioDownloads.remove(requestId)
        if (deferred != null) {
            deferred.complete(data)
            Log.d(TAG, "Audio download data dispatched: $requestId (${data.size} bytes)")
        } else {
            Log.w(TAG, "No pending request for audio download: $requestId")
        }
    }

    fun onWsMessage(data: ByteArray) {
        val text = String(data)
        Log.d(TAG, "WS message from phone: $text")
        onWebSocketMessage?.invoke(text)
    }

    fun onWsStatus(data: ByteArray) {
        val status = String(data)
        Log.d(TAG, "WS status from phone: $status")
        onWebSocketStatus?.invoke(status)
    }
}
