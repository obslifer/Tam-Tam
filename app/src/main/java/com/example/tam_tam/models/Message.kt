package com.example.tam_tam.models

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class Message(
    @SerializedName("sender")
    val sender: String,

    @SerializedName("recipient")
    var recipient: String,

    @SerializedName("content")
    val content: String,

    @SerializedName("timestamp")
    val timestamp: Long,

    @SerializedName("relays")
    val relays: MutableList<String> = mutableListOf(),

    @SerializedName("imageUri")
    val imageUri: String? = null
) {
    companion object {
        private val gson = Gson()

        fun fromJson(json: String): Message {
            return gson.fromJson(json, Message::class.java)
        }
    }

    fun toJson(): String {
        return gson.toJson(this)
    }
}