package com.claudewatch.companion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.claudewatch.companion.chat.ChatAdapter
import com.claudewatch.companion.creature.CreatureState
import com.claudewatch.companion.databinding.ActivityMainBinding
import com.claudewatch.companion.kiosk.KioskManager
import com.claudewatch.companion.network.ConnectionStatus
import com.claudewatch.companion.network.WebSocketClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var kioskManager: KioskManager
    private var webSocketClient: WebSocketClient? = null
    private var idleTimeout: Long = 0

    // Voice recording
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        kioskManager = KioskManager(this)

        setupChatRecyclerView()
        setupClickListeners()
        setupInputHandlers()
        connectWebSocket()

        // Enter kiosk mode if enabled in settings
        if (SettingsActivity.isKioskModeEnabled(this)) {
            kioskManager.enterKioskMode {
                binding.header.visibility = View.VISIBLE
                binding.inputBar.visibility = View.VISIBLE
            }
            binding.header.visibility = View.GONE
        }
    }

    private fun setupChatRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupClickListeners() {
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setupInputHandlers() {
        // Send button
        binding.sendButton.setOnClickListener {
            sendTextMessage()
        }

        // Enter key on keyboard
        binding.messageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendTextMessage()
                true
            } else false
        }

        // Voice button - hold to record
        binding.voiceButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (checkAudioPermission()) {
                        startRecording()
                    } else {
                        requestAudioPermission()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isRecording) {
                        stopRecordingAndSend()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun sendTextMessage() {
        val text = binding.messageInput.text.toString().trim()
        if (text.isEmpty()) return

        binding.messageInput.setText("")
        binding.sendButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    sendTextToServer(text)
                }
                if (!result) {
                    Toast.makeText(this@MainActivity, "Failed to send message", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.sendButton.isEnabled = true
            }
        }
    }

    private fun sendTextToServer(text: String): Boolean {
        val serverAddress = SettingsActivity.getServerAddress(this)
        val baseUrl = "http://${serverAddress.replace(":5567", ":5566")}"
        val url = "$baseUrl/api/message"

        val json = JSONObject().apply {
            put("text", text)
        }

        val request = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error sending text", e)
            false
        }
    }

    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun startRecording() {
        try {
            audioFile = File.createTempFile("recording_", ".m4a", cacheDir)

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            binding.voiceButton.setBackgroundResource(R.drawable.bg_circle_button_recording)
            Toast.makeText(this, "Recording...", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecordingAndSend() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        }
        mediaRecorder = null
        isRecording = false
        binding.voiceButton.setBackgroundResource(R.drawable.bg_circle_button)

        audioFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                sendAudioToServer(file)
            }
        }
    }

    private fun sendAudioToServer(file: File) {
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val serverAddress = SettingsActivity.getServerAddress(this@MainActivity)
                    val baseUrl = "http://${serverAddress.replace(":5567", ":5566")}"
                    val url = "$baseUrl/transcribe"

                    val request = Request.Builder()
                        .url(url)
                        .post(file.asRequestBody("audio/mp4".toMediaType()))
                        .build()

                    val response = httpClient.newCall(request).execute()
                    response.isSuccessful
                }

                if (!result) {
                    Toast.makeText(this@MainActivity, "Failed to send audio", Toast.LENGTH_SHORT).show()
                }

                file.delete()
                audioFile = null

            } catch (e: Exception) {
                Log.e(TAG, "Error sending audio", e)
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Hold mic button to record", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun connectWebSocket() {
        val serverAddress = SettingsActivity.getServerAddress(this)
        webSocketClient = WebSocketClient(serverAddress)

        lifecycleScope.launch {
            webSocketClient?.connectionStatus?.collectLatest { status ->
                updateConnectionUI(status)
                updateCreatureForConnection(status)
            }
        }

        lifecycleScope.launch {
            webSocketClient?.claudeState?.collectLatest { state ->
                updateCreatureState(state.status)
                resetIdleTimeout()
            }
        }

        lifecycleScope.launch {
            webSocketClient?.chatMessages?.collectLatest { messages ->
                chatAdapter.submitList(messages.toList())
                if (messages.isNotEmpty()) {
                    binding.chatRecyclerView.smoothScrollToPosition(messages.size - 1)
                }
            }
        }

        webSocketClient?.connect()
    }

    private fun updateConnectionUI(status: ConnectionStatus) {
        val (color, text) = when (status) {
            ConnectionStatus.CONNECTED -> Pair(R.color.status_connected, R.string.connected)
            ConnectionStatus.CONNECTING -> Pair(R.color.status_connecting, R.string.connecting)
            ConnectionStatus.DISCONNECTED -> Pair(R.color.status_disconnected, R.string.disconnected)
        }

        binding.connectionStatus.setText(text)
        (binding.connectionIndicator.background as? GradientDrawable)?.setColor(
            ContextCompat.getColor(this, color)
        )
    }

    private fun updateCreatureForConnection(status: ConnectionStatus) {
        if (status == ConnectionStatus.DISCONNECTED) {
            binding.creatureView.setState(CreatureState.OFFLINE)
        }
    }

    private fun updateCreatureState(status: String) {
        val creatureState = when (status) {
            "listening" -> CreatureState.LISTENING
            "thinking" -> CreatureState.THINKING
            "speaking" -> CreatureState.SPEAKING
            else -> CreatureState.IDLE
        }
        binding.creatureView.setState(creatureState)
    }

    private fun resetIdleTimeout() {
        idleTimeout = System.currentTimeMillis() + 120_000
        binding.creatureView.postDelayed({
            checkForSleep()
        }, 120_000)
    }

    private fun checkForSleep() {
        if (System.currentTimeMillis() >= idleTimeout) {
            val currentStatus = webSocketClient?.claudeState?.value?.status
            if (currentStatus == "idle") {
                binding.creatureView.setState(CreatureState.SLEEPING)
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val state = webSocketClient?.claudeState?.value?.status
            if (state == "idle") {
                binding.creatureView.setState(CreatureState.IDLE)
                resetIdleTimeout()
            }
            kioskManager.handleTap(ev.x, ev.y)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onResume() {
        super.onResume()
        webSocketClient?.let {
            if (it.connectionStatus.value == ConnectionStatus.DISCONNECTED) {
                it.connect()
            }
        } ?: connectWebSocket()

        if (SettingsActivity.isKioskModeEnabled(this) && !kioskManager.isInKioskMode()) {
            kioskManager.enterKioskMode {
                binding.header.visibility = View.VISIBLE
                binding.inputBar.visibility = View.VISIBLE
            }
            binding.header.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
        webSocketClient?.destroy()
        httpClient.dispatcher.executorService.shutdown()
    }
}
