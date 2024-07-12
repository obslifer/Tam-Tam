package com.example.tam_tam

import android.util.Log
import com.example.tam_tam.models.Message

object MessageHandler {
    private const val TAG = "MessageHandler"

    fun handleReceivedMessage(message: Message, currentUserPhoneNumber: String, currentDeviceNumber: String) {
        if (message.recipient == currentUserPhoneNumber) {
            Log.d(TAG, "Message received: ${message.content}")
            // Display or handle the message as needed
            displayMessage(message)
        } else {
            if (message.relays.size < 10) {
                message.relays.add(currentUserPhoneNumber)
                NearbyService.sendMessageToAllEndpoints(message, currentDeviceNumber)
            } else {
                Log.d(TAG, "Message relay limit reached")
            }
        }
    }

    fun sendMessageToRecipient(recipientPhoneNumber: String, content: String, currentUserPhoneNumber: String, currentDeviceNumber: String) {
        val message = Message(
            sender = currentUserPhoneNumber,
            recipient = recipientPhoneNumber,
            content = content,
            timestamp = System.currentTimeMillis(),
            relays = mutableListOf()
        )
        NearbyService.sendMessageToEndpoint(recipientPhoneNumber, message, currentDeviceNumber)
    }

    fun displayMessage(message: Message) {
        // Code to display the message to the user (e.g., update UI, show notification, etc.)
        // This method should be implemented according to your app's requirements
        Log.d(TAG, "Displaying message from ${message.sender}: ${message.content}")
    }
}
