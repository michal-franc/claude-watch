package com.claudewatch.companion.chat

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.claudewatch.companion.R
import com.claudewatch.companion.network.ChatMessage
import com.claudewatch.companion.network.MessageStatus

class ChatAdapter(
    private val onRetryClick: ((ChatMessage) -> Unit)? = null,
    private val serverBaseUrl: String = ""
) : ListAdapter<ChatMessage, ChatAdapter.MessageViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_CLAUDE = 1
        private const val COPIED_INDICATOR_DURATION_MS = 1500L
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).role == "user") VIEW_TYPE_USER else VIEW_TYPE_CLAUDE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_USER) {
            R.layout.item_chat_user
        } else {
            R.layout.item_chat_claude
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = getItem(position)
        holder.bind(message, serverBaseUrl)

        // Set click listener for failed messages
        if (message.status == MessageStatus.FAILED || message.status == MessageStatus.PENDING) {
            holder.itemView.setOnClickListener {
                onRetryClick?.invoke(message)
            }
        } else {
            holder.itemView.setOnClickListener(null)
            holder.itemView.isClickable = false
        }

        // Long-press to copy message content
        holder.itemView.setOnLongClickListener {
            copyMessageToClipboard(it.context, message.content)
            holder.showCopiedIndicator()
            true
        }
    }

    private fun copyMessageToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Chat message", text)
        clipboard.setPrimaryClip(clip)
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val chatImage: ImageView? = itemView.findViewById(R.id.chatImage)
        private val chatWebView: WebView? = itemView.findViewById(R.id.chatWebView)
        private val statusIndicator: TextView? = itemView.findViewById(R.id.statusIndicator)
        private val copiedIndicator: TextView? = itemView.findViewById(R.id.copiedIndicator)
        private val handler = Handler(Looper.getMainLooper())

        fun bind(message: ChatMessage, serverBaseUrl: String = "") {
            messageText.text = message.content

            // Load image or HTML content if present
            val imageUrl = message.imageUrl
            val isHtml = message.mime?.startsWith("text/html") == true

            if (!imageUrl.isNullOrEmpty() && serverBaseUrl.isNotEmpty() && isHtml) {
                val fullUrl = serverBaseUrl + imageUrl
                // HTML content: show in WebView
                chatWebView?.let { webView ->
                    webView.visibility = View.VISIBLE
                    webView.settings.javaScriptEnabled = true
                    webView.settings.useWideViewPort = true
                    webView.settings.loadWithOverviewMode = true
                    webView.settings.domStorageEnabled = true
                    val inlineHtml = """
                        <html>
                        <head>
                            <meta name="viewport" content="width=960">
                            <style>
                                * { margin: 0; padding: 0; }
                                body { background: #1a1a2e; }
                                iframe { display: block; width: calc(100% - 32px); height: 100vh;
                                         border: none; margin: 0 16px; }
                            </style>
                        </head>
                        <body><iframe src="$fullUrl"></iframe></body>
                        </html>
                    """.trimIndent()
                    webView.loadDataWithBaseURL(fullUrl, inlineHtml, "text/html", "utf-8", null)

                    // Double-tap to fullscreen
                    val detector = GestureDetector(webView.context, object : GestureDetector.SimpleOnGestureListener() {
                        override fun onDoubleTap(e: MotionEvent): Boolean {
                            showFullscreenWebView(webView.context, fullUrl)
                            return true
                        }
                    })
                    webView.setOnTouchListener { v, event ->
                        detector.onTouchEvent(event)
                        false // let WebView handle scrolling too
                    }
                }
                chatImage?.visibility = View.GONE
            } else if (chatImage != null) {
                chatWebView?.visibility = View.GONE
                if (!imageUrl.isNullOrEmpty() && serverBaseUrl.isNotEmpty()) {
                    val fullUrl = serverBaseUrl + imageUrl
                    chatImage.visibility = View.VISIBLE
                    chatImage.load(fullUrl)
                } else {
                    chatImage.visibility = View.GONE
                }
            }

            // Reset copied indicator
            copiedIndicator?.visibility = View.GONE

            // Apply visual styling based on status
            when (message.status) {
                MessageStatus.SENT -> {
                    messageText.alpha = 1.0f
                    statusIndicator?.visibility = View.GONE
                }
                MessageStatus.PENDING -> {
                    messageText.alpha = 0.5f
                    statusIndicator?.visibility = View.VISIBLE
                    statusIndicator?.text = "Sending..."
                }
                MessageStatus.FAILED -> {
                    messageText.alpha = 0.5f
                    statusIndicator?.visibility = View.VISIBLE
                    statusIndicator?.text = "Tap to retry"
                }
            }
        }

        private fun showFullscreenWebView(context: Context, url: String) {
            val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            // Wrap in an iframe with a wide viewport so content fits with margins
            val wrapperHtml = """
                <html>
                <head>
                    <meta name="viewport" content="width=960">
                    <style>
                        * { margin: 0; padding: 0; }
                        body { background: #1a1a2e; }
                        iframe { display: block; width: calc(100% - 32px); height: 100vh;
                                 border: none; margin: 0 16px; }
                    </style>
                </head>
                <body><iframe src="$url"></iframe></body>
                </html>
            """.trimIndent()
            val webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.domStorageEnabled = true
                loadDataWithBaseURL(url, wrapperHtml, "text/html", "utf-8", null)
            }
            dialog.setContentView(webView)
            dialog.window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            dialog.show()
        }

        fun showCopiedIndicator() {
            copiedIndicator?.let { indicator ->
                indicator.visibility = View.VISIBLE
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed({
                    indicator.visibility = View.GONE
                }, COPIED_INDICATOR_DURATION_MS)
            }
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}
