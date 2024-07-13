package com.example.tam_tam.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import android.Manifest
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.Payload
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.charset.StandardCharsets

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var btnPhoto: Button
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var messageList: MutableList<Message>
    private lateinit var recipientPhoneNumber: String
    private lateinit var senderPhoneNumber: String
    private lateinit var currentDeviceNumber: String

    companion object {
        private const val REQUEST_CODE = 100
        private const val PICK_IMAGE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        UserDatabaseHelper.init(this)

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnPhoto = findViewById(R.id.btnPhoto)

        recipientPhoneNumber = intent.getStringExtra("contactPhoneNumber") ?: ""
        senderPhoneNumber = UserDatabaseHelper.getAllUsers().firstOrNull()?.phoneNumber ?: ""
        currentDeviceNumber = "YOUR_DEVICE_NUMBER" // Replace this with the actual method to get the device number

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

        btnPhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openGallery()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE
                )
            }
        }

        // Load previous messages from local database
        loadMessages()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            val selectedImage: Uri? = data.data
            if (selectedImage != null) {
                sendPhoto(selectedImage)
            }
        }
    }

    /*private fun sendPhoto(uri: Uri) {
        try {
            val pfd = contentResolver.openFileDescriptor(uri, "r")
            val filePayload = pfd?.let { Payload.fromFile(it) }
            val filenameMessage = filePayload?.id.toString() + ":" + uri.lastPathSegment
            val filenameBytesPayload = Payload.fromBytes(filenameMessage.toByteArray(StandardCharsets.UTF_8))

            val endpoint = NearbyService.discoveredEndpoints.find { it.name == currentDeviceNumber }

            if (endpoint != null) {
                Nearby.getConnectionsClient(this).sendPayload(endpoint.id, filenameBytesPayload)

                if (filePayload != null) {
                    Nearby.getConnectionsClient(this).sendPayload(endpoint.id, filePayload)
                }
            }

            val message = Message(
                sender = senderPhoneNumber,
                recipient = recipientPhoneNumber,
                content = "",
                timestamp = System.currentTimeMillis(),
                relays = mutableListOf(),
                imageUri = uri // Add this line
            )
            sendMessage(message)
        } catch (e: FileNotFoundException) {
            Log.e("ChatActivity", "File not found", e)
        }
    }*/

    private fun loadMessages() {
        CoroutineScope(Dispatchers.Main).launch {
            val messages = DatabaseHelper.getMessagesForConversation(senderPhoneNumber, recipientPhoneNumber, currentDeviceNumber)
            messageList.clear()
            messageList.addAll(messages)
            chatAdapter.notifyDataSetChanged()

            Log.d("messages", messageList.toString())
        }
    }

    private fun sendPhoto(uri: Uri) {
        NearbyService.sendImageToRecipient(uri, recipientPhoneNumber)
        val message = Message(
            sender = senderPhoneNumber,
            recipient = recipientPhoneNumber,
            content = "",
            timestamp = System.currentTimeMillis(),
            relays = mutableListOf(),
            imageUri = uri
        )
        messageList.add(message)

        CoroutineScope(Dispatchers.Main).launch {
            // Save the message to the local database
            DatabaseHelper.saveMessage(message, message.recipient)
        }

        chatAdapter.notifyItemInserted(messageList.size - 1)
        recyclerView.scrollToPosition(messageList.size - 1)
    }

    private fun sendMessage(message: Message) {
        // Send the message via NearbyService
        NearbyService.sendMessageToRecipient(message)
        messageList.add(message)

        CoroutineScope(Dispatchers.Main).launch {
            // Save the message to the local database
            DatabaseHelper.saveMessage(message, message.recipient)
        }

        chatAdapter.notifyItemInserted(messageList.size - 1)
        recyclerView.scrollToPosition(messageList.size - 1)
    }
}
