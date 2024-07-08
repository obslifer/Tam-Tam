package com.example.tam_tam.routage

import android.util.Log
import com.google.gson.Gson

data class Message(val content: String, val recipientId: String, val intensity: Int) {
    companion object {
        // Fonction companion object pour désérialiser depuis JSON
        fun fromJson(json: String): Message? { // <-- Retirez "Companion" ici
            val gson = Gson()
            return try {
                gson.fromJson(json, Message::class.java)
            } catch (e: Exception) {
                Log.e("Message", "Erreur lors de la désérialisation du message", e)
                null
            }
        }
    }
}
