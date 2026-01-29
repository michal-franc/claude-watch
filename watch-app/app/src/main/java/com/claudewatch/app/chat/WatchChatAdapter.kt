package com.claudewatch.app.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.claudewatch.app.R
import com.claudewatch.app.network.ChatMessage

class WatchChatAdapter : ListAdapter<ChatMessage, WatchChatAdapter.ChatViewHolder>(DIFF_CALLBACK) {

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_CLAUDE = 1

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).role == "user") VIEW_TYPE_USER else VIEW_TYPE_CLAUDE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_USER)
            R.layout.item_watch_chat_user
        else
            R.layout.item_watch_chat_claude

        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)

        fun bind(message: ChatMessage) {
            messageText.text = message.content
        }
    }
}
