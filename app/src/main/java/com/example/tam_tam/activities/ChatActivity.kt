package com.example.tam_tam.activities

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tam_tam.DatabaseHelper
import com.example.tam_tam.R
import com.example.tam_tam.adapters.ChatAdapter
import com.example.tam_tam.models.Message
import com.example.tam_tam.UserDatabaseHelper
import com.example.tam_tam.NearbyService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var messageList: MutableList<Message>
    private lateinit var recipientPhoneNumber: String
    private lateinit var senderPhoneNumber: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        UserDatabaseHelper.init(this)

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        recipientPhoneNumber = intent.getStringExtra("contactPhoneNumber") ?: ""
        senderPhoneNumber = UserDatabaseHelper.getAllUsers().firstOrNull()?.phoneNumber ?: ""

        // Set up RecyclerView
        messageList = mutableListOf()
        chatAdapter = ChatAdapter(messageList, senderPhoneNumber)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = chatAdapter

        btnSend.setOnClickListener {
            val messageContent = etMessage.text.toString()
            if (messageContent.isNotEmpty()) {
                val message = Message(
                    sender = senderPhoneNumber,
                    recipient = recipientPhoneNumber,
                    content = messageContent,
                    timestamp = System.currentTimeMillis(),
                    relays = mutableListOf()
                )
                sendMessage(message)
                etMessage.text.clear()
            }
        }

        // Load previous messages from local database
        loadMessages()
    }

    private fun loadMessages() {
        CoroutineScope(Dispatchers.Main).launch {
            val messages = DatabaseHelper.getMessagesForConversation(senderPhoneNumber, recipientPhoneNumber)
            messageList.clear()
            messageList.addAll(messages)
            chatAdapter.notifyDataSetChanged()
        }
    }

    private fun sendMessage(message: Message) {
        // This is just a placeholder. Implement the actual sending via NearbyService.
        NearbyService.sendMessageToRecipient(message)
        messageList.add(message)

        CoroutineScope(Dispatchers.Main).launch {
            val messages =
                DatabaseHelper.getMessagesForConversation(senderPhoneNumber, recipientPhoneNumber)
            Log.d("message", messages.toString())
        }

        chatAdapter.notifyItemInserted(messageList.size - 1)
        recyclerView.scrollToPosition(messageList.size - 1)

        // Save the message to the local database
        CoroutineScope(Dispatchers.Main).launch {
            DatabaseHelper.saveMessage(message)
        }
    }
}
