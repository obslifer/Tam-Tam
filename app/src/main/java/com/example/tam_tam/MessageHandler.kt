package com.example.tam_tam

import android.util.Log
import com.example.tam_tam.models.Message

object MessageHandler {
    private const val TAG = "MessageHandler"

    // Gère un message reçu
    fun handleReceivedMessage(message: Message, currentUserPhoneNumber: String, currentDeviceNumber: String) {
        if (message.recipient == currentUserPhoneNumber) {
            // Si le message est destiné à l'utilisateur actuel, l'afficher ou le traiter
            Log.d(TAG, "Message received: ${message.content}")
            displayMessage(message)
        } else {
            // Sinon, si le message n'a pas atteint la limite de relais, ajouter l'utilisateur actuel aux relais et envoyer le message à tous les endpoints
            if (message.relays.size < 10) {
                message.relays.add(currentUserPhoneNumber)
                NearbyService.sendMessageToAllEndpoints(message, currentDeviceNumber)
            } else {
                Log.d(TAG, "Message relay limit reached")
            }
        }
    }

    // Envoie un message au destinataire
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

    // Affiche le message à l'utilisateur (cette méthode doit être implémentée selon les besoins de votre application)
    fun displayMessage(message: Message) {
        Log.d(TAG, "Displaying message from ${message.sender}: ${message.content}")
        // Code pour afficher le message à l'utilisateur (par exemple, mettre à jour l'interface utilisateur, afficher une notification, etc.)
    }
}
